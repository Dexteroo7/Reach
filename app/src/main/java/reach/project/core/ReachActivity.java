package reach.project.core;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.appsflyer.AppsFlyerLib;
import com.commonsware.cwac.merge.MergeAdapter;
import com.crittercism.app.Crittercism;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.localytics.android.Localytics;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import reach.backend.entities.messaging.model.MyBoolean;
import reach.backend.entities.userApi.model.MyString;
import reach.backend.entities.userApi.model.OldUserContainerNew;
import reach.project.R;
import reach.project.adapter.ReachMusicAdapter;
import reach.project.adapter.ReachQueueAdapter;
import reach.project.coreViews.ContactsChooserFragment;
import reach.project.coreViews.ContactsListFragment;
import reach.project.coreViews.EditProfileFragment;
import reach.project.coreViews.FeedbackFragment;
import reach.project.coreViews.FriendRequestFragment;
import reach.project.coreViews.InviteFragment;
import reach.project.coreViews.NotificationFragment;
import reach.project.coreViews.PrivacyFragment;
import reach.project.coreViews.PromoCodeDialog;
import reach.project.coreViews.PushSongsFragment;
import reach.project.coreViews.UpdateFragment;
import reach.project.coreViews.UploadHistory;
import reach.project.database.contentProvider.ReachDatabaseProvider;
import reach.project.database.contentProvider.ReachSongProvider;
import reach.project.database.sql.ReachDatabaseHelper;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.database.sql.ReachSongHelper;
import reach.project.onBoarding.AccountCreation;
import reach.project.onBoarding.NumberVerification;
import reach.project.reachProcess.auxiliaryClasses.Connection;
import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.reachProcess.reachService.MusicHandler;
import reach.project.reachProcess.reachService.ProcessManager;
import reach.project.userProfile.MusicListFragment;
import reach.project.userProfile.UserMusicLibrary;
import reach.project.utils.MiscUtils;
import reach.project.utils.MusicScanner;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.StringCompress;
import reach.project.utils.auxiliaryClasses.SuperInterface;
import reach.project.utils.auxiliaryClasses.PushContainer;
import reach.project.utils.auxiliaryClasses.ReachDatabase;
import reach.project.utils.auxiliaryClasses.TransferSong;
import reach.project.viewHelpers.CustomViewPager;
import reach.project.viewHelpers.ViewPagerReusable;

