package reach.project.player;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.google.gson.Gson;

import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.util.Map;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.reachProcess.reachService.MusicHandler;
import reach.project.reachProcess.reachService.ProcessManager;
import reach.project.usageTracking.PostParams;
import reach.project.usageTracking.SongMetadata;
import reach.project.usageTracking.UsageTracker;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

/**
 * Full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class PlayerActivity extends AppCompatActivity {

    public PlayerActivity() {

    }

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices need a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = this::hide;
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = (view, motionEvent) -> {
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS);
        }
        return false;
    };

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
//    private GoogleApiClient client;
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    /////////////////////////////Start from here
    /////////////////////////////

    @Nullable
    private static WeakReference<PlayerActivity> reference = null;
    @Nullable
    private ImageButton pause_play = null;
    @Nullable
    private TextView songNamePlaying = null;
    @Nullable
    private SeekBar seekBar = null;
    @Nullable
    private static MusicData currentPlaying = null;
    private static long serverId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        reference = new WeakReference<>(this);

        setContentView(R.layout.activity_player2);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(view -> toggle());

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        mControlsView.setOnTouchListener(mDelayHideTouchListener);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        pause_play = (ImageButton) findViewById(R.id.pause_play);
        songNamePlaying = (TextView) findViewById(R.id.songNamePlaying);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        LocalUtils.lastSong(songNamePlaying, pause_play); //load last song details
    }

    @Override
    protected void onPause() {
        super.onPause();
        ProcessManager.unInstallMessenger();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ProcessManager.installMessenger(new Messenger(handler));
    }

    private synchronized void togglePlayPause(final boolean pause) {

        if (pause_play != null) {
            if (pause)
                pause_play.setImageResource(R.drawable.play_white_selector);
            else
                pause_play.setImageResource(R.drawable.pause_white_selector);
        }
    }

    private synchronized void updateMusic(boolean paused) {

        if (songNamePlaying != null && currentPlaying != null) {

            songNamePlaying.setText(currentPlaying.getDisplayName());
            updatePrimaryProgress(currentPlaying.getPrimaryProgress(), currentPlaying.getCurrentPosition());
            updateSecondaryProgress(currentPlaying.getSecondaryProgress());
            togglePlayPause(paused);
        }
//        if (songDuration != null)
//            songDuration.setText(MiscUtils.combinationFormatter(data.getDuration()));
//        if (artistName != null)
//            artistName.setText(data.getArtistName());
//        if (likeButton != null) {
//
//            if (data.getType() == 0) {
//                likeButton.setVisibility(View.VISIBLE);
//                if (data.isLiked())
//                    likeButton.setImageResource(R.drawable.like_pink);
//                else
//                    likeButton.setImageResource(R.drawable.like_white);
//            } else
//                likeButton.setVisibility(View.GONE);
//        }
    }

    private synchronized void updatePrimaryProgress(final int progress, final int position) {

//        if (playerPos != null)
//            playerPos.setText(MiscUtils.combinationFormatter(pos));
//        if (progressBarMaximized != null)
//            progressBarMaximized.setProgress(progress);
        if (seekBar != null)
            seekBar.setProgress(progress);
    }

    private synchronized void updateSecondaryProgress(final int progress) {

//        if (progressBarMaximized != null)
//            progressBarMaximized.setSecondaryProgress(progress);
        if (seekBar != null)
            seekBar.setSecondaryProgress(progress);
    }

    private synchronized void updateDuration(final String durationsongNamePlaying) {

//        if (songDuration != null)
//            songDuration.setText(duration);
    }

    public static String ACTION = "reach.project.player.PlayerActivity.ACTION";
    public static String MUSIC_PARCEL = "reach.project.player.PlayerActivity.MUSIC_PARCEL";
    public static String DURATION = "reach.project.player.PlayerActivity.DURATION";
    public static String PRIMARY_PROGRESS = "reach.project.player.PlayerActivity.PRIMARY_PROGRESS";
    public static String SECONDARY_PROGRESS = "reach.project.player.PlayerActivity.SECONDARY_PROGRESS";
    public static String PLAYER_POSITION = "reach.project.player.PlayerActivity.PLAYER_POSITION";

    private final Handler handler = new Handler(msg -> {

        final Bundle bundle;
        if (msg == null || (bundle = msg.getData()) == null)
            return false;

        switch (bundle.getString(ACTION, null)) {

            case ProcessManager.REPLY_LATEST_MUSIC: {

                currentPlaying = bundle.getParcelable(MUSIC_PARCEL);
                MiscUtils.useActivity(reference, activity -> {
                    activity.updateMusic(false);
                });

                break;
            }

            case ProcessManager.REPLY_PAUSED: {

                MiscUtils.useActivity(reference, activity -> activity.togglePlayPause(true));
                break;
            }

            case ProcessManager.REPLY_UN_PAUSED: {

                MiscUtils.useActivity(reference, activity -> activity.togglePlayPause(false));
                break;
            }

            case ProcessManager.REPLY_MUSIC_DEAD: {

                MiscUtils.useActivity(reference, activity -> {

                    activity.togglePlayPause(false);
                    activity.updatePrimaryProgress(0, 0);
                    activity.updateSecondaryProgress(0);
                });
                break;
            }

            case ProcessManager.REPLY_ERROR: {
                //TODO clear views
                break;
            }

            case ProcessManager.REPLY_DURATION: {

                MiscUtils.useActivity(reference, activity -> activity.updateDuration(bundle.getString(DURATION)));
                break;
            }

            case ProcessManager.REPLY_PRIMARY_PROGRESS: {

                final short primaryProgress = bundle.getShort(PRIMARY_PROGRESS);
                final int playerPosition = bundle.getShort(PLAYER_POSITION);
                MiscUtils.useActivity(reference, activity ->
                        activity.updatePrimaryProgress(primaryProgress, playerPosition));
                break;
            }

            case ProcessManager.REPLY_SECONDARY_PROGRESS: {

                final short secondaryProgress = bundle.getShort(SECONDARY_PROGRESS);
                MiscUtils.useActivity(reference, activity -> activity.updateSecondaryProgress(secondaryProgress));
                break;
            }

            default:
                return false;
        }

        return true;
    });

//    @Override
//    public void onStart() {
//        super.onStart();
//
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client.connect();
//        Action viewAction = Action.newAction(
//                Action.TYPE_VIEW, // TODO: choose an action type.
//                "Player Page", // TODO: Define a title for the content shown.
//                // TODO: If you have web page content that matches this app activity's content,
//                // make sure this auto-generated web page URL is correct.
//                // Otherwise, set the URL to null.
//                Uri.parse("http://host/path"),
//                // TODO: Make sure this auto-generated app deep link URI is correct.
//                Uri.parse("android-app://reach.project.player/http/host/path")
//        );
//        AppIndex.AppIndexApi.start(client, viewAction);
//    }

//    @Override
//    public void onStop() {
//        super.onStop();
//
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        Action viewAction = Action.newAction(
//                Action.TYPE_VIEW, // TODO: choose an action type.
//                "Player Page", // TODO: Define a title for the content shown.
//                // TODO: If you have web page content that matches this app activity's content,
//                // make sure this auto-generated web page URL is correct.
//                // Otherwise, set the URL to null.
//                Uri.parse("http://host/path"),
//                // TODO: Make sure this auto-generated app deep link URI is correct.
//                Uri.parse("android-app://reach.project.player/http/host/path")
//        );
//        AppIndex.AppIndexApi.end(client, viewAction);
//        client.disconnect();
//    }

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

            if (SharedPrefUtils.toggleRepeat(view.getContext()))
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

                if (currentPlaying == null)
                    return false;

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

            if (SharedPrefUtils.toggleShuffle(view.getContext()))
                view.setSelected(true);
            else
                view.setSelected(false);
        };

        private static void lastSong(TextView songNamePlaying, ImageButton pause_play) {

            final Boolean[] toSend = new Boolean[]{false, false, false};
            final Context context = songNamePlaying.getContext();

            currentPlaying = SharedPrefUtils.getLastPlayed(context).orNull();

            toSend[0] = (currentPlaying != null);
            toSend[1] = SharedPrefUtils.getShuffle(context);
            toSend[2] = SharedPrefUtils.getRepeat(context);

            if (toSend[0]) {
                //last song is present
                songNamePlaying.setText(currentPlaying.getDisplayName());
//            artistName.setText(currentPlaying.getArtistName());
//            songDuration.setText(MiscUtils.combinationFormatter(currentPlaying.getDuration()));
//            pausePlayMaximized.setImageResource(R.drawable.play_white_selector);
                pause_play.setImageResource(R.drawable.play_white_selector);
            }

            //TODO
//        shuffleBtn.setSelected(toSend[1]);
//        repeatBtn.setSelected(toSend[2]);
        }
    }
}