package reach.project.core;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import reach.backend.entities.userApi.model.MyString;
import reach.backend.entities.userApi.model.OldUserContainer;
import reach.project.R;
import reach.project.adapter.ReachMusicAdapter;
import reach.project.adapter.ReachQueueAdapter;
import reach.project.coreViews.ContactsChooserFragment;
import reach.project.coreViews.ContactsListFragment;
import reach.project.coreViews.EditProfileFragment;
import reach.project.coreViews.FeedbackFragment;
import reach.project.coreViews.InviteFragment;
import reach.project.coreViews.MusicListFragment;
import reach.project.coreViews.PrivacyFragment;
import reach.project.coreViews.PushSongsFragment;
import reach.project.coreViews.UpdateFragment;
import reach.project.coreViews.UploadHistory;
import reach.project.coreViews.UserMusicLibrary;
import reach.project.coreViews.UserProfileView;
import reach.project.database.ReachDatabase;
import reach.project.database.contentProvider.ReachDatabaseProvider;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.contentProvider.ReachSongProvider;
import reach.project.database.sql.ReachDatabaseHelper;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.database.sql.ReachSongHelper;
import reach.project.onBoarding.AccountCreation;
import reach.project.onBoarding.NumberVerification;
import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.reachProcess.reachService.MusicHandler;
import reach.project.reachProcess.reachService.ProcessManager;
import reach.project.utils.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.MusicScanner;
import reach.project.utils.PushContainer;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.SuperInterface;
import reach.project.utils.TransferSong;

