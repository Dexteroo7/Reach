package reach.project.onBoarding;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import com.appspot.able_door_616.userApi.model.UserDataPersistence;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.common.base.Optional;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import reach.project.R;
import reach.project.apps.App;
import reach.project.core.ReachActivity;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.coreViews.myProfile.MyProfileFragment;
import reach.project.pacemaker.Pacemaker;
import reach.project.utils.FireOnce;
import reach.project.utils.MetaDataScanner;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.ThreadLocalRandom;

/**
 * Created by ashish on 19/10/15.
 */
public class SplashActivity extends AppCompatActivity implements SplashInterface {

    @Nullable
    private static WeakReference<SplashActivity> activityWeakReference = null;
    @Nullable
    private static WeakReference<Context> contextWeakReference = null;

    private static final int MY_PERMISSIONS_READ_CONTACTS = 11;
    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 22;
    private static final String TAG = SplashActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //gcm keep-alive
        Pacemaker.scheduleLinear(this, 5);

        activityWeakReference = new WeakReference<>(this);
        contextWeakReference = new WeakReference<>(getApplication());

            List<App> apps = (List<App>) ReachApplication.readCachedFile(SplashActivity.this, StaticData.APP_DATA_CACHE_KEY);
        if(apps==null){
            new GetApplications(SplashActivity.this).execute(this);
        }

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        StaticData.deviceHeight = height;
        StaticData.deviceWidth = width;

