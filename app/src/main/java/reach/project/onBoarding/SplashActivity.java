package reach.project.onBoarding;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Toast;

import com.google.common.base.Optional;

import java.lang.ref.WeakReference;
import java.util.Random;

import reach.backend.entities.userApi.model.OldUserContainerNew;
import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.ancillaryClasses.UseActivity;

/**
 * Created by ashish on 19/10/15.
 */
public class SplashActivity extends AppCompatActivity implements SplashInterface {

    private static String userName, phoneNumber;
    private static long serverId;
    private static WeakReference<SplashActivity> reference;

    private static final int MY_PERMISSIONS_READ_CONTACTS = 11;
    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 22;

    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        reference = new WeakReference<>(this);

        final SharedPreferences preferences = getSharedPreferences("Reach", MODE_PRIVATE);

        userName = SharedPrefUtils.getUserName(preferences);
        phoneNumber = SharedPrefUtils.getPhoneNumber(preferences);
        serverId = SharedPrefUtils.getServerId(preferences);

        new Handler().postDelayed(splashed, 1000);
    }

    private static final Runnable splashed = new Runnable() {
        @Override
        public void run() {

            MiscUtils.useActivity(reference, new UseActivity<SplashActivity>() {
                @Override
                public void work(SplashActivity activity) {

                    if (TextUtils.isEmpty(userName)) {

                        activity.getWindow().setBackgroundDrawableResource(R.color.white);
                        activity.setContentView(R.layout.activity_splash);

                        final FragmentManager fragmentManager = activity.getSupportFragmentManager();
                        if (serverId == 0 || TextUtils.isEmpty(phoneNumber))
                            fragmentManager.beginTransaction().replace(R.id.splashLayout,
                                    WelcomeFragment.newInstance()).commit();
                        else
                            fragmentManager.beginTransaction().replace(R.id.splashLayout,
                                    AccountCreation.newInstance(Optional.absent())).commit(); //ignore oldContainer
                        if (Build.VERSION.SDK_INT >= 23) {

                            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS) != 0) {

                                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CONTACTS))
                                    Toast.makeText(activity, "Permission to access Contacts is required to use the App", Toast.LENGTH_SHORT).show();
                                ActivityCompat.requestPermissions(activity,
                                        new String[]{
                                                Manifest.permission.READ_CONTACTS
                                        }, MY_PERMISSIONS_READ_CONTACTS);
                            } else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != 0) {

                                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                                    Toast.makeText(activity, "Permission to access Storage is required to use the App", Toast.LENGTH_SHORT).show();
                                ActivityCompat.requestPermissions(activity,
                                        new String[]{
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        }, MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
                            }
                        }

                    } else {

                        final Intent intent = new Intent(activity, ReachActivity.class);
                        activity.startActivity(intent);
                        activity.finish();
                    }
                }
            });

        }
    };

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
                finish();
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != 0) {

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        Toast.makeText(this, "Permission to access Storage is required to use the App", Toast.LENGTH_SHORT).show();
                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                            }, MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
                }

            }
        } else if (requestCode == MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE) {
            if (!(grantResults.length > 0 && grantResults[0] == 0)) {
                Toast.makeText(this,
                        "Permission to access Storage is required to use the App",
                        Toast.LENGTH_LONG).show();
                finish();
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != 0) {

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS))
                        Toast.makeText(this, "Permission to access Contacts is required to use the App", Toast.LENGTH_SHORT).show();
                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    Manifest.permission.READ_CONTACTS
                            }, MY_PERMISSIONS_READ_CONTACTS);
                }
            }
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}