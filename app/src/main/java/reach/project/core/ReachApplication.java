package reach.project.core;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.backends.okhttp.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseException;
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

//import android.support.multidex.MultiDex;

/**
 * Created by ashish on 23/3/15.
 */
public class ReachApplication extends Application {

    public static final OkHttpClient okHttpClient;

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

        okHttpClient = new OkHttpClient();

        /////////////////ignore ssl errors
        try {
            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            okHttpClient.setSslSocketFactory(sslSocketFactory);
        } catch (NoSuchAlgorithmException | KeyManagementException ignored) {
            //ignore !!
        }

        okHttpClient.setHostnameVerifier((hostname, session) -> true);
        /////////////////no http response cache
        okHttpClient.setCache(null);
        /////////////////redirects and retries
        okHttpClient.setFollowRedirects(true);
        okHttpClient.setRetryOnConnectionFailure(true);
        okHttpClient.setFollowSslRedirects(true);
        /////////////////double the timeouts
        final long connectionTimeOut = okHttpClient.getConnectTimeout();
        final long readTimeOut = okHttpClient.getReadTimeout();
        final long writeTimeOut = okHttpClient.getWriteTimeout();
        okHttpClient.setConnectTimeout(connectionTimeOut * 2, TimeUnit.MILLISECONDS);
        okHttpClient.setReadTimeout(readTimeOut * 2, TimeUnit.MILLISECONDS);
        okHttpClient.setWriteTimeout(writeTimeOut * 2, TimeUnit.MILLISECONDS);
    }

    private Tracker mTracker;
    private static final HitBuilders.EventBuilder builder = new HitBuilders.EventBuilder();
    private static final HitBuilders.ScreenViewBuilder screenViewBuilder = new HitBuilders.ScreenViewBuilder();

    synchronized public void trackGA(Optional<String> category,
                                     Optional<String> action,
                                     Optional<String> label,
                                     int value) {
        getTracker().send(builder
                .setCategory(category.isPresent() ? category.get() : "")
                .setAction(action.isPresent() ? action.get() : "")
                .setLabel(label.isPresent() ? label.get() : "")
                .setValue(value)
                .build());
        //reset
        builder.setCategory("");
        builder.setAction("");
        builder.setLabel("");
    }

    synchronized public void sendScreenView(Optional<String> screenName) {
        Tracker tracker = getTracker();
        tracker.setScreenName(screenName.isPresent() ? screenName.get() : "");
        tracker.send(screenViewBuilder.build());
    }

    public synchronized void track(@NonNull Optional<String> category,
                                   @NonNull Optional<String> action,
                                   @NonNull Optional<String> label,
                                   int value) {

        //set values
        if (category.isPresent())
            builder.setCategory(category.get());

        if (action.isPresent())
            builder.setAction(action.get());

        if (label.isPresent())
            builder.setLabel(label.get());

        if (value > 0)
            builder.setValue(value);

        //send track
        getTracker().send(builder.build());

        //reset
        builder.setCategory(null);
        builder.setAction(null);
        builder.setLabel(null);
    }

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
        //initialize firebase
        Firebase.setAndroidContext(this);
        try {
            Firebase.getDefaultConfig().setPersistenceEnabled(true);
        } catch (FirebaseException ignored) {
        }
        //initialize fresco
        final ImagePipelineConfig config = OkHttpImagePipelineConfigFactory.newBuilder(this, okHttpClient)
                .setDownsampleEnabled(true)
                .setResizeAndRotateEnabledForNetwork(true)
                .build();

        Fresco.initialize(this, config);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        ////meant for release
        MultiDex.install(this);
    }
}
