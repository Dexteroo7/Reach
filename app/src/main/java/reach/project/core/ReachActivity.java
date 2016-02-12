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
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.LongSparseArray;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.crittercism.app.Crittercism;
import com.google.common.collect.ImmutableList;
import com.squareup.wire.Wire;

import org.json.JSONException;

import java.io.IOException;
import java.lang.ref.WeakReference;
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
import reach.project.coreViews.friends.FriendsFragment;
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
import reach.project.utils.ThreadLocalRandom;
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

    /*public static void openDownloading() {

        MiscUtils.useActivity(reference, activity -> {

            if (activity.mTabHost == null)
                return;

            activity.mTabHost.postDelayed(() -> {
                activity.mTabHost.setCurrentTab(3);
                activity.mTabHost.postDelayed(() -> {
                    final PagerFragment pagerFragment = (PagerFragment) activity.getSupportFragmentManager()
                            .findFragmentByTag("manager_page");
                    pagerFragment.setInnerItem(0, 1);
                }, 500L);

            }, 1000L);
            //activity.viewPager.setCurrentItem(3, true);
            //DOWNLOAD_PAGER.setItem(1);
        });
    }*/

    ////////////////////////////////////////public static final

    public static final String OPEN_MY_FRIENDS = "OPEN_MY_FRIENDS";
    public static final String OPEN_PUSH = "OPEN_PUSH";
    public static final String OPEN_MANAGER_APPS = "OPEN_MANAGER_APPS";
    public static final String OPEN_MY_PROFILE_APPS = "OPEN_MY_PROFILE_APPS";
    public static final String OPEN_MY_PROFILE_APPS_FIRST = "OPEN_MY_PROFILE_APPS_FIRST";
    public static final String OPEN_MY_PROFILE_SONGS = "OPEN_MY_PROFILE_SONGS";
    public static final String OPEN_MANAGER_SONGS_DOWNLOADING = "OPEN_MANAGER_SONGS_DOWNLOADING";
    public static final String OPEN_MANAGER_SONGS_LIBRARY = "OPEN_MANAGER_SONGS_LIBRARY";
    public static final String ADD_PUSH_SONG = "ADD_PUSH_SONG";

    public static final Set<Song> SELECTED_SONGS = MiscUtils.getSet(5);
    public static final Set<App> SELECTED_APPS = MiscUtils.getSet(5);
    public static final LongSparseArray<Boolean> SELECTED_SONG_IDS = new LongSparseArray<>(5);

    ////////////////////////////////////////private static final

    @SuppressWarnings("unchecked")
    private static final Bundle DOWNLOAD_PAGER_BUNDLE = PagerFragment.getBundle("My Files",
            new PagerFragment.Pages(
                    new Class[]{MyLibraryFragment.class, DownloadingFragment.class},
                    new String[]{"My Library", "Downloading"},
                    "Songs"),
            new PagerFragment.Pages(
                    new Class[]{ApplicationFragment.class},
                    new String[]{"My Applications"},
                    "Apps"));

    @SuppressWarnings("unchecked")
    private static final Bundle PUSH_PAGER_BUNDLE = PagerFragment.getBundle("Share",
//            new PagerFragment.Pages(
//                    new Class[]{reach.project.coreViews.push.apps.ApplicationFragment.class},
//                    new String[]{"My Applications"},
//                    "Apps"),
            new PagerFragment.Pages(
                    new Class[]{reach.project.coreViews.push.music.MyLibraryFragment.class},
                    new String[]{"My Library"},
                    "Songs"));

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

    @Nullable
    private static WeakReference<ReachActivity> reference = null;

    private FragmentTabHost mTabHost;

    private static long serverId = 0;

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (reference != null)
            reference.clear();
        reference = null;
        mTabHost = null;
        //viewPager = null;
    }

    @Override
    protected void onPostResume() {

        super.onPostResume();
        Log.i("Ayush", "Called onPostResume");
        processIntent(getIntent());
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

        //track app open event
        final Map<PostParams, String> simpleParams = MiscUtils.getMap(6);
        simpleParams.put(PostParams.USER_ID, serverId + "");
        simpleParams.put(PostParams.DEVICE_ID, MiscUtils.getDeviceId(this));
        simpleParams.put(PostParams.OS, MiscUtils.getOsName());
        simpleParams.put(PostParams.OS_VERSION, Build.VERSION.SDK_INT + "");
        simpleParams.put(PostParams.SCREEN_NAME, "my_reach");
        try {
            simpleParams.put(PostParams.APP_VERSION,
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionCode + "");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        try {
            UsageTracker.trackEvent(simpleParams, UsageTracker.APP_OPEN);
        } catch (JSONException ignored) {
        }

        Crittercism.setUsername(SharedPrefUtils.getUserName(preferences) + " - " +
                SharedPrefUtils.getPhoneNumber(preferences));

        ////////////////////////////////////////

        /*viewPager = (CustomViewPager) findViewById(R.id.mainViewPager);
        viewPager.setPagingEnabled(false);
        viewPager.setOffscreenPageLimit(4);
        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {

                switch (position) {

                    case 0:
                        return FriendsFragment.getInstance();
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

        mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup(this, getSupportFragmentManager(), android.R.id.tabcontent);
        mTabHost.addTab(
                mTabHost.newTabSpec("friends_page").setIndicator("",
                        ContextCompat.getDrawable(this, R.drawable.friends_tab_selector)),
                FriendsFragment.class, null);
        mTabHost.addTab(
                mTabHost.newTabSpec("push_page").setIndicator("",
                        ContextCompat.getDrawable(this, R.drawable.push_tab_selector)),
                PagerFragment.class, PUSH_PAGER_BUNDLE);
        mTabHost.addTab(
                mTabHost.newTabSpec("explore_page").setIndicator("",
                        ContextCompat.getDrawable(this, R.drawable.explore_tab_selector)),
                ExploreFragment.class, null);
        mTabHost.addTab(
                mTabHost.newTabSpec("manager_page").setIndicator("",
                        ContextCompat.getDrawable(this, R.drawable.manager_tab_selector)),
                PagerFragment.class, DOWNLOAD_PAGER_BUNDLE);
        mTabHost.addTab(
                mTabHost.newTabSpec("myprofile_page").setIndicator("",
                        ContextCompat.getDrawable(this, R.drawable.my_profile_tab_selector)),
                MyProfileFragment.class, null);
        mTabHost.setCurrentTab(2);

        /*final TabLayout tabLayout = (TabLayout) findViewById(R.id.mainTabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("1"));
        tabLayout.addTab(tabLayout.newTab().setText("2"));
        tabLayout.addTab(tabLayout.newTab().setText("3"));
        tabLayout.addTab(tabLayout.newTab().setText("4"));
        tabLayout.addTab(tabLayout.newTab().setText("5"));
        for (int index = 1; index < tabLayout.getTabCount(); index++) {

            final TabLayout.Tab tab = tabLayout.getTabAt(index);
            if (tab != null) {
                tab.setCustomView(UNSELECTED_ICONS[index]);
            }
        }

        final int selectedTabPosition = tabLayout.getSelectedTabPosition();
        final TabLayout.Tab selectedTab = tabLayout.getTabAt(selectedTabPosition);
        if (selectedTab != null)
            selectedTab.setCustomView(SELECTED_ICONS[selectedTabPosition]);

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                final int pos = tabLayout.getSelectedTabPosition();
                tab.setCustomView(null);
                tab.setCustomView(SELECTED_ICONS[pos]);
                switch (pos) {
                    case 0:
                        getSupportFragmentManager().beginTransaction().replace(R.id.container, FriendsFragment.getInstance()).commit();
                        return;
                    case 1:
                        getSupportFragmentManager().beginTransaction().replace(R.id.container, PUSH_PAGER).commit();
                        return;
                    case 2:
                        getSupportFragmentManager().beginTransaction().replace(R.id.container, ExploreFragment.newInstance(serverId)).commit();
                        return;
                    case 3:
                        getSupportFragmentManager().beginTransaction().replace(R.id.container, DOWNLOAD_PAGER).commit();
                        return;
                    case 4:
                        getSupportFragmentManager().beginTransaction().replace(R.id.container, MyProfileFragment.newInstance()).commit();
                        return;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                tab.setCustomView(null);
                tab.setCustomView(UNSELECTED_ICONS[tab.getPosition()]);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                tab.setCustomView(null);
                tab.setCustomView(SELECTED_ICONS[tab.getPosition()]);
            }
        });

        tabLayout.getTabAt(2).select();*/

        /*final TabLayout tabLayout = (TabLayout) findViewById(R.id.mainTabLayout);
        tabLayout.setupWithViewPager(viewPager);
        for (int index = 1; index < tabLayout.getTabCount(); index++) {
            final TabLayout.Tab tab = tabLayout.getTabAt(index);
            if (tab != null) {
                tab.setCustomView(UNSELECTED_ICONS[index]);
            }
        }

        final int selectedTabPosition = tabLayout.getSelectedTabPosition();
        final TabLayout.Tab selectedTab = tabLayout.getTabAt(selectedTabPosition);
        if (selectedTab != null)
            selectedTab.setCustomView(SELECTED_ICONS[selectedTabPosition]);

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
        });

        viewPager.setCurrentItem(2);*/

        //check for update, need activity to check
        FireOnce.checkUpdate(reference);
    }

    private synchronized void processIntent(Intent intent) {

        if (intent == null)
            return;

        Log.i("Ayush", "Processing Intent + " + intent.getAction());

        final String action = intent.getAction();
        if (TextUtils.isEmpty(action))
            return;
        try {
            switch (action) {
                case ADD_PUSH_SONG:
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
                            if (mTabHost != null) {
                                mTabHost.postDelayed(() -> {
                                    if (mTabHost == null || isFinishing())
                                        return;
                                    mTabHost.setCurrentTab(3);
                                    mTabHost.postDelayed(() -> {
                                        final PagerFragment fragment = (PagerFragment) getSupportFragmentManager()
                                                .findFragmentByTag("manager_page");
                                        if (fragment == null)
                                            return;
                                        fragment.setItem(0);
                                        fragment.setInnerItem(0, 1);
                                    }, 500L);
                                }, 1000L);
                            }
                        }
                    }
                    break;
                case OPEN_MANAGER_APPS:
                    if (mTabHost != null) {
                        mTabHost.postDelayed(() -> {
                            if (mTabHost == null || isFinishing())
                                return;
                            mTabHost.setCurrentTab(3);
                            mTabHost.postDelayed(() -> {
                                final PagerFragment fragment = (PagerFragment) getSupportFragmentManager()
                                        .findFragmentByTag("manager_page");
                                fragment.setItem(1);
                            }, 500L);
                        }, 1000L);
                    }

                    break;
                case OPEN_MANAGER_SONGS_DOWNLOADING:
                    if (mTabHost != null) {
                        mTabHost.postDelayed(() -> {
                            if (mTabHost == null || isFinishing())
                                return;
                            mTabHost.setCurrentTab(3);
                            mTabHost.postDelayed(() -> {
                                final PagerFragment fragment = (PagerFragment) getSupportFragmentManager()
                                        .findFragmentByTag("manager_page");
                                if (fragment == null)
                                    return;
                                fragment.setItem(0);
                                fragment.setInnerItem(0, 1);
                            }, 500L);
                        }, 1000L);
                    }

                    break;
                case OPEN_MANAGER_SONGS_LIBRARY:
                    if (mTabHost != null) {
                        mTabHost.postDelayed(() -> {
                            if (mTabHost == null || isFinishing())
                                return;
                            mTabHost.setCurrentTab(3);
                            mTabHost.postDelayed(() -> {
                                final PagerFragment fragment = (PagerFragment) getSupportFragmentManager()
                                        .findFragmentByTag("manager_page");
                                fragment.setItem(0);
                                fragment.setInnerItem(0, 0);
                            }, 500L);
                        }, 1000L);
                    }
                    break;
                case OPEN_MY_PROFILE_APPS:
                    if (mTabHost != null) {
                        mTabHost.postDelayed(() -> {
                            if (mTabHost == null || isFinishing())
                                return;
                            mTabHost.setCurrentTab(4);
                            MyProfileFragment.setItem(0);
                        }, 1000L);
                    }
                    break;
                case OPEN_MY_PROFILE_APPS_FIRST:
                    if (mTabHost != null && !isFinishing()) {
                        mTabHost.setCurrentTab(4);
                        MyProfileFragment.setItem(0);
                    }
                    break;
                case OPEN_MY_PROFILE_SONGS:
                    if (mTabHost != null) {
                        mTabHost.postDelayed(() -> {
                            if (mTabHost == null || isFinishing())
                                return;
                            mTabHost.setCurrentTab(4);
                            MyProfileFragment.setItem(1);
                        }, 1000L);
                    }
                    break;
            }
        } catch (IllegalStateException ignored) {}
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
                            ReachDatabaseHelper.COLUMN_SENDER_ID, //4
                            ReachDatabaseHelper.COLUMN_RECEIVER_ID, //5
                            ReachDatabaseHelper.COLUMN_SIZE, //6
                            ReachDatabaseHelper.COLUMN_META_HASH //7

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
                            cursor.getString(7), //meta-hash
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
        reachDatabase.setUniqueId(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE));

        reachDatabase.setDuration(duration);
        reachDatabase.setLogicalClock((short) 0);
        reachDatabase.setStatus(ReachDatabase.NOT_WORKING);

        reachDatabase.setLastActive(0);
        reachDatabase.setReference(0);

        reachDatabase.setAlbumName(albumName);
        reachDatabase.setGenre(genre);

        reachDatabase.setVisibility((short) 1);

        //We call bulk starter always
        MiscUtils.startDownload(reachDatabase, this, null, "PUSH");
    }

    @Override
    public void showSwipeCoach() {
        new Handler().postDelayed(() -> {
            final ViewGroup viewGroup = (ViewGroup) findViewById(android.R.id.content);
            final View coachView = LayoutInflater.from(this).inflate(R.layout.swipe_coach, viewGroup, false);
            viewGroup.addView(coachView);
            coachView.setOnClickListener(v -> viewGroup.removeView(coachView));
        }, 1000L);
    }
}