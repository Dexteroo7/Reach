package reach.project.onBoarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.google.common.base.Optional;

import reach.backend.entities.userApi.model.OldUserContainerNew;
import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.utils.SharedPrefUtils;

/**
 * Created by ashish on 19/10/15.
 */
public class SplashActivity extends AppCompatActivity implements SplashInterface {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences = getSharedPreferences("Reach", MODE_PRIVATE);
        final String userName = SharedPrefUtils.getUserName(preferences);
        final String phoneNumber = SharedPrefUtils.getPhoneNumber(preferences);
        final long serverId = SharedPrefUtils.getServerId(preferences);

        new Handler().postDelayed(() -> {
            if (TextUtils.isEmpty(userName)) {
                getWindow().setBackgroundDrawableResource(R.color.white);
                setContentView(R.layout.activity_splash);
                if (serverId == 0 || TextUtils.isEmpty(phoneNumber))
                    getSupportFragmentManager().beginTransaction().replace(R.id.splashLayout,
                            WelcomeFragment.newInstance()).commit();
                else
                    getSupportFragmentManager().beginTransaction().replace(R.id.splashLayout,
                            AccountCreation.newInstance(Optional.absent())).commit();
            }
            else {
                Intent intent = new Intent(this, ReachActivity.class);
                startActivity(intent);
                finish();
            }
        }, 2000);
    }

    @Override
    public void onOpenNumberVerification() {
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.splashLayout, NumberVerification.newInstance()).commit();
    }

    @Override
    public void onOpenCodeVerification(String key) {
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.splashLayout, CodeVerification.newInstance(key)).commit();
    }

    @Override
    public void onOpenAccountCreation(Optional<OldUserContainerNew> container) {
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.splashLayout, AccountCreation.newInstance(container)).commit();
    }
}