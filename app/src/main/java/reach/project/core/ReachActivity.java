package reach.project.core;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import reach.backend.entities.messaging.model.MyBoolean;
import reach.backend.entities.userApi.model.MyString;
import reach.backend.entities.userApi.model.OldUserContainerNew;
import reach.project.R;
import reach.project.coreViews.MyReachFragment;
import reach.project.coreViews.UpdateFragment;
import reach.project.coreViews.explore.ExploreFragment;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.coreViews.fileManager.apps.fragments.ApplicationFragment;
import reach.project.coreViews.fileManager.music.fragments.MyLibraryFragment;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.yourProfile.YourProfileActivity;
import reach.project.music.MusicScanner;
import reach.project.music.MySongsHelper;
import reach.project.music.PrivacyFragment;
import reach.project.music.PushContainer;
import reach.project.music.PushSongsFragment;
import reach.project.music.TransferSong;
import reach.project.pacemaker.Pacemaker;
import reach.project.reachProcess.auxiliaryClasses.Connection;
import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.reachProcess.reachService.MusicHandler;
import reach.project.reachProcess.reachService.ProcessManager;
import reach.project.usageTracking.PostParams;
import reach.project.usageTracking.SongMetadata;
import reach.project.usageTracking.UsageTracker;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.StringCompress;
import reach.project.utils.auxiliaryClasses.SuperInterface;
import reach.project.utils.viewHelpers.CustomViewPager;
import reach.project.utils.viewHelpers.PagerFragment;

