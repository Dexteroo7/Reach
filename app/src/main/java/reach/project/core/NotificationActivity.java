package reach.project.core;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

import reach.project.R;
import reach.project.coreViews.FriendRequestFragment;
import reach.project.coreViews.NotificationFragment;
import reach.project.viewHelpers.SlidingTabLayout;
import reach.project.viewHelpers.ViewPagerReusable;


public class NotificationActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        final ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setTitle("Notifications");
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(new ViewPagerReusable(
                getSupportFragmentManager(),
                new String[]{"Friend Requests", "Other Notifications"},
                new Fragment[]{
                        FriendRequestFragment.newInstance(),
                        NotificationFragment.newInstance()}));

        SlidingTabLayout slidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        slidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return Color.parseColor("#FFCC0000");
            }
        });
        slidingTabLayout.setViewPager(viewPager);
    }
}
