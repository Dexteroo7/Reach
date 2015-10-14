package reach.project.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by ashish on 14/10/15.
 */
public class InstallTrackersReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        new com.appvirality.android.AppviralityInstallReferrerReceiver().onReceive(context, intent);
        new com.mixpanel.android.mpmetrics.InstallReferrerReceiver().onReceive(context, intent);
        new com.google.android.gms.analytics.CampaignTrackingReceiver().onReceive(context, intent);
        // Now you can pass the same intent on to other services
    }
}