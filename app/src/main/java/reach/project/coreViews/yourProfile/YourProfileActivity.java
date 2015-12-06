package reach.project.coreViews.yourProfile;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.github.florent37.materialviewpager.MaterialViewPager;
import com.github.florent37.materialviewpager.header.HeaderDesign;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONException;

import java.util.Map;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.coreViews.yourProfile.apps.YourProfileAppFragment;
import reach.project.coreViews.yourProfile.music.YourProfileMusicFragment;
import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.usageTracking.PostParams;
import reach.project.usageTracking.SongMetadata;
import reach.project.usageTracking.UsageTracker;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.HandOverMessage;

public class YourProfileActivity extends AppCompatActivity implements HandOverMessage<ReachDatabase> {

    private static final String OPEN_PROFILE = "OPEN_PROFILE";

    public static void openProfile(long userId, Context context) {

        final Intent intent = new Intent(context, YourProfileActivity.class);
        intent.setAction(OPEN_PROFILE);
        intent.putExtra("userId", userId);

        context.startActivity(intent);
    }

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_your_profile);

        preferences = getSharedPreferences("Reach", MODE_PRIVATE);

        final MaterialViewPager materialViewPager = (MaterialViewPager) findViewById(R.id.materialViewPager);
        final Toolbar toolbar = materialViewPager.getToolbar();
        final ViewPager viewPager = materialViewPager.getViewPager();

        toolbar.setTitle("Profile");
        toolbar.setTitleTextColor(Color.WHITE);

        final Intent intent = getIntent();
        final long userId = intent.getLongExtra("userId", 0L);
        if (userId == 0 || !intent.getAction().equals(OPEN_PROFILE)) {
            finish();
            return;
        }

        final Cursor cursor = getContentResolver().query(
                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
                new String[]{ReachFriendsHelper.COLUMN_PHONE_NUMBER,
                        ReachFriendsHelper.COLUMN_USER_NAME,
                        ReachFriendsHelper.COLUMN_NUMBER_OF_SONGS,
                        ReachFriendsHelper.COLUMN_IMAGE_ID,
                        ReachFriendsHelper.COLUMN_NETWORK_TYPE,
                        ReachFriendsHelper.COLUMN_STATUS},
                ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{userId + ""}, null);
        int numberOfSongs = 0;

        if (cursor != null) {

            cursor.moveToFirst();
            final String uName = cursor.getString(1);
            numberOfSongs = cursor.getInt(2);
            final String imageId = cursor.getString(3);

            final RelativeLayout headerRoot = (RelativeLayout) materialViewPager.findViewById(R.id.headerRoot);
            final TextView userName = (TextView) headerRoot.findViewById(R.id.userName);
            final TextView musicCount = (TextView) headerRoot.findViewById(R.id.musicCount);
            final TextView userHandle = (TextView) headerRoot.findViewById(R.id.userHandle);
            final SimpleDraweeView profilePic = (SimpleDraweeView) headerRoot.findViewById(R.id.profilePic);

            userName.setText(uName);
            musicCount.setText(numberOfSongs + "");
            userHandle.setText("@" + uName.toLowerCase().split(" ")[0]);
            profilePic.setImageURI(Uri.parse(StaticData.cloudStorageImageBaseUrl + imageId));

            cursor.close();
        }

        final int finalNumberOfSongs = numberOfSongs;
        viewPager.setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()) {

            @Override
            public Fragment getItem(int position) {
                switch (position) {

                    case 0:
                        return YourProfileAppFragment.newInstance(userId);
                    case 1:
                        return YourProfileMusicFragment.newInstance(userId);
                    default:
                        throw new IllegalStateException("Count and size clash");
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
                        return "Apps";
                    case 1:
                        return "Songs (" + finalNumberOfSongs + ")";
                    default:
                        throw new IllegalStateException("Count and size clash");
                }
            }
        });

        materialViewPager.setMaterialViewPagerListener(page -> {
            switch (page) {
                case 0:
                    return HeaderDesign.fromColorResAndUrl(
                            R.color.reach_grey,
                            "");
                case 1:
                    return HeaderDesign.fromColorResAndUrl(
                            R.color.reach_color,
                            "");
            }
            return null;
        });

        viewPager.setOffscreenPageLimit(viewPager.getAdapter().getCount());
        viewPager.setPageMargin(-1 * (MiscUtils.dpToPx(50)));
        viewPager.setPageTransformer(true, (view, position) -> {

            if (position <= 1) {
                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(0.85f, 1 - Math.abs(position));
                float vertMargin = view.getHeight() * (1 - scaleFactor) / 2;
                float horzMargin = view.getWidth() * (1 - scaleFactor) / 2;
                if (position < 0)
                    view.setTranslationX(horzMargin - vertMargin / 2);
                else
                    view.setTranslationX(-horzMargin + vertMargin / 2);

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
            }
        });
        materialViewPager.getPagerTitleStrip().setViewPager(viewPager);

    }

    @Override
    public void handOverMessage(@Nonnull ReachDatabase reachDatabase) {

        final ContentResolver contentResolver = getContentResolver();
        if (contentResolver == null)
            return;

        /**
         * DISPLAY_NAME, ACTUAL_NAME, SIZE & DURATION all can not be same, effectively its a hash
         */

        final Cursor cursor;

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
                new String[]{reachDatabase.getDisplayName(), reachDatabase.getActualName(),
                        reachDatabase.getLength() + "", reachDatabase.getDuration() + ""},
                null);

        if (cursor != null) {

            if (cursor.moveToFirst()) {

                //if not multiple addition, play the song
                final boolean liked;
                final String temp = cursor.getString(3);
                liked = !TextUtils.isEmpty(temp) && temp.equals("1");

                final MusicData musicData = new MusicData(
                        cursor.getLong(0), //id
                        reachDatabase.getLength(),
                        reachDatabase.getSenderId(),
                        cursor.getLong(1),
                        0,
                        cursor.getString(2),
                        reachDatabase.getDisplayName(),
                        reachDatabase.getArtistName(),
                        "",
                        liked,
                        reachDatabase.getDuration(),
                        (byte) 0);
                MiscUtils.playSong(musicData, this);
                //in both cases close and continue
                cursor.close();
                return;
            }
            cursor.close();
        }

        final String clientName = SharedPrefUtils.getUserName(preferences);

        //new song
        //We call bulk starter always
        final Uri uri = contentResolver.insert(ReachDatabaseProvider.CONTENT_URI,
                ReachDatabaseHelper.contentValuesCreator(reachDatabase));
        if (uri == null) {

            ((ReachApplication) getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                    .setCategory("Add song failed")
                    .setAction("User Name - " + clientName)
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
        StaticData.temporaryFix.execute(MiscUtils.startDownloadOperation(
                this,
                reachDatabase,
                reachDatabase.getReceiverId(), //myID
                reachDatabase.getSenderId(),   //the uploaded
                reachDatabase.getId()));

        ((ReachApplication) getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                .setCategory("Transaction - Add SongBrainz")
                .setAction("User Name - " + clientName)
                .setLabel("SongBrainz - " + reachDatabase.getDisplayName() + ", From - " + reachDatabase.getSenderId())
                .setValue(1)
                .build());

        //usage tracking
        final Map<PostParams, String> simpleParams = MiscUtils.getMap(6);
        simpleParams.put(PostParams.USER_ID, reachDatabase.getReceiverId() + "");
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

        Snackbar.make(findViewById(R.id.root_layout), "Song added, click to view", Snackbar.LENGTH_LONG)
                .setAction("VIEW", v -> {
                    ReachActivity.openDownloading();
                })
                .show();
    }
}