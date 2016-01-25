package reach.project.reachProcess.reachService;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import com.google.common.base.Optional;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicMarkableReference;

import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.reachProcess.auxiliaryClasses.AudioFocusHelper;
import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.reachProcess.auxiliaryClasses.MusicFocusable;
import reach.project.reachProcess.auxiliaryClasses.ReachTask;
import reach.project.utils.MiscUtils;

/**
 * Created by Dexter on 16-05-2015.
 * Infinitely plays all the songs
 * DownloadedList + MyLibrary
 * R.a) Plays next song in list (finds using cursor) : type1
 * R.b) Plays random from cursor, exclude current id : type2
 * ///////////////////////////////////////////////
 * R.c) Receives un-pause : un-pause and start playing or start according to R.a/R.b
 * R.d) Receives musicData : when newSong() is called, musicData object is updated and observer is exited
 * Music player is reinitialized, started, and observer started again.
 */
class MusicHandler extends ReachTask<MusicHandler.MusicHandlerInterface>
        implements Player.DecoderHandler,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        MusicFocusable {

    // indicates the state our service:
    private enum State {
        Playing,    // playback active (media player ready!). (but the media player may actually be
        // paused in this state if we don't have audio focus. But we stay in this state
        // so that we know we have to resume playback once we get focus back)
        Paused      // playback paused (media player ready!)
    }

    // do we have audio focus?
    private enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }

    public MusicHandler(MusicHandlerInterface handlerInterface) {
        super(handlerInterface, Type.MUSIC);
    }

    public void passNewSong(MusicData musicData) {

        musicPass.set(musicData, true);
        Log.i("Downloader", "New song set");
    }

    public boolean isPaused() {
        return player == null || player.isNull() || !player.isPlaying();
    }

    public Optional<MusicData> getCurrentSong() {
        return Optional.fromNullable(currentSong);
    }


    /**
     * The volume we set the media player to when we lose audio focus, but are allowed to reduce
     * the volume instead of stopping playback.
     */
    public final float DUCK_VOLUME = 0.1f;
    private final AtomicMarkableReference<MusicData> musicPass = new AtomicMarkableReference<>(null, false);
    private final AtomicBoolean userPaused = new AtomicBoolean(false);

    private AudioFocus audioFocus = AudioFocus.NoFocusNoDuck;
    private State playerState = State.Playing;
    private AudioFocusHelper audioFocusHelper = null;
    private MusicData currentSong = null;
    private Player player = null;

    @Override
    protected void sanitize() {

        Log.i("Downloader", "Sanitizing music handler");
        currentSong = null;
        userPaused.set(false);
        playerState = State.Playing;
        Log.i("Downloader", "Sanitizing player");
        if (player != null)
            player.cleanUp();
        giveUpAudioFocus();
        audioFocusHelper = null;
        Log.i("Downloader", "music handler sanitized");
    }

    //////////////////////////////////
    @Override
    protected void performTask() {

        kill.set(false);
        Log.i("Downloader", "Starting Music handler");
        player = new Player(this);

        audioFocusHelper = new AudioFocusHelper(handlerInterface.getContext(), this);

        Optional<MusicData> latestMusic = Optional.absent();

        while (!kill.get()) {

            Log.i("Downloader", "Taking out next song");
            /**
             * Upon completion the service is notified,
             * next song is identified and played
             */
            final short result;
            synchronized (musicPass) {
                try {
                    result = takeMusicAndPrepare(latestMusic);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break; //kill service ?
                }
            }

            if (result == 0) //gg fail
                break;
            if (result == 1) //should update the duration
                handlerInterface.updateDuration(MiscUtils.combinationFormatter(currentSong.getDuration()));

            try {
                latestMusic = Optional.fromNullable(observe(currentSong.getDuration() / 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
        Log.i("Downloader", "MUSIC PLAYER DEAD");
        //toggle the notification
        musicPass.set(null, false);
        handlerInterface.musicPlayerDead();
    }

    /**
     * @param latestMusic the Music data
     * @return 0 : disaster, 1 : OK update duration, 2 : OK don't update duration
     */
    private short takeMusicAndPrepare(Optional<MusicData> latestMusic) throws InterruptedException {

        if (!latestMusic.isPresent() || musicPass.isMarked())
            latestMusic = Optional.fromNullable(musicPass.getReference());
        musicPass.set(null, false);
        if (!latestMusic.isPresent()) {
            Log.i("Downloader", "Pulling next song !");
            latestMusic = handlerInterface.nextSong(Optional.fromNullable(currentSong), true);
        }
        if (!latestMusic.isPresent()) {
            Log.i("Downloader", "NO SONG FOUND ERROR !");
            return 0; //gg fail
        }

        currentSong = latestMusic.get();
        Log.i("Downloader", currentSong.getProcessed() + " " + currentSong.getLength());
        Log.i("Downloader", "found new song, needs streaming ? " + (currentSong.getProcessed() < currentSong.getLength()) +
                " name= " + currentSong.getDisplayName());
        handlerInterface.updateSongDetails(currentSong);

        final short toReturn;
        final long duration;

        try {
            duration = (currentSong.getProcessed() < currentSong.getLength()) ?
                    player.createAudioTrackIfNeeded(Optional.fromNullable(currentSong.getPath()), currentSong.getLength()) :
                    player.createMediaPlayerIfNeeded(this, this, currentSong.getPath());
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("Downloader", currentSong.getPath() + " could not be prepared");
            final File check = new File(currentSong.getPath());
            final String missType;
            if (!(check.isFile() && check.length() == currentSong.getLength()))
                missType = "Possible File Corruption " + e.getLocalizedMessage();
            else
                missType = "AudioTrackError " + e.getLocalizedMessage();
            handlerInterface.errorReport(currentSong.getDisplayName(), missType);
            return 0;
        }

        //if we did not know the duration already try to set
        if (currentSong.getDuration() == 0) {

            if (duration == 0)
                return 0; //now if duration is not known fuck off
            currentSong.setDuration(duration); //only update duration for old songs
            toReturn = 1; //update the duration
        } else
            toReturn = 2; //don't update the duration
        Log.i("Downloader", "Player ready " + userPaused.get() + " " + playerState);
        tryToGetAudioFocus();
        if (!userPaused.get() && playerState == State.Playing)
            configAndStartPlayer();

        return toReturn;
    }

    /**
     * This method observes the progress of media player
     * and reports back to the service
     * Killed by :
     * a) kill : whole thread needs to die
     * b) musicPass.isMarked() : new song needs to play
     */
    private MusicData observe(long duration) throws InterruptedException {

        Log.i("Downloader", "starting observer");
        int lastPositionUpdate = 0;
        while (!kill.get() && player != null && currentSong != null) {

            synchronized (musicPass) {

                if (musicPass.isMarked()) {
                    final MusicData latestMusic = musicPass.getReference();
                    musicPass.set(null, false);
                    if (latestMusic != null)
                        return latestMusic;
                }
                //should not block since Music was not found
            }
            final int currentPosition = player.getCurrentPosition();
//            Log.i("Downloader", player.getCurrentPosition() + " sending current position !!");
            if (currentPosition != lastPositionUpdate) {

                lastPositionUpdate = currentPosition;
                final short primaryProgress = (short) ((currentPosition * 100) / duration);
//                currentSong.setCurrentPosition(currentPosition); //un-necessary ?
//                currentSong.setPrimaryProgress(primaryProgress);
                handlerInterface.updatePrimaryProgress(primaryProgress, currentPosition);
            }
            Thread.sleep(500L);
        }
        return null;
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
     * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
     * we have focus, it will play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
     * current focus settings. This method assumes mPlayer != null, so if you are calling it,
     * you have to do so from a context where you are sure this is the case.
     */
    synchronized void configAndStartPlayer() throws IllegalStateException {

        if (audioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause, even if mState
            // is State.Playing. But we stay in the Playing state so that we know we have to resume
            // playback once we get the focus back.
            if (player.isPlaying()) {
                player.pause();
                handlerInterface.paused();
            }
            Log.i("Downloader", "We don't have focus");
            return;
        } else if (audioFocus == AudioFocus.NoFocusCanDuck)
            player.setVolume(DUCK_VOLUME);  // we'll be relatively quiet
        else
            player.setVolume(1.0f); // we can be loud

        if (!player.isPlaying()) {
            Log.i("Downloader", "STARTING PLAYER");
            player.start();
            handlerInterface.unPaused();
        }
    }

    synchronized void userUnPause() {
        userPaused.set(false);
        if (playerState != null)
            playerState = State.Playing;
    }

    synchronized void processPlayRequest() {

        tryToGetAudioFocus();
        //pause reason don't matter
        playerState = State.Playing;
        userPaused.set(false);
        configAndStartPlayer();
    }

    synchronized void processPauseRequest() {

        playerState = State.Paused;
        userPaused.set(true);
        Log.i("Downloader", "PAUSING !!!!");
        player.pause();
        handlerInterface.paused();
    }

    /**
     * @return true means we should push a new song !
     */
    boolean processPlayPause() {
        if (player == null || player.isNull())
            return true;
        if (player.isPlaying())
            processPauseRequest();
        else
            processPlayRequest();
        return false;
    }

    boolean processSeek(short percent) {
        if (player == null || player.isNull() || currentSong == null)
            return false; //push new song
        try {
            if (!player.isPlaying())
                processPlayRequest();
            player.seekTo((int) ((currentSong.getDuration() / 100) * percent));
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        }
        return true;
    }

    void giveUpAudioFocus() {
        if ((audioFocus == AudioFocus.Focused || audioFocus == AudioFocus.NoFocusCanDuck) &&
                audioFocusHelper != null &&
                audioFocusHelper.abandonFocus()) audioFocus = AudioFocus.NoFocusNoDuck;
    }

    void tryToGetAudioFocus() {
        if (audioFocus != AudioFocus.Focused && audioFocusHelper != null && audioFocusHelper.requestFocus())
            audioFocus = AudioFocus.Focused;
    }

    @Override
    public void onGainedAudioFocus() {
        audioFocus = AudioFocus.Focused;
        Log.i("Downloader", "AudioFocus gained");
        // restart media player with new focus settings
        if (playerState == State.Playing && !userPaused.get()) //even if player was not playing, the state is still playing
            configAndStartPlayer();
    }

    @Override
    public void onLostAudioFocus(boolean canDuck) {

        audioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;
        Log.i("Downloader", "AudioFocus lost " + canDuck);
        // start/restart/pause media player with new focus settings
        if (player.isPlaying() && !userPaused.get()) //configure only if ACTUALLY playing
            configAndStartPlayer();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        handlerInterface.pushNextSong(
                handlerInterface.nextSong(Optional.fromNullable(currentSong), true));
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {

        kill.set(true);
        final StringBuilder error = new StringBuilder(2);
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                error.append("MEDIA_ERROR_UNKNOWN");
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                error.append("MEDIA_ERROR_SERVER_DIED");
                break;
        }
        switch (extra) {
            case MediaPlayer.MEDIA_ERROR_IO:
                error.append("MEDIA_ERROR_IO");
                break;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                error.append("MEDIA_ERROR_MALFORMED");
                break;
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                error.append("MEDIA_ERROR_UNSUPPORTED");
                break;
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                error.append("MEDIA_ERROR_TIMED_OUT");
                break;
        }
        Log.i("Downloader", "ERROR during playBack " + error.toString());
        if (currentSong == null)
            handlerInterface.errorReport("", error.toString());
        else
            handlerInterface.errorReport(currentSong.getDisplayName(), error.toString());
        return true; // true indicates we handled the error
    }

    /////////////////////////////////////
    @Override
    public long getProcessed() {
        //if this gets called current song HAS to be reachDatabase
        final Cursor cursor = handlerInterface.getContext().getContentResolver().query(
                Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + currentSong.getId()),
                new String[]{ReachDatabaseHelper.COLUMN_PROCESSED},
                ReachDatabaseHelper.COLUMN_ID + " = ?",
                new String[]{currentSong.getId() + ""}, null);
        if (cursor == null) {
            return 0;
        }
        if (!cursor.moveToFirst()) {
            cursor.close();
            return 0;
        }
        final long processed = cursor.getLong(0); //processed is at 1 in custom cursor
        cursor.close();
        currentSong.setProcessed(processed);
        return processed;
    }

    @Override
    public void updateSecondaryProgress(short percent) {

        currentSong.setSecondaryProgress(percent);
        handlerInterface.updateSecondaryProgress(percent);
    }

    public interface MusicHandlerInterface {

        void pushNextSong(Optional<MusicData> songToPush);

        /**
         * @return the next song depending on whatever rule
         */
        Optional<MusicData> nextSong(Optional<MusicData> currentSong, boolean automatic);

        Context getContext();

        void updateSecondaryProgress(short percent);

        void updatePrimaryProgress(short percent, int position);

        SharedPreferences getSharedPreferences();

        void errorReport(String songName, String missType);

        /**
         * The Music thread has died, update the notification
         */
        void musicPlayerDead();

        /**
         * @param musicData update the notification
         */
        void updateSongDetails(MusicData musicData);

        void updateDuration(String formattedDuration);

        void paused();

        void unPaused();
    }
}