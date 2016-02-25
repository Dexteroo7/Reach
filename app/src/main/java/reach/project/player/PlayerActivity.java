package reach.project.player;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import org.json.JSONException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import reach.project.R;
import reach.project.apps.App;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.coreViews.myProfile.EmptyRecyclerView;
import reach.project.coreViews.push.PushActivity;
import reach.project.coreViews.push.PushContainer;
import reach.project.music.MySongsHelper;
import reach.project.music.MySongsProvider;
import reach.project.music.Song;
import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.reachProcess.reachService.ProcessManager;
import reach.project.usageTracking.PostParams;
import reach.project.usageTracking.SongMetadata;
import reach.project.usageTracking.UsageTracker;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;

public class PlayerActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, HandOverMessage {

    /*private MusicListFragment frag;*/
    private static long userId = 0;
    private MusicListAdapter musicListAdapter;
    private RelativeLayout mMusicListViewContainer;
    private Cursor mLibrarySongsCursor;
    private Cursor mDownloadedSongsCursor;
    private EmptyRecyclerView mMusicRecyclerView;
    private int count=0;
    private CustomLinearLayoutManager layoutManager;
    private static final  String player = "Player";
    private View shuffle;
    private View repeat;
    private View rwdBtn;
    private View fwdBtn;
    public static final String TAG = PlayerActivity.class.getSimpleName();

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
    public static final String MUSIC_LIST_FRAGMENT_TAG = "music_list_fragment";
    public static final float EMPTY_VIEW_ITEM_ALPHA = 0.4f;

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
    @Nullable
    private View pushButton = null;
    private Set<Song> selectedSongs;
    private Set<App> selectedApps;

    //private boolean mMusicFragmentVisible;

    @Override
    public void onBackPressed() {
        if(mMusicListViewContainer.getVisibility() == View.VISIBLE){
            mMusicListViewContainer.setVisibility(View.GONE);
        }
        else {
            MiscUtils.navigateUp(this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        reference = new WeakReference<>(this);
        currentPlaying = SharedPrefUtils.getLastPlayed(this).orNull();



        mMusicRecyclerView = (EmptyRecyclerView) findViewById(R.id.recyclerView);

        /*final long current_playing_song_id = getArguments().getLong(CURRENT_SONG_ID_KEY);*/
        //TODO: Check where in the adapter there is current song and scroll to that position
        mMusicListViewContainer = (RelativeLayout) findViewById(R.id.music_list_container);
        musicListAdapter = new MusicListAdapter(this, this, this,currentPlaying == null ? 0 : currentPlaying.getId());
        layoutManager = new CustomLinearLayoutManager(this);
        mMusicRecyclerView.setLayoutManager(layoutManager);
        mMusicRecyclerView.setAdapter(musicListAdapter);
        mMusicRecyclerView.setEmptyView(findViewById(R.id.empty_imageView));
        getSupportLoaderManager().initLoader(StaticData.DOWNLOAD_LOADER, null, this);
        getSupportLoaderManager().initLoader(StaticData.MY_LIBRARY_LOADER, null, this);

        final String duration = currentPlaying == null ? "00:00" : MiscUtils.combinationFormatter(currentPlaying.getDuration());
        final Uri albumArtUri = currentPlaying == null ? null : AlbumArtUri.getUri(currentPlaying.getAlbumName(),
                currentPlaying.getArtistName(), currentPlaying.getDisplayName(), true).orNull();

        playerPos = (TextView) findViewById(R.id.playerPos);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.playerToolbar);
        toolbar.setTitle(player);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
         shuffle = findViewById(R.id.shuffleBtn);
         repeat = findViewById(R.id.repeatBtn);
         rwdBtn = findViewById(R.id.rwdBtn);
         fwdBtn = findViewById(R.id.fwdBtn);
        //final RelativeLayout musicListFragContainer = (RelativeLayout) findViewById(R.id.music_list_frag_container);
        final SharedPreferences preferences = this.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        userId = SharedPrefUtils.getServerId(preferences);

        toolbar.inflateMenu(R.menu.player_activity_menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();

                //noinspection SimplifiableIfStatement
                if (id == R.id.action_music_list) {
                    if(mMusicListViewContainer.getVisibility() == View.VISIBLE){
                        mMusicListViewContainer.setVisibility(View.GONE);
                    }
                    else if (mMusicListViewContainer.getVisibility() == View.GONE){
                        mMusicListViewContainer.setVisibility(View.VISIBLE);
                    }

                    /*if(!mMusicFragmentVisible) {
                        musicListFragContainer.setVisibility(View.VISIBLE);
                        frag.setCurrentSongId(currentPlaying.getId());
                        getSupportFragmentManager().beginTransaction()*//*.setCustomAnimations(
                                R.animator.card_flip_right_in,
                                R.animator.card_flip_right_out,
                                R.animator.card_flip_left_in,
                                R.animator.card_flip_left_out)*//*.replace(R.id.music_list_frag_container,frag *//*MusicListFragment.getInstance("Songs",currentPlaying==null?null:currentPlaying.getId())*//*, MUSIC_LIST_FRAGMENT_TAG ).commit();
                        mMusicFragmentVisible = true;
                        return true;
                    }
                    else{
                        final Fragment music_list_frag = getSupportFragmentManager().findFragmentByTag(MUSIC_LIST_FRAGMENT_TAG);
                        if(music_list_frag !=null){
                            getSupportFragmentManager().beginTransaction().remove(music_list_frag).commit();
                            //musicListFragContainer.setVisibility(View.GONE);
                            mMusicFragmentVisible = false;
                        }
                    }*/
                }
                return false;
            }
        });

