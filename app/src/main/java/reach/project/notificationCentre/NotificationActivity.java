package reach.project.notificationCentre;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;

import reach.project.R;
import reach.project.utils.MiscUtils;

public class NotificationActivity extends AppCompatActivity {

    public static final String OPEN_REQUESTS = "OPEN_REQUESTS";
    public static final String OPEN_NOTIFICATIONS = "OPEN_NOTIFICATIONS";

    public static void openActivity(Context context, String tab) {

        final Intent intent = new Intent(context, NotificationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("tab", tab);
        context.startActivity(intent);
    }

    public static Intent getIntent(Context context, String tab) {

        final Intent intent = new Intent(context, NotificationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("tab", tab);
        return intent;
    }

    @Override
    public void onBackPressed() {
        MiscUtils.navigateUp(this);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite);

        final Toolbar mToolbar = (Toolbar) findViewById(R.id.inviteToolbar);
        mToolbar.setTitle("Notification Center");
        mToolbar.setNavigationOnClickListener(v -> onBackPressed());
        //TODO: Check which menu items are not required and remove them
        mToolbar.inflateMenu(R.menu.notification_menu);
        mToolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.clear_button:
                    NotificationFragment.clearNotifications();
                    return true;
                default:
                    return false;
            }
        });

        final ViewPager viewPager = (ViewPager) findViewById(R.id.invitePager);
        viewPager.setAdapter(pagerAdapter);
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.inviteTabLayout);
        tabLayout.setupWithViewPager(viewPager);
        final MenuItem clearIcon = mToolbar.getMenu().findItem(R.id.clear_button);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout) {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                clearIcon.setVisible(position == 0);
            }
        });

        final Intent intent = getIntent();
        if (intent == null)
            return;
        final String tab = intent.getStringExtra("tab");
        if (TextUtils.isEmpty(tab))
            return;
        switch (tab) {
            case OPEN_NOTIFICATIONS:
                viewPager.setCurrentItem(0);
                break;
            case OPEN_REQUESTS:
                viewPager.setCurrentItem(1);
                break;
        }
    }

    private final FragmentPagerAdapter pagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return NotificationFragment.newInstance();
                case 1:
                    return FriendRequestFragment.newInstance();
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
                    return "Notifications";
                case 1:
                    return "Requests";
                default:
                    throw new IllegalStateException("Only size of 2 expected");
            }
        }
    };
}
