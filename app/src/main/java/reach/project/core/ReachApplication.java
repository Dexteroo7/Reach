package reach.project.core;

import android.app.Application;

import com.firebase.client.Firebase;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import reach.project.R;
import reach.project.pacemaker.Pacemaker;

/**
 * Created by ashish on 23/3/15.
 */
public class ReachApplication extends Application {

    public synchronized Tracker getTracker() {
        return GoogleAnalytics.getInstance(this).newTracker(R.xml.global_tracker);
    }

    @Override
    public void onCreate() {

        super.onCreate();
        Firebase.setAndroidContext(this);
        Pacemaker.scheduleLinear(this, 5);
    }

    /*@Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        ////meant for release
        MultiDex.install(this);
    }*/
}
