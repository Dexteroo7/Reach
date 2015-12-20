package reach.project.coreViews.explore;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.LongSparseArray;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.HandOverMessage;

import static reach.project.coreViews.explore.ExploreJSON.MusicMetaInfo;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ExploreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ExploreFragment extends Fragment implements ExploreAdapter.Explore,
        ExploreBuffer.Exploration<JSONObject>, HandOverMessage<Integer> {

    private static final Random random = new Random();

    static final LongSparseArray<String> userNameSparseArray = new LongSparseArray<>();

    @Nullable
    private static WeakReference<ExploreFragment> reference = null;
    private static long myServerId = 0;

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

    private final ExploreBuffer<JSONObject> buffer = ExploreBuffer.getInstance(this);
    private final ExploreAdapter exploreAdapter = new ExploreAdapter(this, this);

    @Nullable
    private View rootView = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        myServerId = getArguments().getLong("userId");
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

    public void onDestroyView() {

        super.onDestroyView();
        buffer.close();
        rootView = null;
    }

    @Override
    public synchronized Callable<Collection<JSONObject>> fetchNextBatch() {
//        Toast.makeText(activity, "Server Fetching next batch of 10", Toast.LENGTH_SHORT).show();
        return fetchNextBatch;
    }

    @Override
    public JSONObject getContainerForIndex(int index) {

        //return data
        return buffer.getViewItem(index);
    }

    @Override
    public boolean isDoneForDay(JSONObject exploreJSON) {

        try {
            return exploreJSON.getString("type").equals(ExploreTypes.DONE_FOR_TODAY.name());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean isLoading(JSONObject exploreJSON) {

        try {
            return exploreJSON.getString("type").equals(ExploreTypes.LOADING.name());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public JSONObject getLoadingResponse() {

        final JSONObject loading = new JSONObject();

        try {
            loading.put(ExploreJSON.TYPE.getName(), ExploreTypes.LOADING.name());
            loading.put(ExploreJSON.ID.getName(), random.nextLong());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return loading;
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

    private static final Callable<Collection<JSONObject>> fetchNextBatch = () -> {

//        if (count > 8)
//            return Collections.singletonList(new ExploreContainer(ExploreTypes.DONE_FOR_TODAY, random.nextInt()));

        final JSONObject jsonObject = MiscUtils.useContextFromFragment(reference, context -> {

            //retrieve list of online friends
            final Cursor cursor = context.getContentResolver().query(
                    ReachFriendsProvider.CONTENT_URI,
                    new String[]{
                            ReachFriendsHelper.COLUMN_ID,
                            ReachFriendsHelper.COLUMN_USER_NAME
                    },
                    ReachFriendsHelper.COLUMN_STATUS + " = ?",
                    new String[]{
                            ReachFriendsHelper.ONLINE_REQUEST_GRANTED + ""
                    }, null);

            if (cursor == null)
                return null;

            final JSONArray jsonArray = new JSONArray();
            jsonArray.put(StaticData.devika);
            final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);

            while (cursor.moveToNext()) {

                final long onlineId = cursor.getLong(0);
                final String userName = cursor.getString(1);
                Log.i("Ayush", "Adding online friend id " + onlineId);
                jsonArray.put(onlineId);
                userNameSparseArray.append(onlineId, userName); //save userName
            }
            cursor.close();

            final Map<String, Object> requestMap = MiscUtils.getMap(3);
            requestMap.put("userId", myServerId);
            requestMap.put("friends", jsonArray);
            requestMap.put("lastRequestTime", SharedPrefUtils.getLastRequestTime(preferences));

            return new JSONObject(requestMap);

        }).or(new JSONObject());

        Log.i("Ayush", jsonObject.toString());

        final RequestBody body = RequestBody
                .create(MediaType.parse("application/json; charset=utf-8"), jsonObject.toString());
        final Request request = new Request.Builder()
                .url("http://52.74.175.56:8080/explore/getObjects")
                .post(body)
                .build();
        final Response response = ReachApplication.okHttpClient.newCall(request).execute();
        final JSONArray receivedData = new JSONArray(response.body().string());

        final List<JSONObject> containers = new ArrayList<>();
        for (int index = 0; index < receivedData.length(); index++)
            containers.add(receivedData.getJSONObject(index));

        count += containers.size();

        if (containers.size() > 0)
            MiscUtils.useContextFromFragment(reference, context -> {
                final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
                SharedPrefUtils.storeLastRequestTime(preferences);
            });

        Log.i("Ayush", "Explore has " + containers.size() + " stories");
        return containers;
    };

    @Override
    public void handOverMessage(@Nonnull Integer position) {

        //retrieve full json
        final JSONObject exploreJson = buffer.getViewItem(position);

        if (exploreJson == null)
            return;

        final String type;
        try {
            type = exploreJson.getString("type");
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        if (type.equals(ExploreTypes.MUSIC.name())) {

            try {
                addToDownload(exploreJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else if (type.equals(ExploreTypes.APP.name())) {


        } else if (type.equals(ExploreTypes.MISC.name())) {


        }
    }

    public void addToDownload(JSONObject exploreJSON) throws JSONException {

        final Activity activity = getActivity();
        final ContentResolver contentResolver = activity.getContentResolver();

        //extract meta info to process current click request
        final JSONObject metaInfo = exploreJSON.getJSONObject(ExploreJSON.META_INFO.getName());

        //get user name and imageId
        final long senderId = MiscUtils.get(metaInfo, MusicMetaInfo.SENDER_ID);
        final String userName;

        Cursor cursor = contentResolver.query(
                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + senderId),
                new String[]{ReachFriendsHelper.COLUMN_USER_NAME},
                ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{senderId + ""}, null);

        if (cursor == null)
            return;

        try {
            if (cursor.moveToFirst())
                userName = cursor.getString(0);
            else
                return;
        } finally {
            cursor.close();
        }

        final ReachDatabase reachDatabase = new ReachDatabase();

        reachDatabase.setId(-1);
        reachDatabase.setSongId(MiscUtils.get(metaInfo, MusicMetaInfo.SONG_ID));
        reachDatabase.setReceiverId(myServerId);
        reachDatabase.setSenderId(senderId);

        reachDatabase.setOperationKind((short) 0);
        reachDatabase.setPath("hello_world");
        reachDatabase.setSenderName(userName);
        reachDatabase.setOnlineStatus(ReachFriendsHelper.ONLINE_REQUEST_GRANTED + "");

        reachDatabase.setArtistName(MiscUtils.get(metaInfo, MusicMetaInfo.ARTIST));
        reachDatabase.setIsLiked(false);
        reachDatabase.setDisplayName(MiscUtils.get(metaInfo, MusicMetaInfo.DISPLAY_NAME));
        reachDatabase.setActualName(MiscUtils.get(metaInfo, MusicMetaInfo.ACTUAL_NAME));
        reachDatabase.setLength(MiscUtils.get(metaInfo, MusicMetaInfo.SIZE));
        reachDatabase.setProcessed(0);
        reachDatabase.setAdded(System.currentTimeMillis());
        reachDatabase.setUniqueId(random.nextInt(Integer.MAX_VALUE));

        reachDatabase.setDuration(MiscUtils.get(metaInfo, MusicMetaInfo.DURATION));
        reachDatabase.setLogicalClock((short) 0);
        reachDatabase.setStatus(ReachDatabase.NOT_WORKING);

        reachDatabase.setLastActive(0);
        reachDatabase.setReference(0);

        reachDatabase.setAlbumName(MiscUtils.get(metaInfo, MusicMetaInfo.ALBUM));
        reachDatabase.setGenre("hello_world");

        reachDatabase.setVisibility((short) 1);

        MiscUtils.startDownload(reachDatabase, getActivity(), rootView);
    }
}