        // Checking if the user has given permission to read contacts on API 23(marshmallow) or greater
        if (Build.VERSION.SDK_INT >= 23) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != 0) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS))
                    Toast.makeText(this, "Permission to access Contacts is required to use the App", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_CONTACTS
                        }, MY_PERMISSIONS_READ_CONTACTS);
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != 0) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    Toast.makeText(this, "Permission to access Storage is required to use the App", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        }, MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
            } else //all granted
                proceedAfterChecks();
        } else //no issue of permissions
            proceedAfterChecks();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_READ_CONTACTS) {

            if (!(grantResults.length > 0 && grantResults[0] == 0)) {
                Toast.makeText(this,
                        "Permission to access Contacts is required to use the App",
                        Toast.LENGTH_LONG).show();
                //TODO track
                finish();
            } else {

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != 0) {

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        Toast.makeText(this, "Permission to access Storage is required to use the App", Toast.LENGTH_SHORT).show();
                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                            }, MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
                } else //all granted
                    proceedAfterChecks();
            }
        } else if (requestCode == MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE) {

            if (!(grantResults.length > 0 && grantResults[0] == 0)) {

                Toast.makeText(this,
                        "Storage permission is required to use the App",
                        Toast.LENGTH_LONG).show();
                //TODO track
                finish();
            } else {

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != 0) {

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS))
                        Toast.makeText(this, "Permission to access Contacts is required to use the App", Toast.LENGTH_SHORT).show();
                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    Manifest.permission.READ_CONTACTS
                            }, MY_PERMISSIONS_READ_CONTACTS);
                } else //all granted
                    proceedAfterChecks();
            }
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void proceedAfterChecks() {

        final String chosenEmail = SharedPrefUtils.getEmailId(getSharedPreferences("Reach", MODE_PRIVATE));
        if (TextUtils.isEmpty(chosenEmail)) {

            final Account[] accounts = AccountManager.get(this).getAccountsByType("com.google");
            if (accounts.length == 0) {

                Toast.makeText(this, "You need at-least 1 Google account signed-in to use the app", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            SharedPrefUtils.storeEmailId(getSharedPreferences("Reach", MODE_PRIVATE), accounts[0].name);
        }
        Log.i("Ayush", "Chosen account = " + chosenEmail);

        final Handler handler = new Handler();
        final SharedPreferences preferences = getSharedPreferences("Reach", MODE_PRIVATE);
        final String userName = SharedPrefUtils.getUserName(preferences);
        final String phoneNumber = SharedPrefUtils.getPhoneNumber(preferences);
        final long serverId = SharedPrefUtils.getServerId(preferences);

        //For google analytics
        //track screen
        final Tracker tracker = ((ReachApplication) getApplication()).getTracker();
        tracker.setScreenName("reach.project.onBoarding.SplashActivity");
        tracker.set("&uid", serverId + "");
        tracker.send(new HitBuilders.ScreenViewBuilder().setCustomDimension(1, serverId + "").build());

        //first check playServices
        if (!checkPlayServices(this)) {
            tracker.send(new HitBuilders.EventBuilder("Play Services screwup", userName + " " + phoneNumber + " screwed up").build());
            return; //fail
        }

        if (TextUtils.isEmpty(userName)) {

            if (serverId == 0 || TextUtils.isEmpty(phoneNumber))
                handler.postDelayed(WELCOME_SPLASH, 700L);
            else
                handler.postDelayed(ACCOUNT_CREATION_SPLASH, 700L);
        } else {

            //perform other ops
            if (MiscUtils.isOnline(this)) {

                //TODO chat check
                //perform contact sync
                FireOnce.contactSync(contextWeakReference, serverId, phoneNumber);
                //refresh gcm
                FireOnce.checkGCM(contextWeakReference, serverId);
                //refresh download ops
                FireOnce.refreshOperations(contextWeakReference);

                final Intent intent = new Intent(this, MetaDataScanner.class);
                startService(intent);

            } else
                Toast.makeText(this, "No active networks detected", Toast.LENGTH_SHORT).show();

            handler.postDelayed(OPEN_APP_SPLASH, 2000L);
        }
    }

    @Override
    public void onOpenNumberVerification() {
        if (isFinishing())
            return;
        try {
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                    R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.splashLayout, NumberVerification.newInstance()).commit();
        } catch (IllegalStateException ignored) {
        }

    }

    @Override
    public void onOpenCodeVerification(String phoneNumber, String countryCode) {
        if (isFinishing())
            return;
        try {
            final String randomCode = ThreadLocalRandom.current().nextInt(1000, 10000) + "";
            Log.i("Ayush", randomCode + " code");
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                    R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.splashLayout, CodeVerification.newInstance(randomCode, phoneNumber, countryCode)).commit();
        } catch (IllegalStateException ignored) {
            finish();
        }
    }

    @Override
    public void onOpenAccountCreation(Optional<UserDataPersistence> container) {

        if (isFinishing())
            return;
        try {
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                    R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.splashLayout, AccountCreation.newInstance(container)).commit();
        } catch (IllegalStateException ignored) {
            finish();
        }
    }

    @Override
    public void onOpenScan(String name,
                           long oldUserId,
                           String oldProfilePicId,
                           String oldCoverPicId,
                           Uri newProfilePicUri,
                           Uri newCoverPicUri) {

        if (isFinishing())
            return;
        try {
            getSupportFragmentManager().beginTransaction().replace(R.id.splashLayout,
                    ScanFragment.newInstance(
                            name,
                            oldUserId,
                            oldProfilePicId,
                            oldCoverPicId,
                            newProfilePicUri,
                            newCoverPicUri)).commit();
        } catch (IllegalStateException ignored) {
            finish();
        }
    }

    @Override
    public void onOpenPrivacySettings() {

        if (isFinishing())
            return;
        try {
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                    R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.splashLayout, new MyProfileFragment()).commit();
        } catch (IllegalStateException ignored) {
            finish();
        }

    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     *
     * @param activity to use
     * @return true : continue, false : fail
     */
    public static boolean checkPlayServices(Activity activity) {

        final int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (resultCode == ConnectionResult.SUCCESS)
            return true;

        if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {

            GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
                    StaticData.PLAY_SERVICES_RESOLUTION_REQUEST).show();
        } else {

            Toast.makeText(activity, "This device is not supported", Toast.LENGTH_LONG).show();
            Log.i("GCM_UTILS", "This device is not supported.");
            activity.finish();
        }

        return false;
    }

    private static final Runnable ACCOUNT_CREATION_SPLASH = () -> MiscUtils.useActivity(activityWeakReference, activity -> {

        activity.getWindow().setBackgroundDrawableResource(R.color.white);
        activity.setContentView(R.layout.activity_splash);

        final FragmentManager fragmentManager = activity.getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.splashLayout,
                AccountCreation.newInstance(Optional.absent())).commit(); //ignore oldContainer
    });

    private static final Runnable OPEN_APP_SPLASH = () -> MiscUtils.useActivity(activityWeakReference, activity -> {

        activity.getWindow().setBackgroundDrawableResource(R.color.white);
        activity.setContentView(R.layout.activity_splash);

        final Intent intent = new Intent(activity, ReachActivity.class);
        activity.startActivity(intent);
        activity.finish();
    });

    private static final Runnable WELCOME_SPLASH = () -> MiscUtils.useActivity(activityWeakReference, activity -> {

        if (activity.isFinishing())
            return;
        try {
            activity.getWindow().setBackgroundDrawableResource(R.color.white);
            activity.setContentView(R.layout.activity_splash);

            final FragmentManager fragmentManager = activity.getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.splashLayout,
                    WelcomeFragment.newInstance()).commit();
        } catch (IllegalStateException ignored) {
            activity.finish();
        }

    });

    private static final class GetApplications extends AsyncTask<Context, Void, List<App>> {

        private WeakReference<SplashActivity> weakReference;

        public GetApplications(SplashActivity splash) {
            this.weakReference = new WeakReference<>(splash);
        }

        @Override
        protected List<App> doInBackground(Context... params) {
            Log.d(TAG, "doInBackground: Getting Application Data to Store in FileCache");

            final SharedPreferences preferences = params[0].getSharedPreferences("Reach", Context.MODE_PRIVATE);
            final PackageManager packageManager = params[0].getPackageManager();

            final List<App> apps = MiscUtils.getApplications(packageManager, preferences);
            Collections.sort(apps, StaticData.byName);
           /* final List<App> recentApps = Ordering
                    .from(StaticData.byInstallDate)
                    .compound(StaticData.byName)
                    .greatestOf(apps, 20);
            Collections.sort(apps, StaticData.byName);*/
            if(weakReference.get()!=null){
                try {
                    ReachApplication.createCachedFile(weakReference.get(),StaticData.APP_DATA_CACHE_KEY,apps);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "onPostExecute: Couldn't Store Application Data in Cache, error = " + e.toString() );
                }
            }
            return apps;
        }

        @Override
        protected void onPostExecute(List<App> appData) {
            super.onPostExecute(appData);

        }
    }

}