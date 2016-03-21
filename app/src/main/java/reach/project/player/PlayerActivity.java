package reach.project.player;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
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
import com.facebook.imagepipeline.common.ResizeOptions;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import org.json.JSONException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.myProfile.EmptyRecyclerView;
import reach.project.coreViews.push.PushActivity;
import reach.project.coreViews.push.PushContainer;
import reach.project.music.ReachDatabase;
import reach.project.music.Song;
import reach.project.music.SongCursorHelper;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.reachProcess.reachService.ProcessManager;
import reach.project.usageTracking.PostParams;
import reach.project.usageTracking.SongMetadata;
import reach.project.usageTracking.UsageTracker;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;

public class PlayerActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, HandOverMessage<Cursor> {

    private static final String player = "Player";
    public static final String TAG = PlayerActivity.class.getSimpleName();

    public static void openActivity(Context context) {

        final Intent intent = new Intent(context, PlayerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }

    public static final String ACTION = "reach.project.player.PlayerActivity.ACTION";
    public static final String MUSIC_PARCEL = "reach.project.player.PlayerActivity.MUSIC_PARCEL";
    public static final String DURATION = "reach.project.player.PlayerActivity.DURATION";
    public static final String PRIMARY_PROGRESS = "reach.project.player.PlayerActivity.PRIMARY_PROGRESS";
    public static final String SECONDARY_PROGRESS = "reach.project.player.PlayerActivity.SECONDARY_PROGRESS";
    public static final String PLAYER_POSITION = "reach.project.player.PlayerActivity.PLAYER_POSITION";

//    public static final float EMPTY_VIEW_ITEM_ALPHA = 0.4f;
//    public static final String PLAY_SONG = "reach.project.player.PlayerActivity.PLAY_SONG";

    @Nullable
    private static WeakReference<PlayerActivity> reference = null;
    @Nullable
    private static Song currentPlaying = null;

    @Nullable
    private NowPlayingAdapter nowPlayingAdapter = null;
    @Nullable
    private EmptyRecyclerView mMusicRecyclerView = null;
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
    private ImageView likeButton = null;
    private View shuffle;
    private View repeat;
    private View rwdBtn;
    private View fwdBtn;

    private final ResizeOptions fullAlbumArt = new ResizeOptions(500, 500);
    private final ExecutorService songFinder = MiscUtils.getRejectionExecutor();

    @Override
    public void onBackPressed() {


        final View viewToHide = findViewById(R.id.music_list_container);
        if (viewToHide != null && viewToHide.getVisibility() == View.VISIBLE) {
            viewToHide.setVisibility(View.GONE);
        } else {

            super.onBackPressed();
            MiscUtils.navigateUp(this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //TODO: Check where in the adapter there is current song and scroll to that position
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        reference = new WeakReference<>(this);
        currentPlaying = SharedPrefUtils.getLastPlayed(this).orNull();
        nowPlayingAdapter = new NowPlayingAdapter(this,
                R.layout.music_list_song_item,
                currentPlaying == null ? "0" : currentPlaying.fileHash);

        final View listView = findViewById(R.id.music_list_container);
        final CustomLinearLayoutManager layoutManager = new CustomLinearLayoutManager(this);

        mMusicRecyclerView = (EmptyRecyclerView) findViewById(R.id.recyclerView);
        mMusicRecyclerView.setLayoutManager(layoutManager);
        mMusicRecyclerView.setAdapter(nowPlayingAdapter);
        mMusicRecyclerView.setEmptyView(findViewById(R.id.empty_imageView));

        final String duration = currentPlaying == null ? "00:00" : MiscUtils.combinationFormatter(currentPlaying.getDuration());
        final Uri albumArtUri = currentPlaying == null ? null : AlbumArtUri.getUri(currentPlaying.getAlbum(),
                currentPlaying.getArtist(), currentPlaying.getDisplayName(), true).orNull();

        playerPos = (TextView) findViewById(R.id.playerPos);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.playerToolbar);
        toolbar.setTitle(player);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        shuffle = findViewById(R.id.shuffleBtn);
        repeat = findViewById(R.id.repeatBtn);
        rwdBtn = findViewById(R.id.rwdBtn);
        fwdBtn = findViewById(R.id.fwdBtn);

        toolbar.inflateMenu(R.menu.player_activity_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.action_music_list) {

                if (listView.getVisibility() == View.VISIBLE) {
                    listView.setVisibility(View.GONE);
                } else if (listView.getVisibility() == View.GONE) {

                    if (currentPlaying != null)
                        new SetCurrentPlaying().execute(currentPlaying.getFileHash());

                    listView.setVisibility(View.VISIBLE);
                }
            }
            return false;
        });

        (likeButton = (ImageView) findViewById(R.id.likeBtn)).setOnClickListener(LocalUtils.LIKE_BUTTON_CLICK);
        findViewById(R.id.push_button).setOnClickListener(pushButtonClickListener);
        (seekBar = (SeekBar) findViewById(R.id.seekBar)).setOnSeekBarChangeListener(LocalUtils.PLAYER_SEEK_LISTENER);
        (pause_play = (ImageView) findViewById(R.id.pause_play)).setOnClickListener(LocalUtils.PAUSE_CLICK);
        (songNamePlaying = (TextView) findViewById(R.id.songNamePlaying)).setText(currentPlaying == null ? "" : currentPlaying.getDisplayName());
        (artistName = (TextView) findViewById(R.id.artistName)).setText(currentPlaying == null ? "Currently there is no music playing." : currentPlaying.getArtist());
        (albumArt = (SimpleDraweeView) findViewById(R.id.albumArt)).setImageURI(albumArtUri);
        (songDuration = (TextView) findViewById(R.id.songDuration)).setText(duration);

        if (currentPlaying != null) {

            pause_play.setImageResource(R.drawable.play_white_selector);
            likeButton.setSelected(currentPlaying.isLiked != null && currentPlaying.isLiked);
        }

        /*if (SharedPrefUtils.getIsASongCurrentlyPlaying(preferences)) {
            pause_play.setImageResource(R.drawable.pause_white_selector);
        } else {
            pause_play.setImageResource(R.drawable.play_white_selector);
        }*/

        // Empty view modifications
        rwdBtn.setOnClickListener(LocalUtils.PREVIOUS_CLICK);
        fwdBtn.setOnClickListener(LocalUtils.NEXT_CLICK);


        shuffle.setOnClickListener(LocalUtils.SHUFFLE_CLICK);
        repeat.setOnClickListener(LocalUtils.REPEAT_CLICK);
        shuffle.setSelected(SharedPrefUtils.getShuffle(this));
        repeat.setSelected(SharedPrefUtils.getRepeat(this));


        getSupportLoaderManager().initLoader(StaticData.PLAYER_LOADER, null, this);
    }


