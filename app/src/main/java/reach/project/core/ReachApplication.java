package reach.project.core;

import android.app.Application;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.common.memory.MemoryTrimType;
import com.facebook.common.memory.MemoryTrimmable;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.common.base.Optional;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import reach.project.R;

/**
 * Created by ashish on 23/3/15.
 */
public class ReachApplication extends Application implements MemoryTrimmableRegistry {

    public static OkHttpClient OK_HTTP_CLIENT;

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
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        /////////////////ignore ssl errors
        try {
            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            clientBuilder.sslSocketFactory(sslSocketFactory);
        } catch (NoSuchAlgorithmException | KeyManagementException ignored) {
            //ignore !!
        }

        clientBuilder.hostnameVerifier((hostname, session) -> true);
        /////////////////no http response cache
        clientBuilder.cache(null);
        /////////////////redirects and retries
        clientBuilder.followRedirects(true);
        clientBuilder.retryOnConnectionFailure(true);
        clientBuilder.followSslRedirects(true);
        OK_HTTP_CLIENT = clientBuilder.build();
        clientBuilder = OK_HTTP_CLIENT.newBuilder();
        /////////////////double the timeouts
        final long connectionTimeOut = OK_HTTP_CLIENT.connectTimeoutMillis();
        final long readTimeOut = OK_HTTP_CLIENT.readTimeoutMillis();
        final long writeTimeOut = OK_HTTP_CLIENT.writeTimeoutMillis();
        clientBuilder.connectTimeout(connectionTimeOut * 2, TimeUnit.MILLISECONDS);
        clientBuilder.readTimeout(readTimeOut * 2, TimeUnit.MILLISECONDS);
        clientBuilder.writeTimeout(writeTimeOut * 2, TimeUnit.MILLISECONDS);
        OK_HTTP_CLIENT = clientBuilder.build();
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
        //initialize bug tracking
//        Crittercism.initialize(this, "552eac3c8172e25e67906922");
        //initialize fresco
        final ImagePipelineConfig.Builder configBuilder = ImagePipelineConfig.newBuilder(this)
                .setDecodeFileDescriptorEnabled(true)
                .setDecodeMemoryFileEnabled(true)
                .setResizeAndRotateEnabledForNetwork(true)
                .setWebpSupportEnabled(true)
                .setBitmapsConfig(Bitmap.Config.RGB_565)
                .setMemoryTrimmableRegistry(this);

        if (Build.VERSION.SDK_INT != 19)
            configBuilder.setDownsampleEnabled(true);

        Fresco.initialize(this, configBuilder.build());
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
    }

    @Override
    public void onLowMemory() {

        super.onLowMemory();
        Fresco.getImagePipeline().clearMemoryCaches();
    }

    @Override
    public void onTrimMemory(int level) {

        super.onTrimMemory(level);
        final Iterator<MemoryTrimmable> iterator = trimmables.iterator();
        while (iterator.hasNext()) {

            final MemoryTrimmable trimmable = iterator.next();
            if (trimmable == null) {
                iterator.remove();
                continue;
            }

            switch (level) {

                case TRIM_MEMORY_BACKGROUND:
                    trimmable.trim(MemoryTrimType.OnAppBackgrounded);
                    break;
                case TRIM_MEMORY_COMPLETE:
                    trimmable.trim(MemoryTrimType.OnCloseToDalvikHeapLimit);
                    break;
                case TRIM_MEMORY_MODERATE:
                    trimmable.trim(MemoryTrimType.OnSystemLowMemoryWhileAppInBackground);
                    break;
                case TRIM_MEMORY_RUNNING_CRITICAL:
                    trimmable.trim(MemoryTrimType.OnCloseToDalvikHeapLimit);
                    break;
                case TRIM_MEMORY_RUNNING_LOW:
                    trimmable.trim(MemoryTrimType.OnSystemLowMemoryWhileAppInForeground);
                    break;
                case TRIM_MEMORY_RUNNING_MODERATE:
                    trimmable.trim(MemoryTrimType.OnSystemLowMemoryWhileAppInForeground);
                    break;
                case TRIM_MEMORY_UI_HIDDEN:
                    trimmable.trim(MemoryTrimType.OnAppBackgrounded);
                    break;
            }
        }
    }

    private final Set<MemoryTrimmable> trimmables = new HashSet<>();

    @Override
    public void registerMemoryTrimmable(MemoryTrimmable trimmable) {
        trimmables.add(trimmable);
    }

    @Override
    public void unregisterMemoryTrimmable(MemoryTrimmable trimmable) {
        trimmables.remove(trimmable);
    }
}