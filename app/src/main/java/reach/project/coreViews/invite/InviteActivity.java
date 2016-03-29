package reach.project.coreViews.invite;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import reach.project.R;
import reach.project.utils.MiscUtils;

public class InviteActivity extends AppCompatActivity {

    //private static WeakReference<InviteActivity> reference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite);

        //reference = new WeakReference<>(this);

        final Toolbar mToolbar = (Toolbar) findViewById(R.id.inviteToolbar);

        mToolbar.setTitle("Invite Friends");
        mToolbar.setNavigationOnClickListener(v -> onBackPressed());

        ViewPager viewPager = (ViewPager) findViewById(R.id.invitePager);
        viewPager.setAdapter(new InvitePagerAdapter(getSupportFragmentManager()));
        TabLayout tabLayout = (TabLayout) findViewById(R.id.inviteTabLayout);
        tabLayout.setupWithViewPager(viewPager);

        //sharedPreferences = getSharedPreferences("Reach", Context.MODE_APPEND);

    }

    @Override
    public void onBackPressed() {
        MiscUtils.navigateUp(this);
    }

    private class InvitePagerAdapter extends FragmentPagerAdapter {

        public InvitePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 1:
                    return AllContactsFragment.newInstance();
                case 0:
                    return InviteFragment.newInstance();
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 1:
                    return "Contacts";
                case 0:
                    return "Others";
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