    @Override
    protected void onResume() {

        super.onResume();

        reference = new WeakReference<>(this);
        currentPlaying = SharedPrefUtils.getLastPlayed(this).orNull();

        ProcessManager.installMessenger(new Messenger(handler));
        ProcessManager.submitMusicRequest(
                this,
                Optional.absent(),
                ProcessManager.ACTION_STATUS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ProcessManager.unInstallMessenger();
    }


    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (nowPlayingAdapter != null)
            nowPlayingAdapter.close();

        getSupportLoaderManager().destroyLoader(StaticData.PLAYER_LOADER);
    }

//    private void setEmptyPlayerSettings() {
//
//        likeButton.setEnabled(false);
//        pause_play.setEnabled(false);
//        shuffle.setEnabled(false);
//        repeat.setEnabled(false);
//        rwdBtn.setEnabled(false);
//        fwdBtn.setEnabled(false);
//        seekBar.setEnabled(false);
//        likeButton.setAlpha(EMPTY_VIEW_ITEM_ALPHA);
//        pause_play.setAlpha(EMPTY_VIEW_ITEM_ALPHA);
//        shuffle.setAlpha(EMPTY_VIEW_ITEM_ALPHA);
//        repeat.setAlpha(EMPTY_VIEW_ITEM_ALPHA);
//        rwdBtn.setAlpha(EMPTY_VIEW_ITEM_ALPHA);
//        fwdBtn.setAlpha(EMPTY_VIEW_ITEM_ALPHA);
//    }

