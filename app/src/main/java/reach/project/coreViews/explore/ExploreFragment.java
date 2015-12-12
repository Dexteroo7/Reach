package reach.project.coreViews.explore;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.gms.analytics.HitBuilders;
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
import reach.project.core.ReachActivity;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.usageTracking.PostParams;
import reach.project.usageTracking.SongMetadata;
import reach.project.usageTracking.UsageTracker;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ExploreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ExploreFragment extends Fragment implements ExploreAdapter.Explore,
        ExploreBuffer.Exploration<JSONObject>, HandOverMessage<Integer> {

    private static final Random random = new Random();

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
            loading.put("type", ExploreTypes.LOADING.name());
            loading.put("id", random.nextLong());
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
                            ReachFriendsHelper.COLUMN_ID
                    },
                    ReachFriendsHelper.COLUMN_STATUS + " = ?",
                    new String[]{
                            ReachFriendsHelper.ONLINE_REQUEST_GRANTED + ""
                    }, null);

            if (cursor == null)
                return null;

            final JSONArray jsonArray = new JSONArray();
            final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);

            while (cursor.moveToNext()) {

                final String onlineId = cursor.getString(0);
                Log.i("Ayush", "Adding online friend id " + onlineId);
                jsonArray.put(onlineId);
            }
            cursor.close();

            final Map<String, Object> requestMap = MiscUtils.getMap(3);
            requestMap.put("userId", myServerId);
            requestMap.put("friends", jsonArray);
            requestMap.put("lastRequestTime", SharedPrefUtils.getLastRequestTime(preferences));

            return new JSONObject(requestMap);

        }).or(new JSONObject());

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

