package reach.project.core;

import android.app.Application;

import com.firebase.client.Firebase;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.common.base.Optional;

import reach.project.R;

/**
 * Created by ashish on 23/3/15.
 */
public class ReachApplication extends Application {

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
        Firebase.getDefaultConfig().setPersistenceEnabled(true);
    }
}
