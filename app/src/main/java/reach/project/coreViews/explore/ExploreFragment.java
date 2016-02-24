package reach.project.coreViews.explore;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.android.gms.analytics.HitBuilders;
import com.google.api.client.http.HttpStatusCodes;
import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.joda.time.DateTime;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.music.ReachDatabase;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.ThreadLocalRandom;
import reach.project.utils.ancillaryClasses.SuperInterface;
import reach.project.utils.ancillaryClasses.UseActivityWithResult;
import reach.project.utils.ancillaryClasses.UseContext;
import reach.project.utils.viewHelpers.HandOverMessage;

import static reach.project.coreViews.explore.ExploreJSON.MiscMetaInfo;
import static reach.project.coreViews.explore.ExploreJSON.MusicMetaInfo;

public class ExploreFragment extends Fragment implements ExploreAdapter.Explore,
        ExploreBuffer.ExplorationCallbacks<JsonObject>, HandOverMessage<Integer> {

    @Nullable
    private static WeakReference<ExploreFragment> reference = null;
    private static long myServerId = 0;
    private SharedPreferences preferences;

    public ExploreFragment() {
        reference = new WeakReference<>(this);
    }

    /**
     * @param data the byte[] to transform from
     * @return collection of explore stories, take care to remove loading / done for today etc...
     */
    public static final Function<byte[], Collection<JsonObject>> BYTES_TO_JSON = new Function<byte[], Collection<JsonObject>>() {
        @javax.annotation.Nullable
        @Override
        public Collection<JsonObject> apply(@Nullable byte[] input) {

            if (input == null || input.length == 0)
                return Collections.emptyList();

            final String jsonString;
            try {
                jsonString = new String(input, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }

            final JsonArray jsonElements = new JsonParser().parse(jsonString).getAsJsonArray();
            final Iterator<JsonElement> elementIterator = jsonElements.iterator();
            final List<JsonObject> toReturn = new ArrayList<>(jsonElements.size());

            while (elementIterator.hasNext())
                toReturn.add(elementIterator.next().getAsJsonObject());

            return toReturn;
        }
    };

    /**
     * @param data the collection to transform into byte[]
     * @return byte[] explore stories, take care to remove loading / done for today etc...
     */
    public static final Function<List<JsonObject>, byte[]> JSON_TO_BYTES = new Function<List<JsonObject>, byte[]>() {
        @javax.annotation.Nullable
        @Override
        public byte[] apply(@Nullable List<JsonObject> input) {

            if (input == null || input.isEmpty())
                return new byte[0];

            final JsonArray array = new JsonArray();
            for (JsonObject jsonObject : input)
                array.add(jsonObject);

            final String easyRepresentation = array.toString();
            try {
                return easyRepresentation.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return new byte[0];
            }
        }
    };

    private static final CacheLoader<Long, Pair<String, String>> PAIR_CACHE_LOADER = new CacheLoader<Long, Pair<String, String>>() {
        @Override
        public Pair<String, String> load(@NonNull Long key) {

            return MiscUtils.useContextFromFragment(reference, context -> {

                final Cursor cursor = context.getContentResolver().query(
                        Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + key),
                        new String[]{
                                ReachFriendsHelper.COLUMN_USER_NAME,
                                ReachFriendsHelper.COLUMN_IMAGE_ID
                        },
                        ReachFriendsHelper.COLUMN_ID + " = ?",
                        new String[]{key + ""}, null);

                final Pair<String, String> toReturn;

                if (cursor == null)
                    toReturn = new Pair<>("", "");
                else if (!cursor.moveToFirst()) {
                    cursor.close();
                    toReturn = new Pair<>("", "");
                } else
                    toReturn = new Pair<>(
                            cursor.getString(0),
                            cursor.getString(1));

                return toReturn;
            }).or(new Pair<>("", ""));
        }
    };
    static final LoadingCache<Long, Pair<String, String>> USER_INFO_CACHE = CacheBuilder.newBuilder()
            .initialCapacity(10)
            .build(PAIR_CACHE_LOADER);

    static final ResizeOptions FULL_IMAGE_SIZE = new ResizeOptions(450, 450);
    static final ResizeOptions SMALL_IMAGE_SIZE = new ResizeOptions(200, 200);

    private static final ViewPager.PageTransformer PAGE_TRANSFORMER = (page, position) -> {

        if (position <= 1) {

            // Modify the default slide transition to shrink the page as well
            final float scaleFactor = Math.max(0.85f, 1 - Math.abs(position));
            final float vertMargin = page.getHeight() * (1 - scaleFactor) / 2;
            final float horzMargin = page.getWidth() * (1 - scaleFactor) / 2;
            if (position < 0)
                page.setTranslationX(horzMargin - vertMargin / 2);
            else
                page.setTranslationX(-horzMargin + vertMargin / 2);

            // Scale the page down (between MIN_SCALE and 1)
            page.setScaleX(scaleFactor);
            page.setScaleY(scaleFactor);
        }
    };

    private static final Callable<Collection<JsonObject>> FETCH_NEXT_BATCH = () -> {

        final boolean onlineStatus = MiscUtils.useContextFromFragment(reference, (UseActivityWithResult<Activity, Boolean>) MiscUtils::isOnline).or(false);

        if (!onlineStatus) {

            MiscUtils.runOnUiThreadFragment(reference, (UseContext) context -> {
                Toast.makeText(context, "Could not connect to internet", Toast.LENGTH_SHORT).show();
            });
            return Collections.emptyList();
        }

        final JsonObject jsonObject = MiscUtils.useContextFromFragment(reference, context -> {

            //retrieve list of online friends
            final Cursor cursor = context.getContentResolver().query(
                    ReachFriendsProvider.CONTENT_URI,
                    new String[]{
                            ReachFriendsHelper.COLUMN_ID,
                            ReachFriendsHelper.COLUMN_USER_NAME,
                            ReachFriendsHelper.COLUMN_IMAGE_ID
                    },
                    ReachFriendsHelper.COLUMN_STATUS + " = ?",
                    new String[]{
                            ReachFriendsHelper.ONLINE_REQUEST_GRANTED + ""
                    }, null);

            if (cursor == null)
                return null;

            final JsonArray jsonArray = new JsonArray();

            while (cursor.moveToNext()) {

                final long onlineId = cursor.getLong(0);
                final String userName = cursor.getString(1);
                final String imageId = cursor.getString(2);

                Log.i("Ayush", "Adding online friend id " + onlineId);
                jsonArray.add(onlineId);

                //cache the details
                USER_INFO_CACHE.put(onlineId, new Pair<>(userName, imageId));
            }
            cursor.close();

            final JsonObject toReturn = new JsonObject();
            toReturn.addProperty("userId", myServerId);
            /*MiscUtils.useFragment(reference, fragment -> {
                final long lastRequestTime = SharedPrefUtils.getLastRequestTime(fragment.preferences);
                    toReturn.addProperty("lastRequestTime", lastRequestTime);
            });*/
            toReturn.add("friends", jsonArray);

            return toReturn;

        }).or(new JsonObject());

        Log.i("Ayush", jsonObject.toString());

        final RequestBody body = RequestBody
                .create(MediaType.parse("application/json; charset=utf-8"), jsonObject.toString());
        final Request request = new Request.Builder()
                .url("http://52.74.175.56:8080/explore/getObjects")
                .post(body)
                .build();
        final Response response = ReachApplication.OK_HTTP_CLIENT.newCall(request).execute();
        if (response.code() != HttpStatusCodes.STATUS_CODE_OK)
            return Collections.emptyList();

        final JsonArray receivedData = new JsonParser().parse(response.body().string()).getAsJsonArray();
        final ImagePipeline imagePipeline = Fresco.getImagePipeline();

        final List<JsonObject> containers = new ArrayList<>();
        for (int index = 0; index < receivedData.size(); index++) {

            final JsonObject object = receivedData.get(index).getAsJsonObject();
            Log.d("Ayush", object.toString());

            final ExploreTypes exploreTypes = ExploreTypes.valueOf(MiscUtils.get(object, ExploreJSON.TYPE).getAsString());
            final JsonObject viewInfo = MiscUtils.get(object, ExploreJSON.VIEW_INFO).getAsJsonObject();
            final ImageRequest imageRequest;
            final String image;

            switch (exploreTypes) {

                case MUSIC:

                    image = MiscUtils.get(viewInfo, ExploreJSON.MusicViewInfo.LARGE_IMAGE_URL, "").getAsString();
                    imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(image))
                            .setResizeOptions(FULL_IMAGE_SIZE)
                            .build();
                    break;
                case APP:

                    image = MiscUtils.get(viewInfo, ExploreJSON.AppViewInfo.SMALL_IMAGE_URL, "").getAsString();
                    imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(image))
                            .setResizeOptions(SMALL_IMAGE_SIZE)
                            .build();
                    break;
                case MISC:

                    final String typeText = MiscUtils.get(viewInfo, ExploreJSON.MiscViewInfo.TYPE_TEXT, "").getAsString();
                    //TODO remove later
                    if (!typeText.equalsIgnoreCase("Small"))
                        continue;
                    image = MiscUtils.get(viewInfo, ExploreJSON.MiscViewInfo.LARGE_IMAGE_URL, "").getAsString();
                    imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(image))
                            .setResizeOptions(SMALL_IMAGE_SIZE)
                            .build();
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected type in explore" + exploreTypes.getName());
            }

            imagePipeline.prefetchToDiskCache(imageRequest, null);
            containers.add(object);
        }

        if (containers.size() > 0)
            MiscUtils.useFragment(reference, fragment -> {
                SharedPrefUtils.storeLastRequestTime(fragment.preferences);
            });

        Log.i("Ayush", "Explore has " + containers.size() + " stories");
        return containers;
    };

    /*private static final PopupMenu.OnMenuItemClickListener POP_MENU_CLICK = item -> {

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
                return false;
        }
    };*/

    @Nullable
    private View rootView = null;
    @Nullable
    private ViewPager explorePager = null;
    @Nullable
    private ExploreAdapter exploreAdapter = null;

    @Nullable
    private ExploreBuffer<JsonObject> buffer = null;
    @Nullable
    private SuperInterface mListener = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        preferences = getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE);
        myServerId = SharedPrefUtils.getServerId(preferences);
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_explore, container, false);
        final Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.exploreToolbar);
        toolbar.setTitle("Discover");
        toolbar.inflateMenu(R.menu.explore_menu);
        toolbar.setOnMenuItemClickListener(mListener != null ? mListener.getMenuClickListener() : null);

        exploreAdapter = new ExploreAdapter(this, this);
        /*final LinearLayout exploreToolbarText = (LinearLayout) toolbar.findViewById(R.id.exploreToolbarText);
        final PopupMenu popupMenu = new PopupMenu(getActivity(), exploreToolbarText);

        popupMenu.inflate(R.menu.explore_popup_menu);
        exploreToolbarText.setOnClickListener(v -> popupMenu.show());
        popupMenu.setOnMenuItemClickListener(POP_MENU_CLICK);*/

        explorePager = (ViewPager) rootView.findViewById(R.id.explorer);
        explorePager.setAdapter(exploreAdapter);
