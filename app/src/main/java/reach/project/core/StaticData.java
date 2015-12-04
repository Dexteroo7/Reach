package reach.project.core;

import android.support.v4.util.LongSparseArray;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import reach.backend.applications.appVisibilityApi.AppVisibilityApi;
import reach.backend.applications.classifiedAppsApi.ClassifiedAppsApi;
import reach.backend.entities.feedBackApi.FeedBackApi;
import reach.backend.entities.messaging.Messaging;
import reach.backend.entities.userApi.UserApi;
import reach.backend.music.musicVisibilityApi.MusicVisibilityApi;
import reach.backend.notifications.notificationApi.NotificationApi;
import reach.project.apps.App;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.music.MySongsHelper;
import reach.project.music.Song;
import reach.project.utils.CloudEndPointsUtils;

/**
 * Created by dexter on 20/6/14.
 */
public final class StaticData {

    public static final UserApi userEndpoint;
    public static final FeedBackApi feedBackApi;
    public static final Messaging.MessagingEndpoint messagingEndpoint;
    public static final MusicVisibilityApi musicVisibility;
    public static final NotificationApi notificationApi;

    public static final AppVisibilityApi appVisibilityApi;
    public static final ClassifiedAppsApi classifiedAppsApi;

    static {

        final HttpTransport transport = new NetHttpTransport();
        final JsonFactory factory = new JacksonFactory();
        final HttpRequestInitializer initialize = request -> {

            request.setConnectTimeout(request.getConnectTimeout() * 2);
            request.setReadTimeout(request.getReadTimeout() * 2);
        };

        userEndpoint = CloudEndPointsUtils.updateBuilder(new UserApi.Builder(transport, factory, initialize)).build();
        messagingEndpoint = CloudEndPointsUtils.updateBuilder(new Messaging.Builder(transport, factory, initialize)).build().messagingEndpoint();
        feedBackApi = CloudEndPointsUtils.updateBuilder(new FeedBackApi.Builder(transport, factory, initialize)).build();
        notificationApi = CloudEndPointsUtils.updateBuilder(new NotificationApi.Builder(transport, factory, initialize)).build();
        musicVisibility = CloudEndPointsUtils.updateBuilder(new MusicVisibilityApi.Builder(transport, factory, initialize)).build();

        appVisibilityApi = CloudEndPointsUtils.updateBuilder(new AppVisibilityApi.Builder(transport, factory, initialize)).build();
        classifiedAppsApi = CloudEndPointsUtils.updateBuilder(new ClassifiedAppsApi.Builder(transport, factory, initialize)).build();
    }

    public static final String[] DOWNLOADED_PARTIAL = new String[]{
            ReachDatabaseHelper.COLUMN_ID, //0
            ReachDatabaseHelper.COLUMN_SIZE, //1
            ReachDatabaseHelper.COLUMN_SENDER_ID, //2
            ReachDatabaseHelper.COLUMN_PROCESSED, //3
            ReachDatabaseHelper.COLUMN_PATH, //4
            ReachDatabaseHelper.COLUMN_DISPLAY_NAME, //5
            ReachDatabaseHelper.COLUMN_IS_LIKED, //6
            ReachDatabaseHelper.COLUMN_SONG_ID, //7
            ReachDatabaseHelper.COLUMN_ARTIST, //8
            ReachDatabaseHelper.COLUMN_DURATION}; //9

    public static final String[] DISK_PARTIAL = new String[]{
            MySongsHelper.COLUMN_ARTIST, //0
            MySongsHelper.COLUMN_SONG_ID, //1
            MySongsHelper.COLUMN_SIZE, //2
            MySongsHelper.COLUMN_PATH, //3
            MySongsHelper.COLUMN_DISPLAY_NAME, //4
            MySongsHelper.COLUMN_ID, //5
            MySongsHelper.COLUMN_DURATION}; //6

