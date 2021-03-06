package reach.project.coreViews.invite;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;

public class InviteActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener {
    private Toolbar mToolbar;
    private MenuItem searchViewMenuItem;
    private SearchView searchView;
    private static final int TRIGGER_SERACH =124;

    //private static WeakReference<InviteActivity> reference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite);

        //reference = new WeakReference<>(this);

        mToolbar = (Toolbar) findViewById(R.id.inviteToolbar);
        mToolbar.inflateMenu(R.menu.all_contacts_frag_menu);
        setUpSearchView();

        mToolbar.setTitle("Invite Friends");
        mToolbar.setNavigationOnClickListener(v -> onBackPressed());

        ViewPager viewPager = (ViewPager) findViewById(R.id.invitePager);
        viewPager.setAdapter(new InvitePagerAdapter(getSupportFragmentManager()));
        TabLayout tabLayout = (TabLayout) findViewById(R.id.inviteTabLayout);
        tabLayout.setupWithViewPager(viewPager);
        viewPager.addOnPageChangeListener(this);

        //sharedPreferences = getSharedPreferences("Reach", Context.MODE_APPEND);

    }

    @Override
    public void onBackPressed() {
        MiscUtils.navigateUp(this);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        if(searchViewMenuItem == null){
            return;
        }

        switch (position){
            case 0:

                searchViewMenuItem.setVisible(false);

                break;


            case 1:

                searchViewMenuItem.setVisible(true);

                break;

        }

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }


    private void setUpSearchView() {

        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchViewMenuItem = mToolbar.getMenu().findItem(R.id.search);
        searchView =
                (SearchView) searchViewMenuItem.getActionView();

        //ComponentName componentName = new ComponentName(getContext(), SearchResultsActivity.class);
        searchView.setQueryHint("Search your files");
        //searchView.setSearchableInfo(
        //       searchManager.getSearchableInfo(componentName));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            Bundle bundle = new Bundle();
            Message message = new Message();
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                if(newText == null){
                    return true;
                }

                handler.removeMessages(TRIGGER_SERACH);
                final Message obtained = handler.obtainMessage(TRIGGER_SERACH);
                bundle.putString(StaticData.FILTER_STRING_KEY,newText.toLowerCase());
                if(obtained == null) {
                    message.setData(bundle);
                    message.what = TRIGGER_SERACH;
                    handler.sendMessageDelayed(message, 150);
                }
                else{
                    obtained.setData(bundle);
                    handler.sendMessageDelayed(obtained,150);
                }
                //frag.filter(newText.toLowerCase());


                return true;
            }
        });

        MenuItemCompat.setOnActionExpandListener(searchViewMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return true;
            }
        });
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

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg == null){
                return;
            }
            if (msg.what == TRIGGER_SERACH) {
                Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.invitePager + ":"
                        + 1);
                // based on the current position you can then cast the page to the correct
                // class and call the method:
                if (page != null) {
                    ((AllContactsFragment)page).filter(msg.getData().getString(StaticData.FILTER_STRING_KEY));
                }
            }
        }
    };





}
