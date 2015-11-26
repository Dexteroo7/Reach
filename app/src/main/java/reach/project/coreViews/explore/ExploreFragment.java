package reach.project.coreViews.explore;

import android.content.Context;
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
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.friends.ReachFriendsHelper;
import reach.project.friends.ReachFriendsProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.auxiliaryClasses.SuperInterface;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ExploreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ExploreFragment extends Fragment implements ExploreAdapter.Explore,
        ExploreBuffer.Exploration<ExploreContainer> {

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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (SuperInterface) context;
    }

    private final ExploreBuffer<ExploreContainer> buffer = ExploreBuffer.getInstance(this);
    private ExploreAdapter exploreAdapter;
    private SuperInterface mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        userId = getArguments().getLong("userId");
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_explore, container, false);
        final Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.exploreToolbar);
        LinearLayout exploreToolbarText = (LinearLayout) toolbar.findViewById(R.id.exploreToolbarText);
        PopupMenu popupMenu = new PopupMenu(container.getContext(), exploreToolbarText);
        popupMenu.inflate(R.menu.explore_menu);
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
        mListener.toggleSliding(false);

        final ViewPager explorePager = (ViewPager) rootView.findViewById(R.id.explorer);
        exploreAdapter = new ExploreAdapter(getActivity(), this);

        explorePager.setAdapter(exploreAdapter);
        explorePager.setOffscreenPageLimit(2);
        explorePager.setPageMargin(-1 * (MiscUtils.dpToPx(40)));
        explorePager.setPageTransformer(true, (view, position) -> {
            if (position <= 1) {
                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(0.85f, 1 - Math.abs(position));
                float vertMargin = view.getHeight() * (1 - scaleFactor) / 2;
                float horzMargin = view.getWidth() * (1 - scaleFactor) / 2;
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
//        Toast.makeText(getContext(), "Server Fetching next batch of 10", Toast.LENGTH_SHORT).show();
        return fetchNextBatch;
    }

    private static final Callable<Collection<ExploreContainer>> fetchNextBatch = () -> {

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

            long userId = Long.parseLong(exploreJson.getString("userId"));
            Pair <String, String> pair = MiscUtils.useContextFromFragment(reference, context -> {

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
                String userName = cursor.getString(0);
                String userImageId = cursor.getString(1);
                return new Pair<>(userName, userImageId);
            }).get();

            containers.add(new ExploreContainer(
                    exploreJson.getString("displayName"),
                    exploreJson.getString("artistName"),
                    exploreJson.getString("largeImageUrl"),
                    pair.second,
                    "@" + pair.first.toLowerCase().split(" ")[0],
                    3.0f,
                    ExploreTypes.MUSIC,
                    exploreJson.getLong("contentId")));
        }

        Log.i("Ayush", "Explore has " + containers.size() + " stories");
        return containers;
    };

    @Override
    public ExploreContainer getContainerForIndex(int index) {

        //return data
        return buffer.getViewItem(index);
    }

    @Override
    public boolean isDoneForDay(ExploreContainer container) {
        return container.getTypes().equals(ExploreTypes.DONE_FOR_TODAY);
    }

    @Override
    public boolean isLoading(ExploreContainer container) {
        return container.getTypes().equals(ExploreTypes.LOADING);
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
}