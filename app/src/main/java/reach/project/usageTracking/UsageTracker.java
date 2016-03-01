package reach.project.usageTracking;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.base.Optional;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import reach.project.core.ReachApplication;
import reach.project.utils.MiscUtils;

/**
 * Created by dexter on 23/10/15.
 */
public enum UsageTracker {

    PLAY_SONG, //most + last played
    LIKE_SONG, //liked by user (client)
    DOWNLOAD_SONG, //downloaded by friends
    DOWNLOAD_COMPLETE, //download completed by friends

    ////////////

    LIKE_APP, //user likes an app (his own or someone else's)
    CLICK_APP, //app clicked by user

    ////////////

    APP_OPEN //user opened the app

    ////////////
    ;

    private static final Stack<Request> requests = new Stack<>(); //singleton request object

    private static final Executor asyncTracker = Executors.newSingleThreadExecutor();

    private static final Calendar calendar = Calendar.getInstance();

    static {
        calendar.setTimeZone(TimeZone.getTimeZone("IST"));
    }

    private static final Runnable track = () -> {

        final Request toProcess;

        synchronized (requests) {
            if (requests.isEmpty())
                return; //oops
            toProcess = requests.pop();
        }

        Log.i("Ayush", "Trying to track ! " + toProcess.body().toString());

        final Optional<Integer> statusCode = MiscUtils.autoRetry(() -> ReachApplication.OK_HTTP_CLIENT.newCall(toProcess).execute().code(), //perform the post
                Optional.of(input -> input == null || !(input == 200 || input == 204))); //check for invalid response

        if (!statusCode.isPresent()) {

            //TODO cache the request !
        } else
            Log.i("Ayush", "USAGE TRACKED !");
    };

    public static void trackApp(@NonNull Map<PostParams, String> simpleParams,
                                @NonNull Map<AppMetadata, String> complexParams,
                                @NonNull UsageTracker eventName) throws JSONException {

        final JSONObject jsonObject = new JSONObject();

        ///////////////
        final Set<Map.Entry<PostParams, String>> simpleEntries = simpleParams.entrySet();
        for (Map.Entry<PostParams, String> entry : simpleEntries)
            jsonObject.put(entry.getKey().getValue(), entry.getValue());
        ///////////////
        final Set<Map.Entry<AppMetadata, String>> complexEntries = complexParams.entrySet();
        final StringBuilder builder = new StringBuilder(50);

        String or = "";
        for (Map.Entry<AppMetadata, String> entry : complexEntries) {

            builder.append(or);
            or = "|"; //next time onwards, we append a pipe
            builder.append(entry.getKey().name()).append(":").append(entry.getValue());
        }
        ///////////////

        jsonObject.put(PostParams.META_INFO.getValue(), builder.toString());
        jsonObject.put(PostParams.EVENT_NAME.getValue(), eventName.name());
        jsonObject.put(PostParams.TIME_STAMP.getValue(), calendar.getTimeInMillis());

        final String toPost = jsonObject.toString();
        Log.i("Ayush", "Posting " + toPost);

        synchronized (requests) {

            requests.push(new Request.Builder()
                    .url("http://52.74.175.56:8080/analytics/events")
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), toPost))
                    .build());
        }

        asyncTracker.execute(track);
    }

    public static void trackSong(@NonNull Map<PostParams, String> simpleParams,
                                 @NonNull Map<SongMetadata, String> complexParams,
                                 @NonNull UsageTracker eventName) throws JSONException {

        final JSONObject jsonObject = new JSONObject();

        ///////////////
        final Set<Map.Entry<PostParams, String>> simpleEntries = simpleParams.entrySet();
        for (Map.Entry<PostParams, String> entry : simpleEntries)
            jsonObject.put(entry.getKey().getValue(), entry.getValue());
        ///////////////
        final Set<Map.Entry<SongMetadata, String>> complexEntries = complexParams.entrySet();
        final StringBuilder builder = new StringBuilder(50);

        String or = "";
        for (Map.Entry<SongMetadata, String> entry : complexEntries) {

            builder.append(or);
            or = "|"; //next time onwards, we append a pipe
            builder.append(entry.getKey().name()).append(":").append(entry.getValue());
        }
        ///////////////

        jsonObject.put(PostParams.META_INFO.getValue(), builder.toString());
        jsonObject.put(PostParams.EVENT_NAME.getValue(), eventName.name());
        jsonObject.put(PostParams.TIME_STAMP.getValue(), calendar.getTimeInMillis());

        final String toPost = jsonObject.toString();
        Log.i("Ayush", "Posting " + toPost);

        synchronized (requests) {

            requests.push(new Request.Builder()
                    .url("http://52.74.175.56:8080/analytics/events")
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), toPost))
                    .build());
        }

        asyncTracker.execute(track);
    }

    //TODO
    public static void trackEvent(@NonNull Map<PostParams, String> simpleParams,
                                  @NonNull UsageTracker eventName) throws JSONException {

        final JSONObject jsonObject = new JSONObject();

        ///////////////
        final Set<Map.Entry<PostParams, String>> simpleEntries = simpleParams.entrySet();
        for (Map.Entry<PostParams, String> entry : simpleEntries)
            jsonObject.put(entry.getKey().getValue(), entry.getValue());
        jsonObject.put(PostParams.EVENT_NAME.getValue(), eventName.name());
        ///////////////

        final String toPost = jsonObject.toString();
        Log.i("Ayush", "Posting " + toPost);

        synchronized (requests) {
            requests.push(new Request.Builder()
                    .url("http://52.74.175.56:8080/analytics/events")
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), toPost))
                    .build());
        }

        asyncTracker.execute(track);
    }
}