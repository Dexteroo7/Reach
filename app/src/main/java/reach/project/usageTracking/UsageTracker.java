package reach.project.usageTracking;

import android.support.annotation.NonNull;
import android.util.ArrayMap;
import android.util.Log;

import com.google.common.base.Optional;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Request;

import java.util.Map;
import java.util.Set;
import java.util.Stack;
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

    ////////////

    LIKE_APP, //user likes an app (his own or someone else's)
    REDIRECT_PLAY_STORE, //user redirected to play store
    ;
    ////////////

    private static final Stack<Request> requests = new Stack<>(); //singleton request object

    private static final Executor asyncTracker = Executors.newSingleThreadExecutor();

    private static final Runnable track = () -> {

        final Request toProcess;

        synchronized (requests) {
            if (requests.isEmpty())
                return; //oops
            toProcess = requests.pop();
        }

        Log.i("Ayush", "Trying to track ! " + toProcess.body().toString());

        final Optional<Integer> statusCode = MiscUtils.autoRetry(() -> {

                    final int code = ReachApplication.okHttpClient.newCall(toProcess).execute().code();
                    Log.i("Ayush", "Got code " + code);
                    return code;
                }, //perform the post
                Optional.of(input -> input == null || !(input == 200 || input == 204))); //check for invalid response

        if (!statusCode.isPresent()) {

            //TODO cache the request !
        } else
            Log.i("Ayush", "USAGE TRACKED !");
    };

    public static void trackSong(@NonNull ArrayMap<PostParams, String> simpleParams,
                                 @NonNull ArrayMap<SongMetadata, String> complexParams,
                                 @NonNull UsageTracker eventName) {

        final FormEncodingBuilder formBody = new FormEncodingBuilder();

        ///////////////
        final Set<Map.Entry<PostParams, String>> simpleEntries = simpleParams.entrySet();
        for (Map.Entry<PostParams, String> entry : simpleEntries)
            formBody.add(entry.getKey().getValue(), entry.getValue());
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
        formBody.add(PostParams.META_INFO.getValue(), builder.toString());
        formBody.add(PostParams.EVENT_NAME.getValue(), eventName.name());

        synchronized (requests) {
            requests.push(new Request.Builder()
                    .url("http://52.74.175.56:8080/analytics/events")
                    .post(formBody.build())
                    .build());
        }

        asyncTracker.execute(track);
    }

}
