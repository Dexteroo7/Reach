package reach.project.core;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * Created by dexter on 21/6/14.
 */

public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {

    /**
     * Receiver GCM Broadcasts
     * @param context
     * @param intent
     */
    public void onReceive(Context context, Intent intent) {
        // Explicitly specify that GcmIntentService will handle the intent.
        final ComponentName componentName = new ComponentName(context.getPackageName(),
                GcmIntentService.class.getName());
        // Start the service, keeping the device awake while it is launching.
        context.startService(intent.setComponent(componentName));
        setResultCode(Activity.RESULT_OK);
    }
}