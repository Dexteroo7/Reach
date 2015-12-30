package reach.project.player;

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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
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
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

public class PlayerActivity extends AppCompatActivity {

    @Nullable
    private static WeakReference<PlayerActivity> reference = null;
    @Nullable
    private SimpleDraweeView albumArt = null;
    @Nullable
    private ImageView pause_play = null;
    @Nullable
    private ImageView shuffleBtn = null;
    @Nullable
    private ImageView rwdBtn = null;
    @Nullable
    private ImageView fwdBtn = null;
    @Nullable
    private ImageView repeatBtn = null;
    @Nullable
    private ImageView likeBtn = null;
    @Nullable
    private TextView songNamePlaying = null;
    @Nullable
    private TextView artistName = null;
    @Nullable
    private TextView playerPos = null;
    @Nullable
    private TextView songDuration = null;
    @Nullable
    private SeekBar seekBar = null;
    @Nullable
    private static MusicData currentPlaying = null;
    private static long serverId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        reference = new WeakReference<>(this);
        setContentView(R.layout.activity_player);

        Toolbar toolbar = (Toolbar) findViewById(R.id.playerToolbar);
        toolbar.setNavigationOnClickListener(v -> NavUtils.navigateUpFromSameTask(PlayerActivity.this));

        albumArt = (SimpleDraweeView) findViewById(R.id.albumArt);
        pause_play = (ImageView) findViewById(R.id.pause_play);
        shuffleBtn = (ImageView) findViewById(R.id.shuffleBtn);
        rwdBtn = (ImageView) findViewById(R.id.rwdBtn);
        fwdBtn = (ImageView) findViewById(R.id.fwdBtn);
        repeatBtn = (ImageView) findViewById(R.id.repeatBtn);
        likeBtn = (ImageView) findViewById(R.id.likeBtn);

        songNamePlaying = (TextView) findViewById(R.id.songNamePlaying);
        artistName = (TextView) findViewById(R.id.artistName);
        playerPos = (TextView) findViewById(R.id.playerPos);
        songDuration = (TextView) findViewById(R.id.songDuration);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        LocalUtils.lastSong(songNamePlaying, artistName, songDuration,
                pause_play, shuffleBtn,repeatBtn); //load last song details

        if (pause_play != null)
            pause_play.setOnClickListener(LocalUtils.pauseClick);
        if (shuffleBtn != null)
            shuffleBtn.setOnClickListener(LocalUtils.shuffleClick);
        if (repeatBtn != null)
            repeatBtn.setOnClickListener(LocalUtils.repeatClick);
        if (rwdBtn != null)
            rwdBtn.setOnClickListener(LocalUtils.previousClick);
        if (fwdBtn != null)
            fwdBtn.setOnClickListener(LocalUtils.nextClick);
        if (likeBtn != null)
            likeBtn.setOnClickListener(LocalUtils.likeButtonClick);

        if (seekBar != null)
            seekBar.setOnSeekBarChangeListener(LocalUtils.playerSeekListener);

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

        Log.d("Ashish", "updateMusic called");
        if (currentPlaying == null)
            return;

        if (songNamePlaying != null) {
            songNamePlaying.setText(currentPlaying.getDisplayName());
            updatePrimaryProgress(currentPlaying.getPrimaryProgress(), currentPlaying.getCurrentPosition());
            updateSecondaryProgress(currentPlaying.getSecondaryProgress());
            togglePlayPause(paused);
        }
        if (songDuration != null)
            songDuration.setText(MiscUtils.combinationFormatter(currentPlaying.getDuration()));
        if (artistName != null)
            artistName.setText(currentPlaying.getArtistName());
        if (likeBtn != null) {

            if (currentPlaying.getType() == 0) {
                likeBtn.setVisibility(View.VISIBLE);
                if (currentPlaying.isLiked())
                    likeBtn.setImageResource(R.drawable.explore_download_btn);
                else
                    likeBtn.setImageResource(R.drawable.explore_download_btn);
            } else
                likeBtn.setVisibility(View.GONE);
        }

        final Optional<Uri> uriOptional = AlbumArtUri.getUri(currentPlaying.getAlbumName(),
                    currentPlaying.getArtistName(), currentPlaying.getDisplayName(), true);
        if (albumArt != null && uriOptional != null) {
            if (uriOptional.isPresent()) {
                albumArt.setImageURI(uriOptional.get());
            } else
                albumArt.setImageBitmap(null);
        }
    }

    private synchronized void updatePrimaryProgress(final int progress, final int position) {
        if (playerPos != null)
            playerPos.setText(MiscUtils.combinationFormatter(position));
        if (seekBar != null)
            seekBar.setProgress(progress);
    }

    private synchronized void updateSecondaryProgress(final int progress) {
        if (seekBar != null)
            seekBar.setSecondaryProgress(progress);
    }

    private synchronized void updateDuration(final String durationsongNamePlaying) {
        if (songDuration != null)
            songDuration.setText(durationsongNamePlaying);
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

                    MiscUtils.autoRetryAsync(() -> StaticData.NOTIFICATION_API.addLike(
                            currentPlaying.getSenderId(),
                            serverId,
                            currentPlaying.getDisplayName()).execute(), Optional.absent());
                    currentPlaying.setIsLiked(true);

                    //((ImageView) view).setImageResource(R.drawable.like_pink);
                } else {

                    //((ImageView) view).setImageResource(R.drawable.like_white);
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

        private static void lastSong(TextView songNamePlaying,TextView artistName,
                                     TextView songDuration, ImageView pause_play,
                                     ImageView shuffleBtn, ImageView repeatBtn) {

            final Boolean[] toSend = new Boolean[]{false, false, false};
            final Context context = songNamePlaying.getContext();

            currentPlaying = SharedPrefUtils.getLastPlayed(context).orNull();

            toSend[0] = (currentPlaying != null);
            toSend[1] = SharedPrefUtils.getShuffle(context);
            toSend[2] = SharedPrefUtils.getRepeat(context);

            if (toSend[0]) {
                //last song is present
                songNamePlaying.setText(currentPlaying.getDisplayName());
                artistName.setText(currentPlaying.getArtistName());
                songDuration.setText(MiscUtils.combinationFormatter(currentPlaying.getDuration()));
                pause_play.setImageResource(R.drawable.play_white_selector);
            }

            //TODO
            shuffleBtn.setSelected(toSend[1]);
            repeatBtn.setSelected(toSend[2]);
        }
    }
}