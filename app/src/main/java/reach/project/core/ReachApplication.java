package reach.project.core;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.localytics.android.LocalyticsActivityLifecycleCallbacks;

import reach.project.R;

/**
 * Created by ashish on 23/3/15.
 */
public class ReachApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(
                new LocalyticsActivityLifecycleCallbacks(this));
    }

    public synchronized Tracker getTracker() {
        return GoogleAnalytics.getInstance(this).newTracker(R.xml.global_tracker);
    }
}
