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
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.squareup.wire.Wire;

import org.json.JSONException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import reach.backend.entities.messaging.model.MyBoolean;
import reach.backend.entities.userApi.model.MyString;
import reach.project.R;
import reach.project.coreViews.UpdateFragment;
import reach.project.coreViews.explore.ExploreFragment;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.coreViews.fileManager.apps.ApplicationFragment;
import reach.project.coreViews.fileManager.music.downloading.DownloadingFragment;
import reach.project.coreViews.fileManager.music.myLibrary.MyLibraryFragment;
import reach.project.coreViews.friends.ContactsListFragment;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.myProfile.MyProfileFragment;
import reach.project.coreViews.yourProfile.YourProfileActivity;
import reach.project.music.Song;
import reach.project.pacemaker.Pacemaker;
import reach.project.push.PushContainer;
import reach.project.reachProcess.auxiliaryClasses.Connection;
import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.reachProcess.reachService.ProcessManager;
import reach.project.usageTracking.PostParams;
import reach.project.usageTracking.UsageTracker;
import reach.project.utils.MetaDataScanner;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.StringCompress;
import reach.project.utils.auxiliaryClasses.SuperInterface;
import reach.project.utils.viewHelpers.CustomViewPager;
import reach.project.utils.viewHelpers.PagerFragment;

public class ReachActivity extends AppCompatActivity implements SuperInterface {

    public static String OPEN_MY_FRIENDS = "OPEN_MY_FRIENDS";
    public static String OPEN_MY_PROFILE_APPS = "OPEN_MY_PROFILE_APPS";
    public static String OPEN_MY_PROFILE_MUSIC = "OPEN_MY_PROFILE_MUSIC";
    public static String OPEN_PUSH = "OPEN_PUSH";

    public static long serverId = 0;
    private static WeakReference<ReachActivity> reference = null;
    private static SecureRandom secureRandom = new SecureRandom();

    private static final int MY_PERMISSIONS_READ_CONTACTS = 11;
    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 22;

    public static void openDownloading() {

        MiscUtils.useActivity(reference, activity -> {

            if (activity.viewPager == null)
                return;

            activity.viewPager.setCurrentItem(3, true);
            downloadPager.setItem(1);
        });
    }

    ////////////////////////////////////////

    @SuppressWarnings("unchecked")
    private static final PagerFragment downloadPager = PagerFragment.getNewInstance("Manager",
            new PagerFragment.Pages(
                    new Class[]{ApplicationFragment.class},
                    new String[]{"My Applications"},
                    "Apps"),
            new PagerFragment.Pages(
                    new Class[]{DownloadingFragment.class, MyLibraryFragment.class},
                    new String[]{"Downloading", "My Library"},
                    "Songs"));

    private static final int[] tabUnselectedIcons = new int[]{

            R.drawable.icon_friends_gray,
            R.drawable.icon_send_gray,
            R.drawable.icon_reach_magnet_gray,
            R.drawable.icon_download_gray,
            R.drawable.icon_myprofile_gray,
    };

    private static final int[] tabSelectedIcons = new int[]{

            R.drawable.icon_friends_pink,
            R.drawable.icon_send_pink,
            R.drawable.icon_reach_magnet_pink,
            R.drawable.icon_download_pink,
            R.drawable.icon_myprofile_pink,
    };

    @Nullable
    private CustomViewPager viewPager = null;

