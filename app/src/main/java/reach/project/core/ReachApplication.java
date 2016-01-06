package reach.project.core;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.backends.okhttp.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.common.base.Optional;
import com.squareup.okhttp.OkHttpClient;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import reach.project.R;

/**
 * Created by ashish on 23/3/15.
 */
public class ReachApplication extends Application {

    public static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient();

    static {

        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[]{

                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }
        };

        /////////////////ignore ssl errors
        try {
            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            OK_HTTP_CLIENT.setSslSocketFactory(sslSocketFactory);
        } catch (NoSuchAlgorithmException | KeyManagementException ignored) {
            //ignore !!
        }

        OK_HTTP_CLIENT.setHostnameVerifier((hostname, session) -> true);
        /////////////////no http response cache
        OK_HTTP_CLIENT.setCache(null);
        /////////////////redirects and retries
        OK_HTTP_CLIENT.setFollowRedirects(true);
        OK_HTTP_CLIENT.setRetryOnConnectionFailure(true);
        OK_HTTP_CLIENT.setFollowSslRedirects(true);
        /////////////////double the timeouts
        final long connectionTimeOut = OK_HTTP_CLIENT.getConnectTimeout();
        final long readTimeOut = OK_HTTP_CLIENT.getReadTimeout();
        final long writeTimeOut = OK_HTTP_CLIENT.getWriteTimeout();
        OK_HTTP_CLIENT.setConnectTimeout(connectionTimeOut * 2, TimeUnit.MILLISECONDS);
        OK_HTTP_CLIENT.setReadTimeout(readTimeOut * 2, TimeUnit.MILLISECONDS);
        OK_HTTP_CLIENT.setWriteTimeout(writeTimeOut * 2, TimeUnit.MILLISECONDS);
    }

    private static final HitBuilders.EventBuilder EVENT_BUILDER = new HitBuilders.EventBuilder();
    private static final HitBuilders.ScreenViewBuilder SCREEN_VIEW_BUILDER = new HitBuilders.ScreenViewBuilder();

    public synchronized void sendScreenView(Optional<String> screenName) {

        final Tracker tracker = getTracker();
        tracker.setScreenName(screenName.isPresent() ? screenName.get() : "");
        tracker.send(SCREEN_VIEW_BUILDER.build());
    }

    public synchronized void track(@NonNull Optional<String> category,
                                   @NonNull Optional<String> action,
                                   @NonNull Optional<String> label,
                                   int value) {

        //set values
        if (category.isPresent())
            EVENT_BUILDER.setCategory(category.get());

        if (action.isPresent())
            EVENT_BUILDER.setAction(action.get());

        if (label.isPresent())
            EVENT_BUILDER.setLabel(label.get());

        if (value > 0)
            EVENT_BUILDER.setValue(value);

        //send track
        getTracker().send(EVENT_BUILDER.build());

        //reset
        EVENT_BUILDER.setCategory(null);
        EVENT_BUILDER.setAction(null);
        EVENT_BUILDER.setLabel(null);
    }

    @Nullable
    private Tracker mTracker = null;
    @NonNull
    synchronized public Tracker getTracker() {

        if (mTracker == null) {

            final GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            mTracker = analytics.newTracker(R.xml.global_tracker);
        }
        return mTracker;
    }

    @Override
    public void onCreate() {

        super.onCreate();
        //initialize fresco
        final ImagePipelineConfig config = OkHttpImagePipelineConfigFactory.newBuilder(this, OK_HTTP_CLIENT)
                .setDownsampleEnabled(true)
                .setResizeAndRotateEnabledForNetwork(true)
                .build();

        Fresco.initialize(this, config);
    }

//    @Override
//    protected void attachBaseContext(Context base) {
//        super.attachBaseContext(base);
//        MultiDex.install(this);
//    }
}