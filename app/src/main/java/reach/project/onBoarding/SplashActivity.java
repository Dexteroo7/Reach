package reach.project.onBoarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

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
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.splashLayout, CodeVerification.newInstance((1000 + random.nextInt(10000 - 1000 + 1)) + "", phoneNumber)).commit();
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
}