    public static byte i = 0;
    public static final byte FRIENDS_VERTICAL_LOADER;
    public static final byte FRIENDS_HORIZONTAL_LOADER;
    public static final byte PRIVACY_MY_LIBRARY_LOADER;
    public static final byte PRIVACY_DOWNLOADED_LOADER;
    public static final byte DOWNLOAD_LOADER;
    public static final byte DOWNLOADING_LOADER;
    public static final byte UPLOAD_LOADER;
    public static final byte MY_LIBRARY_LOADER;
    public static final byte PUSH_MY_LIBRARY_LOADER;
    public static final byte PUSH_DOWNLOADED_LOADER;

    public static final byte FULL_LIST_LOADER;
    public static final byte RECENT_LIST_LOADER;

    static {
        FRIENDS_VERTICAL_LOADER = i++;
        FRIENDS_HORIZONTAL_LOADER = i++;
        PRIVACY_MY_LIBRARY_LOADER = i++;
        PRIVACY_DOWNLOADED_LOADER = i++;
        DOWNLOAD_LOADER = i++;
        DOWNLOADING_LOADER = i++;
        UPLOAD_LOADER = i++;
        MY_LIBRARY_LOADER = i++;
        PUSH_MY_LIBRARY_LOADER = i++;
        PUSH_DOWNLOADED_LOADER = i++;
        FULL_LIST_LOADER = i++;
        RECENT_LIST_LOADER = i++;
    }

    public static Comparator<App> primaryApps = (left, right) -> {

        final Long a = left == null || left.installDate == null ? 0 : left.installDate;
        final Long b = right == null || right.installDate == null ? 0 : right.installDate;

        return a.compareTo(b);
    };

    public static Comparator<App> secondaryApps = (left, right) -> {

        final String a = left == null || left.applicationName == null ? "" : left.applicationName;
        final String b = right == null || right.applicationName == null ? "" : right.applicationName;

        return a.compareTo(b);
    };

    public static final Comparator<Song> primaryMusic = (left, right) -> {

        final Long lhs = left == null || left.dateAdded == null ? 0 : left.dateAdded;
        final Long rhs = right == null || right.dateAdded == null ? 0 : right.dateAdded;

        return lhs.compareTo(rhs);
    };

    public static final Comparator<Song> secondaryMusic = (left, right) -> {
        final String lhs = left == null || left.displayName == null ? "" : left.displayName;
        final String rhs = right == null || right.displayName == null ? "" : right.displayName;

        return lhs.compareTo(rhs);
    };

    public static final int PLAYER_BUFFER_DEFAULT = 4096;
    public static final long LUCKY_DELAY = 4000;

    public static final long devika = 5666668701286400L;
    public static final String devikaPhoneNumber = "8860872102";

    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final int ONLINE_LIMIT = 30 * 60 * 1000; //30 minutes timeout
    public static final short NETWORK_CALL_WAIT = 300;
    public static final short NETWORK_RETRY = 5;
    public static final int MINIMUM_FREE_SPACE = 100 * 1024 * 1024; //100mb

    public static final long MINIMUM_PONG_GAP = 18 * 1000; //15 seconds
    public static final short MUSIC_PLAYER = 12;

    public static final String mixPanelId = "7877f44b1ce4a4b2db7790048eb6587a";
    public static final String dropBox = "https://dl.dropboxusercontent.com/u/17710400/Reach_Version.txt";
    public static final String cloudStorageImageBaseUrl = "http://storage.googleapis.com/able-door-616-images/";
    public static final String dropBoxPromo = "https://dl.dropboxusercontent.com/s/p2m01z9opnf3xtu/promo_codes.txt";
    public static final String dropBoxManager = "https://dl.dropboxusercontent.com/s/n04wqrlr0sq0tqn/reach_manager.jpg";
    public static final LongSparseArray<String> networkCache = new LongSparseArray<>();

    //TODO remove
    public static final ExecutorService temporaryFix = Executors.unconfigurableExecutorService(Executors.newCachedThreadPool());
}