    private final PagerAdapter mainPager = new FragmentPagerAdapter(getSupportFragmentManager()) {
        @Override
        public Fragment getItem(int position) {

            switch (position) {

                case 0:
                    return ContactsListFragment.newInstance();
                case 1:
                    return PagerFragment.getNewInstance("Push",
                            new PagerFragment.Pages(
                                    new Class[]{reach.project.coreViews.push.apps.ApplicationFragment.class},
                                    new String[]{"My Applications"},
                                    "Apps"),
                            new PagerFragment.Pages(
                                    new Class[]{reach.project.coreViews.push.myLibrary.MyLibraryFragment.class},
                                    new String[]{"My Library"},
                                    "Songs"));
                case 2:
                    return ExploreFragment.newInstance(serverId);
                case 3:
                    return downloadPager;
                case 4:
                    return MyProfileFragment.newInstance();

                default:
                    throw new IllegalStateException("only 5 tabs expected");
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    ////////////////////////////////////////

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (reference != null)
            reference.clear();
        reference = null;
        viewPager = null;
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

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != 0) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS))
                    Toast.makeText(this, "Permission to access Contacts is required to use the App", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_CONTACTS
                        }, MY_PERMISSIONS_READ_CONTACTS);
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != 0) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    Toast.makeText(this, "Permission to access Storage is required to use the App", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        }, MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
            }
        }

        super.onResume();
    }

    @Override
    protected void onPostResume() {

        super.onPostResume();
        Log.i("Ayush", "Called onPostResume");
        processIntent(getIntent());
    }

    @Override
    public void onOpenLibrary(long userId) {

        if (!isFinishing())
            YourProfileActivity.openProfile(userId, this);
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
        processIntent(intent);
        super.onNewIntent(intent);
    }

    @SuppressLint("RtlHardcoded")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reach);

        reference = new WeakReference<>(this);

        //gcm keep-alive
        Pacemaker.scheduleLinear(this, 5);

        final SharedPreferences preferences = getSharedPreferences("Reach", MODE_PRIVATE);
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

        //fetch username and phoneNumber
        final String userName = SharedPrefUtils.getUserName(preferences);
        final String phoneNumber = SharedPrefUtils.getPhoneNumber(preferences);

        //initialize bug tracking
        //Crittercism.initialize(this, "552eac3c8172e25e67906922");
        //Crittercism.setUsername(userName + " " + phoneNumber);

        //track screen
        final Tracker tracker = ((ReachApplication) getApplication()).getTracker();
        tracker.setScreenName("reach.project.core.ReachActivity");
        tracker.set("&uid", serverId + "");
        tracker.send(new HitBuilders.ScreenViewBuilder().setCustomDimension(1, serverId + "").build());

        //first check playServices
        if (!LocalUtils.checkPlayServices(this)) {
            tracker.send(new HitBuilders.EventBuilder("Play Services screwup", userName + " " + phoneNumber + " screwed up").build());
            return; //fail
        }

        ////////////////////////////////////////

        viewPager = (CustomViewPager) findViewById(R.id.mainViewPager);
        viewPager.setPagingEnabled(false);
        viewPager.setOffscreenPageLimit(5);
        viewPager.setAdapter(mainPager);

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.mainTabLayout);
        tabLayout.setupWithViewPager(viewPager);
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

        final int selectedTabPosition = tabLayout.getSelectedTabPosition();
        final TabLayout.Tab selectedTab = tabLayout.getTabAt(selectedTabPosition);
        if (selectedTab != null)
            selectedTab.setIcon(tabSelectedIcons[selectedTabPosition]);

        for (int index = 1; index < tabLayout.getTabCount(); index++) {

            final TabLayout.Tab tab = tabLayout.getTabAt(index);
            if (tab != null) {
                tab.setIcon(tabUnselectedIcons[index]);
            }
        }

        //routine stuff
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

        if (networkPresent) {

            //refresh gcm
            StaticData.temporaryFix.submit(LocalUtils::checkGCM);
            //refresh download ops
            new LocalUtils.RefreshOperations().executeOnExecutor(StaticData.temporaryFix);
            //Music scanner TODO perm check
            MiscUtils.useContextFromContext(reference, activity -> {

                final Intent intent = new Intent(activity, MetaDataScanner.class);
                intent.putExtra("first", false);
                activity.startService(intent);
                return null;
            });
        }
    }

    private synchronized void processIntent(Intent intent) {

        if (intent == null)
            return;

        Log.i("Ayush", "Processing Intent");

        if (intent.getBooleanExtra("firstTime", false)) {
            if (viewPager != null)
                viewPager.setCurrentItem(5, true);
        }

//        if (intent.getBooleanExtra("openNotificationFragment", false))
//            onOpenNotificationDrawer();
//        else if (intent.getBooleanExtra("openFriendRequests", false)) {
//            if (viewPager != null)
//                viewPager.setCurrentItem(0);
//        } else if (intent.getBooleanExtra("openNotifications", false)) {
//            if (viewPager != null)
//                viewPager.setCurrentItem(1);
//        else
        if (!TextUtils.isEmpty(intent.getAction()) && intent.getAction().equals("process_multiple")) {

            Log.i("Ayush", "FOUND PUSH DATA");

            final String compressed = intent.getStringExtra("data");

            byte[] unCompressed;
            try {
                unCompressed = StringCompress.deCompressStringToBytes(compressed);
            } catch (IOException e) {
                e.printStackTrace();
                unCompressed = null;
            }

            if (unCompressed != null && unCompressed.length > 0) {

                PushContainer pushContainer;
                try {
                    pushContainer = new Wire(PushContainer.class).parseFrom(unCompressed, PushContainer.class);
                } catch (IOException e) {
                    e.printStackTrace();
                    pushContainer = null;
                }

                if (pushContainer != null && pushContainer.song != null && !pushContainer.song.isEmpty()) {

                    for (Song song : pushContainer.song) {

                        if (song == null)
                            continue;

                        addSongToQueue(song.songId,
                                pushContainer.senderId,
                                song.size,
                                song.displayName,
                                song.actualName,
                                true,
                                pushContainer.userName,
                                ReachFriendsHelper.ONLINE_REQUEST_GRANTED + "",
                                "0",
                                song.artist,
                                song.duration,
                                song.album,
                                song.genre);
                    }
                    new LocalUtils.RefreshOperations().executeOnExecutor(StaticData.temporaryFix);
                }
                ///////////
            }
        }

        intent.removeExtra("openNotificationFragment");
        intent.removeExtra("openPlayer");
        intent.removeExtra("openFriendRequests");
        intent.removeExtra("openNotifications");
    }

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
                            0,
                            cursor.getString(2),
                            displayName,
                            artistName,
                            "",
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
        MiscUtils.startDownload(reachDatabase, this, null);
    }

    private enum LocalUtils {
        ;

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

        public static void checkGCM() {

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

    }
}