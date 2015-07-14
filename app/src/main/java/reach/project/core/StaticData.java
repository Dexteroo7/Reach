package reach.project.core;

import android.support.v4.util.LongSparseArray;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import reach.backend.entities.feedBackApi.FeedBackApi;
import reach.backend.entities.messaging.Messaging;
import reach.backend.entities.userApi.UserApi;
import reach.backend.notifications.notificationApi.NotificationApi;
import reach.project.database.sql.ReachDatabaseHelper;
import reach.project.database.sql.ReachSongHelper;
import reach.project.utils.CloudEndPointsUtils;

/**
 * Created by dexter on 20/6/14.
 */
public final class StaticData {

    static {

        final HttpTransport transport = new NetHttpTransport();
        final JsonFactory factory = new JacksonFactory();
        final HttpRequestInitializer initialize = new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {
            }
        };
        userEndpoint = CloudEndPointsUtils.updateBuilder(new UserApi.Builder(transport, factory, initialize)).build();
        messagingEndpoint = CloudEndPointsUtils.updateBuilder(new Messaging.Builder(transport, factory, initialize)).build();
        feedBackApi = CloudEndPointsUtils.updateBuilder(new FeedBackApi.Builder(transport, factory, initialize)).build();
        notificationApi = CloudEndPointsUtils.updateBuilder(new NotificationApi.Builder(transport, factory, initialize)).build();
    }

    public static final String [] DOWNLOADED_LIST = new String[]{ //count = 14
            ReachDatabaseHelper.COLUMN_ID, //0
            ReachDatabaseHelper.COLUMN_LENGTH, //1
            ReachDatabaseHelper.COLUMN_RECEIVER_ID, //2
            ReachDatabaseHelper.COLUMN_PROCESSED, //3
            ReachDatabaseHelper.COLUMN_PATH, //4
            ReachDatabaseHelper.COLUMN_DISPLAY_NAME, //5

            ReachDatabaseHelper.COLUMN_STATUS, //6
            ReachDatabaseHelper.COLUMN_OPERATION_KIND, //7
            ReachDatabaseHelper.COLUMN_SENDER_ID, //8
            ReachDatabaseHelper.COLUMN_LOGICAL_CLOCK,//9
            ReachDatabaseHelper.COLUMN_SONG_ID, //10

            ReachDatabaseHelper.COLUMN_SENDER_NAME, //11
            ReachDatabaseHelper.COLUMN_ONLINE_STATUS, //12
            ReachDatabaseHelper.COLUMN_NETWORK_TYPE, //13
            ReachDatabaseHelper.COLUMN_IS_LIKED}; //14

    public static final String [] DISK_LIST = new String[] { //count = 8
            ReachSongHelper.COLUMN_SIZE,
            ReachSongHelper.COLUMN_PATH,
            ReachSongHelper.COLUMN_DISPLAY_NAME,
            ReachSongHelper.COLUMN_ID,
            ReachSongHelper.COLUMN_DURATION,
            ReachSongHelper.COLUMN_ARTIST,
            ReachSongHelper.COLUMN_ALBUM,
            ReachSongHelper.COLUMN_SONG_ID,
    };

    public static final String [] DOWNLOADED_PARTIAL = new String[]{
            ReachDatabaseHelper.COLUMN_ID, //0
            ReachDatabaseHelper.COLUMN_LENGTH, //1
            ReachDatabaseHelper.COLUMN_SENDER_ID, //2
            ReachDatabaseHelper.COLUMN_PROCESSED, //3
            ReachDatabaseHelper.COLUMN_PATH, //4
            ReachDatabaseHelper.COLUMN_DISPLAY_NAME, //5
            ReachDatabaseHelper.COLUMN_IS_LIKED, //6
            ReachDatabaseHelper.COLUMN_SONG_ID}; //7

    public static final String [] DISK_PARTIAL = new String[]{
            ReachSongHelper.COLUMN_ARTIST, //0
            ReachSongHelper.COLUMN_SONG_ID, //1
            ReachSongHelper.COLUMN_SIZE, //2
            ReachSongHelper.COLUMN_PATH, //3
            ReachSongHelper.COLUMN_DISPLAY_NAME, //4
            ReachSongHelper.COLUMN_ID, //5
            ReachSongHelper.COLUMN_DURATION}; //6

    public static final String[] DISK_COMPLETE_NO_PATH =
            {
                    ReachSongHelper.COLUMN_ID,

                    ReachSongHelper.COLUMN_SONG_ID,
                    ReachSongHelper.COLUMN_USER_ID,

                    ReachSongHelper.COLUMN_DISPLAY_NAME,
                    ReachSongHelper.COLUMN_ACTUAL_NAME,

                    ReachSongHelper.COLUMN_ARTIST,
                    ReachSongHelper.COLUMN_ALBUM,

                    ReachSongHelper.COLUMN_DURATION,
                    ReachSongHelper.COLUMN_SIZE,

                    ReachSongHelper.COLUMN_VISIBILITY,
            };

    public static final short ALBUM_LOADER = 0;
    public static final short ARTIST_LOADER = 1;
    public static final short FRIENDS_LOADER = 2;
    public static final short SONGS_LOADER = 3;
    public static final short PLAY_LIST_LOADER = 4;
    public static final short DOWNLOAD_LOADER = 5;
    public static final short UPLOAD_LOADER = 6;
    public static final short MY_LIBRARY_LOADER = 7;

    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String cloudStorageImageBaseUrl = "http://storage.googleapis.com/able-door-616.appspot.com/";
    public static final int ONLINE_LIMIT = 30 * 60 * 1000; //30 minutes timeout
    public static final short NETWORK_CALL_WAIT = 300;
    public static final short NETWORK_RETRY = 5;
    public static final int MINIMUM_FREE_SPACE = 100 * 1024 * 1024; //100mb

    public static final long MINIMUM_PONG_GAP = 15 * 1000; //15 seconds
    public static final short MUSIC_PLAYER = 12;

    public static final UserApi userEndpoint;
    public static final FeedBackApi feedBackApi;
    public static final Messaging messagingEndpoint;
    public static final NotificationApi notificationApi;

    public static final String dropBox = "https://dl.dropboxusercontent.com/u/17710400/Reach_Version.txt";

    //When using threadPool in ReachProcess, take care, as its a parameter to kill service
    public static final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    public static final LongSparseArray<String> networkCache = new LongSparseArray<>();

    ////meant for release
    public static boolean debugMode = true;
}
