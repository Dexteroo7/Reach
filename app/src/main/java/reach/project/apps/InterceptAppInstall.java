package reach.project.apps;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

/**
 * TODO reduce frequency, only download filter, do not notify on hidden apps ?
 */
public class InterceptAppInstall extends BroadcastReceiver {

    private static final int FOUND_NEW_NOTIFICATION = 1;

    @Nullable
    private static WeakReference<Context> reference = null;

    @Override
    public void onReceive(Context context, Intent intent) {

        reference = new WeakReference<>(context);
        Log.i("Ayush", intent.getAction() + " ACTION");
        if (intent.getBooleanExtra("android.intent.extra.REPLACING", false))
            return;

        final PackageManager packageManager = context.getPackageManager();
        final List<ApplicationInfo> applicationInfoList = MiscUtils.getInstalledApps(packageManager);
        if (applicationInfoList == null || applicationInfoList.isEmpty())
            return;

        //Iterate installed packages to find out the intercepted package package
        final String findPackageName = intent.getDataString();
        @Nullable
        ApplicationInfo interceptedApplication = null;

        for (ApplicationInfo applicationInfo : applicationInfoList)
            if (findPackageName.contains(applicationInfo.packageName)) {
                interceptedApplication = applicationInfo;
                break;
            }

        //weird
        if (interceptedApplication == null)
            return;

        //get list of hidden packages
        final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        final Map<String, Boolean> packageVisibilities = SharedPrefUtils.getPackageVisibilities(preferences);

        //construct the application object
        final App.Builder appBuilder = new App.Builder();
        appBuilder.launchIntentFound(packageManager.getLaunchIntentForPackage(interceptedApplication.packageName) != null);
        appBuilder.applicationName(interceptedApplication.loadLabel(packageManager) + "");
        appBuilder.description(interceptedApplication.loadDescription(packageManager) + "");
        appBuilder.packageName(interceptedApplication.packageName);
        appBuilder.processName(interceptedApplication.processName);

        try {
            appBuilder.installDate(
                    packageManager.getPackageInfo(interceptedApplication.packageName, 0).firstInstallTime);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //Probability that network is absent when a package install intent is received is very low
        if (!MiscUtils.isOnline(context))
            return;

        //update backend
        if (packageVisibilities.containsKey(appBuilder.packageName)) {

            //found old visibility
            if (packageVisibilities.get(appBuilder.packageName))
                new UpdateVisibility().execute(appBuilder, SharedPrefUtils.getServerId(preferences), VISIBLE);
            else
                new UpdateVisibility().execute(appBuilder, SharedPrefUtils.getServerId(preferences), NOT_VISIBLE);
        } else
            new UpdateVisibility().execute(appBuilder, SharedPrefUtils.getServerId(preferences), NOT_DEFINED);

    }

    private static final byte VISIBLE = 0;
    private static final byte NOT_VISIBLE = 1;
    private static final byte NOT_DEFINED = 2;

    private final class UpdateVisibility extends AsyncTask<Object, Void, App> {

        @Override
        protected App doInBackground(Object... params) {

            if (params == null || params.length != 3)
                throw new IllegalArgumentException("Insufficient argument given");

            if (!(params[0] instanceof App.Builder && params[1] instanceof Long && params[2] instanceof Byte))
                throw new IllegalArgumentException("Given arguments of unexpected type");

            final App.Builder appBuilder = (App.Builder) params[0];
            final long serverId = (long) params[1];
            final byte status = (byte) params[2];

            final String packageName = appBuilder.packageName;
            final boolean visibility;

            /**
             * TODO combine all server calls into 1 and ADD the new package to blob
             */

            if (status == NOT_DEFINED) {

                visibility = true;
                /*
                 *first try from online persistence
                 */
                //TODO
//                final JsonMap visibilityMap = MiscUtils.autoRetry(() ->
//                        StaticData.APP_VISIBILITY_API.get(serverId).execute().getVisibility(), Optional.absent()).orNull();
//
//                if (visibilityMap != null && visibilityMap.containsKey(packageName))
//                    visibility = (boolean) visibilityMap.get(packageName);
//                else {
//
//                    /**
//                     * new package, get the default visibility
//                     */
//                    final StringList stringList = new StringList();
//                    stringList.setUserId(serverId);
//                    stringList.setStringList(Collections.singletonList(appBuilder.packageName));
//
//                    final List<String> hiddenPackageList;
//                    final StringList hiddenPackages = MiscUtils.autoRetry(() -> StaticData.CLASSIFIED_APPS_API.getDefaultState(
//                            stringList).execute(), Optional.absent()).orNull();
//                    visibility = !(hiddenPackages != null &&
//                            (hiddenPackageList = hiddenPackages.getStringList()) != null &&
//                            !hiddenPackageList.isEmpty() && hiddenPackageList.contains(appBuilder.packageName));
//                }


            } else if (status == VISIBLE)
                visibility = true;
            else if (status == NOT_VISIBLE)
                visibility = false;
            else
                throw new IllegalArgumentException("Unexpected byte constant found " + status);

            appBuilder.visible(visibility);

            //TODO
//            MiscUtils.autoRetry(() -> StaticData.APP_VISIBILITY_API.update(
//                    serverId,
//                    appBuilder.packageName,
//                    appBuilder.visible).execute(), Optional.absent());

            //TODO add the new package

            return appBuilder.build();
        }

        @Override
        protected void onPostExecute(App app) {

            super.onPostExecute(app);

            MiscUtils.useContextFromContext(reference, context -> {

                final Intent openMyProfileApps = ReachActivity.getIntent(context);
                openMyProfileApps.setAction(ReachActivity.OPEN_MY_PROFILE_APPS);

                final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                        .setContentTitle(app.applicationName)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setOnlyAlertOnce(true)
                        .setSmallIcon(R.drawable.icon_notification)
                        .setContentIntent(PendingIntent.getActivity(context, 0, openMyProfileApps, PendingIntent.FLAG_CANCEL_CURRENT));

                if (app.visible)
                    notificationBuilder.setContentText("is visible to your friends");
                else
                    notificationBuilder.setContentText("is not visible to your friends");

                try {
                    notificationBuilder.setLargeIcon(((BitmapDrawable) context.getPackageManager().getApplicationIcon(app.packageName)).getBitmap());
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(FOUND_NEW_NOTIFICATION, notificationBuilder.build());

                final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
                SharedPrefUtils.addPackageVisibility(preferences, app.packageName, app.visible);
            });
        }
    }
}
