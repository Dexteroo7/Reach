package reach.project.core;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LongSparseArray;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.common.collect.ImmutableList;
import com.squareup.wire.Wire;

import org.json.JSONException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;

import reach.project.R;
import reach.project.ancillaryViews.SettingsActivity;
import reach.project.apps.App;
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
import reach.project.coreViews.push.PushActivity;
import reach.project.coreViews.push.PushContainer;
import reach.project.music.Song;
import reach.project.notificationCentre.NotificationActivity;
import reach.project.player.PlayerActivity;
import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.usageTracking.PostParams;
import reach.project.usageTracking.UsageTracker;
import reach.project.utils.FireOnce;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.StringCompress;
import reach.project.utils.ancillaryClasses.SuperInterface;
import reach.project.utils.viewHelpers.PagerFragment;

public class ReachActivity extends AppCompatActivity implements SuperInterface {

    public static void openActivity(Context context) {

        final Intent intent = new Intent(context, ReachActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }

    public static Intent getIntent(Context context) {

        final Intent intent = new Intent(context, ReachActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return intent;
    }

    public static void openDownloading() {

        /*MiscUtils.useActivity(reference, activity -> {

            if (activity.viewPager == null)
                return;

            activity.viewPager.setCurrentItem(3, true);
            DOWNLOAD_PAGER.setItem(1);
        });*/
    }

    ////////////////////////////////////////public static final

    public static final String OPEN_MY_FRIENDS = "OPEN_MY_FRIENDS";
    public static final String OPEN_PUSH = "OPEN_PUSH";
    public static final String OPEN_MY_PROFILE_APPS = "OPEN_MY_PROFILE_APPS";
    public static final String OPEN_MY_PROFILE_MUSIC = "OPEN_MY_PROFILE_MUSIC";
    public static final String ADD_PUSH_SONG = "ADD_PUSH_SONG";

    public static final Set<Song> SELECTED_SONGS = MiscUtils.getSet(5);
    public static final Set<App> SELECTED_APPS = MiscUtils.getSet(5);
    public static final LongSparseArray<Boolean> SELECTED_SONG_IDS = new LongSparseArray<>(5);
    private FragmentManager fragmentManager;

    ////////////////////////////////////////private static final

    private static final SecureRandom ID_GENERATOR = new SecureRandom();
//    private static final int MY_PERMISSIONS_READ_CONTACTS = 11;
//    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 22;

    @SuppressWarnings("unchecked")
    private static final PagerFragment DOWNLOAD_PAGER = PagerFragment.getNewInstance("Manager",
            new PagerFragment.Pages(
                    new Class[]{ApplicationFragment.class},
                    new String[]{"My Applications"},
                    "Apps"),
            new PagerFragment.Pages(
                    new Class[]{DownloadingFragment.class, MyLibraryFragment.class},
                    new String[]{"Downloading", "My Library"},
                    "Songs"));

    @SuppressWarnings("unchecked")
    private static final PagerFragment PUSH_PAGER = PagerFragment.getNewInstance("Push",
//            new PagerFragment.Pages(
//                    new Class[]{reach.project.coreViews.push.apps.ApplicationFragment.class},
//                    new String[]{"My Applications"},
//                    "Apps"),
            new PagerFragment.Pages(
                    new Class[]{reach.project.coreViews.push.music.MyLibraryFragment.class},
                    new String[]{"My Library"},
                    "Songs"));

    private static final int[] UNSELECTED_ICONS = new int[]{

            R.layout.tab_icon,
            R.layout.tab_icon2,
            R.layout.tab_icon3,
            R.layout.tab_icon4,
            R.layout.tab_icon5
    };

    private static final int[] SELECTED_ICONS = new int[]{

            R.layout.tab_icon6,
            R.layout.tab_icon7,
            R.layout.tab_icon8,
            R.layout.tab_icon9,
            R.layout.tab_icon10
    };

    ////////////////////////////////////////

    private final Toolbar.OnMenuItemClickListener menuClickListener = item -> {

        switch (item.getItemId()) {

            case R.id.push_button: {

                if (SELECTED_SONGS.isEmpty() && SELECTED_APPS.isEmpty()) {
                    Toast.makeText(this, "First select some songs", Toast.LENGTH_SHORT).show();
                    return false;
                }

                final SharedPreferences preferences = getSharedPreferences("Reach", Context.MODE_PRIVATE);
                final PushContainer pushContainer = new PushContainer.Builder()
                        .senderId(SharedPrefUtils.getServerId(preferences))
                        .userName(SharedPrefUtils.getUserName(preferences))
                        .userImage(SharedPrefUtils.getImageId(preferences))
                        .firstSongName(SELECTED_SONGS.isEmpty() ? "" : SELECTED_SONGS.iterator().next().displayName)
                        .firstAppName(SELECTED_APPS.isEmpty() ? "" : SELECTED_APPS.iterator().next().applicationName)
                        .song(ImmutableList.copyOf(SELECTED_SONGS))
                        .app(ImmutableList.copyOf(SELECTED_APPS))
                        .songCount(SELECTED_SONGS.size())
                        .appCount(SELECTED_APPS.size())
                        .build();

                try {
                    PushActivity.startPushActivity(pushContainer, this);
                } catch (IOException e) {

                    e.printStackTrace();
                    //TODO Track
                    Toast.makeText(this, "Could not push", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            case R.id.player_button:
                final Intent playerIntent = new Intent(this, PlayerActivity.class);
                playerIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(playerIntent);
                return true;
            case R.id.notif_button:
                final Intent notificationIntent = new Intent(this, NotificationActivity.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(notificationIntent);
                return true;
            case R.id.settings_button:
                final Intent settingsIntent = new Intent(ReachActivity.this, SettingsActivity.class);
                settingsIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(settingsIntent);
                return true;
        }

        return false;
    };

    //@Nullable
    //private CustomViewPager viewPager = null;
    @Nullable
    private static WeakReference<ReachActivity> reference = null;
    private static long serverId = 0;

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (reference != null)
            reference.clear();
        reference = null;
        //viewPager = null;
    }

    /*@Override
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
    }*/

    @Override
    protected void onPostResume() {

        super.onPostResume();
        Log.i("Ayush", "Called onPostResume");
        processIntent(getIntent());

        /*if (Build.VERSION.SDK_INT >= 23) {

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
        }*/
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

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reach);

        reference = new WeakReference<>(this);

        final SharedPreferences preferences = getSharedPreferences("Reach", MODE_PRIVATE);
        serverId = SharedPrefUtils.getServerId(preferences);
        fragmentManager = getSupportFragmentManager();

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

        //initialize bug tracking
        //Crittercism.initialize(this, "552eac3c8172e25e67906922");
        //Crittercism.setUsername(userName + " " + phoneNumber);

        ////////////////////////////////////////

        /*viewPager = (CustomViewPager) findViewById(R.id.mainViewPager);
        viewPager.setPagingEnabled(false);
        viewPager.setOffscreenPageLimit(5);
        viewPager.setAdapter(new FragmentPagerAdapter(fragmentManager) {
            @Override
            public Fragment getItem(int position) {

                switch (position) {

                    case 0:
                        return ContactsListFragment.getInstance();
                    case 1:
                        return PUSH_PAGER;
                    case 2:
                        return ExploreFragment.newInstance(serverId);
                    case 3:
                        return DOWNLOAD_PAGER;
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
        });*/

        final ContactsListFragment fragment1 = ContactsListFragment.getInstance();
        final ExploreFragment fragment3 = ExploreFragment.newInstance(serverId);
        final MyProfileFragment fragment5 = MyProfileFragment.newInstance();

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.mainTabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("1"));
        tabLayout.addTab(tabLayout.newTab().setText("2"));
        tabLayout.addTab(tabLayout.newTab().setText("3"));
        tabLayout.addTab(tabLayout.newTab().setText("4"));
        tabLayout.addTab(tabLayout.newTab().setText("5"));
        for (int index = 0; index < tabLayout.getTabCount(); index++) {
            final TabLayout.Tab tab = tabLayout.getTabAt(index);
            if (tab != null) {
                tab.setCustomView(UNSELECTED_ICONS[index]);
            }
        }
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if(tabLayout.getSelectedTabPosition() == 0){
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment1).commit();
                }else if(tabLayout.getSelectedTabPosition() == 1){
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, PUSH_PAGER).commit();
                }else if(tabLayout.getSelectedTabPosition() == 2){
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment3).commit();
                }else if(tabLayout.getSelectedTabPosition() == 3){
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, DOWNLOAD_PAGER).commit();
                }else if(tabLayout.getSelectedTabPosition() == 4){
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment5).commit();
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        tabLayout.getTabAt(2).select();

