package reach.project.core;

import android.support.v4.util.LongSparseArray;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.util.Comparator;

import reach.backend.entities.feedBackApi.FeedBackApi;
import reach.backend.entities.messaging.Messaging;
import reach.backend.entities.userApi.UserApi;
import reach.backend.notifications.notificationApi.NotificationApi;
import reach.project.apps.App;
import reach.project.utils.CloudEndPointsUtils;

/**
 * Created by dexter on 20/6/14.
 */
public final class StaticData {

    public static final UserApi USER_API;
    public static final FeedBackApi FEED_BACK_API;
    public static final Messaging.MessagingEndpoint MESSAGING_API;
    public static final NotificationApi NOTIFICATION_API;
    public static final String NULL_STRING = "NULL";
    public static final String PERCENTAGE_STRING = "%";
    public static final String USER_ID_KEY = "userId";
    //public static final String CALL_POST_RESUME_KEY = "call_post_resume";

    public static final String NO_SONGS_TEXT = "No songs!";
    public static final String FILTER_STRING_KEY = "search_filter_key" ;
    public static int deviceWidth=0;
    public static int deviceHeight=0;
    public static int downloadedSongsCount=0;
    public static int librarySongsCount=0;
    public static int appsCount=0;
    public static int friendsCount=0;
    public static final String zero = "0";
    public static final String one = "1";
    public static final byte oneByte = 1;
    public static final byte zeroByte = 0;
    public static final String youtube_thumbnail_prefix = "https://i.ytimg.com/vi/";
    public static final String youtube_thumbnail_postfix = "/hqdefault.jpg";

    static {

        final HttpTransport transport = new NetHttpTransport();
        final JsonFactory factory = new JacksonFactory();
        final HttpRequestInitializer initialize = request -> {

            request.setConnectTimeout(request.getConnectTimeout() * 2);
            request.setReadTimeout(request.getReadTimeout() * 2);
        };

        USER_API = CloudEndPointsUtils.updateBuilder(new UserApi.Builder(transport, factory, initialize)).build();
        MESSAGING_API = CloudEndPointsUtils.updateBuilder(new Messaging.Builder(transport, factory, initialize)).build().messagingEndpoint();
        FEED_BACK_API = CloudEndPointsUtils.updateBuilder(new FeedBackApi.Builder(transport, factory, initialize)).build();
        NOTIFICATION_API = CloudEndPointsUtils.updateBuilder(new NotificationApi.Builder(transport, factory, initialize)).build();
    }

    /*public static final String[] DOWNLOADED_PARTIAL = new String[]{
            SongHelper.COLUMN_ID, //0
            SongHelper.COLUMN_SIZE, //1
            SongHelper.COLUMN_SENDER_ID, //2
            SongHelper.COLUMN_PROCESSED, //3
            SongHelper.COLUMN_PATH, //4
            SongHelper.COLUMN_DISPLAY_NAME, //5
            SongHelper.COLUMN_IS_LIKED, //6
            SongHelper.COLUMN_SONG_ID, //7
            SongHelper.COLUMN_ARTIST, //8
            SongHelper.COLUMN_DURATION, //9
            SongHelper.COLUMN_META_HASH}; //10*/

    /*public static final String[] DISK_PARTIAL = new String[]{
            MySongsHelper.COLUMN_ARTIST, //0
            MySongsHelper.COLUMN_SONG_ID, //1
            MySongsHelper.COLUMN_SIZE, //2
            MySongsHelper.COLUMN_PATH, //3
            MySongsHelper.COLUMN_DISPLAY_NAME, //4
            MySongsHelper.COLUMN_ID, //5
            MySongsHelper.COLUMN_DURATION, //6
            MySongsHelper.COLUMN_IS_LIKED, //7
            SongHelper.COLUMN_META_HASH}; //8
*/

    public static byte i = 0;
    public static final byte FRIENDS_VERTICAL_LOADER;
    public static final byte FRIENDS_HORIZONTAL_LOADER;
    public static final byte CONTACTS_CHOOSER_LOADER;
    public static final byte ALL_CONTACTS_LOADER;

    public static final byte MY_LIBRARY_LOADER;
    public static final byte DOWNLOADING_LOADER;
    public static final byte UPLOADING_LOADER;
    public static final byte PLAYER_LOADER;

    public static final byte PRIVACY_MY_LIBRARY_LOADER;
    public static final byte PUSH_MY_LIBRARY_LOADER;

    static {
        FRIENDS_VERTICAL_LOADER = i++;
        FRIENDS_HORIZONTAL_LOADER = i++;
        PRIVACY_MY_LIBRARY_LOADER = i++;
        DOWNLOADING_LOADER = i++;
        UPLOADING_LOADER = i++;
        PLAYER_LOADER = i++;
        MY_LIBRARY_LOADER = i++;
        PUSH_MY_LIBRARY_LOADER = i++;
        CONTACTS_CHOOSER_LOADER = i++;
        ALL_CONTACTS_LOADER = i++;
    }

    public static Comparator<App> byInstallDate = (left, right) -> {

        final Long a = left == null || left.installDate == null ? 0 : left.installDate;
        final Long b = right == null || right.installDate == null ? 0 : right.installDate;

        return a.compareTo(b);
    };

    public static Comparator<App> byName = (left, right) -> {

        final String a = left == null || left.applicationName == null ? "" : left.applicationName;
        final String b = right == null || right.applicationName == null ? "" : right.applicationName;

        return a.compareToIgnoreCase(b);
    };

    public static final int PLAYER_BUFFER_DEFAULT = 4096;
    public static final long LUCKY_DELAY = 4000;

    public static final long DEVIKA = 5666668701286400L;
    public static final String DEVIKA_PHONE_NUMBER = "8860872102";

    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final int ONLINE_LIMIT = 90 * 1000; //90 seconds timeout
    public static final short NETWORK_CALL_WAIT = 300;
    public static final short NETWORK_RETRY = 5;
    public static final int MINIMUM_FREE_SPACE = 50 * 1024 * 1024; //50mb

    public static final long MINIMUM_PONG_GAP = 18 * 1000; //15 seconds
    public static final short MUSIC_PLAYER = 12;

    public static final String SCOPE = "server:client_id:528178870551-m0emotumn7gb4r9hted42p58tr3s8ukt.apps.googleusercontent.com";
    public static final String DROP_BOX = "https://dl.dropboxusercontent.com/u/17710400/Reach_Version.txt";
    public static final String CLOUD_STORAGE_IMAGE_BASE_URL = "http://storage.googleapis.com/able-door-616-images/";
    public static final String DROP_BOX_PROMO = "https://dl.dropboxusercontent.com/s/p2m01z9opnf3xtu/promo_codes.txt";
    public static final String DROP_BOX_MANAGER = "https://dl.dropboxusercontent.com/s/n04wqrlr0sq0tqn/reach_manager.jpg";

    public static final LongSparseArray<String> NETWORK_CACHE = new LongSparseArray<>();
}