public class ReachActivity extends AppCompatActivity implements
        SuperInterface {

    public static String OPEN_MY_FRIENDS = "OPEN_MY_FRIENDS";
    public static String OPEN_MY_PROFILE_APPS = "OPEN_MY_PROFILE_APPS";
    public static String OPEN_MY_PROFILE_MUSIC = "OPEN_MY_PROFILE_MUSIC";
    public static String OPEN_PUSH = "OPEN_PUSH";

    public static long serverId = 0;
    private static WeakReference<ReachActivity> reference = null;
    private static SecureRandom secureRandom = new SecureRandom();

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    private SharedPreferences preferences;
    private MenuItem liveHelpItem;
    private static final int MY_PERMISSIONS_READ_CONTACTS = 11;
    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 22;
    ////////////////////////////////////////
    private static MusicData currentPlaying;
    ////////////////////////////////////////
    private Firebase firebaseReference = null;

    private ImageButton pausePlayMinimized; //small
    private SwipeRefreshLayout downloadRefresh;

//    private final AbsListView.OnScrollListener scrollListener = new AbsListView.OnScrollListener() {
//
//        @Override
//        public void onScrollStateChanged(AbsListView absListView, int i) {
//        }
//
//        @Override
//        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//
//            boolean enable = false;
//            if (view.getChildCount() > 0) {
//
//                final boolean firstItemVisible = view.getFirstVisiblePosition() == 0;
//                final boolean topOfFirstItemVisible = view.getChildAt(0).getTop() == 0;
//                enable = firstItemVisible && topOfFirstItemVisible;
//            }
//
//            downloadRefresh.setEnabled(enable);
//        }
//    };

    private final View.OnClickListener navHeaderClickListener = v -> onOpenProfile();

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (reference != null)
            reference.clear();
        reference = null;
        currentPlaying = null;
        firebaseReference = null;
    }

    @Override
    protected void onPause() {

        super.onPause();

        final PackageManager packageManager;
        if ((packageManager = getPackageManager()) == null)
            return;

        if (firebaseReference != null)
            firebaseReference.child("chat").child(serverId + "").removeEventListener(LocalUtils.listenerForUnReadChats);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_READ_CONTACTS) {
            if (!(grantResults.length > 0 && grantResults[0] == 0)) {
                Toast.makeText(this,
                        "Permission to access Contacts is required to use the App",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        } else if (requestCode == MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE) {
            if (!(grantResults.length > 0 && grantResults[0] == 0)) {
                Toast.makeText(this,
                        "Permission to access Storage is required to use the App",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {

        if (Build.VERSION.SDK_INT >= 23) {

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_CONTACTS) != 0) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_CONTACTS))
                    Toast.makeText(this,
                            "Permission to access Contacts is required to use the App",
                            Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_CONTACTS
                        }, MY_PERMISSIONS_READ_CONTACTS);
            } else if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != 0) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    Toast.makeText(this,
                            "Permission to access Storage is required to use the App",
                            Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        }, MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
            }
        }

        //TODO onResume is called twice sometimes
        lastSong();

        final PackageManager packageManager;
        if ((packageManager = getPackageManager()) == null)
            return;

        super.onResume();
    }

    @Override
    protected void onPostResume() {

        super.onPostResume();
        Log.i("Ayush", "Called onResume");
        processIntent(getIntent());

        if (firebaseReference != null && serverId != 0)
            firebaseReference.child("chat").child(serverId + "").addChildEventListener(LocalUtils.listenerForUnReadChats);
    }

    @Override
    public void onOpenProfile() {
//        if (isFinishing())
//            return;
//        try {
//            fragmentManager.beginTransaction()
//                    .addToBackStack(null).replace(R.id.mainContainer, EditProfileFragment.newInstance(), "edit_profile_fragment").commit();
//        } catch (IllegalStateException ignored) {
//            finish();
//        }
    }

    @Override
    public void onOpenNotificationDrawer() {

    }

    @Override
    public void onOpenNavigationDrawer() {

    }

    @Override
    public void onAccountCreated() {

        if (isFinishing())
            return;
//        try {
//            //containerFrame.setPadding(0, topPadding, 0, 0);
//            //slidingUpPanelLayout.getChildAt(0).setPadding(0, topPadding, 0, 0);
//            fragmentManager.beginTransaction()
//                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
//                    .replace(R.id.mainContainer, PrivacyFragment.newInstance(true), "privacy_fragment").commit();
//        } catch (IllegalStateException ignored) {
//            finish();
//        }
    }

    @Override
    public void startNumberVerification() {

        if (isFinishing())
            return;
//        try {
//            Log.i("Downloader", "Start number verification");
//            fragmentManager.beginTransaction()
//                    .replace(R.id.mainContainer, NumberVerification.newInstance(), "number_verification").commit();
//        } catch (IllegalStateException ignored) {
//            finish();
//        }
    }

    @Override
    public void closeDrawers() {

    }

    @Override
    public void onPrivacyDone() {

        if (isFinishing())
            return;
        try {

            serverId = SharedPrefUtils.getServerId(preferences);

            //load fragment
//            fragmentManager.beginTransaction()
//                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
//                    .replace(R.id.mainContainer, MyReachFragment.newInstance(), "my_reach").commit();
            //load adapters
            //add chat listener as it was not added earlier
            if (firebaseReference != null && serverId != 0) {
                firebaseReference.child("chat").child(serverId + "").addChildEventListener(LocalUtils.listenerForUnReadChats);
                //update time stamp
                final Map<String, Object> userData = MiscUtils.getMap(3);
                userData.put("uid", serverId);
                userData.put("newMessage", true);
                userData.put("lastActivated", System.currentTimeMillis());
                firebaseReference.child("user").child(serverId + "").updateChildren(userData);
            }

        } catch (IllegalStateException ignored) {
            finish();
        }
    }

    @Override
    public void onPushNext(HashSet<TransferSong> songsList) {

        if (isFinishing())
            return;
//        try {
//            fragmentManager.beginTransaction()
//                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
//                    .addToBackStack(null).replace(R.id.mainContainer, ContactsChooserFragment.newInstance(songsList), "contacts_chooser").commit();
//        } catch (IllegalStateException ignored) {
//            finish();
//        }
    }

    @Override
    public void onOpenInvitePage() {

        if (isFinishing())
            return;
//        try {
//            fragmentManager.beginTransaction()
//                    .addToBackStack(null)
//                    .replace(R.id.mainContainer, InviteFragment.newInstance(), "invite_fragment").commit();
//        } catch (IllegalStateException ignored) {
//            finish();
//        }
    }

    @Override
    public void updateDetails(File image, String userName) {

//        ((SimpleDraweeView) findViewById(R.id.userImageNav)).setImageURI(Uri.fromFile(image));
//        ((TextView) findViewById(R.id.userNameNav)).setText(SharedPrefUtils.getUserName(preferences));
    }

    @Override
    public Optional<Firebase> getFireBase() {
        return Optional.fromNullable(firebaseReference);
    }

    @Override
    public void onOpenLibrary(long userId) {

        if (!isFinishing())
            YourProfileActivity.openProfile(userId, this);
    }

    @Override
    public void onOpenPushLibrary() {

        if (isFinishing())
            return;
//        try {
//            fragmentManager.beginTransaction()
//                    .addToBackStack(null).replace(R.id.mainContainer, PushSongsFragment.newInstance(), "push_library").commit();
//        } catch (IllegalStateException ignored) {
//            finish();
//        }
    }

    @Override
    public void toggleDrawer(boolean lock) {

    }

    @Override
    public void toggleSliding(boolean show) {

    }

    @Override
    public void anchorFooter() {

    }

    @Override
    public void setUpNavigationViews() {

    }

    @Override
    public void startAccountCreation(Optional<OldUserContainerNew> container) {

        if (isFinishing())
            return;
//        try {
//            fragmentManager.beginTransaction()
//                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
//                    .replace(R.id.mainContainer, AccountCreation.newInstance(container), "account_creation").commit();
//        } catch (IllegalStateException ignored) {
//            finish();
//        }
    }

    @Override
    public void startMusicListFragment(long id, String albumName, String artistName, String playListName, int type) {

//        if (isFinishing())
//            return;
//        try {
//            fragmentManager.beginTransaction()
//                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
//                    .addToBackStack(null)
//                    .replace(R.id.container, MusicListFragment.newTypeInstance(id, albumName, artistName, playListName, type), "now_playing")
//                    .commit();
//        } catch (IllegalStateException ignored) {
//            finish();
//        }
    }