        //tabLayout.setupWithViewPager(viewPager);
        /*for (int index = 1; index < tabLayout.getTabCount(); index++) {
            final TabLayout.Tab tab = tabLayout.getTabAt(index);
            if (tab != null) {
                tab.setCustomView(UNSELECTED_ICONS[index]);
            }
        }

        final int selectedTabPosition = tabLayout.getSelectedTabPosition();
        final TabLayout.Tab selectedTab = tabLayout.getTabAt(selectedTabPosition);
        if (selectedTab != null) {
            selectedTab.setCustomView(SELECTED_ICONS[selectedTabPosition]);
        }

        tabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                super.onTabSelected(tab);
                tab.setCustomView(null);
                tab.setCustomView(SELECTED_ICONS[tab.getPosition()]);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                super.onTabUnselected(tab);
                tab.setCustomView(null);
                tab.setCustomView(UNSELECTED_ICONS[tab.getPosition()]);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                super.onTabReselected(tab);
                tab.setCustomView(null);
                tab.setCustomView(SELECTED_ICONS[tab.getPosition()]);
            }
        });*/

        //viewPager.setCurrentItem(2);

        //check for update, need activity to check
        FireOnce.checkUpdate(reference);
    }

    private synchronized void processIntent(Intent intent) {

        if (intent == null)
            return;

        Log.i("Ayush", "Processing Intent");

        /*if (intent.getBooleanExtra("firstTime", false)) {
            if (viewPager != null)
                viewPager.setCurrentItem(5, false);
        }*/

//        if (intent.getBooleanExtra("openNotificationFragment", false))
//            onOpenNotificationDrawer();
//        else if (intent.getBooleanExtra("openFriendRequests", false)) {
//            if (viewPager != null)
//                viewPager.setCurrentItem(0);
//        } else if (intent.getBooleanExtra("openNotifications", false)) {
//            if (viewPager != null)
//                viewPager.setCurrentItem(1);
//        else
        if (!TextUtils.isEmpty(intent.getAction()) && intent.getAction().equals(ADD_PUSH_SONG)) {

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
                    FireOnce.refreshOperations(reference);
                    openDownloading();
                }
                ///////////
            }
        }

//        intent.removeExtra("openNotificationFragment");
//        intent.removeExtra("openPlayer");
//        intent.removeExtra("openFriendRequests");
//        intent.removeExtra("openNotifications");
    }

    @Override
    public Toolbar.OnMenuItemClickListener getMenuClickListener() {
        return menuClickListener;
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
        reachDatabase.setUniqueId(ID_GENERATOR.nextInt(Integer.MAX_VALUE));

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
}