                (likeButton = findViewById(R.id.likeBtn)).setOnClickListener(LocalUtils.LIKE_BUTTON_CLICK);
                (pushButton = findViewById(R.id.push_button)).setOnClickListener(pushButtonClickListener);
        (seekBar = (SeekBar) findViewById(R.id.seekBar)).setOnSeekBarChangeListener(LocalUtils.PLAYER_SEEK_LISTENER);
        (pause_play = (ImageView) findViewById(R.id.pause_play)).setOnClickListener(LocalUtils.PAUSE_CLICK);
        (songNamePlaying = (TextView) findViewById(R.id.songNamePlaying)).setText(currentPlaying == null ? "" : currentPlaying.getDisplayName());
        (artistName = (TextView) findViewById(R.id.artistName)).setText(currentPlaying == null ? "Currently there is no music." : currentPlaying.getArtistName());
        (albumArt = (SimpleDraweeView) findViewById(R.id.albumArt)).setImageURI(albumArtUri);
        (songDuration = (TextView) findViewById(R.id.songDuration)).setText(duration);

        if (currentPlaying != null) {
            pause_play.setImageResource(R.drawable.play_white_selector);
        }
        // Empty view modifications

        rwdBtn.setOnClickListener(LocalUtils.PREVIOUS_CLICK);
        fwdBtn.setOnClickListener(LocalUtils.NEXT_CLICK);


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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDownloadedSongsCursor = null;
        mLibrarySongsCursor = null;
        getSupportLoaderManager().destroyLoader(StaticData.DOWNLOAD_LOADER);
        getSupportLoaderManager().destroyLoader(StaticData.MY_LIBRARY_LOADER);
    }
    
    private void setEmptyPlayerSettings(){
        likeButton.setEnabled(false);
        pause_play.setEnabled(false);
        shuffle.setEnabled(false);
        repeat.setEnabled(false);
        rwdBtn.setEnabled(false);
        fwdBtn.setEnabled(false);
        seekBar.setEnabled(false);
        likeButton.setAlpha(EMPTY_VIEW_ITEM_ALPHA);
        pause_play.setAlpha(EMPTY_VIEW_ITEM_ALPHA);
        shuffle.setAlpha(EMPTY_VIEW_ITEM_ALPHA);
        repeat.setAlpha(EMPTY_VIEW_ITEM_ALPHA);
        rwdBtn.setAlpha(EMPTY_VIEW_ITEM_ALPHA);
        fwdBtn.setAlpha(EMPTY_VIEW_ITEM_ALPHA);
    }

    View.OnClickListener pushButtonClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {

            if(currentPlaying == null){
                Toast.makeText(PlayerActivity.this, "Sorry couldn't share!", Toast.LENGTH_SHORT).show();
                return;
            }

            selectedSongs = MiscUtils.getSet(1);
            selectedApps = MiscUtils.getSet(1);
            final Song song = MySongsHelper.convertMusicDataToSong(currentPlaying);
            Log.i(TAG,"Name of the song to push = " + song.displayName);
            selectedSongs.add(song);

            if (selectedSongs.isEmpty() && selectedApps.isEmpty()) {
                Toast.makeText(PlayerActivity.this, "First select some songs", Toast.LENGTH_SHORT).show();
            }

            final SharedPreferences preferences = getSharedPreferences("Reach", Context.MODE_PRIVATE);
            final PushContainer pushContainer = new PushContainer.Builder()
                    .senderId(SharedPrefUtils.getServerId(preferences))
                    .userName(SharedPrefUtils.getUserName(preferences))
                    .userImage(SharedPrefUtils.getImageId(preferences))
                    .firstSongName(selectedSongs.isEmpty() ? "" : selectedSongs.iterator().next().displayName)
                    .firstAppName(selectedApps.isEmpty() ? "" : selectedApps.iterator().next().applicationName)
                    .song(ImmutableList.copyOf(selectedSongs))
                    .app(ImmutableList.copyOf(selectedApps))
                    .songCount(selectedSongs.size())
                    .appCount(selectedApps.size())
                    .build();

            try {
                PushActivity.startPushActivity(pushContainer, PlayerActivity.this);
            } catch (IOException e) {

                e.printStackTrace();
                //TODO Track
                Toast.makeText(PlayerActivity.this, "Could not push", Toast.LENGTH_SHORT).show();
            }

        }
    };

    private synchronized void togglePlayPause(final boolean pause) {

        if (pause_play != null) {
            if (pause)
                pause_play.setImageResource(R.drawable.play_white_selector);
            else
                pause_play.setImageResource(R.drawable.pause_white_selector);
        }
    }


    private void findTheSongInTheCursor(){
        new AsyncTask<Void,Void,Integer>(){


            @Override
            protected Integer doInBackground(Void... params) {
                if(currentPlaying!=null) {
                    if (mDownloadedSongsCursor != null) {
                        int i = 0;
                        for (mDownloadedSongsCursor.moveToFirst(); !mDownloadedSongsCursor.isAfterLast(); mDownloadedSongsCursor.moveToNext()) {
                            if (mDownloadedSongsCursor.getLong(20) == currentPlaying.getId()) {

                                return i;
                            }
                            i++;
                        }


                    }
                    if (mLibrarySongsCursor != null) {
                        int i = 0;
                        for (mLibrarySongsCursor.moveToFirst(); !mLibrarySongsCursor.isAfterLast(); mLibrarySongsCursor.moveToNext()) {
                            if (mLibrarySongsCursor.getLong(0) == currentPlaying.getId()) {
                                if (mDownloadedSongsCursor != null) {
                                    final int count = mDownloadedSongsCursor.getCount();

                                    return (count + i);
                                }
                                return i;
                            }
                            i++;

                        }
                    }
                     return 0;
                }
                return -1;
            }

            @Override
            protected void onPostExecute(Integer position) {
                super.onPostExecute(position);
                Log.i("PlayerActivity","position = " + position);
                PlayerActivity activity;
                if(reference == null || (activity = reference.get()) == null || position == -1){
                    return;
                }
                else{
                    activity.layoutManager.scrollToPositionWithOffset(position,2);
                }

                //mMusicRecyclerView.scrollToPosition(position);

            }
        }.execute();

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
                //TODO: After next the flow comes here maybe, Check
                Log.i(TAG, "ProcessManager.REPLY_LATEST_MUSIC");

                    currentPlaying = bundle.getParcelable(MUSIC_PARCEL);
                    MiscUtils.useActivity(reference, activity -> activity.updateMusic(false));
                    Log.i(TAG, "Previously playing song id =  " + musicListAdapter.getCurrentlyPlayingSongId());
                    musicListAdapter.setCurrentlyPlayingSongId(currentPlaying.getId());
                    Log.i(TAG, "Current playing song id =  " + musicListAdapter.getCurrentlyPlayingSongId());
                    musicListAdapter.notifyDataSetChanged();
                    findTheSongInTheCursor();




                //CursorIndexOutOfBoundsException
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == StaticData.MY_LIBRARY_LOADER)
            return new CursorLoader(this,
                    MySongsProvider.CONTENT_URI,
                    MySongsHelper.DISK_LIST,
                    null, null,
                    MySongsHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE"); //show all songs !
        else if (id == StaticData.DOWNLOAD_LOADER)
            return new CursorLoader(this,
                    ReachDatabaseProvider.CONTENT_URI,
                    ReachDatabaseHelper.MUSIC_DATA_LIST,
                    /*ReachDatabaseHelper.COLUMN_STATUS + " = ? and " +*/ //show only finished
                            ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
                    new String[]{/*ReachDatabase. + "",*/ "0"},
                    ReachDatabaseHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE");

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed())
            return;

       // final int count = data.getCount();
        if (loader.getId() == StaticData.MY_LIBRARY_LOADER) {

//            Log.i("Ayush", "MyLibrary file manager " + count);

            musicListAdapter.setNewMyLibraryCursor(data);
            mLibrarySongsCursor = data;


        } else if (loader.getId() == StaticData.DOWNLOAD_LOADER) {

//            Log.i("Ayush", "Downloaded file manager " + count);

            musicListAdapter.setNewDownLoadCursor(data);
            mDownloadedSongsCursor = data;

        }
        count++;
        if(count == 2){
            Log.d("PlayerActivity", "AsyncTAsk called");
            musicListAdapter.notifyDataSetChanged();
            if(musicListAdapter.getItemCount()==0){
                setEmptyPlayerSettings();
            }
            findTheSongInTheCursor();
            mMusicRecyclerView.checkIfEmpty(musicListAdapter.getItemCount());
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (musicListAdapter == null)
            return;

        if (loader.getId() == StaticData.MY_LIBRARY_LOADER)
            musicListAdapter.setNewMyLibraryCursor(null);
        else if (loader.getId() == StaticData.DOWNLOAD_LOADER)
            musicListAdapter.setNewDownLoadCursor(null);

    }

    @Override
    public void handOverMessage(@Nonnull Object message) {

        if (message instanceof Cursor) {

            final Cursor cursor = (Cursor) message;
            final int count = cursor.getColumnCount();

            // To play songs of the user (not the downloaded ones)
            if (count == MySongsHelper.DISK_LIST.length) {

                final MusicData musicData = MySongsHelper.getMusicData(cursor, userId);
                MiscUtils.playSong(musicData, this);

            }
            //To play the songs downloaded from reach
            else if (count == ReachDatabaseHelper.MUSIC_DATA_LIST.length) {

                final MusicData musicData = ReachDatabaseHelper.getMusicData(cursor);
                MiscUtils.playSong(musicData, this);
            } else
                throw new IllegalArgumentException("Unknown column count found");
            // Music Data is used for recent list songs
        } else if (message instanceof MusicData) {
            MiscUtils.playSong((MusicData) message, this);
        } else
            throw new IllegalArgumentException("Unknown type handed over");

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

        public static final View.OnClickListener REPEAT_CLICK = view -> {

            if (SharedPrefUtils.toggleRepeat(view.getContext()))
                view.setSelected(true);
            else
                view.setSelected(false);
        };

        //TODO: One suggested solution, Reboot the music list fragment here after you get a response from the service to display the song, only if the fragment is visible
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
                Log.i(TAG, " song liked, id = " + currentPlaying.getId() + " song type = " + currentPlaying.getType() );
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
                            context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode + "");
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