    View.OnClickListener pushButtonClickListener = v -> {

        if (currentPlaying == null) {
            Toast.makeText(PlayerActivity.this, "Sorry couldn't share!", Toast.LENGTH_SHORT).show();
            return;
        }

        final Set<Song> selectedSongs = MiscUtils.getSet(1);
        //final Song song = MySongsHelper.convertMusicDataToSong(currentPlaying);
        final Song song = currentPlaying;

        Log.i(TAG, "Name of the song to push = " + song.displayName);
        Log.i(TAG, "Id of the song to push = " + song.songId);
        Log.i(TAG, "Size of the song to push = " + song.size);
        selectedSongs.add(song);

        if (selectedSongs.isEmpty()) {
            Toast.makeText(PlayerActivity.this, "Sorry couldn't share!", Toast.LENGTH_SHORT).show();
            return;
        }

        final SharedPreferences preferences = getSharedPreferences("Reach", Context.MODE_PRIVATE);
        final PushContainer pushContainer = new PushContainer.Builder()
                .senderId(SharedPrefUtils.getServerId(preferences))
                .userName(SharedPrefUtils.getUserName(preferences))
                .userImage(SharedPrefUtils.getImageId(preferences))
                .firstSongName(selectedSongs.isEmpty() ? "" : selectedSongs.iterator().next().displayName)
                .song(ImmutableList.copyOf(selectedSongs))
                .songCount(selectedSongs.size())
                .app(Collections.emptyList())
                .appCount(0)
                .firstAppName("")
                .build();

        try {
            PushActivity.startPushActivity(pushContainer, PlayerActivity.this);
        } catch (IOException e) {

            e.printStackTrace();
            //TODO Track
            Toast.makeText(PlayerActivity.this, "Could not push", Toast.LENGTH_SHORT).show();
        }

    };

    private synchronized void updateMusic(boolean paused) {

        Log.d("Ayush", "updateMusic called");
        if (currentPlaying == null)
            return;

        if (songNamePlaying != null)
            songNamePlaying.setText(currentPlaying.getDisplayName());
        if (artistName != null)
            artistName.setText(currentPlaying.getArtist());

        if (likeButton != null)
            likeButton.setSelected(currentPlaying.isLiked);

        updateDuration(MiscUtils.combinationFormatter(currentPlaying.getDuration()));
        updatePrimaryProgress(currentPlaying.getPrimaryProgress(), currentPlaying.getCurrentPosition());
        updateSecondaryProgress((short) currentPlaying.getSecondaryProgress());
        togglePlayPause(paused);

        if (albumArt != null) {

            final Optional<Uri> uriOptional = AlbumArtUri.getUri(
                    currentPlaying.getAlbum(),
                    currentPlaying.getArtist(),
                    currentPlaying.getDisplayName(),
                    true);

            if (uriOptional.isPresent())
                MiscUtils.setUriToView(albumArt, uriOptional.get(), fullAlbumArt);
            else
                albumArt.setImageBitmap(null);
        }

        if (nowPlayingAdapter != null) {

            nowPlayingAdapter.setCurrentPlayingHash(currentPlaying.getFileHash());
            new SetCurrentPlaying().executeOnExecutor(songFinder,currentPlaying.getFileHash());
        }
    }

