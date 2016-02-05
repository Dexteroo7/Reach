package reach.project.player;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
import reach.project.music.MySongsHelper;
import reach.project.music.MySongsProvider;
import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.reachProcess.reachService.ProcessManager;
import reach.project.usageTracking.PostParams;
import reach.project.usageTracking.SongMetadata;
import reach.project.usageTracking.UsageTracker;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

public class PlayerActivity extends AppCompatActivity {

    public static void openActivity(Context context) {

        final Intent intent = new Intent(context, PlayerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }

    public static Intent getIntent(Context context) {

        final Intent intent = new Intent(context, PlayerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return intent;
    }

    public static final String ACTION = "reach.project.player.PlayerActivity.ACTION";
    public static final String MUSIC_PARCEL = "reach.project.player.PlayerActivity.MUSIC_PARCEL";
    public static final String DURATION = "reach.project.player.PlayerActivity.DURATION";
    public static final String PRIMARY_PROGRESS = "reach.project.player.PlayerActivity.PRIMARY_PROGRESS";
    public static final String SECONDARY_PROGRESS = "reach.project.player.PlayerActivity.SECONDARY_PROGRESS";
    public static final String PLAYER_POSITION = "reach.project.player.PlayerActivity.PLAYER_POSITION";

    public static final String PLAY_SONG = "reach.project.player.PlayerActivity.PLAY_SONG";

    @Nullable
    private static WeakReference<PlayerActivity> reference = null;
    @Nullable
    private static MusicData currentPlaying = null;

    @Nullable
    private SimpleDraweeView albumArt = null;
    @Nullable
    private ImageView pause_play = null;
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
    private View likeButton = null;

    @Override
    public void onBackPressed() {
        MiscUtils.navigateUp(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        reference = new WeakReference<>(this);
        currentPlaying = SharedPrefUtils.getLastPlayed(this).orNull();

        final String duration = currentPlaying == null ? "" : MiscUtils.combinationFormatter(currentPlaying.getDuration());
        final Uri albumArtUri = currentPlaying == null ? null : AlbumArtUri.getUri(currentPlaying.getAlbumName(),
                currentPlaying.getArtistName(), currentPlaying.getDisplayName(), true).orNull();

        playerPos = (TextView) findViewById(R.id.playerPos);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.playerToolbar);
        toolbar.setTitle("Player");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        (likeButton = findViewById(R.id.likeBtn)).setOnClickListener(LocalUtils.LIKE_BUTTON_CLICK);
        (seekBar = (SeekBar) findViewById(R.id.seekBar)).setOnSeekBarChangeListener(LocalUtils.PLAYER_SEEK_LISTENER);
        (pause_play = (ImageView) findViewById(R.id.pause_play)).setOnClickListener(LocalUtils.PAUSE_CLICK);
        (songNamePlaying = (TextView) findViewById(R.id.songNamePlaying)).setText(currentPlaying == null ? "" : currentPlaying.getDisplayName());
        (artistName = (TextView) findViewById(R.id.artistName)).setText(currentPlaying == null ? "" : currentPlaying.getArtistName());
        (albumArt = (SimpleDraweeView) findViewById(R.id.albumArt)).setImageURI(albumArtUri);
        (songDuration = (TextView) findViewById(R.id.songDuration)).setText(duration);

        if (currentPlaying != null)
            pause_play.setImageResource(R.drawable.play_white_selector);

        findViewById(R.id.rwdBtn).setOnClickListener(LocalUtils.PREVIOUS_CLICK);
        findViewById(R.id.fwdBtn).setOnClickListener(LocalUtils.NEXT_CLICK);

        final View shuffle = findViewById(R.id.shuffleBtn);
        final View repeat = findViewById(R.id.repeatBtn);
        shuffle.setOnClickListener(LocalUtils.SHUFFLE_CLICK);
        repeat.setOnClickListener(LocalUtils.REPEAT_CLICK);
        shuffle.setSelected(SharedPrefUtils.getShuffle(this));
        repeat.setSelected(SharedPrefUtils.getRepeat(this));
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

        Log.d("Ayush", "updateMusic called");
        if (currentPlaying == null)
            return;

        if (songNamePlaying != null) {

            songNamePlaying.setText(currentPlaying.getDisplayName());
            updatePrimaryProgress(currentPlaying.getPrimaryProgress(), currentPlaying.getCurrentPosition());
            updateSecondaryProgress((short) currentPlaying.getSecondaryProgress());
            togglePlayPause(paused);
        }
        if (songDuration != null)
            songDuration.setText(MiscUtils.combinationFormatter(currentPlaying.getDuration()));
        if (artistName != null)
            artistName.setText(currentPlaying.getArtistName());

        if (likeButton != null) {

            if (currentPlaying.isLiked())
                likeButton.setSelected(true);
            else
                likeButton.setSelected(false);
        }

        final Optional<Uri> uriOptional = AlbumArtUri.getUri(currentPlaying.getAlbumName(),
                currentPlaying.getArtistName(), currentPlaying.getDisplayName(), true);
        if (albumArt != null && uriOptional != null) {
            Uri albumUri = null;
            if (uriOptional.isPresent())
                albumUri = uriOptional.get();
            albumArt.setController(MiscUtils.getControllerResize(albumArt.getController(),
                    albumUri, 500, 500));
        }
    }

    private synchronized void updatePrimaryProgress(final int progress, final long position) {

        if (playerPos != null) {

//            Log.i("Ayush", "Player got position " + position + " " + MiscUtils.combinationFormatter(position));
            playerPos.setText(MiscUtils.combinationFormatter(position));
        }
        if (seekBar != null)
            seekBar.setProgress(progress);
    }

    private synchronized void updateSecondaryProgress(final short progress) {
        if (seekBar != null) {

            Log.i("Ayush", "Setting secondary progress " + progress);
            seekBar.setSecondaryProgress(progress);
        }
    }

    private synchronized void updateDuration(final String songNamePlaying) {
        if (songDuration != null)
            songDuration.setText(songNamePlaying);
    }

    private final Handler handler = new Handler(msg -> {

        final Bundle bundle;
        if (msg == null || (bundle = msg.getData()) == null)
            return false;

        switch (bundle.getString(ACTION, null)) {

            case ProcessManager.REPLY_LATEST_MUSIC: {

                currentPlaying = bundle.getParcelable(MUSIC_PARCEL);
                MiscUtils.useActivity(reference, activity -> activity.updateMusic(false));
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
                    activity.updateSecondaryProgress((short) 0);
                });
                break;
            }

            case ProcessManager.REPLY_ERROR: {

                MiscUtils.useActivity(reference, activity -> {

                    activity.togglePlayPause(false);
                    activity.updatePrimaryProgress(0, 0);
                    activity.updateSecondaryProgress((short) 0);
                    Toast.makeText(activity, "Play when download completes", Toast.LENGTH_SHORT).show();
                });
                break;
            }

            case ProcessManager.REPLY_DURATION: {

                MiscUtils.useActivity(reference, activity -> activity.updateDuration(bundle.getString(DURATION)));
                break;
            }

            case ProcessManager.REPLY_PRIMARY_PROGRESS: {

                final short primaryProgress = bundle.getShort(PRIMARY_PROGRESS);
                final int playerPosition = bundle.getInt(PLAYER_POSITION);
                MiscUtils.useActivity(reference, activity -> activity.updatePrimaryProgress(primaryProgress, playerPosition));
                break;
            }

            case ProcessManager.REPLY_SECONDARY_PROGRESS: {

                final short secondaryProgress = bundle.getShort(SECONDARY_PROGRESS);
                Log.i("Ayush", "Porting secondary progress " + secondaryProgress);
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

        public static final SeekBar.OnSeekBarChangeListener PLAYER_SEEK_LISTENER = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    ProcessManager.submitMusicRequest(seekBar.getContext(), Optional.of(progress + ""), ProcessManager.ACTION_SEEK);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };

        public static final View.OnClickListener REPEAT_CLICK = view -> {

            if (SharedPrefUtils.toggleRepeat(view.getContext()))
                view.setSelected(true);
            else
                view.setSelected(false);
        };

        public static final View.OnClickListener NEXT_CLICK = v -> ProcessManager.submitMusicRequest(
                v.getContext(),
                Optional.absent(),
                ProcessManager.ACTION_NEXT);

        public static final View.OnClickListener PREVIOUS_CLICK = v -> ProcessManager.submitMusicRequest(
                v.getContext(),
                Optional.absent(),
                ProcessManager.ACTION_PREVIOUS);

        private static boolean toggleLiked(Context context) {

            if (currentPlaying == null)
                return false;

            final ContentValues values = new ContentValues();
            //CARE WE USE SQL TABLE ID HERE
            if (currentPlaying.getType() == MusicData.DOWNLOADED) {

                values.put(ReachDatabaseHelper.COLUMN_IS_LIKED, !currentPlaying.isLiked() ? 1 : 0);
                return context.getContentResolver().update(
                        Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + currentPlaying.getId()),
                        values,
                        ReachDatabaseHelper.COLUMN_ID + " = ?",
                        new String[]{currentPlaying.getId() + ""}) > 0 && !currentPlaying.isLiked();
            } else if (currentPlaying.getType() == MusicData.MY_LIBRARY) {

                values.put(MySongsHelper.COLUMN_IS_LIKED, !currentPlaying.isLiked() ? 1 : 0);
                return context.getContentResolver().update(
                        MySongsProvider.CONTENT_URI,
                        values,
                        MySongsHelper.COLUMN_SONG_ID + " = ?",
                        new String[]{currentPlaying.getId() + ""}) > 0 && !currentPlaying.isLiked();
            } else
                throw new IllegalStateException("current playing has invalid type " + currentPlaying.getType());
        }

        public static final AdapterView.OnClickListener LIKE_BUTTON_CLICK = view -> {

            if (currentPlaying == null)
                return;

            final Context context = view.getContext();
            final long serverId = SharedPrefUtils.getServerId(context.getSharedPreferences("Reach", MODE_PRIVATE));

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

                final Map<SongMetadata, String> complexParams = MiscUtils.getMap(9);
                complexParams.put(SongMetadata.SONG_ID, currentPlaying.getId() + "");
                complexParams.put(SongMetadata.META_HASH, currentPlaying.getMetaHash());
                complexParams.put(SongMetadata.ARTIST, currentPlaying.getArtistName());
                complexParams.put(SongMetadata.TITLE, currentPlaying.getDisplayName());
                complexParams.put(SongMetadata.DURATION, currentPlaying.getDuration() + "");
                complexParams.put(SongMetadata.SIZE, currentPlaying.getLength() + "");
                complexParams.put(SongMetadata.UPLOADER_ID, currentPlaying.getSenderId() + "");
                complexParams.put(SongMetadata.ALBUM, currentPlaying.getAlbumName());

                try {
                    UsageTracker.trackSong(simpleParams, complexParams, UsageTracker.LIKE_SONG);
                } catch (JSONException ignored) {
                }

                MiscUtils.autoRetryAsync(() -> StaticData.NOTIFICATION_API.addLike(
                        currentPlaying.getSenderId(),
                        serverId,
                        currentPlaying.getDisplayName()).execute(), Optional.absent());
                currentPlaying.setIsLiked(true);
            } else {
                currentPlaying.setIsLiked(false);
            }

            if (currentPlaying.isLiked())
                view.setSelected(true);
            else
                view.setSelected(false);

            Snackbar.make(view.getRootView(), "You liked " + currentPlaying.getDisplayName(),
                    Snackbar.LENGTH_SHORT).show();
        };

        public static final View.OnClickListener PAUSE_CLICK = v -> {

            if (currentPlaying != null)
                ProcessManager.submitMusicRequest(
                        v.getContext(),
                        Optional.of(new Gson().toJson(currentPlaying, MusicData.class)),
                        ProcessManager.ACTION_PLAY_PAUSE);
            else
                ProcessManager.submitMusicRequest(
                        v.getContext(),
                        Optional.absent(),
                        ProcessManager.ACTION_PLAY_PAUSE);
        };

        public static final View.OnClickListener SHUFFLE_CLICK = view -> {

            if (SharedPrefUtils.toggleShuffle(view.getContext()))
                view.setSelected(true);
            else
                view.setSelected(false);
        };
    }
}