//    @Override
//    public boolean onClose() {
//
//        if (searchView != null) {
//            searchView.setQuery(null, true);
//            searchView.clearFocus();
//        }
////
////        selectionDownloader = ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?";
////        selectionArgumentsDownloader = new String[]{0 + ""};
////        getLoaderManager().restartLoader(StaticData.DOWNLOAD_LOADER, null, this);
////
////        selectionMyLibrary = MySongsHelper.COLUMN_USER_ID + " = ?";
////        selectionArgumentsMyLibrary = new String[]{serverId + ""};
////        getLoaderManager().restartLoader(StaticData.MY_LIBRARY_LOADER, null, this);
//        onQueryTextChange(null);
//        return false;
//    }
//
//    @Override
//    public boolean onQueryTextSubmit(String query) {
//        return false;
//    }
//
//    @Override
//    public boolean onQueryTextChange(String newText) {
//        if (searchView == null)
//            return false;
//
//        // Called when the action bar search text has changed.  Update
//        // the search filter, and restart the loader to do a new query
//        // with this filter.
//        final String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
//        // Don't do anything if the filter hasn't actually changed.
//        // Prevents restarting the loader when restoring state.
//        if (mCurFilter == null && newFilter == null) {
//            return true;
//        }
//        if (mCurFilter != null && mCurFilter.equals(newFilter)) {
//            return true;
//        }
//        mCurFilter = newFilter;
//
//        if (TextUtils.isEmpty(newText)) {
//
//            selectionDownloader = ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?";
//            selectionMyLibrary = MySongsHelper.COLUMN_USER_ID + " = ?";
//            selectionArgumentsDownloader = new String[]{"0"};
//            selectionArgumentsMyLibrary = new String[]{serverId + ""};
//        } else {
//
//            selectionDownloader = ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ? and (" +
//                    ReachDatabaseHelper.COLUMN_ACTUAL_NAME + " LIKE ? or " +
//                    ReachDatabaseHelper.COLUMN_DISPLAY_NAME + " LIKE ?)";
//            selectionMyLibrary = MySongsHelper.COLUMN_USER_ID + " = ? and (" +
//                    ReachDatabaseHelper.COLUMN_ACTUAL_NAME + " LIKE ? or " +
//                    ReachDatabaseHelper.COLUMN_DISPLAY_NAME + " LIKE ?)";
//            selectionArgumentsDownloader = new String[]{"0",
//                    "%" + mCurFilter + "%",
//                    "%" + mCurFilter + "%"};
//            selectionArgumentsMyLibrary = new String[]{serverId + "",
//                    "%" + mCurFilter + "%",
//                    "%" + mCurFilter + "%"};
//        }
//
//        getLoaderManager().restartLoader(StaticData.DOWNLOAD_LOADER, null, this);
//        getLoaderManager().restartLoader(StaticData.MY_LIBRARY_LOADER, null, this);
//        Log.i("Downloader", "SEARCH SUBMITTED !");
//        return true;
//    }

    @Override
    protected void onNewIntent(Intent intent) {

        Log.d("Ayush", "Received new Intent");
        if (intent != null)
            processIntent(intent);
        super.onNewIntent(intent);
    }

    @SuppressLint("RtlHardcoded")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Pacemaker.scheduleLinear(this, 5);

        preferences = getSharedPreferences("Reach", MODE_PRIVATE);
