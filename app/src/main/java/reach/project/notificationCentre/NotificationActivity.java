package reach.project.notificationCentre;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import reach.project.R;

public class NotificationActivity extends AppCompatActivity {

    public static void openActivity(Context context) {

        final Intent intent = new Intent(context, NotificationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }

    public static Intent getIntent(Context context) {

        final Intent intent = new Intent(context, NotificationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite);

        final Toolbar mToolbar = (Toolbar) findViewById(R.id.inviteToolbar);
        mToolbar.setTitle("Notifications");
        mToolbar.setNavigationOnClickListener(v -> NavUtils.navigateUpFromSameTask(NotificationActivity.this));

        final ViewPager viewPager = (ViewPager) findViewById(R.id.invitePager);
        viewPager.setAdapter(pagerAdapter);
        ((TabLayout) findViewById(R.id.inviteTabLayout)).setupWithViewPager(viewPager);
    }

    private final FragmentPagerAdapter pagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return FriendRequestFragment.newInstance();
                case 1:
                    return NotificationFragment.newInstance();
                default:
                    throw new IllegalStateException("Only size of 2 expected");
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Requests";
                case 1:
                    return "Notifications";
                default:
                    throw new IllegalStateException("Only size of 2 expected");
            }
        }
    };
}
