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
    }

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

    public static final String dropBox = "https://dl.dropboxusercontent.com/u/17710400/Reach_Version.txt";

    //When using threadPool in ReachProcess, take care, as its a parameter to kill service
    public static final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    public static final LongSparseArray<String> networkCache = new LongSparseArray<>();

    public static boolean debugMode = true;
}