//        fragmentManager = getSupportFragmentManager();
        reference = new WeakReference<>(this);
        serverId = SharedPrefUtils.getServerId(preferences);
        //track app open event

        final Map<PostParams, String> simpleParams = MiscUtils.getMap(6);
        simpleParams.put(PostParams.USER_ID, serverId + "");
        simpleParams.put(PostParams.DEVICE_ID, MiscUtils.getDeviceId(this));
        simpleParams.put(PostParams.OS, MiscUtils.getOsName());
        simpleParams.put(PostParams.OS_VERSION, Build.VERSION.SDK_INT + "");
        simpleParams.put(PostParams.SCREEN_NAME, "my_reach");
        try {
            simpleParams.put(PostParams.APP_VERSION,
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        try {
            UsageTracker.trackEvent(simpleParams, UsageTracker.APP_OPEN);
        } catch (JSONException ignored) {
        }

        // Setup our Firebase mFirebaseRef
        firebaseReference = new Firebase("https://flickering-fire-7874.firebaseio.com/");
        firebaseReference.keepSynced(true);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        toggleDrawer(true);

        //small
        toggleSliding(false);

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
            new LocalUtils.CheckUpdate().executeOnExecutor(StaticData.temporaryFix);

        //fetch username and phoneNumber
        final String userName = SharedPrefUtils.getUserName(preferences);
        final String phoneNumber = SharedPrefUtils.getUserNumber(preferences);

        //initialize bug tracking
        //Crittercism.initialize(this, "552eac3c8172e25e67906922");
        //Crittercism.setUsername(userName + " " + phoneNumber);

        //initialize MixPanel
        MixpanelAPI mixpanel = MixpanelAPI.getInstance(this, "7877f44b1ce4a4b2db7790048eb6587a");
        MixpanelAPI.People ppl = mixpanel.getPeople();

        //initialize GA tracker
        final Tracker tracker = ((ReachApplication) getApplication()).getTracker();
        tracker.setScreenName("reach.project.core.ReachActivity");

        if (serverId != 0) {

            tracker.set("&uid", serverId + "");
            tracker.send(new HitBuilders.ScreenViewBuilder().setCustomDimension(1, serverId + "").build());
            mixpanel.identify(serverId + "");
            JSONObject props = new JSONObject();
            try {
                props.put("UserID", serverId + "");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mixpanel.registerSuperPropertiesOnce(props);
            ppl.identify(serverId + "");
            ppl.set("UserID", serverId + "");
        } else
            tracker.send(new HitBuilders.ScreenViewBuilder().build());
        if (!TextUtils.isEmpty(phoneNumber))
            ppl.set("$phone", phoneNumber + "");
        if (!TextUtils.isEmpty(userName))
            ppl.set("$name", userName + "");

        //first check playServices
        if (!LocalUtils.checkPlayServices(this)) {
            tracker.send(new HitBuilders.EventBuilder("Play Services screwup", userName + " " + phoneNumber + " screwed up").build());
            return; //fail
        }

        if (serverId == 0 || TextUtils.isEmpty(phoneNumber)) {

            startNumberVerification();
            toggleSliding(false);
        } else if (TextUtils.isEmpty(userName)) {

            startAccountCreation(Optional.absent());
            toggleSliding(false);
        } else {

            final CustomViewPager viewPager = (CustomViewPager) findViewById(R.id.mainViewPager);
            viewPager.setPagingEnabled(false);
            viewPager.setOffscreenPageLimit(5);

            final Fragment[] fragments = new Fragment[]{

                    MyReachFragment.newInstance(),
                    PushSongsFragment.newInstance(),
                    ExploreFragment.newInstance(serverId),
                    PagerFragment.getNewInstance(
                            new PagerFragment.Pages(
                                    new Class[]{ApplicationFragment.class},
                                    new String[]{""},
                                    "Apps"),
                            new PagerFragment.Pages(
                                    new Class[]{MyLibraryFragment.class},
                                    new String[]{"My Library"},
                                    "Songs")),
                    PrivacyFragment.newInstance(false),
            };

            viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
                @Override
                public Fragment getItem(int position) {
                    return fragments[position];
                }

                @Override
                public int getCount() {
                    return fragments.length;
                }
            });

            final TabLayout tabLayout = (TabLayout) findViewById(R.id.mainTabLayout);
            final int[] tabUnselectedIcons = new int[]{

                    R.drawable.icon_friends_gray,
                    R.drawable.icon_send_gray,
                    R.drawable.icon_grey,
                    R.drawable.icon_download_gray,
                    R.drawable.icon_myprofile_gray,
            };
            final int[] tabSelectedIcons = new int[]{

                    R.drawable.icon_friends_gray,
                    R.drawable.icon_send_pink,
                    R.drawable.icon_plain,
                    R.drawable.icon_download_pink,
                    R.drawable.icon_myprofile_pink,
            };
            tabLayout.setupWithViewPager(viewPager);

            for (int i = 0; i < tabLayout.getTabCount(); i++) {
                final TabLayout.Tab tab = tabLayout.getTabAt(i);
                if (tab != null)
                    tab.setIcon(tabUnselectedIcons[i]);
            }
            tabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    super.onTabSelected(tab);
                    tab.setIcon(tabSelectedIcons[tab.getPosition()]);
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    super.onTabUnselected(tab);
                    tab.setIcon(tabUnselectedIcons[tab.getPosition()]);
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    super.onTabReselected(tab);
                    tab.setIcon(tabSelectedIcons[tab.getPosition()]);
                }
            });

            /*tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    tab.setIcon(tabSelectedIcons[tab.getPosition()]);
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    tab.setIcon(tabUnselectedIcons[tab.getPosition()]);
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    tab.setIcon(tabSelectedIcons[tab.getPosition()]);
                }
            });*/

            lastSong();
            //some stuff
            if (networkPresent)
                AsyncTask.SERIAL_EXECUTOR.execute(LocalUtils.networkOps);
        }
    }

    private void lastSong() {

        final Boolean[] toSend = new Boolean[]{false, false, false};
        currentPlaying = SharedPrefUtils.getLastPlayed(getSharedPreferences("reach_process", MODE_PRIVATE)).orNull();

        toSend[0] = (currentPlaying != null);
        toSend[1] = SharedPrefUtils.getShuffle(preferences);
        toSend[2] = SharedPrefUtils.getRepeat(preferences);
    }

    private synchronized void processIntent(Intent intent) {

        Log.i("Ayush", "Processing Intent");

        if (intent.getBooleanExtra("openNotificationFragment", false))
            onOpenNotificationDrawer();
//        else if (intent.getBooleanExtra("openFriendRequests", false)) {
//            if (viewPager != null)
//                viewPager.setCurrentItem(0);
//        } else if (intent.getBooleanExtra("openNotifications", false)) {
//            if (viewPager != null)
//                viewPager.setCurrentItem(1);
        else if (!TextUtils.isEmpty(intent.getAction()) && intent.getAction().equals("process_multiple")) {

            Log.i("Ayush", "FOUND PUSH DATA");

            final String compressed = intent.getStringExtra("data");
            String unCompressed;
            try {
                unCompressed = StringCompress.decompress(Base64.decode(compressed, Base64.DEFAULT));
            } catch (IOException e) {
                e.printStackTrace();
                unCompressed = "";
            }

            Log.i("Ayush", "UNCOMPRESSED " + unCompressed);

            if (!TextUtils.isEmpty(unCompressed)) {

                final PushContainer pushContainer = new Gson().fromJson(unCompressed, PushContainer.class);

                Log.i("Ayush", "FOUND PUSH CONTAINER");

                if (pushContainer != null && !TextUtils.isEmpty(pushContainer.getSongData())) {

                    final HashSet<TransferSong> transferSongs = new Gson().fromJson(
                            pushContainer.getSongData(),
                            new TypeToken<HashSet<TransferSong>>() {
                            }.getType());

                    if (transferSongs != null && !transferSongs.isEmpty()) {

                        for (TransferSong transferSong : transferSongs) {

                            if (transferSong == null)
                                continue;

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
                                    transferSong.getDuration(),
                                    transferSong.getAlbumName(),
                                    transferSong.getGenre());
                        }
                        new LocalUtils.RefreshOperations().executeOnExecutor(StaticData.temporaryFix);
                    }
                }
            }
        }

        intent.removeExtra("openNotificationFragment");
        intent.removeExtra("openPlayer");
        intent.removeExtra("openFriendRequests");
        intent.removeExtra("openNotifications");
    }

