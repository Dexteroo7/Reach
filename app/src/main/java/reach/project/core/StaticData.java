package reach.project.core;

import android.support.v4.util.LongSparseArray;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import reach.backend.entities.feedBackApi.FeedBackApi;
import reach.backend.entities.messaging.Messaging;
import reach.backend.entities.userApi.UserApi;
import reach.backend.music.musicVisibilityApi.MusicVisibilityApi;
import reach.backend.notifications.notificationApi.NotificationApi;
import reach.project.music.songs.ReachSongHelper;
import reach.project.uploadDownload.ReachDatabaseHelper;
import reach.project.utils.CloudEndPointsUtils;

/**
 * Created by dexter on 20/6/14.
 */
public final class StaticData {

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
            ReachSongHelper.COLUMN_ARTIST, //0
            ReachSongHelper.COLUMN_SONG_ID, //1
            ReachSongHelper.COLUMN_SIZE, //2
            ReachSongHelper.COLUMN_PATH, //3
            ReachSongHelper.COLUMN_DISPLAY_NAME, //4
            ReachSongHelper.COLUMN_ID, //5
            ReachSongHelper.COLUMN_DURATION}; //6

    public static final byte ALBUM_LOADER = 0;
    public static final byte ARTIST_LOADER = 1;
    public static final byte FRIENDS_LOADER = 2;
    public static final byte PRIVACY_MY_LIBRARY_LOADER = 3;
    public static final byte PRIVACY_DOWNLOADED_LOADER = 4;
    public static final byte PLAY_LIST_LOADER = 5;
    public static final byte DOWNLOAD_LOADER = 6;
    public static final byte UPLOAD_LOADER = 7;
    public static final byte MY_LIBRARY_LOADER = 8;
    public static final byte PUSH_MY_LIBRARY_LOADER = 9;
    public static final byte PUSH_DOWNLOADED_LOADER = 10;

    public static final byte FULL_LIST_LOADER = 10;
    public static final byte RECENT_LIST_LOADER = 11;

    public static final int PLAYER_BUFFER_DEFAULT = 4096;
    public static final long LUCKY_DELAY = 4000;

    public static final long devika = 5666668701286400L;
    public static final String devikaPhoneNumber = "8860872102";

    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final int ONLINE_LIMIT = 30 * 60 * 1000; //30 minutes timeout
    public static final short NETWORK_CALL_WAIT = 300;
    public static final short NETWORK_RETRY = 5;
    public static final int MINIMUM_FREE_SPACE = 100 * 1024 * 1024; //100mb

    public static final long MINIMUM_PONG_GAP = 15 * 1000; //15 seconds
    public static final short MUSIC_PLAYER = 12;

    public static final String cloudStorageImageBaseUrl = "http://storage.googleapis.com/able-door-616-images/";

    public static final UserApi userEndpoint;
    public static final FeedBackApi feedBackApi;
    public static final Messaging.MessagingEndpoint messagingEndpoint;
    public static final MusicVisibilityApi musicVisibility;
    public static final NotificationApi notificationApi;

    public static final String dropBox = "https://dl.dropboxusercontent.com/u/17710400/Reach_Version.txt";
    public static final String dropBoxPromo = "https://dl.dropboxusercontent.com/s/p2m01z9opnf3xtu/promo_codes.txt";
    public static final String dropBoxManager = "https://dl.dropboxusercontent.com/s/n04wqrlr0sq0tqn/reach_manager.jpg";
    public static final LongSparseArray<String> networkCache = new LongSparseArray<>();
}