public class ReachActivity extends ActionBarActivity implements
        SuperInterface,
        FragmentManager.OnBackStackChangedListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        SearchView.OnQueryTextListener,
        SearchView.OnCloseListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    
    private FragmentManager fragmentManager;
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private DrawerLayout mDrawerLayout;
    private int navPos = 99;
    private SearchView searchView;
    private FrameLayout.LayoutParams params;
    private ImageView upArrow;
    private String selectionDownloader, selectionMyLibrary, mCurFilter;
    private String[] selectionArgumentsDownloader;
    private String[] selectionArgumentsMyLibrary;
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private int topPadding;
    private boolean enablePadding = false;
    private TextView emptyTV1, emptyTV2;
    ////////////////////////////////////////
    private MusicData currentPlaying;
    ////////////////////////////////////////
    private TextView songNameMinimized, songNameMaximized, artistName, songDuration;
    private TextView playerPos;
    private SeekBar progressBarMaximized;
    private SeekBar progressBarMinimized;
    private ListView queueListView;
    private ImageView shuffleBtn, repeatBtn, pausePlayMaximized, likeButton; //fullscreen
    private ImageButton pausePlayMinimized; //small
    private SwipeRefreshLayout downloadRefresh;
    private MergeAdapter combinedAdapter = null;
    private ReachQueueAdapter queueAdapter = null;
    private ReachMusicAdapter musicAdapter = null;

    private static WeakReference<ReachActivity> reference = null;
    public static long serverId = 0;

    @Override
    protected void onDestroy() {

        if (reference!=null)
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
        mNavigationDrawerFragment = null;
        mDrawerLayout = null;
        slidingUpPanelLayout = null;
        currentPlaying = null;
        songNameMinimized = songNameMaximized = artistName = songDuration = playerPos = null;
        progressBarMinimized = progressBarMaximized = null;
        queueListView = null;
        shuffleBtn = repeatBtn = pausePlayMaximized = null;
        pausePlayMinimized = null;
        downloadRefresh = null;

        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (isFinishing())
            return super.onOptionsItemSelected(item);
        final int id = item.getItemId();
        try {
            switch (id) {

                case android.R.id.home: {
                    if (fragmentManager.getBackStackEntryCount() > 0) {
                        switch (item.getItemId()) {
                            case android.R.id.home:
                                fragmentManager.popBackStack();
                                return true;
                        }
                    }
                    if (fragmentManager.getBackStackEntryCount() == 0 && navPos > 0) {
                        onBackPressed();
                        return true;
                    }
                }
            }
        } catch (IllegalStateException ignored) {
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {

        super.onPause();

        final PackageManager packageManager;
        if((packageManager = getPackageManager()) == null)
            return;
        packageManager.setComponentEnabledSetting(
                new ComponentName(getApplicationContext(),
                        PlayerUpdateListener.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    @Override
    protected void onResume() {

        super.onResume();

        final PackageManager packageManager;
        if (getIntent() == null || slidingUpPanelLayout == null || (packageManager = getPackageManager()) == null)
            return;

        packageManager.setComponentEnabledSetting(
                new ComponentName(this, PlayerUpdateListener.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        /*NotificationManagerCompat managerCompat = NotificationManagerCompat.from(this);
        int nID = getIntent().getIntExtra("notifID",0);
        if (nID!=0) {
            Log.d("Ashish", "1stNotif");
            managerCompat.cancel(nID);
        }*/

        if (getIntent().getBooleanExtra("openPlayer", false)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    slidingUpPanelLayout.setPanelState(PanelState.EXPANDED);
                }
            }, 1500);
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {

        if (isFinishing())
            return;

        if (position > 0) {

            final ActionBar actionBar = getSupportActionBar();
            if (actionBar != null)
                actionBar.setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            toggleDrawer(true);
        }
        try {
            switch (position) {

                case 0: {

                    final SharedPreferences sharedPreferences = getSharedPreferences("Reach", MODE_MULTI_PROCESS);
                    if (SharedPrefUtils.isUserAbsent(sharedPreferences)) {

                        //fire number verification
                        Log.i("Downloader", "USER NUMBER EMPTY OPEN SPLASH");
                        fragmentManager.beginTransaction()
                                .replace(R.id.container, NumberVerification.newInstance(), "number_verification").commit();
                    } else if(navPos > 0) {

                        enablePadding = true;
                        fragmentManager.beginTransaction()
                                .replace(R.id.container, ContactsListFragment.newInstance(false), "contacts_fragment").commit();
                    }
                    break;
                }
                case 1: {
                    fragmentManager
                            .beginTransaction()
                            .replace(R.id.container, PrivacyFragment.newInstance(false), "privacy_fragment").commit();
                    break;
                }
                case 2: {
                    //upload history
                    fragmentManager
                            .beginTransaction()
                            .replace(R.id.container, UploadHistory.newUploadInstance(), "upload_history").commit();
                    break;
                }
                case 3: {
                    fragmentManager
                            .beginTransaction()
                            .replace(R.id.container, InviteFragment.newInstance(false), "invite_fragment").commit();
                    break;
                }
                case 4: {
                    fragmentManager
                            .beginTransaction()
                            .replace(R.id.container, FeedbackFragment.newInstance(), "feedback_fragment").commit();
                    break;
                }

            }
        } catch (IllegalStateException ignored) {
        }
        navPos = position;
    }

    @Override
    public void onOpenProfile() {
        if (isFinishing())
            return;
        try {
            fragmentManager.beginTransaction()
                    .addToBackStack(null).replace(R.id.container, EditProfileFragment.newInstance(), "edit_profile_fragment").commit();
        } catch (IllegalStateException ignored) {
        }
    }

    @Override
    public void onAccountCreated() {

        if (isFinishing())
            return;
        try {
            final Optional<ActionBar> optional = Optional.fromNullable(getSupportActionBar());
            if (optional.isPresent())
                optional.get().show();
            slidingUpPanelLayout.getChildAt(0).setPadding(0, topPadding, 0, 0);
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.container, PrivacyFragment.newInstance(true), "privacy_fragment").commit();
        } catch (IllegalStateException ignored) {
        }
    }

    @Override
    public void accountCreationError() {

        if (isFinishing())
            return;
        try {
            Log.i("Downloader", "Start number verification");
            fragmentManager.beginTransaction()
                    .replace(R.id.container, NumberVerification.newInstance(), "number_verification").commit();
        } catch (IllegalStateException ignored) {
        }
    }

    @Override
    public void onBackPressed() {

        if (isFinishing())
            return;
        try {
            if (slidingUpPanelLayout != null &&
                    slidingUpPanelLayout.getPanelState() == PanelState.EXPANDED) {
                slidingUpPanelLayout.setPanelState(PanelState.COLLAPSED);
                return;
            }
            if (fragmentManager.getBackStackEntryCount() == 0 && navPos > 0) {

                navPos = 0;
                fragmentManager
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left)
                        .replace(R.id.container, ContactsListFragment.newInstance(false), "contacts_fragment").commit();
            } else
                super.onBackPressed();
        } catch (IllegalStateException ignored) {}
    }

    @Override
    public void onPrivacyDone() {
        if (isFinishing())
            return;
        try {

            serverId = SharedPrefUtils.getServerId(getSharedPreferences("Reach", MODE_MULTI_PROCESS));
            selectionArgumentsMyLibrary = new String[]{serverId + ""};
            getLoaderManager().restartLoader(StaticData.MY_LIBRARY_LOADER, null, this);
            getLoaderManager().restartLoader(StaticData.DOWNLOAD_LOADER, null, this);

            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.container, ContactsListFragment.newInstance(true), "my_reach").commit();
        } catch (IllegalStateException ignored) {
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
        }
    }

    @Override
    public void onOpenProfileView(long id) {
        if (isFinishing())
            return;
        try {
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right).addToBackStack(null)
                    .addToBackStack(null).replace(R.id.container, UserProfileView.newInstance(id), "user_profile").commit();
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
                    .addToBackStack(null).replace(R.id.container, UserMusicLibrary.newInstance(id), "user_library").commit();
        } catch (IllegalStateException ignored) {
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
        }
    }

    @Override
    public void setUpDrawer() {

        if (mNavigationDrawerFragment != null)
            mNavigationDrawerFragment.setUp(R.id.navigation_drawer, mDrawerLayout);
    }

    @Override
    public void toggleDrawer(boolean lock) {
        if (mDrawerLayout != null) {
            if (lock)
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            else
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    @Override
    public void toggleSliding(final boolean show) {

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    slidingUpPanelLayout.getChildAt(1).setVisibility(View.VISIBLE);
                    slidingUpPanelLayout.setPanelHeight(MiscUtils.dpToPx(70));
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
    public void anchorFooter(boolean first) {

        if (first)
            slidingUpPanelLayout.setPanelState(PanelState.EXPANDED);
        else {
            slidingUpPanelLayout.setAnchorPoint(0.3f);
            slidingUpPanelLayout.setPanelState(PanelState.ANCHORED);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (slidingUpPanelLayout.getPanelState() == PanelState.ANCHORED)
                        slidingUpPanelLayout.setPanelState(PanelState.COLLAPSED);
                    slidingUpPanelLayout.setAnchorPoint(1f);
                }
            }, 2000);
        }
    }

    @Override
    public void setUpNavigationViews() {

        if (mNavigationDrawerFragment != null) {
            mNavigationDrawerFragment.setNavViews(this);
        }
    }

    @Override
    public void startAccountCreation(Optional<OldUserContainer> container) {

        if (isFinishing())
            return;
        try {
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.container, AccountCreation.newInstance(container), "account_creation").commit();
        } catch (IllegalStateException ignored) {
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
        }
    }

    @Override
    public void onNextClicked() {

        if (isFinishing())
            return;
        try {
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.container, ContactsListFragment.newInstance(true), "my_reach").commit();
        } catch (IllegalStateException ignored) {
        }
    }

