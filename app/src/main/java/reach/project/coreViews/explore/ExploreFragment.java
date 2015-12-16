package reach.project.coreViews.explore;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ExploreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ExploreFragment extends Fragment implements ExploreAdapter.Explore,
        ExploreBuffer.Exploration<ExploreContainer>, HandOverMessage<Long> {

    private static final Random random = new Random();
    private static WeakReference<ExploreFragment> reference;
    private static long userId = 0;

    public static ExploreFragment newInstance(long userId) {

        final Bundle args;
        ExploreFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new ExploreFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing ExploreFragment object :)");
            args = fragment.getArguments();
        }
        args.putLong("userId", userId);
        return fragment;
    }

    private final ExploreBuffer<ExploreContainer> buffer = ExploreBuffer.getInstance(this);
    private final ExploreAdapter exploreAdapter = new ExploreAdapter(this, this);
    private View rootView = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        userId = getArguments().getLong("userId");
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_explore, container, false);
        final Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.exploreToolbar);
        toolbar.inflateMenu(R.menu.explore_menu);
        final LinearLayout exploreToolbarText = (LinearLayout) toolbar.findViewById(R.id.exploreToolbarText);
        final PopupMenu popupMenu = new PopupMenu(getActivity(), exploreToolbarText);

        popupMenu.inflate(R.menu.explore_popup_menu);
        exploreToolbarText.setOnClickListener(v -> popupMenu.show());
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.explore_menu_1:
                    if (item.isChecked())
                        item.setChecked(false);
                    else
                        item.setChecked(true);
                    return true;
                case R.id.explore_menu_2:
                    if (item.isChecked())
                        item.setChecked(false);
                    else
                        item.setChecked(true);
                    return true;
                default:
                    return true;
            }
        });

        final ViewPager explorePager = (ViewPager) rootView.findViewById(R.id.explorer);
        explorePager.setAdapter(exploreAdapter);
        explorePager.setOffscreenPageLimit(2);
        explorePager.setPageMargin(-1 * (MiscUtils.dpToPx(40)));
        explorePager.setPageTransformer(true, (view, position) -> {

            if (position <= 1) {
                // Modify the default slide transition to shrink the page as well
                final float scaleFactor = Math.max(0.85f, 1 - Math.abs(position));
                final float vertMargin = view.getHeight() * (1 - scaleFactor) / 2;
                final float horzMargin = view.getWidth() * (1 - scaleFactor) / 2;
                if (position < 0)
                    view.setTranslationX(horzMargin - vertMargin / 2);
                else
                    view.setTranslationX(-horzMargin + vertMargin / 2);

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
            }
        });

        return rootView;
    }

    /*@Override
    public void onDestroyView() {
        super.onDestroyView();
        buffer.close();
        exploreAdapter = null;
    }*/

    @Override
    public synchronized Callable<Collection<ExploreContainer>> fetchNextBatch() {
//        Toast.makeText(activity, "Server Fetching next batch of 10", Toast.LENGTH_SHORT).show();
        return fetchNextBatch;
    }

    @Override
    public ExploreContainer getContainerForIndex(int index) {

        //return data
        return buffer.getViewItem(index);
    }

    @Override
    public boolean isDoneForDay(ExploreContainer container) {
        return container.types.equals(ExploreTypes.DONE_FOR_TODAY);
    }

    @Override
    public boolean isLoading(ExploreContainer container) {
        return container.types.equals(ExploreTypes.LOADING);
    }

    @Override
    public ExploreContainer getLoadingResponse() {
        return new ExploreContainer(ExploreTypes.LOADING, new Random().nextLong());
    }

    @Override
    public int getCount() {
        return buffer.currentBufferSize();
    }

    @Override
    public synchronized void notifyDataAvailable() {

        //This is UI thread !
        exploreAdapter.notifyDataSetChanged();
    }

    private static short count = 0;
    private static final Callable<Collection<ExploreContainer>> fetchNextBatch = () -> {

        if (count > 8)
            return Collections.singletonList(new ExploreContainer(ExploreTypes.DONE_FOR_TODAY, random.nextInt()));

        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("userId", userId);
        final JSONArray jsonArray = new JSONArray();

        //retrieve list of online friends
        MiscUtils.useContextFromFragment(reference, context -> {

            final Cursor cursor = context.getContentResolver().query(
                    ReachFriendsProvider.CONTENT_URI,
                    new String[]{
                            ReachFriendsHelper.COLUMN_ID
                    },
                    ReachFriendsHelper.COLUMN_STATUS + " = ?",
                    new String[]{
                            ReachFriendsHelper.ONLINE_REQUEST_GRANTED + ""
                    }, null);

            if (cursor == null)
                return;

            while (cursor.moveToNext()) {

                final JSONObject onlineFriendId = new JSONObject();
                final String onlineId = cursor.getString(0);
                Log.i("Ayush", "Adding online friend id " + onlineId);
                try {
                    onlineFriendId.put("friendId", onlineId);
                } catch (JSONException ignored) {
                }
                jsonArray.put(onlineFriendId);
            }
        });

        jsonObject.put("friends", jsonArray);

        final RequestBody body = RequestBody
                .create(MediaType.parse("application/json; charset=utf-8"), jsonObject.toString());
        final Request request = new Request.Builder()
                .url("http://52.74.175.56:8080/explore/getObjects")
                .post(body)
                .build();
        final Response response = ReachApplication.okHttpClient.newCall(request).execute();
        final JSONArray receivedData = new JSONArray(response.body().string());
        final List<ExploreContainer> containers = new ArrayList<>();

        JSONObject exploreJson;
        for (int i = 0; i < receivedData.length(); i++) {

            exploreJson = receivedData.getJSONObject(i);
//            final int contentType = exploreJson.getInt("contentType");
//            exploreJson.getLong("userId")

            Log.i("Ayush", "Got Explore response " + exploreJson.getString("displayName"));

            final long userId = Long.parseLong(exploreJson.getString("userId"));
            final Pair<String, String> pair = MiscUtils.useContextFromFragment(reference, context -> {

                final Cursor cursor = context.getContentResolver().query(
                        Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
                        new String[]{ReachFriendsHelper.COLUMN_USER_NAME,
                                ReachFriendsHelper.COLUMN_IMAGE_ID},
                        ReachFriendsHelper.COLUMN_ID + " = ?",
                        new String[]{userId + ""}, null);

                if (cursor == null)
                    return null;
                if (!cursor.moveToFirst())
                    cursor.close();
                return new Pair<>(cursor.getString(0), //userName
                        cursor.getString(1)); //userImageId

            }).get();

            final MusicContainer musicContainer = new MusicContainer(
                    exploreJson.getString("largeImageUrl"),
                    pair.second,
                    pair.first,
                    ExploreTypes.MUSIC,
                    random.nextLong());

            musicContainer.actualName = exploreJson.getString("actualName");
            musicContainer.displayName = exploreJson.getString("displayName");
            musicContainer.artistName = exploreJson.getString("artistName");
            musicContainer.albumName = exploreJson.getString("albumName");

            musicContainer.length = exploreJson.getLong("size");
            musicContainer.songId = exploreJson.getLong("contentId");
            musicContainer.duration = exploreJson.getLong("duration");
            musicContainer.senderId = userId;

            containers.add(musicContainer);
        }

        count += containers.size();

        Log.i("Ayush", "Explore has " + containers.size() + " stories");
        return containers;
    };

    @Override
    public void handOverMessage(@Nonnull Long id) {

        MusicContainer musicContainer = null;
        final int size = buffer.currentBufferSize();
        for (int index = 0; index < size; index++) {

            final ExploreContainer container = buffer.getViewItem(index);
            if (container.id == id) {
                musicContainer = (MusicContainer) container;
                break;
            }
        }

        if (musicContainer == null)
            return;

        final ReachDatabase reachDatabase = new ReachDatabase();

        reachDatabase.setId(-1);
        reachDatabase.setSongId(musicContainer.songId);
        reachDatabase.setReceiverId(userId);
        reachDatabase.setSenderId(musicContainer.senderId);

        reachDatabase.setOperationKind((short) 0);
        reachDatabase.setPath("hello_world");
        reachDatabase.setSenderName(musicContainer.userHandle);
        reachDatabase.setOnlineStatus(0 + "");

        reachDatabase.setArtistName(musicContainer.artistName);
        reachDatabase.setIsLiked(false);
        reachDatabase.setDisplayName(musicContainer.displayName);
        reachDatabase.setActualName(musicContainer.actualName);
        reachDatabase.setLength(musicContainer.length);
        reachDatabase.setProcessed(0);
        reachDatabase.setAdded(System.currentTimeMillis());
        reachDatabase.setUniqueId(random.nextInt(Integer.MAX_VALUE));

        reachDatabase.setDuration(musicContainer.duration);
        reachDatabase.setLogicalClock((short) 0);
        reachDatabase.setStatus(ReachDatabase.NOT_WORKING);

        reachDatabase.setLastActive(0);
        reachDatabase.setReference(0);

        reachDatabase.setAlbumName(musicContainer.albumName);
        reachDatabase.setGenre("hello_world");

        reachDatabase.setVisibility((short) 1);

        MiscUtils.startDownload(reachDatabase, getActivity(), rootView);
    }
}