    private synchronized void togglePlayPause(boolean pause) {

        if (pause_play != null)
            if (pause)
                pause_play.setImageResource(R.drawable.play_white_selector);
            else
                pause_play.setImageResource(R.drawable.pause_white_selector);
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
//            Log.i("Ayush", "Setting secondary progress " + progress);
            seekBar.setSecondaryProgress(progress);
        }
    }

    private synchronized void updateDuration(final String duration) {
        if (songDuration != null)
            songDuration.setText(duration);
    }

    private static final Handler handler = new Handler(msg -> {

        final Bundle bundle;
        if (msg == null || (bundle = msg.getData()) == null)
            return false;

        switch (bundle.getString(ACTION, null)) {

            case ProcessManager.REPLY_LATEST_MUSIC: {

                Log.i(TAG, "ProcessManager.REPLY_LATEST_MUSIC");
                currentPlaying = (Song) bundle.getSerializable(MUSIC_PARCEL);
                if (currentPlaying != null)
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
//                Log.i("Ayush", "Porting secondary progress " + secondaryProgress);
                MiscUtils.useActivity(reference, activity -> activity.updateSecondaryProgress(secondaryProgress));
                break;
            }

            default:
                return false;
        }

        return true;
    });

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.PLAYER_LOADER)
            return new CursorLoader(this,
                    SongProvider.CONTENT_URI,
                    SongCursorHelper.SONG_HELPER.getProjection(),
                    "(" + SongHelper.COLUMN_OPERATION_KIND + " = ? and " + SongHelper.COLUMN_STATUS + " = ?) or " +
                            SongHelper.COLUMN_OPERATION_KIND + " = ?",
                    new String[]{
                            ReachDatabase.OperationKind.DOWNLOAD_OP.getString(),
                            ReachDatabase.Status.FINISHED.getString(),
                            ReachDatabase.OperationKind.OWN.getString()},
                    SongHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE");
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed() || nowPlayingAdapter == null || mMusicRecyclerView == null)
            return;

        if (loader.getId() == StaticData.PLAYER_LOADER) {

            nowPlayingAdapter.setCursor(data);
//            if (nowPlayingAdapter.getItemCount() == 0)
//                setEmptyPlayerSettings();
        }

        mMusicRecyclerView.checkIfEmpty(nowPlayingAdapter.getItemCount());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (nowPlayingAdapter == null)
            return;

        if (loader.getId() == StaticData.PLAYER_LOADER)
            nowPlayingAdapter.setCursor(null);
    }

    @Override
    public void handOverMessage(@Nonnull Cursor cursor) {

        final Song song = SongCursorHelper.SONG_HELPER.parse(cursor);
        MiscUtils.playSong(song, this);
    }

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

        public static final View.OnClickListener REPEAT_CLICK = view ->
                view.setSelected(SharedPrefUtils.toggleRepeat(view.getContext()));

        public static final View.OnClickListener NEXT_CLICK = v -> ProcessManager.submitMusicRequest(
                v.getContext(),
                Optional.absent(),
                ProcessManager.ACTION_NEXT);

        public static final View.OnClickListener PREVIOUS_CLICK = v -> ProcessManager.submitMusicRequest(
                v.getContext(),
                Optional.absent(),
                ProcessManager.ACTION_PREVIOUS);

        public static final View.OnClickListener PAUSE_CLICK = v -> ProcessManager.submitMusicRequest(
                v.getContext(),
                Optional.fromNullable(currentPlaying),
                ProcessManager.ACTION_PLAY_PAUSE);

        public static final View.OnClickListener SHUFFLE_CLICK = view ->
                view.setSelected(SharedPrefUtils.toggleShuffle(view.getContext()));

        public static final AdapterView.OnClickListener LIKE_BUTTON_CLICK = view ->
                view.setSelected(toggleLiked(view.getContext()));

        private static boolean toggleLiked(Context context) {

            if (currentPlaying == null)
                return false;
//            Log.i(TAG, " song liked, id = " + currentPlaying.fileHash + " song type = " + currentPlaying.getType());

            final long serverId = SharedPrefUtils.getServerId(context.getSharedPreferences("Reach", MODE_PRIVATE));
            final ContentValues values = new ContentValues();
            values.put(SongHelper.COLUMN_IS_LIKED, !currentPlaying.isLiked ? 1 : 0);

            final boolean updateSuccess = context.getContentResolver().update(
                    SongProvider.CONTENT_URI,
                    values,
                    SongHelper.COLUMN_META_HASH + " = ?",
                    new String[]{currentPlaying.getFileHash() + ""}) > 0;

            //toggle if update success
            if (updateSuccess) {

                currentPlaying = new Song.Builder(currentPlaying).isLiked(!currentPlaying.isLiked).build();
                SharedPrefUtils.storeLastPlayed(context, currentPlaying); //update

                if (currentPlaying.isLiked) {

                    final Map<PostParams, String> simpleParams = MiscUtils.getMap(6);
                    simpleParams.put(PostParams.USER_ID, serverId + "");
                    simpleParams.put(PostParams.DEVICE_ID, MiscUtils.getDeviceId(context));
                    simpleParams.put(PostParams.OS, MiscUtils.getOsName());
                    simpleParams.put(PostParams.OS_VERSION, Build.VERSION.SDK_INT + "");
                    try {
                        simpleParams.put(PostParams.APP_VERSION,
                                context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode + "");
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    simpleParams.put(PostParams.SCREEN_NAME, "unknown");

                    final Map<SongMetadata, String> complexParams = MiscUtils.getMap(9);
                    complexParams.put(SongMetadata.META_HASH, currentPlaying.getFileHash());
                    complexParams.put(SongMetadata.ARTIST, currentPlaying.getArtist());
                    complexParams.put(SongMetadata.TITLE, currentPlaying.getDisplayName());
                    complexParams.put(SongMetadata.DURATION, currentPlaying.getDuration() + "");
                    complexParams.put(SongMetadata.SIZE, currentPlaying.getSize() + "");
                    complexParams.put(SongMetadata.UPLOADER_ID, currentPlaying.getSenderId() + "");
                    complexParams.put(SongMetadata.ALBUM, currentPlaying.getAlbum());

                    try {
                        UsageTracker.trackSong(simpleParams, complexParams, UsageTracker.LIKE_SONG);
                    } catch (JSONException ignored) {
                    }

                    //send notification if not own
                    if (serverId != currentPlaying.getSenderId() && currentPlaying.getType() == Song.Type.DOWNLOADED)
                        MiscUtils.autoRetryAsync(() -> StaticData.NOTIFICATION_API.addLike(
                                currentPlaying.getSenderId(),
                                serverId,
                                currentPlaying.getDisplayName()).execute(), Optional.absent());
                }
            }

            return currentPlaying.isLiked;
        }
    }

    public static final class SetCurrentPlaying extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... params) {

            //will always be non-null
            final String hashToFind = params[0];

            final Cursor cursor = MiscUtils.useActivityWithResult(reference, activity -> activity.getContentResolver().query(
                    SongProvider.CONTENT_URI,
                    new String[]{SongHelper.COLUMN_META_HASH},
                    "(" + SongHelper.COLUMN_OPERATION_KIND + " = ? and " + SongHelper.COLUMN_STATUS + " = ?) or " +
                            SongHelper.COLUMN_OPERATION_KIND + " = ?",
                    new String[]{
                            ReachDatabase.OperationKind.DOWNLOAD_OP.getString(),
                            ReachDatabase.Status.FINISHED.getString(),
                            ReachDatabase.OperationKind.OWN.getString()},
                    SongHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE")).orNull();

            if (cursor == null)
                return 0;
            if (!cursor.moveToFirst()) {
                cursor.close();
                return 0;
            }

            int counter = 0;
            while (cursor.moveToNext()) {

                counter++;
                if (hashToFind.equals(cursor.getString(0)))
                    return counter;
            }

            //not found
            return 0;
        }

        @Override
        protected void onPostExecute(Integer position) {

            super.onPostExecute(position);
            MiscUtils.useActivity(reference, activity -> {

                if (activity.mMusicRecyclerView != null && activity.nowPlayingAdapter != null) {

                    activity.nowPlayingAdapter.updatePosition(position);
                    activity.mMusicRecyclerView.scrollToPosition(position);
                }
            });
        }
    }
}