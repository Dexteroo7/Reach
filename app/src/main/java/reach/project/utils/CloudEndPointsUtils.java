package reach.project.utils;

import android.util.Log;

import com.google.api.client.googleapis.services.AbstractGoogleClient;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;

import java.io.IOException;

/**
 * Created by Dexter on 28-03-2015.
 */
public enum CloudEndPointsUtils {;

    /*
     * TODO: Need to change this to 'true' if you're running your backend
     * locally using the DevAppServer. See
     * http://developers.google.com/eclipse/docs/cloud_endpoints for more
     * information.
     */
//    protected final boolean LOCAL_ANDROID_RUN = false;

    /*
     * The root URL of where your DevAppServer is running (if you're running the
     * DevAppServer locally).
     */
//    protected final String LOCAL_APP_ENGINE_SERVER_URL = "http://localhost:8888/";

    /*
     * The root URL of where your DevAppServer is running when it's being
     * accessed via the Android emulator (if you're running the DevAppServer
     * locally). In this case, you're running behind Android's virtual router.
     * See
     * http://developer.android.com/tools/devices/emulator.html#networkaddresses
     * for more information.
     */
//    protected final String LOCAL_APP_ENGINE_SERVER_URL_FOR_ANDROID = "http://10.0.2.2:8888";

    /**
     * Updates the Google client builder to connect the appropriate server based
     * on whether LOCAL_ANDROID_RUN is true or false.
     *
     * @param builder Google client builder
     * @return same Google client builder
     */
    public static <B extends AbstractGoogleClient.Builder> B updateBuilder(
            B builder) {

        // only enable GZip when connecting to remote server
        final boolean enableGZip = builder.getRootUrl().startsWith("https:");

        builder.setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
            public void initialize(AbstractGoogleClientRequest<?> request)
                    throws IOException {

                if (!enableGZip) {
                    Log.i("Ayush", "Disabling gzip Content");
                    request.setDisableGZipContent(true);
                }
            }
        });

        return builder;
    }

//    /**
//     * Logs the given message and shows an error alert dialog with it.
//     *
//     * @param activity activity
//     * @param tag      log tag to use
//     * @param message  message to log and show or {@code null} for none
//     */
//    public void logAndShow(Activity activity, String tag, String message) {
//        Log.e(tag, message);
//        showError(activity, message);
//    }
//
//    /**
//     * Logs the given throwable and shows an error alert dialog with its
//     * message.
//     *
//     * @param activity activity
//     * @param tag      log tag to use
//     * @param t        throwable to log and show
//     */
//    public void logAndShow(Activity activity, String tag, Throwable t) {
//        Log.e(tag, "Error", t);
//        String message = t.getMessage();
//        // Exceptions that occur in your Cloud Endpoint implementation classes
//        // are wrapped as GoogleJsonResponseExceptions
//        if (t instanceof GoogleJsonResponseException) {
//            GoogleJsonError details = ((GoogleJsonResponseException) t)
//                    .getDetails();
//            if (details != null) {
//                message = details.getMessage();
//            }
//        }
//        showError(activity, message);
//    }
//
//    /**
//     * Shows an error alert dialog with the given message.
//     *
//     * @param activity activity
//     * @param message  message to show or {@code null} for none
//     */
//    public void showError(final Activity activity, String message) {
//        final String errorMessage = message == null ? "Error" : "[Error ] "
//                + message;
//        activity.runOnUiThread(new Runnable() {
//            public void run() {
//                Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG)
//                        .show();
//            }
//        });
//    }
}
