package reach.project.onBoarding;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.common.base.Optional;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;

import reach.backend.entities.userApi.model.MyString;
import reach.backend.entities.userApi.model.OldUserContainerNew;
import reach.project.R;
import reach.project.ancillaryViews.UpdateFragment;
import reach.project.core.ReachActivity;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.utils.FireOnce;
import reach.project.utils.MetaDataScanner;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

/**
 * Created by ashish on 19/10/15.
 */
public class SplashActivity extends AppCompatActivity implements SplashInterface {

    private static WeakReference<SplashActivity> activityWeakReference;
    private static WeakReference<Context> contextWeakReference;

    private static final int MY_PERMISSIONS_READ_CONTACTS = 11;
    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 22;

    private static final ExecutorService checkUpdateService = MiscUtils.getRejectionExecutor();
    private static final ExecutorService checkGCMService = MiscUtils.getRejectionExecutor();

    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        activityWeakReference = new WeakReference<>(this);
        contextWeakReference = new WeakReference<>(getApplication());

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
    public void onOpenNumberVerification() {
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.splashLayout, NumberVerification.newInstance()).commit();
    }

    @Override
    public void onOpenCodeVerification(String phoneNumber) {

        final String randomCode = (1000 + random.nextInt(10000 - 1000 + 1)) + "";
        Log.i("Ayush", randomCode + " code");
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.splashLayout, CodeVerification.newInstance(randomCode, phoneNumber)).commit();
    }

    @Override
    public void onOpenAccountCreation(Optional<OldUserContainerNew> container) {
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.splashLayout, AccountCreation.newInstance(container)).commit();
    }

    @Override
    public void onOpenScan(String name, String imageFilePath, String imageId, String phoneNumber) {
        getSupportFragmentManager().beginTransaction().replace(R.id.splashLayout,
                ScanFragment.newInstance(name, imageFilePath, imageId, phoneNumber)).commit();
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

        final Handler handler = new Handler();
        final SharedPreferences preferences = getSharedPreferences("Reach", MODE_PRIVATE);

        final String userName = SharedPrefUtils.getUserName(preferences);
        final String phoneNumber = SharedPrefUtils.getPhoneNumber(preferences);
        final long serverId = SharedPrefUtils.getServerId(preferences);

        //track screen
        final Tracker tracker = ((ReachApplication) getApplication()).getTracker();
        tracker.setScreenName("reach.project.core.ReachActivity");
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
                FireOnce.contactSync(this);
                //check for update
                new CheckUpdate().executeOnExecutor(checkUpdateService);
                //refresh gcm
                checkGCMService.submit(() -> checkGCM(serverId));
                //refresh download ops
                FireOnce.refreshOperations(contextWeakReference);
                //Music scanner
                final Intent intent = new Intent(this, MetaDataScanner.class);
                intent.putExtra("first", false);
                startService(intent);

            } else
                Toast.makeText(this, "No active networks detected", Toast.LENGTH_SHORT).show();

            handler.postDelayed(OPEN_APP_SPLASH, 2000L);
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

    public static void checkGCM(long serverId) {

        if (serverId == 0)
            return;

        final MyString dataToReturn = MiscUtils.autoRetry(() -> StaticData.USER_API.getGcmId(serverId).execute(), Optional.absent()).orNull();

        //check returned gcm
        final String gcmId;
        if (dataToReturn == null || //fetch failed
                TextUtils.isEmpty(gcmId = dataToReturn.getString()) || //null gcm
                gcmId.equals("hello_world")) { //bad gcm

            //network operation
            if (MiscUtils.updateGCM(serverId, contextWeakReference))
                Log.i("Ayush", "GCM updated !");
            else
                Log.i("Ayush", "GCM check failed");
        }
    }

    private static class CheckUpdate extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {

            Scanner reader = null;
            try {
                reader = new Scanner(new URL(StaticData.DROP_BOX).openStream());
                return reader.nextInt();
            } catch (Exception ignored) {
            } finally {

                if (reader != null) {
                    reader.close();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer result) {

            super.onPostExecute(result);

            if (result == null)
                return;

            Log.i("Ayush", "LATEST VERSION " + result);

            MiscUtils.useContextFromContext(activityWeakReference, activity -> {

                final int version;
                try {
                    version = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    return;
                }

                if (version < result) {

                    final UpdateFragment updateFragment = new UpdateFragment();
                    updateFragment.setCancelable(false);
                        try {
                            updateFragment.show(activity.getSupportFragmentManager(), "update");
                        } catch (IllegalStateException | WindowManager.BadTokenException ignored) {
                            activity.finish();
                        }
                }
            });
        }
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

        activity.getWindow().setBackgroundDrawableResource(R.color.white);
        activity.setContentView(R.layout.activity_splash);

        final FragmentManager fragmentManager = activity.getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.splashLayout,
                WelcomeFragment.newInstance()).commit();
    });
}