public class ReachActivity extends AppCompatActivity implements
        SuperInterface,
        FragmentManager.OnBackStackChangedListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        SearchView.OnQueryTextListener,
        SearchView.OnCloseListener {

    public static long serverId = 0;

    private static WeakReference<ReachActivity> reference = null;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    private Toolbar mToolbar;

    private SharedPreferences preferences;
    private FragmentManager fragmentManager;
    private DrawerLayout mDrawerLayout;
    private SearchView searchView;

    private ReachQueueAdapter queueAdapter = null;
    private ReachMusicAdapter musicAdapter = null;

    private String selectionDownloader, selectionMyLibrary, mCurFilter;
    private String[] selectionArgumentsDownloader;
    private String[] selectionArgumentsMyLibrary;
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private int topPadding;
    private FrameLayout containerFrame;
    private TextView emptyTV1, emptyTV2;
    ////////////////////////////////////////
    private static MusicData currentPlaying;

    ////////////////////////////////////////
    private TextView songNameMinimized, songNameMaximized, artistName, songDuration;
    private TextView playerPos;
    private SeekBar progressBarMaximized;
    private SeekBar progressBarMinimized;
    private ListView queueListView;
    private ImageView shuffleBtn, repeatBtn, pausePlayMaximized, likeButton; //fullscreen
    private CustomViewPager viewPager;

    private MergeAdapter combinedAdapter = null;

    private ImageButton pausePlayMinimized; //small
    private SwipeRefreshLayout downloadRefresh;

    private final SwipeRefreshLayout.OnRefreshListener refreshListener = () ->
            new LocalUtils.RefreshOperations().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    private final SlidingUpPanelLayout.PanelSlideListener slideListener = new SlidingUpPanelLayout.PanelSlideListener() {


        String actionBarTitle = "Reach", actionBarSubtitle = "";
        Drawable actionBarIcon;

        @Override
        public void onPanelSlide(View view, float v) {

            if (v > 0.99f) {
                findViewById(R.id.playerShadow).setVisibility(View.GONE);
                findViewById(R.id.player).setVisibility(View.GONE);
            } else if (v < 0.99f) {
                findViewById(R.id.playerShadow).setVisibility(View.VISIBLE);
                findViewById(R.id.player).setVisibility(View.VISIBLE);
            }
            findViewById(R.id.player).setAlpha(1f - v);
        }

        @Override
        public void onPanelCollapsed(View view) {

            final ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {

                actionBar.setTitle(actionBarTitle);
                actionBar.setSubtitle(actionBarSubtitle);
                actionBar.setIcon(actionBarIcon);
                final Menu mToolbarMenu = mToolbar.getMenu();
                if (searchView != null) {
                    searchView.setQuery(null, false);
                    ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                    searchView = null;
                }
                mToolbarMenu.removeItem(R.id.player_search);
                for (int i = 0; i < mToolbarMenu.size(); i++)
                    mToolbarMenu.getItem(i).setVisible(true);
            }
            if (fragmentManager.getBackStackEntryCount() == 0) {
                setUpDrawer();
                toggleDrawer(false);
            }
        }

        @Override
        public void onPanelExpanded(View view) {

            final ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {

                final Menu mToolbarMenu = mToolbar.getMenu();
                for (int i = 0; i < mToolbarMenu.size(); i++)
                    mToolbarMenu.getItem(i).setVisible(false);
                mToolbar.inflateMenu(R.menu.player_menu);
                searchView = (SearchView) mToolbar.getMenu().findItem(R.id.player_search).getActionView();
                searchView.setOnQueryTextListener(ReachActivity.this);
                searchView.setOnCloseListener(ReachActivity.this);

                if (actionBar.getTitle() != null && !actionBar.getTitle().equals("My Library")) {

                    actionBarTitle = (String) actionBar.getTitle();
                    actionBarSubtitle = (String) actionBar.getSubtitle();
                    actionBarIcon = mToolbar.getLogo();
                    actionBar.setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
                    actionBar.setTitle("My Library");
                    actionBar.setSubtitle("");
                    actionBar.setIcon(0);
                }
            }

            if (fragmentManager.getBackStackEntryCount() == 0)
                toggleDrawer(true);
        }

        @Override
        public void onPanelAnchored(View view) {
        }

        @Override
        public void onPanelHidden(View view) {
        }
    };


    private final AbsListView.OnScrollListener scrollListener = new AbsListView.OnScrollListener() {

        @Override
        public void onScrollStateChanged(AbsListView absListView, int i) {
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            boolean enable = false;
            if (view.getChildCount() > 0) {
                final boolean firstItemVisible = view.getFirstVisiblePosition() == 0;
                final boolean topOfFirstItemVisible = view.getChildAt(0).getTop() == 0;
                enable = firstItemVisible && topOfFirstItemVisible;
            }

            downloadRefresh.setEnabled(enable);
        }
    };


    private View.OnClickListener navHeaderClickListener = v -> onOpenProfile();

    private View.OnClickListener footerClickListener = v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=reach.project")));

    private NavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem menuItem) {
            //Closing drawer on item click
            mDrawerLayout.closeDrawers();

            //Check to see which item was being clicked and perform appropriate action
            switch (menuItem.getItemId()) {

                case R.id.navigation_item_1:
                    fragmentManager
                            .beginTransaction()
                            .addToBackStack(null)
                            .replace(R.id.container, PrivacyFragment.newInstance(false), "privacy_fragment").commit();
                    return true;
                case R.id.navigation_item_2:
                    PromoCodeDialog promoCodeDialog = PromoCodeDialog.newInstance();
                    if (promoCodeDialog.isAdded())
                        promoCodeDialog.dismiss();
                    promoCodeDialog.show(fragmentManager, "promo_dialog");
                    return true;
                case R.id.navigation_item_3:
                    fragmentManager
                            .beginTransaction()
                            .addToBackStack(null)
                            .replace(R.id.container, InviteFragment.newInstance(), "invite_fragment").commit();
                    return true;
                case R.id.navigation_item_4:
                    fragmentManager
                            .beginTransaction()
                            .addToBackStack(null)
                            .replace(R.id.container, UploadHistory.newUploadInstance(), "upload_history").commit();
                    return true;
                case R.id.navigation_item_5:
                    fragmentManager
                            .beginTransaction()
                            .addToBackStack(null)
                            .replace(R.id.container, FeedbackFragment.newInstance(), "feedback_fragment").commit();
                    return true;
                default:
                    return true;

            }
        }
    };

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (reference != null)
            reference.clear();
        reference = null;

        if (searchView != null) {
            searchView.setOnQueryTextListener(null);
            searchView.setOnCloseListener(null);
            searchView.setQuery(null, false);
        }
        searchView = null;

        getLoaderManager().destroyLoader(StaticData.DOWNLOAD_LOADER);
        getLoaderManager().destroyLoader(StaticData.MY_LIBRARY_LOADER);
        if (queueAdapter != null &&
                queueAdapter.getCursor() != null &&
                !queueAdapter.getCursor().isClosed())
            queueAdapter.getCursor().close();
        if (musicAdapter != null &&
                musicAdapter.getCursor() != null &&
                !musicAdapter.getCursor().isClosed())
            musicAdapter.getCursor().close();

        combinedAdapter = null;
        queueAdapter = null;
        musicAdapter = null;
        mDrawerLayout = null;
        slidingUpPanelLayout = null;
        currentPlaying = null;
        songNameMinimized = songNameMaximized = artistName = songDuration = playerPos = null;
        progressBarMinimized = progressBarMaximized = null;
        queueListView = null;
        shuffleBtn = repeatBtn = pausePlayMaximized = null;
        pausePlayMinimized = null;
        downloadRefresh = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (isFinishing())
            return super.onOptionsItemSelected(item);
        final int id = item.getItemId();
        try {
            switch (id) {

                case android.R.id.home: {

                    if (slidingUpPanelLayout != null &&
                            slidingUpPanelLayout.getPanelState() == PanelState.EXPANDED) {
                        onBackPressed();
                    } else {
                        if (fragmentManager.getBackStackEntryCount() > 0)
                            fragmentManager.popBackStack();
                        else if ((fragmentManager.getBackStackEntryCount() == 0) && (!mDrawerLayout.isDrawerOpen(Gravity.LEFT))) {
                            mDrawerLayout.openDrawer(Gravity.LEFT);
                        }
                    }
                    return true;
                }
            }
        } catch (IllegalStateException ignored) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {

        super.onPause();

        final PackageManager packageManager;
        if ((packageManager = getPackageManager()) == null)
            return;
        packageManager.setComponentEnabledSetting(
                new ComponentName(this, PlayerUpdateListener.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    @Override
    protected void onResume() {

        //TODO onResume is called twice sometimes
        currentPlaying = SharedPrefUtils.getLastPlayed(getSharedPreferences("reach_process", MODE_MULTI_PROCESS)).orNull();

        final PackageManager packageManager;
        if ((packageManager = getPackageManager()) == null)
            return;

        packageManager.setComponentEnabledSetting(
                new ComponentName(this, PlayerUpdateListener.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        super.onResume();
    }

    @Override
    protected void onPostResume() {

        Log.i("Ayush", "Called onResume");
        processIntent(getIntent());
        super.onPostResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onOpenProfile() {
        if (isFinishing())
            return;
        try {
            fragmentManager.beginTransaction()
                    .addToBackStack(null).replace(R.id.container, EditProfileFragment.newInstance(), "edit_profile_fragment").commit();
        } catch (IllegalStateException ignored) {
            finish();
        }
    }

    private void addNotificationDrawer() {

        viewPager = (CustomViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(new ViewPagerReusable(
                fragmentManager,
                new String[]{"Requests", "Notifications"},
                new Fragment[]{
                        FriendRequestFragment.newInstance(serverId),
                        NotificationFragment.newInstance(serverId)}));
        viewPager.setPagingEnabled(false);

        final TabLayout slidingTabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        slidingTabLayout.post(() -> slidingTabLayout.setupWithViewPager(viewPager));
    }

    @Override
    public void onOpenNotificationDrawer() {

        if (mDrawerLayout == null)
            return;
        if (!mDrawerLayout.isDrawerOpen(Gravity.RIGHT))
            mDrawerLayout.openDrawer(Gravity.RIGHT);
        else
            mDrawerLayout.closeDrawer(Gravity.RIGHT);
    }

    @Override
    public void onAccountCreated() {

        if (isFinishing())
            return;

//        if (!StaticData.debugMode) {
//            // Crittercism
//            Crittercism.initialize(this, "552eac3c8172e25e67906922");
//            Crittercism.setUsername(userName + " " + phoneNumber);
//            //  Get tracker
//            final Tracker t = ((ReachApplication) activity.getApplication()).getTracker();
//            t.setScreenName("reach.project.core.ReachActivity");
//            t.send(new HitBuilders.ScreenViewBuilder().build());
//        }

        try {
            final Optional<ActionBar> optional = Optional.fromNullable(getSupportActionBar());
            if (optional.isPresent())
                optional.get().show();
            containerFrame.setPadding(0, topPadding, 0, 0);
            //slidingUpPanelLayout.getChildAt(0).setPadding(0, topPadding, 0, 0);
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.container, PrivacyFragment.newInstance(true), "privacy_fragment").commit();
        } catch (IllegalStateException ignored) {
            finish();
        }
    }

    @Override
    public void startNumberVerification() {

        if (isFinishing())
            return;
        try {
            Log.i("Downloader", "Start number verification");
            fragmentManager.beginTransaction()
                    .replace(R.id.container, NumberVerification.newInstance(), "number_verification").commit();
        } catch (IllegalStateException ignored) {
            finish();
        }
    }

    @Override
    public void closeDrawers() {
        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawers();
    }

    @Override
    public void onBackPressed() {

        if (isFinishing())
            return;
        try {
            if (mDrawerLayout.isDrawerOpen(Gravity.RIGHT))
                mDrawerLayout.closeDrawer(Gravity.RIGHT);
            else if (mDrawerLayout.isDrawerOpen(Gravity.LEFT))
                mDrawerLayout.closeDrawer(Gravity.LEFT);
            else if (slidingUpPanelLayout != null &&
                    slidingUpPanelLayout.getPanelState() == PanelState.EXPANDED) {
                slidingUpPanelLayout.setPanelState(PanelState.COLLAPSED);
            } else
                super.onBackPressed();
        } catch (IllegalStateException ignored) {
            finish();
        }
    }

    @Override
    public void onPrivacyDone() {
        if (isFinishing())
            return;
        try {

            serverId = SharedPrefUtils.getServerId(preferences);
            selectionArgumentsMyLibrary = new String[]{serverId + ""};
            slidingUpPanelLayout.getChildAt(0).setPadding(0, 0, 0, MiscUtils.dpToPx(60));

            //load fragment
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.container, ContactsListFragment.newInstance(), "my_reach").commit();
            //load notification drawer
            addNotificationDrawer();
            //load adapters
            new LoadAdapters().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);

        } catch (IllegalStateException ignored) {
            finish();
        }
    }


    @Override
    public void onPushNext(HashSet<TransferSong> songsList) {
        if (isFinishing())
            return;
        try {
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .addToBackStack(null).replace(R.id.container, ContactsChooserFragment.newInstance(songsList), "contacts_chooser").commit();
        } catch (IllegalStateException ignored) {
            finish();
        }
    }

    @Override
    public void onOpenInvitePage() {
        if (isFinishing())
            return;
        try {
            fragmentManager.beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.container, InviteFragment.newInstance(), "invite_fragment").commit();
        } catch (IllegalStateException ignored) {
        }
    }

    @Override
    public void onOpenLibrary(long id) {
        if (isFinishing())
            return;
        try {
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .addToBackStack(null).replace(R.id.container, UserMusicLibrary.newInstance(id), "user_library " + id).commit();
        } catch (IllegalStateException ignored) {
            finish();
        }
    }

    @Override
    public void onOpenPushLibrary() {
        if (isFinishing())
            return;
        try {
            fragmentManager.beginTransaction()
                    .addToBackStack(null).replace(R.id.container, PushSongsFragment.newInstance(), "push_library").commit();
        } catch (IllegalStateException ignored) {
            finish();
        }
    }

    @Override
    public void setUpDrawer() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_drawer);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    @Override
    public void toggleDrawer(boolean lock) {
        if (mDrawerLayout != null)
            if (lock)
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            else
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    @Override
    public void toggleSliding(final boolean show) {

        new Handler().post(() -> {

            if (slidingUpPanelLayout != null) {
                if (show) {
                    slidingUpPanelLayout.getChildAt(1).setVisibility(View.VISIBLE);
                    slidingUpPanelLayout.setPanelHeight(MiscUtils.dpToPx(63));
                    slidingUpPanelLayout.setPanelState(PanelState.COLLAPSED);
                } else {
                    slidingUpPanelLayout.getChildAt(1).setVisibility(View.GONE);
                    slidingUpPanelLayout.setPanelHeight(MiscUtils.dpToPx(0));
                    slidingUpPanelLayout.setPanelState(PanelState.HIDDEN);
                }
            }
        });
    }

    @Override
    public void anchorFooter() {
        if (slidingUpPanelLayout == null)
            return;
        slidingUpPanelLayout.setPanelState(PanelState.EXPANDED);
    }

    @Override
    public void setUpNavigationViews() {

        final SharedPreferences sharedPreferences = getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
        final SwitchCompat netToggle = (SwitchCompat) findViewById(R.id.netToggle);
        if (SharedPrefUtils.getMobileData(sharedPreferences))
            netToggle.setChecked(true);
        else
            netToggle.setChecked(false);

        netToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked)
                SharedPrefUtils.setDataOn(sharedPreferences);
            else {
                SharedPrefUtils.setDataOff(sharedPreferences);
                ////////////////////purge all upload operations, but retain paused operations
                //TODO
                getContentResolver().delete(
                        ReachDatabaseProvider.CONTENT_URI,
                        ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ? and " +
                                ReachDatabaseHelper.COLUMN_STATUS + " != ?",
                        new String[]{1 + "", ReachDatabase.PAUSED_BY_USER + ""});
            }
        });
        final String path = SharedPrefUtils.getImageId(sharedPreferences);
        if (!TextUtils.isEmpty(path) && !path.equals("hello_world"))
            Picasso.with(ReachActivity.this).load(StaticData.cloudStorageImageBaseUrl + path)
                    .into((ImageView) findViewById(R.id.userImageNav));
        ((TextView) findViewById(R.id.userNameNav))
                .setText(SharedPrefUtils.getUserName(sharedPreferences));
        ////////////////////
        final Cursor countCursor = getContentResolver().query(
                ReachSongProvider.CONTENT_URI,
                ReachSongHelper.projection,
                ReachSongHelper.COLUMN_USER_ID + " = ?",
                new String[]{SharedPrefUtils.getServerId(sharedPreferences) + ""},
                null);
        if (countCursor == null) return;
        if (!countCursor.moveToFirst()) {
            countCursor.close();
            return;
        }
        final long count = countCursor.getCount();
        countCursor.close();
        ((TextView) findViewById(R.id.numberOfSongsNav)).setText(count + " Songs");
    }

    @Override
    public void startAccountCreation(Optional<OldUserContainerNew> container) {

        if (isFinishing())
            return;
        try {
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.container, AccountCreation.newInstance(container), "account_creation").commit();
        } catch (IllegalStateException ignored) {
            finish();
        }
    }

    @Override
    public void startMusicListFragment(long id, String albumName, String artistName, String playListName, int type) {

        if (isFinishing())
            return;
        try {
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .addToBackStack(null)
                    .replace(R.id.container, MusicListFragment.newTypeInstance(id, albumName, artistName, playListName, type), "now_playing")
                    .commit();
        } catch (IllegalStateException ignored) {
            finish();
        }
    }

    @Override
    public void onBackStackChanged() {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            if (searchView != null)
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(searchView.getWindowToken(), 0);
            final Optional<ActionBar> actionBar = Optional.fromNullable(getSupportActionBar());
            if (actionBar.isPresent())
                actionBar.get().setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            if (mDrawerLayout != null)
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
        } else {
            setUpDrawer();
            toggleDrawer(false);
        }
    }

    @Override
    public boolean onClose() {

        searchView.setQuery(null, true);
        searchView.clearFocus();
        selectionDownloader = ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?";
        selectionMyLibrary = ReachSongHelper.COLUMN_USER_ID + " = ?";
        selectionArgumentsDownloader = new String[]{0 + ""};
        selectionArgumentsMyLibrary = new String[]{serverId + ""};
        getLoaderManager().restartLoader(StaticData.DOWNLOAD_LOADER, null, this);
        getLoaderManager().restartLoader(StaticData.MY_LIBRARY_LOADER, null, this);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (searchView == null)
            return false;

        // Called when the action bar search text has changed.  Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        final String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
        // Don't do anything if the filter hasn't actually changed.
        // Prevents restarting the loader when restoring state.
        if (mCurFilter == null && newFilter == null) {
            return true;
        }
        if (mCurFilter != null && mCurFilter.equals(newFilter)) {
            return true;
        }
        mCurFilter = newFilter;

        if (TextUtils.isEmpty(newText)) {

            selectionDownloader = ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?";
            selectionMyLibrary = ReachSongHelper.COLUMN_USER_ID + " = ?";
            selectionArgumentsDownloader = new String[]{0 + ""};
            selectionArgumentsMyLibrary = new String[]{serverId + ""};
        } else {

            selectionDownloader = ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ? and (" +
                    ReachDatabaseHelper.COLUMN_ACTUAL_NAME + " LIKE ? or " +
                    ReachDatabaseHelper.COLUMN_DISPLAY_NAME + " LIKE ?)";
            selectionMyLibrary = ReachSongHelper.COLUMN_USER_ID + " = ? and (" +
                    ReachDatabaseHelper.COLUMN_ACTUAL_NAME + " LIKE ? or " +
                    ReachDatabaseHelper.COLUMN_DISPLAY_NAME + " LIKE ?)";
            selectionArgumentsDownloader = new String[]{"0",
                    "%" + mCurFilter + "%",
                    "%" + mCurFilter + "%"};
            selectionArgumentsMyLibrary = new String[]{serverId + "",
                    "%" + mCurFilter + "%",
                    "%" + mCurFilter + "%"};
        }
        getLoaderManager().restartLoader(StaticData.DOWNLOAD_LOADER, null, this);
        getLoaderManager().restartLoader(StaticData.MY_LIBRARY_LOADER, null, this);
        Log.i("Downloader", "SEARCH SUBMITTED !");
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {

        Log.d("Ayush", "Received new Intent");
        processIntent(intent);
        super.onNewIntent(intent);
    }

    @SuppressLint("RtlHardcoded")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        preferences = getSharedPreferences("Reach", MODE_MULTI_PROCESS);
        fragmentManager = getSupportFragmentManager();
        reference = new WeakReference<>(this);
        serverId = SharedPrefUtils.getServerId(preferences);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        final Optional<ActionBar> actionBar = Optional.fromNullable(getSupportActionBar());
        if (actionBar.isPresent()) {
            actionBar.get().setDisplayShowHomeEnabled(false);
            actionBar.get().hide();
        }

        slidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        searchView = new SearchView(this);
        toggleDrawer(true);

        //small
        toggleSliding(false);
        containerFrame = (FrameLayout) findViewById(R.id.containerFrame);
        topPadding = containerFrame.getPaddingTop();
        containerFrame.setPadding(0, 0, 0, 0);
        //navigation-drawer
        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.LEFT);

        progressBarMinimized = (SeekBar) findViewById(R.id.progressBar);
        songNameMinimized = (TextView) findViewById(R.id.songNamePlaying);
        pausePlayMinimized = (ImageButton) findViewById(R.id.pause_play);
        //full-screen
        progressBarMaximized = (SeekBar) findViewById(R.id.playerProgress);
        songNameMaximized = (TextView) findViewById(R.id.songTitle);
        artistName = (TextView) findViewById(R.id.artistName);
        playerPos = (TextView) findViewById(R.id.playerPos);
        songDuration = (TextView) findViewById(R.id.songDuration);
        shuffleBtn = (ImageView) findViewById(R.id.shuffleBtn);
        repeatBtn = (ImageView) findViewById(R.id.repeatBtn);
        pausePlayMaximized = (ImageView) findViewById(R.id.playBtn);
        likeButton = (ImageView) findViewById(R.id.likeBtn);
        //reachQueue
        queueListView = (ListView) findViewById(R.id.queueListView);
        downloadRefresh = (SwipeRefreshLayout) findViewById(R.id.downloadRefresh);

        final NavigationView mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
        final View navListView = mNavigationView.getChildAt(0);
        final FrameLayout.LayoutParams frameLayoutParams = (FrameLayout.LayoutParams) navListView.getLayoutParams();
        frameLayoutParams.setMargins(0, 0, 0, MiscUtils.dpToPx(50));
        navListView.setLayoutParams(frameLayoutParams);

        mNavigationView.setNavigationItemSelectedListener(navigationItemSelectedListener);
        fragmentManager.addOnBackStackChangedListener(this);

        findViewById(R.id.userImageNav).setOnClickListener(navHeaderClickListener);
        findViewById(R.id.footer).setOnClickListener(footerClickListener);
        findViewById(R.id.fwdBtn).setOnClickListener(LocalUtils.nextClick);
        findViewById(R.id.rwdBtn).setOnClickListener(LocalUtils.previousClick);

        pausePlayMaximized.setOnClickListener(LocalUtils.pauseClick);
        pausePlayMinimized.setOnClickListener(LocalUtils.pauseClick);
        queueListView.setOnItemClickListener(LocalUtils.myLibraryClickListener);
        queueListView.setOnScrollListener(scrollListener);
        downloadRefresh.setColorSchemeColors(getResources().getColor(R.color.reach_color), getResources().getColor(R.color.reach_grey));
        downloadRefresh.setOnRefreshListener(refreshListener);
        shuffleBtn.setOnClickListener(LocalUtils.shuffleClick);
        repeatBtn.setOnClickListener(LocalUtils.repeatClick);
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);
        likeButton.setOnClickListener(LocalUtils.likeButtonClick);

        slidingUpPanelLayout.setPanelSlideListener(slideListener);
        progressBarMaximized.setOnSeekBarChangeListener(LocalUtils.playerSeekListener);
        progressBarMinimized.setOnSeekBarChangeListener(LocalUtils.playerSeekListener);

        selectionDownloader = ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?";
        selectionMyLibrary = ReachSongHelper.COLUMN_USER_ID + " = ?";
        selectionArgumentsDownloader = new String[]{"0"};
        selectionArgumentsMyLibrary = new String[]{serverId + ""};

        //accountCreation ? numberVerification ? contactListFragment ? and other stuff
        loadFragment();
    }

    private void loadFragment() {

        final boolean networkPresent;
        final NetworkInfo networkInfo =
                ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            // There are no active networks.
            Toast.makeText(this, "No active networks detected", Toast.LENGTH_SHORT).show();
            networkPresent = false;
        } else
            networkPresent = true;

        if (networkPresent) //check for update
            new LocalUtils.CheckUpdate().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        //fetch username and phoneNumber
        final String userName = SharedPrefUtils.getUserName(preferences);
        final String phoneNumber = SharedPrefUtils.getUserNumber(preferences);

        //initialize bug tracking
        if (!StaticData.debugMode) {
            Crittercism.initialize(this, "552eac3c8172e25e67906922");
            Crittercism.setUsername(userName + " " + phoneNumber);
        }

        //initialize GA tracker
        final Tracker tracker = ((ReachApplication) getApplication()).getTracker();
        tracker.setScreenName("reach.project.core.ReachActivity");
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

        //initialize AppsFlyer
        AppsFlyerLib.setAppsFlyerKey("JSwfk37zArmeNLNCd4grKR");
        AppsFlyerLib.sendTracking(this);

        //initialize Localytics
        AsyncTask.SERIAL_EXECUTOR.execute(() -> {

            final String locID = Localytics.getCustomerId();
            if (locID == null || TextUtils.isEmpty(locID)) {
                Localytics.setCustomerId(phoneNumber);
                Localytics.setCustomerFullName(userName);
            }
        });

        //first check playServices
        if (!LocalUtils.checkPlayServices(this)) {
            tracker.send(new HitBuilders.EventBuilder("Play Services screwup", userName + " " + phoneNumber + " screwed up").build());
            return; //fail
        }

        if (serverId == 0 || TextUtils.isEmpty(phoneNumber)) {

            startNumberVerification();
            toggleSliding(false);
        } else if (TextUtils.isEmpty(userName)) {

            startAccountCreation(Optional.<OldUserContainerNew>absent());
            toggleSliding(false);
        } else {

            try {

                containerFrame.setPadding(0, topPadding, 0, 0);
                slidingUpPanelLayout.getChildAt(0).setPadding(0, 0, 0, MiscUtils.dpToPx(60));
                fragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                        .replace(R.id.container, ContactsListFragment.newInstance(), "my_reach").commit();
                //load notification drawer
                addNotificationDrawer();
                //load adapters
                new LoadAdapters().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
                //load last song
                new LastSong().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                //some stuff
                if (networkPresent)
                    AsyncTask.SERIAL_EXECUTOR.execute(LocalUtils.networkOps);

            } catch (IllegalStateException ignored) {
            }
        }
    }

    private final class LoadAdapters extends AsyncTask<ReachActivity, Void, ReachActivity> {

        @Override
        protected ReachActivity doInBackground(ReachActivity... params) {

            /**
             * Set up adapter for Music player
             */
            combinedAdapter = new MergeAdapter();
            //combinedAdapter.addView(LocalUtils.getDownloadedTextView(params[0]));
            emptyTV1 = LocalUtils.getEmptyDownload(params[0]);
            combinedAdapter.addView(emptyTV1, false);
            combinedAdapter.addAdapter(queueAdapter = new ReachQueueAdapter(params[0], null, 0));
            queueAdapter.getSwipeLayoutResourceId(0);
            combinedAdapter.addView(LocalUtils.getMyLibraryTExtView(params[0]));
            emptyTV2 = LocalUtils.getEmptyLibrary(params[0]);
            combinedAdapter.addView(emptyTV2, false);
            combinedAdapter.addAdapter(musicAdapter = new ReachMusicAdapter(params[0], R.layout.my_musiclist_item, null, 0,
                    ReachMusicAdapter.PLAYER));
            return params[0];
        }

        @Override
        protected void onPostExecute(ReachActivity activity) {

            super.onPostExecute(activity);
            if (combinedAdapter == null)
                return;
            queueListView.setAdapter(combinedAdapter);
            getLoaderManager().initLoader(StaticData.MY_LIBRARY_LOADER, null, activity);
            getLoaderManager().initLoader(StaticData.DOWNLOAD_LOADER, null, activity);
        }
    }

    private final class LastSong extends AsyncTask<Void, Void, Boolean[]> {

        @Override
        protected Boolean[] doInBackground(Void... params) {


            final Boolean[] toSend = new Boolean[]{false, false, false};
            currentPlaying = SharedPrefUtils.getLastPlayed(getSharedPreferences("reach_process", MODE_MULTI_PROCESS)).orNull();

            toSend[0] = (currentPlaying != null);
            toSend[1] = SharedPrefUtils.getShuffle(preferences);
            toSend[2] = SharedPrefUtils.getRepeat(preferences);

            return toSend;
        }

        @Override
        protected void onPostExecute(Boolean[] booleans) {

            super.onPostExecute(booleans);

            if (booleans[0] && currentPlaying!=null) {
                //last song is present
                songNameMinimized.setText(currentPlaying.getDisplayName());
                songNameMaximized.setText(currentPlaying.getDisplayName());
                artistName.setText(currentPlaying.getArtistName());
                songDuration.setText(MiscUtils.combinationFormatter(currentPlaying.getDuration()));
                pausePlayMaximized.setImageResource(R.drawable.play_white_selector);
                pausePlayMinimized.setImageResource(R.drawable.play_white_selector);
            }

            shuffleBtn.setSelected(booleans[1]);
            repeatBtn.setSelected(booleans[2]);
        }
    }

    private synchronized void processIntent(Intent intent) {

        if (intent != null) {

            if (intent.getBooleanExtra("openNotificationFragment", false))
                onOpenNotificationDrawer();
            else if (intent.getBooleanExtra("openPlayer", false))
                new Handler().postDelayed(() -> {
                    if (slidingUpPanelLayout != null)
                        slidingUpPanelLayout.setPanelState(PanelState.EXPANDED);
                }, 1500);
            else if (intent.getBooleanExtra("openFriendRequests", false)) {
                if (!mDrawerLayout.isDrawerOpen(Gravity.RIGHT))
                    mDrawerLayout.openDrawer(Gravity.RIGHT);
                viewPager.setCurrentItem(0);
            } else if (intent.getBooleanExtra("openNotifications", false)) {
                if (!mDrawerLayout.isDrawerOpen(Gravity.RIGHT))
                    mDrawerLayout.openDrawer(Gravity.RIGHT);
                viewPager.setCurrentItem(1);
            } else if (!TextUtils.isEmpty(intent.getAction()) && intent.getAction().equals("process_multiple")) {

                final String compressed = intent.getStringExtra("data");
                String unCompressed;
                try {
                    unCompressed = StringCompress.decompress(Base64.decode(compressed, Base64.DEFAULT));
                } catch (IOException e) {
                    e.printStackTrace();
                    unCompressed = "";
                }

                if (!TextUtils.isEmpty(unCompressed)) {

                    final PushContainer pushContainer = new Gson().fromJson(unCompressed, PushContainer.class);
                    final HashSet<TransferSong> transferSongs = new Gson().fromJson(
                            pushContainer.getSongData(),
                            new TypeToken<HashSet<TransferSong>>() {
                            }.getType());

                    for (TransferSong transferSong : transferSongs) {

                        addSongToQueue(transferSong.getSongId(),
                                pushContainer.getSenderId(),
                                transferSong.getSize(),
                                transferSong.getDisplayName(),
                                transferSong.getActualName(),
                                true,
                                pushContainer.getUserName(),
                                ReachFriendsHelper.ONLINE_REQUEST_GRANTED + "",
                                pushContainer.getNetworkType(),
                                transferSong.getArtistName(),
                                transferSong.getDuration());
                    }
                    new LocalUtils.RefreshOperations().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }

            intent.removeExtra("openNotificationFragment");
            intent.removeExtra("openPlayer");
            intent.removeExtra("openFriendRequests");
            intent.removeExtra("openNotifications");
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {


        if (id == StaticData.DOWNLOAD_LOADER) {

            return new CursorLoader(this,
                    ReachDatabaseProvider.CONTENT_URI,
                    ReachDatabaseHelper.ADAPTER_LIST,
                    selectionDownloader,
                    selectionArgumentsDownloader,
                    ReachDatabaseHelper.COLUMN_ADDED + " DESC");
        } else if (id == StaticData.MY_LIBRARY_LOADER) {

            return new CursorLoader(this,
                    ReachSongProvider.CONTENT_URI,
                    ReachSongHelper.DISK_LIST,
                    selectionMyLibrary,
                    selectionArgumentsMyLibrary,
                    ReachSongHelper.COLUMN_DISPLAY_NAME + " ASC");
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (loader.getId() == StaticData.MY_LIBRARY_LOADER && data != null && !data.isClosed()) {
            musicAdapter.swapCursor(data);
            int count = data.getCount();
            if (count == 0 && queueListView != null)
                combinedAdapter.setActive(emptyTV2, true);
            else
                combinedAdapter.setActive(emptyTV2, false);
        }

        if (loader.getId() == StaticData.DOWNLOAD_LOADER && data != null && !data.isClosed()) {

            queueAdapter.swapCursor(data);
            int count = data.getCount();
            if (count == 0 && queueListView != null)
                combinedAdapter.setActive(emptyTV1, true);
            else
                combinedAdapter.setActive(emptyTV1, false);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (loader.getId() == StaticData.MY_LIBRARY_LOADER)
            musicAdapter.swapCursor(null);
        if (loader.getId() == StaticData.DOWNLOAD_LOADER)
            queueAdapter.swapCursor(null);
    }

    @Override
    public void addSongToQueue(long songId, long senderId, long size,
                               String displayName, String actualName,
                               boolean multiple, String userName,
                               String onlineStatus, String networkType,
                               String artistName, long duration) {

        final ContentResolver contentResolver = getContentResolver();
        if (contentResolver == null)
            return;

        final Cursor cursor;
        if (multiple)
            cursor = contentResolver.query(
                    ReachDatabaseProvider.CONTENT_URI,
                    new String[]{ReachDatabaseHelper.COLUMN_ID},
                    ReachDatabaseHelper.COLUMN_SONG_ID + " = ? and " +
                            ReachDatabaseHelper.COLUMN_SENDER_ID + " = ? and " +
                            ReachDatabaseHelper.COLUMN_RECEIVER_ID + " = ?",
                    new String[]{songId + "", senderId + "", serverId + ""},
                    null);
        else
            //this cursor can be used to play if entry exists
            cursor = contentResolver.query(
                    ReachDatabaseProvider.CONTENT_URI,
                    new String[]{

                            ReachDatabaseHelper.COLUMN_ID, //0
                            ReachDatabaseHelper.COLUMN_PROCESSED, //1
                            ReachDatabaseHelper.COLUMN_PATH, //2

                            ReachDatabaseHelper.COLUMN_IS_LIKED, //3
                            ReachDatabaseHelper.COLUMN_SENDER_ID,
                            ReachDatabaseHelper.COLUMN_RECEIVER_ID,
                            ReachDatabaseHelper.COLUMN_LENGTH,

                    },

                    ReachDatabaseHelper.COLUMN_SONG_ID + " = ? and " +
                            ReachDatabaseHelper.COLUMN_SENDER_ID + " = ? and " +
                            ReachDatabaseHelper.COLUMN_RECEIVER_ID + " = ? and " +
                            ReachDatabaseHelper.COLUMN_LENGTH + " = ?",
                    new String[]{songId + "", senderId + "", serverId + "", size + ""},
                    null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {

                //song already found
                if (!multiple) {

                    //if not multiple addition, play the song
                    final boolean liked;
                    final String temp = cursor.getString(3);
                    liked = !TextUtils.isEmpty(temp) && temp.equals("1");

                    final MusicData musicData = new MusicData(
                            cursor.getLong(0), //id
                            size,
                            senderId,
                            cursor.getLong(1),
                            cursor.getString(2),
                            displayName,
                            artistName,
                            liked,
                            duration,
                            (byte) 0);
                    LocalUtils.playSong(musicData, this);
                }
                //in both cases close and continue
                cursor.close();
                return;
            }
            cursor.close();
        }

        //new song

        final ReachDatabase reachDatabase = new ReachDatabase();

        reachDatabase.setId(-1);
        reachDatabase.setSongId(songId);
        reachDatabase.setReceiverId(serverId);
        reachDatabase.setSenderId(senderId);

        reachDatabase.setOperationKind((short) 0);
        reachDatabase.setPath("hello_world");
        reachDatabase.setSenderName(userName);
        reachDatabase.setOnlineStatus(onlineStatus);

        reachDatabase.setArtistName(artistName);
        reachDatabase.setIsLiked(false);
        reachDatabase.setDisplayName(displayName);
        reachDatabase.setActualName(actualName);
        reachDatabase.setLength(size);
        reachDatabase.setProcessed(0);
        reachDatabase.setAdded(System.currentTimeMillis());

        reachDatabase.setDuration(duration);
        reachDatabase.setLogicalClock((short) 0);
        reachDatabase.setStatus(ReachDatabase.NOT_WORKING);

        reachDatabase.setLastActive(0);
        reachDatabase.setReference(0);

        //We call bulk starter always
        final String[] splitter = contentResolver.insert(ReachDatabaseProvider.CONTENT_URI,
                ReachDatabaseHelper.contentValuesCreator(reachDatabase)).toString().split("/");
        if (splitter.length == 0)
            return;
        reachDatabase.setId(Long.parseLong(splitter[splitter.length - 1].trim()));
        //start this operation
        if (!multiple)
            AsyncTask.THREAD_POOL_EXECUTOR.execute(MiscUtils.startDownloadOperation(
                    this,
                    reachDatabase,
                    reachDatabase.getReceiverId(), //myID
                    reachDatabase.getSenderId(),   //the uploaded
                    reachDatabase.getId()));

        if (!StaticData.debugMode) {
            ((ReachApplication) getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                    .setCategory("Transaction - Add Song")
                    .setAction("User Name - " + SharedPrefUtils.getUserName(getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)))
                    .setLabel("Song - " + reachDatabase.getDisplayName() + ", From - " + reachDatabase.getSenderId())
                    .setValue(1)
                    .build());
            Map<String, String> tagValues = new HashMap<>();
            tagValues.put("User Name", SharedPrefUtils.getUserName(getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)));
            tagValues.put("From", String.valueOf(reachDatabase.getSenderId()));
            tagValues.put("Song", reachDatabase.getDisplayName());
            Localytics.tagEvent("Transaction - Add Song", tagValues);
        }
    }

    private enum LocalUtils {
        ;


        public static final SeekBar.OnSeekBarChangeListener playerSeekListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    ProcessManager.submitMusicRequest(seekBar.getContext(), Optional.of(progress + ""), MusicHandler.ACTION_SEEK);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };

        public static final View.OnClickListener repeatClick = view -> {

            if (SharedPrefUtils.toggleRepeat(view.getContext().getSharedPreferences("Reach", MODE_MULTI_PROCESS)))
                view.setSelected(true);
            else
                view.setSelected(false);
        };

        public static final View.OnClickListener nextClick = v -> ProcessManager.submitMusicRequest(
                v.getContext(),
                Optional.<String>absent(),
                MusicHandler.ACTION_NEXT);

        public static final View.OnClickListener previousClick = v -> ProcessManager.submitMusicRequest(
                v.getContext(),
                Optional.<String>absent(),
                MusicHandler.ACTION_PREVIOUS);

        public static final AdapterView.OnClickListener likeButtonClick = new View.OnClickListener() {

            private boolean toggleLiked(Context context) {

                final ContentValues values = new ContentValues();
                values.put(ReachDatabaseHelper.COLUMN_IS_LIKED, !currentPlaying.isLiked() ? 1 : 0);

                return context.getContentResolver().update(
                        Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + currentPlaying.getId()),
                        values,
                        ReachDatabaseHelper.COLUMN_ID + " = ?",
                        new String[]{currentPlaying.getId() + ""}) > 0 && !currentPlaying.isLiked();
            }

            @Override
            public void onClick(View view) {

                if (currentPlaying == null || currentPlaying.getType() == 1)
                    return;

                final Context context = view.getContext();

                if (toggleLiked(context)) {

                    MiscUtils.autoRetryAsync(() -> StaticData.notificationApi.addLike(
                            currentPlaying.getSenderId(),
                            serverId,
                            currentPlaying.getDisplayName()).execute(), Optional.<Predicate<Void>>absent());
                    currentPlaying.setIsLiked(true);
                    ((ImageView) view).setImageResource(R.drawable.like_pink);
                } else {

                    ((ImageView) view).setImageResource(R.drawable.like_white);
                    currentPlaying.setIsLiked(false);
                }
            }
        };

        public static final View.OnClickListener pauseClick = v -> {

            if (currentPlaying != null)
                ProcessManager.submitMusicRequest(
                        v.getContext(),
                        Optional.of(new Gson().toJson(currentPlaying, MusicData.class)),
                        MusicHandler.ACTION_PLAY_PAUSE);
            else
                ProcessManager.submitMusicRequest(
                        v.getContext(),
                        Optional.<String>absent(),
                        MusicHandler.ACTION_PLAY_PAUSE);
        };

        public static final View.OnClickListener shuffleClick = view -> {

            if (SharedPrefUtils.toggleShuffle(view.getContext().getSharedPreferences("Reach", MODE_MULTI_PROCESS)))
                view.setSelected(true);
            else
                view.setSelected(false);
        };

        /**
         * Listener for music player click listener
         * uses -> StaticData.DOWNLOADED_LIST & StaticData.DISK_LIST
         */
        public static final AdapterView.OnItemClickListener myLibraryClickListener = (adapterView, view, position, l) -> {

            final Cursor cursor = (Cursor) adapterView.getAdapter().getItem(position);
            final Context context = view.getContext();

            if (cursor.getColumnCount() == ReachDatabaseHelper.ADAPTER_LIST.length)
                playSong(ReachDatabaseHelper.getMusicData(cursor), context);
            else
                playSong(ReachSongHelper.getMusicData(cursor, serverId), context);
        };

        public static void toast(final String message) {

            MiscUtils.useContextFromContext(reference, activity -> {
                activity.runOnUiThread(() -> Toast.makeText(activity, message, Toast.LENGTH_SHORT).show());
                return null;
            });
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

        public static Runnable networkOps = new Runnable() {

            private void checkGCM() {

                if (serverId == 0)
                    return;

                final MyString dataToReturn = MiscUtils.autoRetry(() -> StaticData.userEndpoint.getGcmId(serverId).execute(), Optional.<Predicate<MyString>>absent()).orNull();

                //check returned gcm
                final String gcmId;
                if (dataToReturn == null || //fetch failed
                        TextUtils.isEmpty(gcmId = dataToReturn.getString()) || //null gcm
                        gcmId.equals("hello_world")) { //bad gcm

                    //network operation
                    if (MiscUtils.updateGCM(serverId, reference))
                        Log.i("Ayush", "GCM updated !");
                    else {
                        Log.i("Ayush", "GCM check failed");
                        toast("Network error, GCM failed");
                    }
                } else if (gcmId.equals("user_deleted")) {
                    //TODO restart app sign-up
                }
            }

            @Override
            public void run() {

                ////////////////////////////////////////
                //refresh gcm
                checkGCM();
                //refresh download ops
                new LocalUtils.RefreshOperations().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                //Music scanner
                MiscUtils.useContextFromContext(reference, activity -> {

                    final Intent intent = new Intent(activity, MusicScanner.class);
                    intent.putExtra("first", false);
                    activity.startService(intent);
                    return null;
                });
                ////////////////////////////////////////
            }
        };

        public static TextView getMyLibraryTExtView(Context context) {
            final TextView textView = new TextView(context);
            textView.setText("My Songs");
            textView.setTextColor(context.getResources().getColor(R.color.reach_color));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
            textView.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(10), 0, 0);
            return textView;
        }

        public static TextView getEmptyDownload(Context context) {

            final TextView emptyTV1 = new TextView(context);
            emptyTV1.setText("Add songs to download");
            emptyTV1.setTextColor(context.getResources().getColor(R.color.darkgrey));
            emptyTV1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            emptyTV1.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(10), 0, 0);
            return emptyTV1;
        }

        public static TextView getEmptyLibrary(Context context) {

            final TextView emptyTV2 = new TextView(context);
            emptyTV2.setText("No Music on your phone");
            emptyTV2.setTextColor(context.getResources().getColor(R.color.darkgrey));
            emptyTV2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            emptyTV2.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(10), 0, 0);
            return emptyTV2;
        }

        //id = -1 : disk else downloader
        public static boolean playSong(MusicData musicData, Context context) {

            //stop any other play clicks till current is processed
            //sanity check
//            Log.i("Ayush", id + " " + length + " " + senderId + " " + processed + " " + path + " " + displayName + " " + artistName + " " + type + " " + isLiked + " " + duration);
            if (musicData.getLength() == 0 || TextUtils.isEmpty(musicData.getPath())) {
                Toast.makeText(context, "Bad song", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (musicData.getProcessed() == 0) {
                Toast.makeText(context, "Streaming will start in a few seconds", Toast.LENGTH_SHORT).show();
                return false;
            }

            ProcessManager.submitMusicRequest(context,
                    Optional.of(musicData),
                    MusicHandler.ACTION_NEW_SONG);
            ////////////////////////////////////////
            return true;
        }

        private static class CheckUpdate extends AsyncTask<Void, Void, Integer> {

            @Override
            protected Integer doInBackground(Void... params) {

                Scanner reader = null;
                try {
                    reader = new Scanner(new URL(StaticData.dropBox).openStream());
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

                MiscUtils.useContextFromContext(reference, activity -> {

                    final int version;
                    try {
                        version = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        return null;
                    }

                    if (version < result) {

                        final UpdateFragment updateFragment = new UpdateFragment();
                        updateFragment.setCancelable(false);
                        try {
                            updateFragment.show(activity.fragmentManager, "update");
                        } catch (IllegalStateException | WindowManager.BadTokenException ignored) {
                        }
                    }
                    return null;
                });
            }
        }

        public static class RefreshOperations extends AsyncTask<Void, Void, Void> {

            /**
             * Create a contentProviderOperation, we do not update
             * if the operation is paused.
             * We do not mess with the status if it was paused, working, relay or finished !
             *
             * @param contentValues the values to use
             * @param id            the id of the entry
             * @return the contentProviderOperation
             */
            private ContentProviderOperation getUpdateOperation(ContentValues contentValues, long id) {
                return ContentProviderOperation
                        .newUpdate(Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + id))
                        .withValues(contentValues)
                        .withSelection(ReachDatabaseHelper.COLUMN_ID + " = ? and " +
                                        ReachDatabaseHelper.COLUMN_STATUS + " != ? and " +
                                        ReachDatabaseHelper.COLUMN_STATUS + " != ? and " +
                                        ReachDatabaseHelper.COLUMN_STATUS + " != ? and " +
                                        ReachDatabaseHelper.COLUMN_STATUS + " != ?",
                                new String[]{
                                        id + "",
                                        ReachDatabase.PAUSED_BY_USER + "",
                                        ReachDatabase.WORKING + "",
                                        ReachDatabase.RELAY + "",
                                        ReachDatabase.FINISHED + ""})
                        .build();
            }

            private ContentProviderOperation getForceUpdateOperation(ContentValues contentValues, long id) {
                return ContentProviderOperation
                        .newUpdate(Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + id))
                        .withValues(contentValues)
                        .withSelection(ReachDatabaseHelper.COLUMN_ID + " = ?",
                                new String[]{id + ""})
                        .build();
            }

            private String generateRequest(ReachDatabase reachDatabase) {

                return "CONNECT" + new Gson().toJson
                        (new Connection(
                                ////Constructing connection object
                                "REQ",
                                reachDatabase.getSenderId(),
                                reachDatabase.getReceiverId(),
                                reachDatabase.getSongId(),
                                reachDatabase.getProcessed(),
                                reachDatabase.getLength(),
                                UUID.randomUUID().getMostSignificantBits(),
                                UUID.randomUUID().getMostSignificantBits(),
                                reachDatabase.getLogicalClock(), ""));
            }

            private String fakeResponse(ReachDatabase reachDatabase) {

                return new Gson().toJson
                        (new Connection(
                                ////Constructing connection object
                                "RELAY",
                                reachDatabase.getSenderId(),
                                reachDatabase.getReceiverId(),
                                reachDatabase.getSongId(),
                                reachDatabase.getProcessed(),
                                reachDatabase.getLength(),
                                UUID.randomUUID().getMostSignificantBits(),
                                UUID.randomUUID().getMostSignificantBits(),
                                reachDatabase.getLogicalClock(), ""));
            }

            private ArrayList<ContentProviderOperation> bulkStartDownloads(List<ReachDatabase> reachDatabases) {

                //TODO maintain list of global profiles
                //if global do not send REQ, fetch URL first, try that

                final ArrayList<ContentProviderOperation> operations =
                        new ArrayList<>();

                for (ReachDatabase reachDatabase : reachDatabases) {

                    final ContentValues values = new ContentValues();
                    if (reachDatabase.getProcessed() >= reachDatabase.getLength()) {

                        //mark finished
                        if (reachDatabase.getStatus() != ReachDatabase.FINISHED) {

                            values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.FINISHED);
                            values.put(ReachDatabaseHelper.COLUMN_PROCESSED, reachDatabase.getLength());
                            operations.add(getForceUpdateOperation(values, reachDatabase.getId()));
                        }
                        continue;
                    }

                    final MyBoolean myBoolean;
                    if (reachDatabase.getSenderId() == StaticData.devika) {

                        //hit cloud
                        MiscUtils.useContextFromContext(reference, context -> {
                            ProcessManager.submitNetworkRequest(context, fakeResponse(reachDatabase));
                            return null;
                        });

                        myBoolean = new MyBoolean();
                        myBoolean.setGcmexpired(false);
                        myBoolean.setOtherGCMExpired(false);
                    } else {
                        //sending REQ to senderId
                        myBoolean = MiscUtils.sendGCM(
                                generateRequest(reachDatabase),
                                reachDatabase.getSenderId(),
                                reachDatabase.getReceiverId());
                    }

                    if (myBoolean == null) {
                        Log.i("Ayush", "GCM sending resulted in shit");
                        values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.GCM_FAILED);
                    } else if (myBoolean.getGcmexpired()) {
                        Log.i("Ayush", "GCM re-registry needed");
                        values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.GCM_FAILED);
                    } else if (myBoolean.getOtherGCMExpired()) {
                        Log.i("Downloader", "SENDING GCM FAILED " + reachDatabase.getSenderId());
                        values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.GCM_FAILED);
                    } else {
                        Log.i("Downloader", "GCM SENT " + reachDatabase.getSenderId());
                        values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.NOT_WORKING);
                    }
                    operations.add(getUpdateOperation(values, reachDatabase.getId()));
                }
                return operations;
            }

            @Override
            protected Void doInBackground(Void... params) {

                final Cursor cursor = MiscUtils.useContextFromContext(reference, activity -> {

                    return activity.getContentResolver().query(
                            ReachDatabaseProvider.CONTENT_URI,
                            ReachDatabaseHelper.projection,
                            ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ? and " +
                                    ReachDatabaseHelper.COLUMN_STATUS + " != ?",
                            new String[]{
                                    "0", //only downloads
                                    ReachDatabase.PAUSED_BY_USER + ""}, null); //should not be paused
                }).orNull();

                if (cursor == null) {
                    if (params != null && params.length == 1)
                        return params[0];
                    return null;
                }

                final List<ReachDatabase> reachDatabaseList = new ArrayList<>(cursor.getCount());
                while (cursor.moveToNext())
                    reachDatabaseList.add(ReachDatabaseHelper.cursorToProcess(cursor));
                cursor.close();

                if (reachDatabaseList.size() > 0) {

                    final ArrayList<ContentProviderOperation> operations = bulkStartDownloads(reachDatabaseList);
                    if (operations.size() > 0) {

                        MiscUtils.useContextFromContext(reference, activity -> {

                            try {
                                Log.i("Downloader", "Starting Download op " + operations.size());
                                activity.getContentResolver().applyBatch(ReachDatabaseProvider.AUTHORITY, operations);
                            } catch (RemoteException | OperationApplicationException e) {
                                e.printStackTrace();
                            }
                            return null;
                        });
                    }
                }

                if (params != null && params.length == 1)
                    return params[0];
                return null;
            }

            @Override
            protected void onPostExecute(Void gg) {

                super.onPostExecute(gg);

                MiscUtils.useContextFromContext(reference, reachActivity -> {

                    if (reachActivity.downloadRefresh != null)
                        reachActivity.downloadRefresh.setRefreshing(false);
                    return null;
                });
            }
        }

    }

    public static class PlayerUpdateListener extends BroadcastReceiver {


        private synchronized void togglePlayPause(final boolean pause, final ReachActivity activity) {

            activity.runOnUiThread(() -> {

                if (activity.pausePlayMaximized != null) {
//                        if (activity.paused = pause)
                    if (pause)
                        activity.pausePlayMaximized.setImageResource(R.drawable.play_white_selector);
                    else
                        activity.pausePlayMaximized.setImageResource(R.drawable.pause_white_selector);
                }

                if (activity.pausePlayMinimized != null) {
                    if (pause)
                        activity.pausePlayMinimized.setImageResource(R.drawable.play_white_selector);
                    else
                        activity.pausePlayMinimized.setImageResource(R.drawable.pause_white_selector);
                }
            });
        }

        private synchronized void updateMusic(final MusicData data, boolean paused, final ReachActivity activity) {

            activity.runOnUiThread(() -> {

                if (activity.songNameMinimized != null)
                    activity.songNameMinimized.setText(data.getDisplayName());
                if (activity.songNameMaximized != null)
                    activity.songNameMaximized.setText(data.getDisplayName());
                if (activity.songDuration != null)
                    activity.songDuration.setText(MiscUtils.combinationFormatter(data.getDuration()));
                if (activity.artistName != null)
                    activity.artistName.setText(data.getArtistName());
                if (activity.likeButton != null) {

                    if (data.getType() == 0) {
                        activity.likeButton.setVisibility(View.VISIBLE);
                        if (data.isLiked())
                            activity.likeButton.setImageResource(R.drawable.like_pink);
                        else
                            activity.likeButton.setImageResource(R.drawable.like_white);
                    } else
                        activity.likeButton.setVisibility(View.GONE);
                }

            });
            updatePrimaryProgress(data.getPrimaryProgress(), data.getCurrentPosition(), activity);
            updateSecondaryProgress(data.getSecondaryProgress(), activity);
            togglePlayPause(paused, activity);
        }

        private synchronized void updatePrimaryProgress(final int progress, final int pos, final ReachActivity activity) {

            activity.runOnUiThread(() -> {
                if (activity.playerPos != null)
                    activity.playerPos.setText(MiscUtils.combinationFormatter(pos));
                if (activity.progressBarMaximized != null)
                    activity.progressBarMaximized.setProgress(progress);
                if (activity.progressBarMinimized != null)
                    activity.progressBarMinimized.setProgress(progress);
            });
        }

        private synchronized void updateSecondaryProgress(final int progress, final ReachActivity activity) {

            activity.runOnUiThread(() -> {
                if (activity.progressBarMaximized != null)
                    activity.progressBarMaximized.setSecondaryProgress(progress);
                if (activity.progressBarMinimized != null)
                    activity.progressBarMinimized.setSecondaryProgress(progress);
            });
        }

        private synchronized void updateDuration(final String duration, final ReachActivity activity) {

            activity.runOnUiThread(() -> {
                if (activity.songDuration != null)
                    activity.songDuration.setText(duration);
            });
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent == null || TextUtils.isEmpty(intent.getAction())) {
                Log.i("MusicPlayer", "Received null action");
                return;
            }

            final ReachActivity activity;
            if (reference == null || (activity = reference.get()) == null)
                return;

            switch (intent.getAction()) {

                case ProcessManager.REPLY_LATEST_MUSIC: {
                    Log.i("Downloader", "REPLY_LATEST_MUSIC received");
                    //update the currentPlaying for like and such
                    currentPlaying = intent.getParcelableExtra("message");
                    updateMusic(currentPlaying, false, activity);
                    break;
                }
                case ProcessManager.REPLY_MUSIC_DEAD: {
                    Log.i("Downloader", "REPLY_MUSIC_DEAD received");
//                        updateMusic(new MusicData("", "", "", 0, 0, 0, 0, (byte) 0, false, 0), true);
                    togglePlayPause(false, activity);
                    updatePrimaryProgress((short) 0, 0, activity);
                    break;
                }
                case ProcessManager.REPLY_ERROR: {
                    Log.i("Downloader", "REPLY_ERROR received");
                    updateMusic(new MusicData(0, 0, 0, 0, "", "", "", false, 0, (byte) 0), true, activity);
                    activity.runOnUiThread(() -> Toast.makeText(activity, "Error", Toast.LENGTH_SHORT).show());
                    break;
                }
                case ProcessManager.REPLY_PAUSED: {
                    Log.i("Downloader", "REPLY_PAUSED received");
                    togglePlayPause(true, activity);
                    break;
                }
                case ProcessManager.REPLY_UN_PAUSED: {
                    Log.i("Downloader", "REPLY_UN_PAUSED received");
                    togglePlayPause(false, activity);
                    break;
                }
                case ProcessManager.REPLY_PRIMARY_PROGRESS: {
//                    Log.i("Downloader", "REPLY_PRIMARY_PROGRESS received");
                    updatePrimaryProgress(intent.getShortExtra("progress", (short) 0), intent.getIntExtra("position", 0) * 1000, activity);
                    break;
                }
                case ProcessManager.REPLY_SECONDARY_PROGRESS: {
//                    Log.i("Downloader", "REPLY_SECONDARY_PROGRESS received");
                    updateSecondaryProgress(intent.getShortExtra("progress", (short) 0), activity);
                    break;
                }
                case ProcessManager.REPLY_DURATION: {
                    Log.i("Downloader", "REPLY_SECONDARY_PROGRESS received");
                    updateDuration(intent.getStringExtra("message"), activity);
                    break;
                }
            }

        }
    }
}