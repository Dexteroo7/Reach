package reach.project.notificationCentre;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import reach.project.R;

public class NotificationActivity extends AppCompatActivity {

    //private static WeakReference<InviteActivity> reference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite);

        //reference = new WeakReference<>(this);

        final Toolbar mToolbar = (Toolbar) findViewById(R.id.inviteToolbar);

        mToolbar.setTitle("Notifications");
        mToolbar.setNavigationOnClickListener(v -> {
            NavUtils.navigateUpFromSameTask(NotificationActivity.this);
        });

        ViewPager viewPager = (ViewPager) findViewById(R.id.invitePager);
        viewPager.setAdapter(new InvitePagerAdapter(getSupportFragmentManager()));
        TabLayout tabLayout = (TabLayout) findViewById(R.id.inviteTabLayout);
        tabLayout.setupWithViewPager(viewPager);

        //sharedPreferences = getSharedPreferences("Reach", Context.MODE_APPEND);

    }

    private class InvitePagerAdapter extends FragmentPagerAdapter {

        public InvitePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return FriendRequestFragment.newInstance();
                case 1:
                    return NotificationFragment.newInstance();
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Requests";
                case 1:
                    return "Notifications";
                default:
                    return "";
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