//    @Override
//    public void OnSplash() {
//
//        if (isFinishing())
//            return;
//        try {
//            fragmentManager.beginTransaction()
//                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
//                    .replace(R.id.container, NumberVerification.newInstance(), "number_verfication").commit();
//        } catch (IllegalStateException ignored) {
//        }
//    }

    @Override
    public void goLibrary(long id) {
        if (isFinishing())
            return;
        try {
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .addToBackStack(null).replace(R.id.container, UserMusicLibrary.newInstance(id), "user_library").commit();
        } catch (IllegalStateException ignored) {
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
            toggleDrawer(true);
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

    private class CheckUpdate extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {

            BufferedReader reader = null;
            try {

                reader = new BufferedReader(new InputStreamReader(new URL(StaticData.dropBox).openStream()));
                final StringBuilder total = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    total.append(line);
                }
                final String result = total.toString();
                return result.trim();
            } catch (Exception ignored) {
            } finally {
                if (reader != null)
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {

            super.onPostExecute(result);
            if (isCancelled() || isFinishing() || TextUtils.isEmpty(result))
                return;

            final int currentVersion;
            try {
                currentVersion = Integer.parseInt(result);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return;
            }
            final int version;
            try {
                version = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return;
            }
            if (version < currentVersion && !isFinishing()) {

                final UpdateFragment updateFragment = new UpdateFragment();
                updateFragment.setCancelable(false);
                try {
                    updateFragment.show(fragmentManager, "update");
                } catch (IllegalStateException ignored) {
                }
            }
        }
    }

    private void initialize(final SharedPreferences sharedPreferences) {

        final NetworkInfo networkInfo =
                ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            // There are no active networks.
            Toast.makeText(this, "No active networks detected", Toast.LENGTH_SHORT).show();
            return;
        }
        final String userName = SharedPrefUtils.getUserName(sharedPreferences);
        final String phoneNumber = SharedPrefUtils.getUserNumber(sharedPreferences);

        if (!StaticData.debugMode) {
            // Crittercism
            Crittercism.initialize(this, "552eac3c8172e25e67906922");
            Crittercism.setUsername(userName + " " + phoneNumber);
            // Check for new update
            new CheckUpdate().executeOnExecutor(StaticData.threadPool);
            //  Get tracker.
            final Tracker t = ((ReachApplication) getApplication()).getTracker();
            //  Set screen name.
            t.setScreenName("reach.project.core.ReachActivity");
            //  Send a screen view.
            t.send(new HitBuilders.ScreenViewBuilder().build());
        }

        StaticData.threadPool.submit(new Runnable() {

            /**
             * Check the device to make sure it has the Google Play Services APK. If
             * it doesn't, display a dialog that allows users to download the APK from
             * the Google Play Store or enable it in the device's system settings.
             */
            private boolean checkPlayServices(Activity activity) {

                if (activity == null)
                    return true;

                final int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
                if (resultCode != ConnectionResult.SUCCESS) {
                    if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                        GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
                                StaticData.PLAY_SERVICES_RESOLUTION_REQUEST).show();
                    } else {
                        Toast.makeText(activity, "This device is not supported", Toast.LENGTH_LONG).show();
                        Log.i("GCM_UTILS", "This device is not supported.");
                        finish();
                    }
                    return false;
                }
                return true;
            }

            private void checkGCM() {

                if (serverId == 0)
                    return;

                final MyString dataToReturn = MiscUtils.autoRetry(new DoWork<MyString>() {
                    @Override
                    protected MyString doWork() throws IOException {
                        return StaticData.userEndpoint.getGcmId(serverId).execute();
                    }
                }, Optional.<Predicate<MyString>>absent()).orNull();
                final String gcmId;
                if (dataToReturn == null || TextUtils.isEmpty(gcmId = dataToReturn.getString())) {

                    Log.i("Ayush", "GcmId ObjectFetch failed");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            final Context context = reference.get();
                            if (context != null)
                                Toast.makeText(context, "Network error, GCM failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else if (gcmId.equals("user_deleted")) {
                    //TODO restart app sign-up
                } else if (gcmId.equals("hello_world") && !MiscUtils.updateGCM(serverId, reference)) {
                    Log.i("Ayush", "GCM check failed");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            final Context context = reference.get();
                            if (context != null)
                                Toast.makeText(context, "Network error, GCM failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void run() {

                // Check for play services
                if (!checkPlayServices(reference.get()))
                    Log.i("GCM_UTILS", "No valid Google Play Services APK found.");
                /////Verify User data
                if (!TextUtils.isEmpty(phoneNumber) || !TextUtils.isEmpty(userName)) {
                    //check if gcmID is present on server
                    checkGCM();
                    //Run music-scanner
                    final Intent intent = new Intent(mNavigationDrawerFragment.getActivity(), MusicScanner.class);
                    intent.putExtra("ReturnNow", false);
                    startService(intent);
                }
                //if service is running register callback
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {

        Log.d("Downloader", "Received new Intent");
        if (intent != null && !TextUtils.isEmpty(intent.getAction()) && intent.getAction().equals("process_multiple"))
            processMultiple(intent);
        new RefreshOperations().execute();
        super.onNewIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);

        final SharedPreferences sharedPreferences = getSharedPreferences("Reach", MODE_MULTI_PROCESS);
        serverId = SharedPrefUtils.getServerId(sharedPreferences);
        reference = new WeakReference<>(this);
        fragmentManager = getSupportFragmentManager();

        setContentView(R.layout.activity_my);
        initialize(sharedPreferences);

        final Optional<ActionBar> actionBar = Optional.fromNullable(getSupportActionBar());
        if (actionBar.isPresent()) {
            actionBar.get().setDisplayShowHomeEnabled(false);
            actionBar.get().hide();
        }
        slidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        if (!enablePadding)
            slidingUpPanelLayout.getChildAt(0).setPadding(0, 0, 0, 0);
        topPadding = slidingUpPanelLayout.getChildAt(0).getPaddingTop();
        toggleSliding(false);

        upArrow = (ImageView) findViewById(R.id.upArrow);
        params = (FrameLayout.LayoutParams) findViewById(R.id.fullPlayer).getLayoutParams();
        //small
        searchView = (SearchView) findViewById(R.id.reachQueueSearch);
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
        //navigation-drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        selectionDownloader = ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?";
        selectionMyLibrary = ReachSongHelper.COLUMN_USER_ID + " = ?";
        selectionArgumentsDownloader = new String[]{"0"};
        selectionArgumentsMyLibrary = new String[]{serverId + ""};

        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                fragmentManager.findFragmentById(R.id.navigation_drawer);
        fragmentManager.addOnBackStackChangedListener(this);

        findViewById(R.id.fwdBtn).setOnClickListener(nextClick);
        findViewById(R.id.rwdBtn).setOnClickListener(previousClick);
        pausePlayMaximized.setOnClickListener(pauseClick);
        pausePlayMinimized.setOnClickListener(pauseClick);
        queueListView.setOnItemClickListener(myLibraryClickListener);
        queueListView.setOnScrollListener(scrollListener);
        downloadRefresh.setOnRefreshListener(refreshListener);
        shuffleBtn.setOnClickListener(shuffleClick);
        repeatBtn.setOnClickListener(repeatClick);
        ((EditText) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text)).setTextColor(Color.WHITE);
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);
        likeButton.setOnClickListener(likeButtonClick);
        slidingUpPanelLayout.setPanelSlideListener(slideListener);
        progressBarMaximized.setOnSeekBarChangeListener(seekListener);
        progressBarMinimized.setOnSeekBarChangeListener(seekListener);

        if (SharedPrefUtils.getShuffle(sharedPreferences))
            shuffleBtn.setSelected(true);
        else
            shuffleBtn.setSelected(false);
        if (SharedPrefUtils.getRepeat(sharedPreferences))
            repeatBtn.setSelected(true);
        else
            repeatBtn.setSelected(false);

        combinedAdapter = new MergeAdapter();
        combinedAdapter.addView(getDownloadedTextView());
        emptyTV1 = new TextView(this);
        emptyTV1.setText("Add songs to download");
        emptyTV1.setTextColor(getResources().getColor(R.color.darkgrey));
        emptyTV1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        emptyTV1.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(10), 0, 0);
        combinedAdapter.addView(emptyTV1, false);
        combinedAdapter.addAdapter(queueAdapter = new ReachQueueAdapter(this, R.layout.reach_queue_item, null, 0));
        combinedAdapter.addView(getMyLibraryTExtView());
        emptyTV2 = new TextView(this);
        emptyTV2.setText("No music on your phone");
        emptyTV2.setTextColor(getResources().getColor(R.color.darkgrey));
        emptyTV2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        emptyTV2.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(10), 0, 0);
        combinedAdapter.addView(emptyTV2, false);
        combinedAdapter.addAdapter(musicAdapter = new ReachMusicAdapter(this, R.layout.my_musiclist_item, null, 0,
                ReachMusicAdapter.PLAYER));
        queueListView.setAdapter(combinedAdapter);

        currentPlaying = SharedPrefUtils.getLastPlayed(getSharedPreferences("reach_process", MODE_MULTI_PROCESS)).orNull();
        if (currentPlaying != null) {
            songNameMinimized.setText(currentPlaying.getDisplayName());
            songNameMaximized.setText(currentPlaying.getDisplayName());
            artistName.setText(currentPlaying.getArtistName());
            songDuration.setText(MiscUtils.combinationFormatter(currentPlaying.getDuration()));
            pausePlayMaximized.setImageResource(R.drawable.play_white_selector);
            pausePlayMinimized.setImageResource(R.drawable.play_white_selector);
        }

        final Intent intent = getIntent();
        if (intent != null && !TextUtils.isEmpty(intent.getAction()) && intent.getAction().equals("process_multiple"))
            processMultiple(intent);
        new RefreshOperations().executeOnExecutor(StaticData.threadPool);
        getLoaderManager().initLoader(StaticData.MY_LIBRARY_LOADER, null, this);
        getLoaderManager().initLoader(StaticData.DOWNLOAD_LOADER, null, this);
    }

    private TextView getDownloadedTextView() {
        final TextView textView = new TextView(this);
        textView.setText("Downloaded");
        textView.setTextColor(Color.parseColor("#42353e"));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        textView.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(10), 0, 0);
        return textView;
    }

    private TextView getMyLibraryTExtView() {
        final TextView textView = new TextView(this);
        textView.setText("My Library");
        textView.setTextColor(Color.parseColor("#42353e"));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        textView.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(10), 0, 0);
        return textView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.DOWNLOAD_LOADER)
            return new CursorLoader(this,
                    ReachDatabaseProvider.CONTENT_URI,
                    MusicData.DOWNLOADED_LIST,
                    selectionDownloader,
                    selectionArgumentsDownloader,
                    ReachDatabaseHelper.COLUMN_ADDED + " DESC");

        return new CursorLoader(this,
                ReachSongProvider.CONTENT_URI,
                MusicData.DISK_LIST,
                selectionMyLibrary,
                selectionArgumentsMyLibrary,
                ReachSongHelper.COLUMN_DISPLAY_NAME + " ASC");
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

    private final SlidingUpPanelLayout.PanelSlideListener slideListener = new SlidingUpPanelLayout.PanelSlideListener() {
        @Override
        public void onPanelSlide(View view, float v) {

            final Optional<ActionBar> actionBar = Optional.fromNullable(getSupportActionBar());
            if (actionBar.isPresent()) {
                if (v > 0.85f) {
                    params.setMargins(0, 0, 0, 0);
                    actionBar.get().hide();
                    upArrow.setVisibility(View.GONE);
                    findViewById(R.id.player).setVisibility(View.GONE);
                    searchView.setVisibility(View.VISIBLE);
                } else if (v < 0.85f) {
                    params.setMargins(0, MiscUtils.dpToPx(10), 0, 0);
                    actionBar.get().show();
                    upArrow.setVisibility(View.VISIBLE);
                    findViewById(R.id.player).setVisibility(View.VISIBLE);
                    searchView.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public void onPanelCollapsed(View view) {
            if (fragmentManager.getBackStackEntryCount() == 0)
                toggleDrawer(false);
        }

        @Override
        public void onPanelExpanded(View view) {
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

    private final SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            new RefreshOperations().execute(downloadRefresh);
        }
    };

    //id = -1 : disk else downloader
    public boolean playSong(long id, long length, long senderId, long processed, String path,
                            String displayName, String artistName, byte type, String isLiked, long duration) {

        //stop any other play clicks till current is processed
        //sanity check
        Log.i("Ayush", id + " " + length + " " + senderId + " " + processed + " " + path + " " + displayName + " " + artistName + " " + type + " " + isLiked + " " + duration);
        if (length == 0 || senderId == 0 || TextUtils.isEmpty(path) || TextUtils.isEmpty(displayName)) {
            Toast.makeText(this, "Bad song", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (processed == 0) {
            Toast.makeText(this, "Streaming Has Not Started Yet", Toast.LENGTH_SHORT).show();
            return false;
        }
        final MusicData data = new MusicData(displayName, path, artistName, id, length,
                senderId, processed, type, isLiked.equals("true"), duration);
        ProcessManager.submitMusicRequest(this,
                Optional.of(new Gson().toJson(data, MusicData.class)),
                MusicHandler.ACTION_NEW_SONG);
        /////////////////////////////////////////////////////
        return true;
    }

    private final AdapterView.OnClickListener likeButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (currentPlaying == null || currentPlaying.getType() == 1)
                return;
            if (toggleLiked()) {

                final Cursor cursor = getContentResolver().query(
                        Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + currentPlaying.getSenderId()),
                        new String[]{ReachFriendsHelper.COLUMN_USER_NAME, ReachFriendsHelper.COLUMN_IMAGE_ID},
                        ReachFriendsHelper.COLUMN_ID + " = ?",
                        new String[]{currentPlaying.getSenderId() + ""}, null);

                if (cursor == null)
                    return;
                if (!cursor.moveToFirst()) {
                    cursor.close();
                    return;
                }

                MiscUtils.autoRetryAsync(new DoWork<reach.backend.entities.messaging.model.MyString>() {
                    @Override
                    protected reach.backend.entities.messaging.model.MyString doWork() throws IOException {

                        return StaticData.messagingEndpoint.messagingEndpoint().sendManualNotification(
                                currentPlaying.getSenderId(), 0, "likes " + currentPlaying.getDisplayName(),
                                SharedPrefUtils.getUserName(getSharedPreferences("Reach", MODE_MULTI_PROCESS))).execute();
                    }
                }, Optional.<Predicate<reach.backend.entities.messaging.model.MyString>>absent());
                currentPlaying.setIsLiked(true);
                likeButton.setImageResource(R.drawable.like_pink);
            } else {
                likeButton.setImageResource(R.drawable.like_white);
                currentPlaying.setIsLiked(false);
            }
        }
    };

    private boolean toggleLiked() {

        final ContentValues values = new ContentValues();
        values.put(ReachDatabaseHelper.COLUMN_IS_LIKED, !currentPlaying.isLiked() + "");

        return getContentResolver().update(
                Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + currentPlaying.getId()),
                values,
                ReachDatabaseHelper.COLUMN_ID + " = ?",
                new String[]{currentPlaying.getId() + ""}) > 0 && !currentPlaying.isLiked();
    }

    private final AdapterView.OnItemClickListener myLibraryClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

            final Cursor cursor = (Cursor) combinedAdapter.getItem(position);
            if (cursor.getColumnCount() == MusicData.DOWNLOADED_LIST.length) {

                final long senderId = cursor.getLong(8);
                final String artistName;
                final long duration;
                final Cursor artistCursor = getContentResolver().query(
                        ReachSongProvider.CONTENT_URI,
                        new String[]{ReachSongHelper.COLUMN_ID,
                                ReachSongHelper.COLUMN_USER_ID,
                                ReachSongHelper.COLUMN_SONG_ID,
                                ReachSongHelper.COLUMN_ARTIST,
                                ReachSongHelper.COLUMN_DURATION},
                        ReachSongHelper.COLUMN_USER_ID + " = ? and " +
                                ReachSongHelper.COLUMN_SONG_ID + " = ?",
                        new String[]{senderId + "", cursor.getLong(10) + ""}, null);

                if (artistCursor == null) {
                    artistName = "";
                    duration = 0;
                } else if (!artistCursor.moveToFirst()) {
                    artistName = "";
                    duration = 0;
                    artistCursor.close();
                } else {
                    artistName = artistCursor.getString(3);
                    duration = artistCursor.getLong(4);
                    artistCursor.close();
                }
                playSong(
                        cursor.getLong(0),    //id
                        cursor.getLong(1),    //length
                        senderId,             //senderId
                        cursor.getLong(3),    //processed
                        cursor.getString(4),  //path
                        cursor.getString(5),  //displayName
                        artistName,           //artistName
                        (byte) 0,
                        cursor.getString(14),
                        duration);
                return;
            }
            playSong(
                    cursor.getLong(7),    //id
                    cursor.getLong(0),    //length
                    serverId,             //senderId
                    cursor.getLong(0),    //processed = length
                    cursor.getString(1),  //path
                    cursor.getString(2),  //displayName
                    cursor.getString(5), //artistName
                    (byte) 1, //type
                    "false", //isLiked
                    cursor.getLong(4)); //duration
        }
    };

    private final AbsListView.OnScrollListener scrollListener = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView absListView, int i) {
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            boolean enable = false;
            if (queueListView.getChildCount() > 0) {
                final boolean firstItemVisible = queueListView.getFirstVisiblePosition() == 0;
                final boolean topOfFirstItemVisible = queueListView.getChildAt(0).getTop() == 0;
                enable = firstItemVisible && topOfFirstItemVisible;
            }
            downloadRefresh.setEnabled(enable);
        }
    };

    private final SeekBar.OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {
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

    private final View.OnClickListener pauseClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

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
        }
    };

    private final View.OnClickListener shuffleClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (SharedPrefUtils.toggleShuffle(getSharedPreferences("Reach", MODE_MULTI_PROCESS)))
                shuffleBtn.setSelected(true);
            else
                shuffleBtn.setSelected(false);
        }
    };

    private final View.OnClickListener repeatClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (SharedPrefUtils.toggleRepeat(getSharedPreferences("Reach", MODE_MULTI_PROCESS)))
                repeatBtn.setSelected(true);
            else
                repeatBtn.setSelected(false);
        }
    };

    private final View.OnClickListener nextClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            ProcessManager.submitMusicRequest(
                    v.getContext(),
                    Optional.<String>absent(),
                    MusicHandler.ACTION_NEXT);
        }
    };
    private final View.OnClickListener previousClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            ProcessManager.submitMusicRequest(
                    v.getContext(),
                    Optional.<String>absent(),
                    MusicHandler.ACTION_PREVIOUS);
        }
    };

    private void processMultiple(Intent intent) {

        final PushContainer pushContainer = new Gson().fromJson(intent.getStringExtra("data"), PushContainer.class);
        final HashSet<TransferSong> transferSongs = new Gson().fromJson(
                pushContainer.getSongData(),
                new TypeToken<HashSet<TransferSong>>() {
                }.getType());
        final long senderId = pushContainer.getSenderId();

        for (TransferSong transferSong : transferSongs) {

            addSongToQueue(transferSong.getSongId(),
                    senderId,
                    transferSong.getSize(),
                    transferSong.getDisplayName(),
                    transferSong.getActualName(),
                    true,
                    pushContainer.getUserName(),
                    ReachFriendsHelper.ONLINE_REQUEST_GRANTED + "",
                    pushContainer.getNetworkType(),
                    transferSong.getArtistName(),
                    transferSong.getDuration());
        } // No intent
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
                            ReachDatabaseHelper.COLUMN_ID,
                            ReachDatabaseHelper.COLUMN_SENDER_ID,
                            ReachDatabaseHelper.COLUMN_RECEIVER_ID,
                            ReachDatabaseHelper.COLUMN_LENGTH,

                            ReachDatabaseHelper.COLUMN_PROCESSED,
                            ReachDatabaseHelper.COLUMN_PATH,
                            ReachDatabaseHelper.COLUMN_IS_LIKED},

                    ReachDatabaseHelper.COLUMN_SONG_ID + " = ? and " +
                            ReachDatabaseHelper.COLUMN_SENDER_ID + " = ? and " +
                            ReachDatabaseHelper.COLUMN_RECEIVER_ID + " = ? and " +
                            ReachDatabaseHelper.COLUMN_LENGTH + " = ?",
                    new String[]{songId + "", senderId + "", serverId + "", size + ""},
                    null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                if (!multiple) {
                    playSong(cursor.getLong(0),
                            size,
                            senderId,
                            cursor.getLong(4),
                            cursor.getString(5),
                            displayName,
                            artistName,
                            (byte) 0,
                            cursor.getString(6),
                            duration);
                }
                cursor.close();
                return;
            }
            cursor.close();
        }
        final ReachDatabase reachDatabase = new ReachDatabase();
        reachDatabase.setSongId(songId);
        reachDatabase.setReceiverId(serverId);
        reachDatabase.setSenderId(senderId);
        reachDatabase.setOperationKind((short) 0);
        /**
         * 0 = download; 1 = upload;
         */
        reachDatabase.setDisplayName(displayName);
        reachDatabase.setActualName(actualName);

        reachDatabase.setSenderName(userName);
        reachDatabase.setOnlineStatus(onlineStatus);
        reachDatabase.setNetworkType(networkType);

        reachDatabase.setLength(size);
        reachDatabase.setProcessed(0);
        reachDatabase.setLastActive(0);
        reachDatabase.setAdded(System.currentTimeMillis());
        reachDatabase.setLogicalClock((short) 0);
        reachDatabase.setStatus(ReachDatabase.NOT_WORKING);
        //We call bulk starter always
        final String[] splitter = contentResolver.insert(ReachDatabaseProvider.CONTENT_URI,
                ReachDatabaseHelper.contentValuesCreator(reachDatabase)).toString().split("/");
        if (splitter.length == 0)
            return;
        reachDatabase.setId(Long.parseLong(splitter[splitter.length - 1].trim()));
        //start this operation
        if (!multiple)
            StaticData.threadPool.submit(MiscUtils.startDownloadOperation(reachDatabase, getContentResolver()));

        if (!StaticData.debugMode) {
            ((ReachApplication) getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                    .setCategory("Transaction - Add Song")
                    .setAction("User Name - " + SharedPrefUtils.getUserName(getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)))
                    .setLabel("Song - " + reachDatabase.getDisplayName() + ", From - " + reachDatabase.getSenderId())
                    .setValue(1)
                    .build());
        }
    }

    private class RefreshOperations extends AsyncTask<SwipeRefreshLayout, Void, SwipeRefreshLayout> {
        @Override
        protected SwipeRefreshLayout doInBackground(SwipeRefreshLayout... params) {

            final ContentResolver contentResolver = getContentResolver();
            if (contentResolver == null) {
                if (params != null && params.length == 1)
                    return params[0];
                return null;
            }
//            final ContentValues database = new ContentValues();
//            database.put(ReachDatabaseHelper.COLUMN_NETWORK_TYPE, -1);
//            database.put(ReachDatabaseHelper.COLUMN_ONLINE_STATUS, ReachFriendsHelper.OFFLINE_REQUEST_GRANTED);
//            contentResolver.update(
//                    ReachDatabaseProvider.CONTENT_URI,
//                    database,
//                    ReachDatabaseHelper.COLUMN_STATUS + " != ?",
//                    new String[]{ReachDatabase.FINISHED + ""});
            final Cursor cursor = contentResolver.query(
                    ReachDatabaseProvider.CONTENT_URI,
                    ReachDatabaseHelper.projection,
                    ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ? and " +
                    ReachDatabaseHelper.COLUMN_STATUS + " != ? and " +
                    ReachDatabaseHelper.COLUMN_STATUS + " != ?",
                    new String[]{"0", ""+ReachDatabase.FINISHED, ""+ReachDatabase.PAUSED_BY_USER}, null);
            if (cursor == null) {
                if (params != null && params.length == 1)
                    return params[0];
                return null;
            }

            final List<ReachDatabase> reachDatabaseList = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {

                reachDatabaseList.add(ReachDatabaseHelper.cursorToProcess(cursor));
//                final ReachDatabase reachDatabase = ReachDatabaseHelper.cursorToProcess(cursor);
//                //If the service is running, we tread carefully
//                if (reachDatabase.getProcessed() < reachDatabase.getLength() &&
//                        (reachDatabase.getStatus() == ReachDatabase.NOT_WORKING || //not working
//                                reachDatabase.getStatus() == ReachDatabase.GCM_FAILED || //gcm failed
//                                reachDatabase.getStatus() == ReachDatabase.FILE_NOT_FOUND || //file not found on host
//                                reachDatabase.getStatus() == ReachDatabase.FILE_NOT_CREATED)) { //disk error
//                    reachDatabaseList.add(reachDatabase);
//                }
            }
            cursor.close();
            if (reachDatabaseList.size() > 0)
                MiscUtils.startBulkDownloadOperation(reachDatabaseList, contentResolver).run();

            if (params != null && params.length == 1)
                return params[0];
            return null;
        }

        @Override
        protected void onPostExecute(SwipeRefreshLayout refreshLayout) {

            super.onPostExecute(refreshLayout);
            if (isCancelled() || isFinishing() || refreshLayout == null)
                return;
            if (refreshLayout.isRefreshing())
                refreshLayout.setRefreshing(false);
        }
    }

    public static class PlayerUpdateListener extends BroadcastReceiver {

        private synchronized void togglePlayPause(final boolean pause, final ReachActivity activity) {

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

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
                }
            });
        }

        private synchronized void updateMusic(final MusicData data, boolean paused, final ReachActivity activity) {

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
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

                }
            });
            updatePrimaryProgress(data.getPrimaryProgress(), data.getCurrentPosition(), activity);
            updateSecondaryProgress(data.getSecondaryProgress(), activity);
            togglePlayPause(paused, activity);
        }

        private synchronized void updatePrimaryProgress(final short progress, final int pos, final ReachActivity activity) {

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (activity.playerPos != null)
                        activity.playerPos.setText(MiscUtils.combinationFormatter(pos));
                    if (activity.progressBarMaximized != null)
                        activity.progressBarMaximized.setProgress(progress);
                    if (activity.progressBarMinimized != null)
                        activity.progressBarMinimized.setProgress(progress);
                }
            });
//            if (activity.paused)
//                togglePlayPause(false, activity);
        }

        private synchronized void updateSecondaryProgress(final short progress, final ReachActivity activity) {

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (activity.progressBarMaximized != null)
                        activity.progressBarMaximized.setSecondaryProgress(progress);
                    if (activity.progressBarMinimized != null)
                        activity.progressBarMinimized.setSecondaryProgress(progress);
                }
            });
        }

        private synchronized void updateDuration(final String duration, final ReachActivity activity) {

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (activity.songDuration != null)
                        activity.songDuration.setText(duration);
                }
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
                    if (intent.getStringExtra("message") != null) {
                        //update the currentPlaying for like and such
                        activity.currentPlaying = new Gson().fromJson(intent.getStringExtra("message"), MusicData.class);
                        updateMusic(activity.currentPlaying, false, activity);
                    }
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
                    updateMusic(new MusicData("", "", "", 0, 0, 0, 0, (byte) 0, false, 0), true, activity);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, "Error", Toast.LENGTH_SHORT).show();
                        }
                    });
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