//    @Override
//    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
//
//        if (id == StaticData.DOWNLOAD_LOADER) {
//
//            return new CursorLoader(this,
//                    ReachDatabaseProvider.CONTENT_URI,
//                    ReachDatabaseHelper.ADAPTER_LIST,
//                    selectionDownloader,
//                    selectionArgumentsDownloader,
//                    ReachDatabaseHelper.COLUMN_DATE_ADDED + " DESC");
//        } else if (id == StaticData.MY_LIBRARY_LOADER) {
//
//            return new CursorLoader(this,
//                    MySongsProvider.CONTENT_URI,
//                    MySongsHelper.DISK_LIST,
//                    selectionMyLibrary,
//                    selectionArgumentsMyLibrary,
//                    MySongsHelper.COLUMN_DISPLAY_NAME + " ASC");
//        }
//
//        return null;
//    }
//
//    @Override
//    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//
//        if (loader.getId() == StaticData.MY_LIBRARY_LOADER && data != null && !data.isClosed()) {
//
//            musicAdapter.swapCursor(data);
//            if (data.getCount() == 0 && queueListView != null)
//                combinedAdapter.setActive(emptyTV2, true);
//            else
//                combinedAdapter.setActive(emptyTV2, false);
//        } else if (loader.getId() == StaticData.DOWNLOAD_LOADER && data != null && !data.isClosed()) {
//
//            queueAdapter.swapCursor(data);
//            if (data.getCount() == 0 && queueListView != null)
//                combinedAdapter.setActive(emptyTV1, true);
//            else
//                combinedAdapter.setActive(emptyTV1, false);
//        }
//    }
//
//    @Override
//    public void onLoaderReset(Loader<Cursor> loader) {
//
//        if (loader.getId() == StaticData.MY_LIBRARY_LOADER)
//            musicAdapter.swapCursor(null);
//        if (loader.getId() == StaticData.DOWNLOAD_LOADER)
//            queueAdapter.swapCursor(null);
//    }

    @Override
    public void addSongToQueue(long songId, long senderId, long size,
                               String displayName, String actualName,
                               boolean multiple, String userName, String onlineStatus,
                               String networkType, String artistName, long duration,
                               String albumName, String genre) {

        final ContentResolver contentResolver = getContentResolver();
        if (contentResolver == null)
            return;

        /**
         * DISPLAY_NAME, ACTUAL_NAME, SIZE & DURATION all can not be same, effectively its a hash
         */

        final Cursor cursor;
        if (multiple)
            cursor = contentResolver.query(
                    ReachDatabaseProvider.CONTENT_URI,
                    new String[]{ReachDatabaseHelper.COLUMN_ID},
                    ReachDatabaseHelper.COLUMN_DISPLAY_NAME + " = ? and " +
                            ReachDatabaseHelper.COLUMN_ACTUAL_NAME + " = ? and " +
                            ReachDatabaseHelper.COLUMN_SIZE + " = ? and " +
                            ReachDatabaseHelper.COLUMN_DURATION + " = ?",
                    new String[]{displayName, actualName, size + "", duration + ""},
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
                            ReachDatabaseHelper.COLUMN_SIZE,

                    },

                    ReachDatabaseHelper.COLUMN_DISPLAY_NAME + " = ? and " +
                            ReachDatabaseHelper.COLUMN_ACTUAL_NAME + " = ? and " +
                            ReachDatabaseHelper.COLUMN_SIZE + " = ? and " +
                            ReachDatabaseHelper.COLUMN_DURATION + " = ?",
                    new String[]{displayName, actualName, size + "", duration + ""},
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
                    MiscUtils.playSong(musicData, this);
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
        reachDatabase.setUniqueId(secureRandom.nextInt(Integer.MAX_VALUE));

        reachDatabase.setDuration(duration);
        reachDatabase.setLogicalClock((short) 0);
        reachDatabase.setStatus(ReachDatabase.NOT_WORKING);

        reachDatabase.setLastActive(0);
        reachDatabase.setReference(0);

        reachDatabase.setAlbumName(albumName);
        reachDatabase.setGenre(genre);

        reachDatabase.setVisibility((short) 1);

        //We call bulk starter always
        final Uri uri = contentResolver.insert(ReachDatabaseProvider.CONTENT_URI,
                ReachDatabaseHelper.contentValuesCreator(reachDatabase));
        if (uri == null) {

            ((ReachApplication) getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                    .setCategory("Add song failed")
                    .setAction("User Name - " + SharedPrefUtils.getUserName(preferences))
                    .setLabel("Song - " + reachDatabase.getDisplayName() + ", From - " + reachDatabase.getSenderId())
                    .setValue(1)
                    .build());
            return;
        }

        final String[] splitter = uri.toString().split("/");
        if (splitter.length == 0)
            return;
        reachDatabase.setId(Long.parseLong(splitter[splitter.length - 1].trim()));
        //start this operation
        if (!multiple)
            StaticData.temporaryFix.execute(MiscUtils.startDownloadOperation(
                    this,
                    reachDatabase,
                    reachDatabase.getReceiverId(), //myID
                    reachDatabase.getSenderId(),   //the uploaded
                    reachDatabase.getId()));

        //tracing shit

        ((ReachApplication) getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                .setCategory("Transaction - Add SongBrainz")
                .setAction("User Name - " + SharedPrefUtils.getUserName(preferences))
                .setLabel("SongBrainz - " + reachDatabase.getDisplayName() + ", From - " + reachDatabase.getSenderId())
                .setValue(1)
                .build());

        MixpanelAPI mixpanel = MixpanelAPI.getInstance(this, "7877f44b1ce4a4b2db7790048eb6587a");
        JSONObject props = new JSONObject();
        try {
            props.put("User Name", SharedPrefUtils.getUserName(getSharedPreferences("Reach", Context.MODE_PRIVATE)));
            props.put("From", reachDatabase.getDisplayName());
            props.put("Song", reachDatabase.getSenderId());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mixpanel.track("Transaction - Add Song", props);

        //usage tracking
        final Map<PostParams, String> simpleParams = MiscUtils.getMap(6);
        simpleParams.put(PostParams.USER_ID, serverId + "");
        simpleParams.put(PostParams.DEVICE_ID, MiscUtils.getDeviceId(this));
        simpleParams.put(PostParams.OS, MiscUtils.getOsName());
        simpleParams.put(PostParams.OS_VERSION, Build.VERSION.SDK_INT + "");
        try {
            simpleParams.put(PostParams.APP_VERSION,
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        simpleParams.put(PostParams.SCREEN_NAME, "unknown");

        final Map<SongMetadata, String> complexParams = MiscUtils.getMap(5);
        complexParams.put(SongMetadata.SONG_ID, reachDatabase.getSongId() + "");
        complexParams.put(SongMetadata.ARTIST, reachDatabase.getArtistName());
        complexParams.put(SongMetadata.TITLE, reachDatabase.getDisplayName());
        complexParams.put(SongMetadata.DURATION, reachDatabase.getDuration() + "");
        complexParams.put(SongMetadata.SIZE, reachDatabase.getLength() + "");

        try {
            UsageTracker.trackSong(simpleParams, complexParams, UsageTracker.DOWNLOAD_SONG);
        } catch (JSONException ignored) {
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

            if (SharedPrefUtils.toggleRepeat(view.getContext().getSharedPreferences("Reach", MODE_PRIVATE)))
                view.setSelected(true);
            else
                view.setSelected(false);
        };

        public static final View.OnClickListener nextClick = v -> ProcessManager.submitMusicRequest(
                v.getContext(),
                Optional.absent(),
                MusicHandler.ACTION_NEXT);

        public static final View.OnClickListener previousClick = v -> ProcessManager.submitMusicRequest(
                v.getContext(),
                Optional.absent(),
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

                    //usage tracking
                    final Map<PostParams, String> simpleParams = MiscUtils.getMap(6);
                    simpleParams.put(PostParams.USER_ID, serverId + "");
                    simpleParams.put(PostParams.DEVICE_ID, MiscUtils.getDeviceId(context));
                    simpleParams.put(PostParams.OS, MiscUtils.getOsName());
                    simpleParams.put(PostParams.OS_VERSION, Build.VERSION.SDK_INT + "");
                    try {
                        simpleParams.put(PostParams.APP_VERSION,
                                context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    simpleParams.put(PostParams.SCREEN_NAME, "unknown");

                    final Map<SongMetadata, String> complexParams = MiscUtils.getMap(5);
                    complexParams.put(SongMetadata.SONG_ID, currentPlaying.getId() + "");
                    complexParams.put(SongMetadata.ARTIST, currentPlaying.getArtistName());
                    complexParams.put(SongMetadata.TITLE, currentPlaying.getDisplayName());
                    complexParams.put(SongMetadata.DURATION, currentPlaying.getDuration() + "");
                    complexParams.put(SongMetadata.SIZE, currentPlaying.getLength() + "");

                    try {
                        UsageTracker.trackSong(simpleParams, complexParams, UsageTracker.LIKE_SONG);
                    } catch (JSONException ignored) {
                    }

                    MiscUtils.autoRetryAsync(() -> StaticData.notificationApi.addLike(
                            currentPlaying.getSenderId(),
                            serverId,
                            currentPlaying.getDisplayName()).execute(), Optional.absent());
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
                        Optional.absent(),
                        MusicHandler.ACTION_PLAY_PAUSE);
        };

        public static final View.OnClickListener shuffleClick = view -> {

            if (SharedPrefUtils.toggleShuffle(view.getContext().getSharedPreferences("Reach", MODE_PRIVATE)))
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
                MiscUtils.playSong(ReachDatabaseHelper.getMusicData(cursor), context);
            else
                MiscUtils.playSong(MySongsHelper.getMusicData(cursor, serverId), context);
        };

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

                final MyString dataToReturn = MiscUtils.autoRetry(() -> StaticData.userEndpoint.getGcmId(serverId).execute(), Optional.absent()).orNull();

                //check returned gcm
                final String gcmId;
                if (dataToReturn == null || //fetch failed
                        TextUtils.isEmpty(gcmId = dataToReturn.getString()) || //null gcm
                        gcmId.equals("hello_world")) { //bad gcm

                    //network operation
                    if (MiscUtils.updateGCM(serverId, reference))
                        Log.i("Ayush", "GCM updated !");
                    else
                        Log.i("Ayush", "GCM check failed");
                }
            }

            @Override
            public void run() {

                ////////////////////////////////////////
                //check devikaChat token
                MiscUtils.useActivity(reference, activity -> MiscUtils.checkChatToken(
                        new WeakReference<>(activity.preferences),
                        new WeakReference<>(activity.firebaseReference),
                        reference));
                //refresh gcm
                checkGCM();
                //refresh download ops
                new LocalUtils.RefreshOperations().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
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
//                        try {
//                            updateFragment.show(activity.fragmentManager, "update");
//                        } catch (IllegalStateException | WindowManager.BadTokenException ignored) {
//                            activity.finish();
//                        }
                    }
                    return null;
                });
            }
        }

        //TODO optimize database fetch !
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
            }
        }

        public static final ChildEventListener listenerForUnReadChats = new ChildEventListener() {

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                if (dataSnapshot.getValue() instanceof Map) {

                    final Map value = (Map) dataSnapshot.getValue();
                    final Object admin = value.get("admin");
                    final Object status = value.get("status");

                    final String adminValue = String.valueOf(admin);
                    final String statusValue = String.valueOf(status);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        };
    }
}