//        JSONObject exploreJson;
//        for (int i = 0; i < receivedData.length(); i++) {
//
//            exploreJson = receivedData.getJSONObject(i);
////            final int contentType = exploreJson.getInt("contentType");
////            exploreJson.getLong("userId")
//
//            Log.i("Ayush", "Got Explore response " + exploreJson.getString("displayName"));
//
//            final long userId = Long.parseLong(exploreJson.getString("userId"));
//            final Pair<String, String> pair = MiscUtils.useContextFromFragment(reference, context -> {
//
//                final Cursor cursor = context.getContentResolver().query(
//                        Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
//                        new String[]{ReachFriendsHelper.COLUMN_USER_NAME,
//                                ReachFriendsHelper.COLUMN_IMAGE_ID},
//                        ReachFriendsHelper.COLUMN_ID + " = ?",
//                        new String[]{userId + ""}, null);
//
//                if (cursor == null)
//                    return null;
//
//                try {
//
//                    if (cursor.moveToFirst())
//                        return new Pair<>(cursor.getString(0), //userName
//                                cursor.getString(1)); //userImageId
//                } finally {
//                    cursor.close();
//                }
//
//                return null;
//            }).get();
//
//            final MusicContainer musicContainer = new MusicContainer(
//                    exploreJson.getString("largeImageUrl"),
//                    pair.second,
//                    pair.first,
//                    ExploreTypes.MUSIC,
//                    random.nextLong());
//
//            musicContainer.actualName = exploreJson.getString("actualName");
//            musicContainer.displayName = exploreJson.getString("displayName");
//            musicContainer.artistName = exploreJson.getString("artistName");
//            musicContainer.albumName = exploreJson.getString("albumName");
//
//            musicContainer.length = exploreJson.getLong("size");
//            musicContainer.songId = exploreJson.getLong("contentId");
//            musicContainer.duration = exploreJson.getLong("duration");
//            musicContainer.senderId = userId;
//
//            containers.add(musicContainer);
//        }

        count += containers.size();

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
        final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);

        //extract meta info to process current click request
        final JSONObject metaInfo = exploreJSON.getJSONObject("metaInfo");

        //get user name and imageId
        final long senderId = Long.parseLong(metaInfo.getString("userId"));
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
        reachDatabase.setSongId(metaInfo.getLong("contentId"));
        reachDatabase.setReceiverId(myServerId);
        reachDatabase.setSenderId(senderId);

        reachDatabase.setOperationKind((short) 0);
        reachDatabase.setPath("hello_world");
        reachDatabase.setSenderName(userName);
        reachDatabase.setOnlineStatus(ReachFriendsHelper.ONLINE_REQUEST_GRANTED + "");

        reachDatabase.setArtistName(metaInfo.getString("artistName"));
        reachDatabase.setIsLiked(false);
        reachDatabase.setDisplayName(metaInfo.getString("displayName"));
        reachDatabase.setActualName(metaInfo.getString("actualName"));
        reachDatabase.setLength(metaInfo.getLong("size"));
        reachDatabase.setProcessed(0);
        reachDatabase.setAdded(System.currentTimeMillis());
        reachDatabase.setUniqueId(random.nextInt(Integer.MAX_VALUE));

        reachDatabase.setDuration(metaInfo.getLong("duration"));
        reachDatabase.setLogicalClock((short) 0);
        reachDatabase.setStatus(ReachDatabase.NOT_WORKING);

        reachDatabase.setLastActive(0);
        reachDatabase.setReference(0);

        reachDatabase.setAlbumName(metaInfo.getString("albumName"));
        reachDatabase.setGenre("hello_world");

        reachDatabase.setVisibility((short) 1);

        /**
         * DISPLAY_NAME, ACTUAL_NAME, SIZE & DURATION all can not be same, effectively its a hash
         */

        //this cursor can be used to play if entry exists
        cursor = contentResolver.query(
                ReachDatabaseProvider.CONTENT_URI,
                new String[]{

                        ReachDatabaseHelper.COLUMN_ID, //0
                        ReachDatabaseHelper.COLUMN_PROCESSED, //1
                        ReachDatabaseHelper.COLUMN_PATH, //2

                        ReachDatabaseHelper.COLUMN_IS_LIKED, //3
                        ReachDatabaseHelper.COLUMN_SENDER_ID, //4
                        ReachDatabaseHelper.COLUMN_RECEIVER_ID, //5
                        ReachDatabaseHelper.COLUMN_SIZE, //6
                        ReachDatabaseHelper.COLUMN_DATE_ADDED //7
                },

                ReachDatabaseHelper.COLUMN_DISPLAY_NAME + " = ? and " +
                        ReachDatabaseHelper.COLUMN_ACTUAL_NAME + " = ? and " +
                        ReachDatabaseHelper.COLUMN_SIZE + " = ? and " +
                        ReachDatabaseHelper.COLUMN_DURATION + " = ?",
                new String[]{reachDatabase.getDisplayName(), reachDatabase.getActualName(),
                        reachDatabase.getLength() + "", reachDatabase.getDuration() + ""},
                null);

        if (cursor != null) {

            if (cursor.moveToFirst()) {

                //if not multiple addition, play the song
                final String likeString = cursor.getString(3);
                final boolean liked = !TextUtils.isEmpty(likeString) && likeString.equals("1");

                final MusicData musicData = new MusicData(
                        cursor.getLong(0), //id
                        reachDatabase.getLength(),
                        reachDatabase.getSenderId(),
                        cursor.getLong(1), //processed
                        cursor.getLong(7), //date added
                        cursor.getString(2), //path of song
                        reachDatabase.getDisplayName(),
                        reachDatabase.getArtistName(),
                        reachDatabase.getAlbumName(),
                        liked,
                        reachDatabase.getDuration(),
                        MusicData.DOWNLOADED); //type downloaded
                MiscUtils.playSong(musicData, activity);
                //in both cases close and continue
                cursor.close();
                return;
            }
            cursor.close();
        }

        final String clientName = SharedPrefUtils.getUserName(sharedPreferences);

        //new song
        //We call bulk starter always
        final Uri uri = contentResolver.insert(ReachDatabaseProvider.CONTENT_URI,
                ReachDatabaseHelper.contentValuesCreator(reachDatabase));
        if (uri == null) {

            ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                    .setCategory("Add song failed")
                    .setAction("User Name - " + clientName)
                    .setLabel("Song - " + reachDatabase.getDisplayName() + ", From - " + reachDatabase.getSenderId())
                    .setValue(1)
                    .build());
            return;
        }

        final String[] splitter = uri.toString().split("/");
        if (splitter.length == 0)
            return;
        reachDatabase.setId(Long.parseLong(splitter[splitter.length - 1].trim()));
        //start this operation
        StaticData.temporaryFix.execute(MiscUtils.startDownloadOperation(
                activity,
                reachDatabase,
                reachDatabase.getReceiverId(), //myID
                reachDatabase.getSenderId(),   //the uploaded
                reachDatabase.getId()));

        ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                .setCategory("Transaction - Add SongBrainz")
                .setAction("User Name - " + clientName)
                .setLabel("SongBrainz - " + reachDatabase.getDisplayName() + ", From - " + reachDatabase.getSenderId())
                .setValue(1)
                .build());

        //usage tracking
        final Map<PostParams, String> simpleParams = MiscUtils.getMap(6);
        simpleParams.put(PostParams.USER_ID, reachDatabase.getReceiverId() + "");
        simpleParams.put(PostParams.DEVICE_ID, MiscUtils.getDeviceId(activity));
        simpleParams.put(PostParams.OS, MiscUtils.getOsName());
        simpleParams.put(PostParams.OS_VERSION, Build.VERSION.SDK_INT + "");
        try {
            simpleParams.put(PostParams.APP_VERSION,
                    activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        simpleParams.put(PostParams.SCREEN_NAME, "unknown");

        final Map<SongMetadata, String> complexParams = MiscUtils.getMap(5);
        complexParams.put(SongMetadata.SONG_ID, reachDatabase.getSongId() + "");
        complexParams.put(SongMetadata.ARTIST, reachDatabase.getArtistName());
        complexParams.put(SongMetadata.TITLE, reachDatabase.getDisplayName());
        complexParams.put(SongMetadata.DURATION, reachDatabase.getDuration() + "");
        complexParams.put(SongMetadata.SIZE, reachDatabase.getLength() + "");

        try {
            UsageTracker.trackSong(simpleParams, complexParams, UsageTracker.DOWNLOAD_SONG);
        } catch (JSONException ignored) {
        }

        if (rootView != null)
            Snackbar.make(rootView, "\"Song added, click to view\"", Snackbar.LENGTH_LONG)
                    .setAction("VIEW", v -> {

                        ReachActivity.openDownloading();
                    })
                    .show();

    }
}