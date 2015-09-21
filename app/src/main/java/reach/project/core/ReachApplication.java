package reach.project.core;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import reach.project.R;

/**
 * Created by ashish on 23/3/15.
 */
public class ReachApplication extends Application {

    public synchronized Tracker getTracker() {
        return GoogleAnalytics.getInstance(this).newTracker(R.xml.global_tracker);
    }
//
//    @Override
//    protected void attachBaseContext(Context base) {
//        super.attachBaseContext(base);
//        ////meant for release
//        MultiDex.install(this);
//    }
}