//        explorePager.setOffscreenPageLimit(1);
        explorePager.setPageMargin(-1 * (MiscUtils.dpToPx(25)));
        explorePager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                ((ReachApplication) getActivity().getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                        .setCategory("Explore - Page Swiped")
                        .setAction("User Name - " + SharedPrefUtils.getUserName(preferences))
                        .setValue(1)
                        .build());
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        if (!SharedPrefUtils.getExploreCoach1Seen(preferences)) {
            mListener.showSwipeCoach();
            SharedPrefUtils.setExploreCoach1Seen(preferences);
        }
        return rootView;
    }

    public void onDestroyView() {
        super.onDestroyView();
        Log.d("Ayush", "ExploreFragment - onDestroyView");

        rootView = null;
        explorePager = null;
        exploreAdapter = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Ayush", "ExploreFragment - onCreate");
    }

    @Override
    public synchronized Callable<Collection<JsonObject>> fetchNextBatch() {
//        Toast.makeText(activity, "Server Fetching next batch of 10", Toast.LENGTH_SHORT).show();
        return FETCH_NEXT_BATCH;
    }

    @Override
    public JsonObject getContainerForIndex(int index) {

        //return data
        return buffer.getViewItem(index); //test can not be null
    }

    @Override
    public int getCount() {
        return buffer != null ? buffer.currentBufferSize() : 0; //test can not be null
    }

    @Override
    public boolean isDoneForToday() {
        return buffer == null;
    }

    @Override
    public synchronized void notifyDataAvailable() {

        //This is UI thread !
        Log.i("Ayush", "Notifying data set changed on explore adapter");
        if (exploreAdapter != null)
            exploreAdapter.notifyDataSetChanged();
    }

    @Override
    public void loadedFromCache(int count) {

        notifyDataAvailable();
        if (explorePager != null)
            explorePager.postDelayed(new ScrollToLast(count), 1500L);
    }

    @Override
    public void handOverMessage(@Nonnull Integer position) {

        //retrieve full json
        final JsonObject exploreJson = buffer.getViewItem(position); //test can not be null

        if (exploreJson == null)
            return;

        final ExploreTypes type = ExploreTypes.valueOf(MiscUtils.get(exploreJson, ExploreJSON.TYPE).getAsString());

        switch (type) {

            case MUSIC:
                addToDownload(exploreJson);
                break;

            case APP:
                MiscUtils.openAppInPlayStore(getActivity(), MiscUtils.get(exploreJson, ExploreJSON.PACKAGE_NAME)
                        .getAsString(), MiscUtils.get(exploreJson, ExploreJSON.ID).getAsLong(), "EXPLORE");
                break;

            case MISC:
                final JsonObject metaInfo = exploreJson.get(ExploreJSON.META_INFO.getName()).getAsJsonObject();
                final String activityClass = MiscUtils.get(metaInfo, MiscMetaInfo.CLASS_NAME).getAsString();
                Class<?> mClass = null;
                if (activityClass != null) {
                    try {
                        mClass = Class.forName(activityClass);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                final Intent intent = new Intent(getActivity(), mClass);
                startActivity(intent);
                break;
        }

    }

    public void addToDownload(JsonObject exploreJSON) {

        final Activity activity = getActivity();
        final ContentResolver contentResolver = activity.getContentResolver();

        //extract meta info to process current click request
        final JsonObject metaInfo = exploreJSON.get(ExploreJSON.META_INFO.getName()).getAsJsonObject();

        //get user name and imageId
        final long senderId = MiscUtils.get(metaInfo, MusicMetaInfo.SENDER_ID).getAsLong();
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
        reachDatabase.setSongId(MiscUtils.get(metaInfo, MusicMetaInfo.SONG_ID).getAsLong());
        reachDatabase.setReceiverId(myServerId);
        reachDatabase.setSenderId(senderId);

        reachDatabase.setOperationKind(ReachDatabase.OperationKind.DOWNLOAD_OP);
        reachDatabase.setPath("hello_world");
        reachDatabase.setUserName(userName);
        reachDatabase.setOnlineStatus(ReachFriendsHelper.ONLINE_REQUEST_GRANTED + "");

        reachDatabase.setDisplayName(MiscUtils.get(metaInfo, MusicMetaInfo.DISPLAY_NAME).getAsString());
        reachDatabase.setActualName(MiscUtils.get(metaInfo, MusicMetaInfo.ACTUAL_NAME).getAsString());
        reachDatabase.setArtistName(MiscUtils.get(metaInfo, MusicMetaInfo.ARTIST, "").getAsString());
        reachDatabase.setAlbumName(MiscUtils.get(metaInfo, MusicMetaInfo.ALBUM, "").getAsString());

        reachDatabase.setIsLiked(false);
        reachDatabase.setLength(MiscUtils.get(metaInfo, MusicMetaInfo.SIZE).getAsLong());
        reachDatabase.setProcessed(0);
        reachDatabase.setDateAdded(DateTime.now());
        reachDatabase.setUniqueId(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE));

        reachDatabase.setDuration(MiscUtils.get(metaInfo, MusicMetaInfo.DURATION).getAsLong());
        reachDatabase.setLogicalClock((short) 0);
        reachDatabase.setStatus(ReachDatabase.Status.NOT_WORKING);

        reachDatabase.setLastActive(0);
        reachDatabase.setReference(0);

        reachDatabase.setGenre("hello_world");

        reachDatabase.setVisibility(true);

        MiscUtils.startDownload(reachDatabase, getActivity(), rootView, "EXPLORE");
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        buffer = ExploreBuffer.getInstance(this, context.getCacheDir(), BYTES_TO_JSON, JSON_TO_BYTES);
        try {
            mListener = (SuperInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement SplashInterface");
        }
    }

    @Override
    public void onDetach() {

        super.onDetach();
        Log.d("Ayush", "ExploreFragment - onDetach");
        mListener = null;
        if (buffer != null)
            buffer.close();
    }

    private final class ScrollToLast implements Runnable {

        private final int scrollTo;

        private ScrollToLast(int scrollTo) {
            this.scrollTo = scrollTo;
        }

        @Override
        public void run() {

            //sanity check
            if (explorePager == null || rootView == null)
                return;

            //magic scroll position should be available
            if (!(scrollTo > 1))
                return;

            final int currentItem = explorePager.getCurrentItem();
            //user has somehow started scrolling
            if (currentItem > 0)
                return;

            explorePager.setCurrentItem(scrollTo - 2, true);
        }
    }
}