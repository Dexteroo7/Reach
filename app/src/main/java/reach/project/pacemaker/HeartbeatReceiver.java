package reach.project.pacemaker;

/**
 * Created by dexter on 02/10/15.
 */

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import reach.project.usageTracking.PostParams;
import reach.project.usageTracking.UsageTracker;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

/**
 * Created by Raveesh on 23/07/15.
 */
public class HeartbeatReceiver extends BroadcastReceiver {

    private static final Intent GTALK_HEART_BEAT_INTENT = new Intent("com.google.android.intent.action.GTALK_HEARTBEAT");
    private static final Intent MCS_MCS_HEARTBEAT_INTENT = new Intent("com.google.android.intent.action.MCS_HEARTBEAT");

    private static WeakReference<Context> reference = null;
    private static long serverId = 0;

    @Override
    public void onReceive(Context context, Intent intent) {

        context.sendBroadcast(GTALK_HEART_BEAT_INTENT);
        context.sendBroadcast(MCS_MCS_HEARTBEAT_INTENT);
        Log.v("Heartbeater", "HeartbeatReceiver sent heartbeat request");
        scheduleNext(context, intent);
        reference = new WeakReference<>(context);
        serverId = SharedPrefUtils.getServerId(context.getSharedPreferences("Reach", Context.MODE_PRIVATE));

        trackApplications();
    }

    /**
     * Schedules the next heartbeat when required
     *
     * @param context Context from the broadcast receiver onReceive
     * @param intent  Intent from the broadcast receiver onReceive
     */
    private void scheduleNext(Context context, Intent intent) {

        final int type = intent.getIntExtra(Pacemaker.KEY_TYPE, Pacemaker.TYPE_LINEAR);
        if (type == Pacemaker.TYPE_EXPONENTIAL) {

            int delay = intent.getIntExtra(Pacemaker.KEY_DELAY, 5);
            delay = delay * 2;
            int max = intent.getIntExtra(Pacemaker.KEY_MAX, 60);
            if (delay > max) {
                Log.d("Heartbeater", "Killing Heartbeater as delay now exceeds max");
                return;
            }
            Pacemaker.scheduleExponential(context, delay, max);
        } else {
            Log.d("Heartbeater", "Ignored linear schedule request since it should already be there");
        }
    }

    public static void trackApplications() {

        final List<String> openPackages = new ArrayList<>();

        MiscUtils.useContextFromContext(reference, context -> {

            final ActivityManager activityManager =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

            if (Build.VERSION.SDK_INT < 21) {

                @SuppressWarnings("deprecation")
                final List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(100);
                for (ActivityManager.RunningTaskInfo info : tasks) {

                    openPackages.add(info.topActivity.getPackageName());
                    Log.i("Ayush", info.topActivity.getPackageName());
                }
            } else {

                final List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo info : processes) {

                    openPackages.add(info.processName);
                    Log.i("Ayush", info.processName + "");
                }
            }
        });

        if (openPackages.isEmpty() || serverId == 0)
            return;

        final Map<PostParams, String> simpleParams = MiscUtils.getMap(6);
        simpleParams.put(PostParams.USER_ID, serverId + "");
        simpleParams.put(PostParams.OS, MiscUtils.getOsName());
        simpleParams.put(PostParams.OS_VERSION, Build.VERSION.SDK_INT + "");
        simpleParams.put(PostParams.SCREEN_NAME, "my_reach");

        MiscUtils.useContextFromContext(reference, context -> {

            simpleParams.put(PostParams.DEVICE_ID, MiscUtils.getDeviceId(context));

            try {
                simpleParams.put(PostParams.APP_VERSION,
                        context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

        });

        try {
            UsageTracker.trackOpenApplications(simpleParams, openPackages, UsageTracker.ACTIVE_APPLICATIONS);
        } catch (JSONException ignored) {
        }
    }
}

