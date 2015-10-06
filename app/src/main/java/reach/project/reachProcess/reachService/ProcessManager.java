package reach.project.reachProcess.reachService;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.gms.analytics.HitBuilders;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.friends.ReachFriendsHelper;
import reach.project.friends.ReachFriendsProvider;
import reach.project.music.songs.ReachSongHelper;
import reach.project.music.songs.ReachSongProvider;
import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.reachProcess.auxiliaryClasses.ReachTask;
import reach.project.uploadDownload.ReachDatabase;
import reach.project.uploadDownload.ReachDatabaseHelper;
import reach.project.uploadDownload.ReachDatabaseProvider;
import reach.project.utils.SharedPrefUtils;

/**
 * Created by Dexter on 14-05-2015.
 */
public class ProcessManager extends Service implements
        MusicHandler.MusicHandlerInterface,
        NetworkHandler.NetworkHandlerInterface {

    private enum NotificationState {
        Network,
        Music,
        Both,
        Dead
    }

    //////////////////////////////////
    private NotificationState notificationState = NotificationState.Dead;
    //////////////////////////////////
    //no confirmation
    public static final String REPLY_PRIMARY_PROGRESS = "reach.project.reachProcess.reachService.ProcessManager.REPLY_PRIMARY_PROGRESS";
    public static final String REPLY_SECONDARY_PROGRESS = "reach.project.reachProcess.reachService.ProcessManager.REPLY_SECONDARY_PROGRESS";
    //confirmation
    public static final String REPLY_LATEST_MUSIC = "reach.project.reachProcess.reachService.ProcessManager.REPLY_LATEST_MUSIC";
    public static final String REPLY_DURATION = "reach.project.reachProcess.reachService.ProcessManager.REPLY_DURATION";
    public static final String REPLY_PAUSED = "reach.project.reachProcess.reachService.ProcessManager.REPLY_PAUSED";
    public static final String REPLY_UN_PAUSED = "reach.project.reachProcess.reachService.ProcessManager.REPLY_UN_PAUSED";
    public static final String REPLY_MUSIC_DEAD = "reach.project.reachProcess.reachService.ProcessManager.REPLY_MUSIC_DEAD";
    public static final String REPLY_ERROR = "reach.project.reachProcess.reachService.ProcessManager.REPLY_ERROR";

    private void sendMessage(Context context, Optional<?> message, String action) {

        final Intent intent = new Intent(context, ReachActivity.PlayerUpdateListener.class);
        intent.setAction(action);
        if (message.isPresent()) {
            if (message.get() instanceof String)
                intent.putExtra("message", (String) message.get());
            else if (message.get() instanceof MusicData)
                intent.putExtra("message", (MusicData) message.get());
        }
        context.sendBroadcast(intent);
    }

    private void updatePrimaryProgress(Context context, short progress, int position, String action) {

        final Intent intent = new Intent(context, ReachActivity.PlayerUpdateListener.class);
        intent.setAction(action);
        intent.putExtra("progress", progress);
        intent.putExtra("position", position);
        context.sendBroadcast(intent);
    }

    private void updateSecondaryProgress(Context context, short progress, String action) {

        final Intent intent = new Intent(context, ReachActivity.PlayerUpdateListener.class);
        intent.setAction(action);
        intent.putExtra("progress", progress);
        context.sendBroadcast(intent);
    }

    public static void submitNetworkRequest(@NonNull Context context, @NonNull String message) {
        helper(context, Optional.of(message), NetworkHandler.ACTION_NETWORK_MESSAGE);
    }

    public static void submitMusicRequest(@NonNull Context context, @NonNull Optional<?> message, @NonNull String action) {
        helper(context, message, action);
    }

    private static void helper(@NonNull Context context, @NonNull Optional<?> message, @NonNull String action) {

        final Intent intent = new Intent(context, ProcessManager.class);
        intent.setAction(action);
        if (message.isPresent()) {
            if (message.get() instanceof String)
                intent.putExtra("message", (String) message.get());
            else if (message.get() instanceof MusicData)
                intent.putExtra("message", (MusicData) message.get());
        }
        context.startService(intent);
    }
    //////////////////////////////////
    /**
     * Pi = ith parent_thread
     * Cij = jth child_thread of ith parent_thread
     * *******************************************
     * P1 : Music_Player //explicitly killed by User
     * C11 : HttpHandler
     * C12 : HttpReader
     * C13 : HttpWriter
     * P2 : Network_Controller //auto-start, self kill, User can also kill it
     * C21 : LanHandler
     * P3 : Communicator
     */

    /**
     * Sanitize pattern :
     * close() : close will signal the 'Runnable', either parent or child to kill itself,
     * basically stop the infinite while loop. In the next loop cycle, the break will happen.
     * Once loop breaks sanitize() happens. close() is ALWAYS called by the parent. It basically
     * toggles an atomic boolean and thread-safety is assured.
     * <p>
     * sanitize() : sanitize resets the global data of the current Runnable and calls
     * 'close()' of any child Runnable. By reset I mean, close all closeables, clear stuff etc.
     * The complete global space needs to be purged. Cleaning thread-local space might also be necessary.
     * sanitize() is strictly private. Always called ar start and end of run()
     */

    public static WeakReference<ProcessManager> reference;

    private final Random random = new Random();
    private final ExecutorService fixedPool = Executors.newFixedThreadPool(8); //buffer of 1 thread
    private final ListeningExecutorService sameThreadExecutor = MoreExecutors.sameThreadExecutor();
    private final Semaphore killCheck = new Semaphore(2, true); //fixed 2 parents
    private final NetworkHandler networkHandler = new NetworkHandler(this);
    private final MusicHandler musicHandler = new MusicHandler(this);
    private final Stack<MusicId> musicStack = new Stack<>(); //id, type, push only when in shuffle mode

    private Future<?> musicFuture, networkFuture;
    private PowerManager.WakeLock wakeLock = null;
    private static long serverId;

    private void close() {

        networkHandler.close();
        musicHandler.close();
        musicStack.clear();
        if (musicFuture != null)
            musicFuture.cancel(true);
        if (networkFuture != null)
            networkFuture.cancel(true);
        if (wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
        if (reference != null) {
            reference.clear();
            reference = null;
        }

        musicFuture = networkFuture = null;
        wakeLock = null;

        fixedPool.shutdownNow();
        stopSelf();
        Log.i("Downloader", "KILLING SERVICE !!!");
    }

    private final class MusicId {
        final long id;
        final byte type;

        private MusicId(long id, byte type) {
            this.id = id;
            this.type = type;
        }
    }

    private void closeCursor(Optional<Cursor> cursor) {
        if (cursor.isPresent())
            cursor.get().close();
    }

    private void pushSongToStack(MusicData musicData) {

        if (!musicStack.isEmpty() &&
                musicStack.peek().id == musicData.getId() &&
                musicStack.peek().type == musicData.getType()) return;
        musicStack.push(new MusicId(musicData.getId(), musicData.getType()));
    }

    private Optional<MusicData> shufflePrevious() {

        if (musicStack.isEmpty())
            return previousSong(musicHandler.getCurrentSong());
        final MusicId currentSong = musicStack.pop(); //ignore current song !
        if (musicStack.isEmpty()) {
            musicStack.push(currentSong);
            return previousSong(musicHandler.getCurrentSong());
        }
        final MusicId lastSong = musicStack.pop(); //ignore current song !
        final Cursor cursor;
        if (lastSong.type == 0)
            cursor = getContentResolver().query(
                    Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + lastSong.id),
                    StaticData.DOWNLOADED_PARTIAL,
                    ReachDatabaseHelper.COLUMN_ID + " = ? and " +
                            ReachDatabaseHelper.COLUMN_PROCESSED + " > ?",
                    new String[]{lastSong.id + "", "0"}, null);
        else
            cursor = getContentResolver().query(
                    ReachSongProvider.CONTENT_URI,
                    StaticData.DISK_PARTIAL,
                    ReachSongHelper.COLUMN_USER_ID + " = ? and " +
                            ReachSongHelper.COLUMN_SONG_ID + " = ?",
                    new String[]{serverId + "", lastSong.id + ""}, null);
        if (cursor != null)
            cursor.moveToFirst();
        return playFromCursor(Optional.fromNullable(cursor), lastSong.type);
    }

    private Optional<MusicData> shuffleNext(long id, byte type) {

        final Cursor reachSongCursor, myLibraryCursor;
        if (type == 0) {
            reachSongCursor = getContentResolver().query(
                    ReachDatabaseProvider.CONTENT_URI,
                    StaticData.DOWNLOADED_PARTIAL,
                    ReachDatabaseHelper.COLUMN_ID + " != ? and " +
                            ReachDatabaseHelper.COLUMN_PROCESSED + " > ?", //all except that id
                    new String[]{id + "", "0"}, null);
            myLibraryCursor = getMyLibraryCursor();
        } else {
            reachSongCursor = getReachDatabaseCursor();
            myLibraryCursor = getContentResolver().query(
                    ReachSongProvider.CONTENT_URI,
                    StaticData.DISK_PARTIAL,
                    ReachSongHelper.COLUMN_USER_ID + " = ? and " +
                            ReachSongHelper.COLUMN_SONG_ID + " != ?",
                    new String[]{serverId + "", id + ""}, null);
        }

        if (reachSongCursor == null || !reachSongCursor.moveToFirst()) {
            closeCursor(Optional.fromNullable(reachSongCursor));
            return playFromCursor(chooseRandomFromCursor(myLibraryCursor), (byte) 1);
        }
        if (myLibraryCursor == null || !myLibraryCursor.moveToFirst()) {
            closeCursor(Optional.fromNullable(myLibraryCursor));
            return playFromCursor(chooseRandomFromCursor(reachSongCursor), (byte) 0);
        }

        final int reachCount = reachSongCursor.getCount();
        final int myLibraryCount = myLibraryCursor.getCount();
        final int chosenPosition = random.nextInt(reachCount + myLibraryCount); //0-index
        if (reachCount > chosenPosition && reachSongCursor.move(chosenPosition)) {
            closeCursor(Optional.of(myLibraryCursor));
            return playFromCursor(Optional.of(reachSongCursor), (byte) 0);
        } else if (myLibraryCount > chosenPosition - reachCount && myLibraryCursor.move(chosenPosition - reachCount)) {
            closeCursor(Optional.of(reachSongCursor));
            return playFromCursor(Optional.of(myLibraryCursor), (byte) 1);
        }
        //random position failed
        else if (reachCount > myLibraryCount) {
            closeCursor(Optional.of(myLibraryCursor));
            return playFromCursor(chooseRandomFromCursor(reachSongCursor), (byte) 0);
        } else {
            closeCursor(Optional.of(reachSongCursor));
            return playFromCursor(chooseRandomFromCursor(myLibraryCursor), (byte) 1);
        }
    }

    private synchronized int getTotalDownloads() {
        return getContentResolver().query(
                ReachDatabaseProvider.CONTENT_URI,
                new String[]{ReachDatabaseHelper.COLUMN_ID},
                ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ? and " +
                        ReachDatabaseHelper.COLUMN_STATUS + " = ?",
                new String[]{"0", ReachDatabase.RELAY + ""}, null).getCount();
    }

    //RETURNS TOTAL NUMBER OF PEOPLE WHO ARE UPLOADING
    private synchronized int getTotalUploads() {
        return getContentResolver().query(
                ReachDatabaseProvider.CONTENT_URI,
                new String[]{ReachDatabaseHelper.COLUMN_ID},
                ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ? and " +
                        ReachDatabaseHelper.COLUMN_STATUS + " = ?",
                new String[]{"1", ReachDatabase.RELAY + ""}, null).getCount();
    }

    private String generateNotificationText(int totalDownloads, int totalUploads) {

        if (totalDownloads > 0 && totalUploads == 0) {
            //only download
            final Cursor songNameCursor = getContentResolver().query(
                    ReachDatabaseProvider.CONTENT_URI,
                    new String[]{ReachDatabaseHelper.COLUMN_DISPLAY_NAME},
                    ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ? and " +
                            ReachDatabaseHelper.COLUMN_STATUS + " = ?",
                    new String[]{"0", ReachDatabase.RELAY + ""},
                    ReachDatabaseHelper.COLUMN_DATE_ADDED + " DESC");

            if (songNameCursor == null)
                return totalDownloads + " songs are being downloaded";
            if (!songNameCursor.moveToFirst()) {
                songNameCursor.close();
                return totalDownloads + " songs are being downloaded";
            }
            final String name = songNameCursor.getString(0);
            songNameCursor.close();
            if (totalDownloads == 1)
                return "Downloading " + name;
            if (totalDownloads == 2)
                return "Downloading " + name + " and 1 other song";
            return "Downloading " + name + " and " + (totalDownloads - 1) + " other songs";

        } else if (totalUploads > 0 && totalDownloads == 0) {
            //only upload
            final Cursor receiverIdCursor = getContentResolver().query(
                    ReachDatabaseProvider.CONTENT_URI,
                    new String[]{ReachDatabaseHelper.COLUMN_RECEIVER_ID},
                    ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ? and " +
                            ReachDatabaseHelper.COLUMN_STATUS + " = ?",
                    new String[]{"1", ReachDatabase.RELAY + ""}, null);

            if (receiverIdCursor == null)
                return totalUploads + " songs are being uploaded";
            final int count = receiverIdCursor.getCount();
            if (count == 0)
                return totalUploads + " songs are being uploaded";
            final HashSet<Long> receiverIds = new HashSet<>(count);
            while (receiverIdCursor.moveToNext())
                receiverIds.add(receiverIdCursor.getLong(0));
            receiverIdCursor.close();

            final Cursor getUserNameCursor = getContentResolver().query(
                    ReachFriendsProvider.CONTENT_URI,
                    new String[]{ReachFriendsHelper.COLUMN_USER_NAME},
                    ReachFriendsHelper.COLUMN_ID + " = ?",
                    new String[]{receiverIds.iterator().next() + ""}, null);
            if (getUserNameCursor == null)
                return totalUploads + " songs are being uploaded";
            if (!getUserNameCursor.moveToFirst()) {
                getUserNameCursor.close();
                return totalUploads + " songs are being uploaded";
            }
            final String name = getUserNameCursor.getString(0);
            final int people = receiverIds.size();
            receiverIds.clear();
            getUserNameCursor.close();

            if (people == 1)
                return name + " is downloading " + totalUploads + " songs from you";
            if (receiverIds.size() == 2)
                return name + " and 1 other person are downloading " + totalUploads + " songs from you";
            else
                return name + " and " + (people - 1) + " other people are downloading " + totalUploads + " songs from you";
        } else if (totalDownloads > 0 && totalUploads > 0) {
            //both
            return "Downloading " + totalDownloads + " songs and uploading " + totalUploads + " songs";
        }
        return null;
    }

    private void notificationNetwork() {

        final int totalDownloads = getTotalDownloads();
        final int totalUploads = getTotalUploads();
        if (totalDownloads == 0 && totalUploads == 0)
            return;

        Log.i("Downloader", "NOTIFICATION NETWORK !");
        final RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_updown);
        remoteViews.setTextViewText(R.id.NupCount, totalUploads + "");
        remoteViews.setTextViewText(R.id.NdownCount, totalDownloads + "");
        remoteViews.setTextViewText(R.id.NsongNamePlaying, generateNotificationText(totalDownloads, totalUploads));

        final Intent foreGround = new Intent(this, ReachActivity.class);
        if (totalUploads == 0)
            foreGround.putExtra("openPlayer", true);
        foreGround.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        final NotificationCompat.Builder note = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_icon_notif)
                .setContentTitle("Playing Music")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContent(remoteViews)
                .setOnlyAlertOnce(true)
                .setContentIntent(PendingIntent.getActivity(this, 0, foreGround, PendingIntent.FLAG_CANCEL_CURRENT));
        startForeground(StaticData.MUSIC_PLAYER, note.build());
    }

    private void notificationMusic() {

        final Optional<MusicData> currentSong = musicHandler.getCurrentSong();
        final boolean paused = musicHandler.isPaused();
        if (!currentSong.isPresent())
            return;

        final RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_player);
        remoteViews.setOnClickPendingIntent(R.id.Npause_play, PendingIntent.getService(this, 0, new Intent(MusicHandler.ACTION_PLAY_PAUSE, null, this, ProcessManager.class), 0));
        remoteViews.setOnClickPendingIntent(R.id.NcloseButton, PendingIntent.getService(this, 0, new Intent(MusicHandler.ACTION_KILL, null, this, ProcessManager.class), 0));
        remoteViews.setOnClickPendingIntent(R.id.NprevTrack, PendingIntent.getService(this, 0, new Intent(MusicHandler.ACTION_PREVIOUS, null, this, ProcessManager.class), 0));
        remoteViews.setOnClickPendingIntent(R.id.NnextTrack, PendingIntent.getService(this, 0, new Intent(MusicHandler.ACTION_NEXT, null, this, ProcessManager.class), 0));
        remoteViews.setTextViewText(R.id.NsongNamePlaying, currentSong.get().getDisplayName());
        remoteViews.setTextViewText(R.id.NartistName, currentSong.get().getArtistName());
        if (paused)
            remoteViews.setImageViewResource(R.id.Npause_play, R.drawable.play_white_selector);
        else
            remoteViews.setImageViewResource(R.id.Npause_play, R.drawable.pause_white_selector);

        final Intent foreGround = new Intent(this, ReachActivity.class);
        foreGround.putExtra("openPlayer", true);
        foreGround.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        final NotificationCompat.Builder note = new NotificationCompat.Builder(this);
        Notification notification = note
                .setSmallIcon(R.drawable.ic_icon_notif)
                .setContentTitle("Playing Music")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContent(remoteViews)
                .setOnlyAlertOnce(true)
                .setContentIntent(PendingIntent.getActivity(this, 0, foreGround, PendingIntent.FLAG_CANCEL_CURRENT))
                .build();
        startForeground(StaticData.MUSIC_PLAYER, notification);
    }

    private void notificationBoth() {

        final Optional<MusicData> currentSong = musicHandler.getCurrentSong();
        final int totalDownloads = getTotalDownloads();
        final int totalUploads = getTotalUploads();

        if (!currentSong.isPresent() && (totalDownloads > 0 || totalUploads > 0)) {
            notificationNetwork();
            return;
        } else if (currentSong.isPresent() && totalDownloads == 0 && totalUploads == 0) {
            notificationMusic();
            return;
        } else if (!currentSong.isPresent() && totalDownloads == 0 && totalUploads == 0)
            return;

        final boolean paused = musicHandler.isPaused();
        final RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_both);
        remoteViews.setOnClickPendingIntent(R.id.Npause_play, PendingIntent.getService(this, 0, new Intent(MusicHandler.ACTION_PLAY_PAUSE, null, this, ProcessManager.class), 0));
        remoteViews.setOnClickPendingIntent(R.id.NcloseButton, PendingIntent.getService(this, 0, new Intent(MusicHandler.ACTION_KILL, null, this, ProcessManager.class), 0));
        remoteViews.setOnClickPendingIntent(R.id.NprevTrack, PendingIntent.getService(this, 0, new Intent(MusicHandler.ACTION_PREVIOUS, null, this, ProcessManager.class), 0));
        remoteViews.setOnClickPendingIntent(R.id.NnextTrack, PendingIntent.getService(this, 0, new Intent(MusicHandler.ACTION_NEXT, null, this, ProcessManager.class), 0));
        remoteViews.setTextViewText(R.id.NsongNamePlaying, currentSong.get().getDisplayName());
        remoteViews.setTextViewText(R.id.NartistName, currentSong.get().getArtistName());
        remoteViews.setTextViewText(R.id.NupCount, totalUploads + "");
        remoteViews.setTextViewText(R.id.NdownCount, totalDownloads + "");
        if (paused)
            remoteViews.setImageViewResource(R.id.Npause_play, R.drawable.play_white_selector);
        else
            remoteViews.setImageViewResource(R.id.Npause_play, R.drawable.pause_white_selector);

        final Intent foreGround = new Intent(this, ReachActivity.class);
        if (totalUploads == 0)
            foreGround.putExtra("openPlayer", true);
        foreGround.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        final NotificationCompat.Builder note = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_icon_notif)
                .setContentTitle("Playing Music")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContent(remoteViews)
                .setOnlyAlertOnce(true)
                .setContentIntent(PendingIntent.getActivity(this, 0, foreGround, PendingIntent.FLAG_CANCEL_CURRENT));
        startForeground(StaticData.MUSIC_PLAYER, note.build());
    }

    @Override
    public void updateNetworkDetails() {

        switch (notificationState) {

            case Network:
                notificationNetwork();
                break;
            case Music:
                notificationState = NotificationState.Both;
                notificationBoth();
                break;
            case Both:
                notificationBoth();
                break;
            case Dead:
                notificationState = NotificationState.Network;
                notificationNetwork();
                break;
        }
    }

    @Override
    public void updateSongDetails(MusicData musicData) {
        //insert Music player into notification
        Log.i("Downloader", "UPDATING SONG DETAILS");
        sendMessage(this, Optional.of(musicData), REPLY_LATEST_MUSIC);
        final String toSend = new Gson().toJson(musicData, MusicData.class);
        SharedPrefUtils.storeLastPlayed(getSharedPreferences("reach_process", MODE_PRIVATE), toSend);
        /**
         * GA stuff
         */
        ((ReachApplication) getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                .setCategory("Play song")
                .setAction("User Name - " + SharedPrefUtils.getUserName(getSharedPreferences("Reach", Context.MODE_PRIVATE)))
                .setLabel("SongBrainz - " + musicData.getDisplayName())
                .setValue(1)
                .build());

        MixpanelAPI mixpanel = MixpanelAPI.getInstance(this, "7877f44b1ce4a4b2db7790048eb6587a");
        JSONObject props = new JSONObject();
        try {
            props.put("User Name", SharedPrefUtils.getUserName(getSharedPreferences("Reach", Context.MODE_PRIVATE)));
            props.put("Song", musicData.getDisplayName());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mixpanel.track("Play song", props);

        switch (notificationState) {

            case Network:
                notificationState = NotificationState.Both;
                notificationBoth();
                break;
            case Music:
                notificationMusic();
                break;
            case Both:
                notificationBoth();
                break;
            case Dead:
                notificationState = NotificationState.Music;
                notificationMusic();
                break;
        }
    }

    @Override
    public void removeNetwork() {

        networkHandler.close();
        switch (notificationState) {

            case Network:
                notificationState = NotificationState.Dead;
                stopForeground(true);
                break;
            case Music:
                notificationMusic();
                break;
            case Both:
                notificationState = NotificationState.Music;
                notificationMusic();
                break;
            case Dead:
                notificationState = NotificationState.Dead;
                stopForeground(true);
                break;
        }
    }

    @Override
    public void downloadFail(String songName, String reason) {

        ((ReachApplication) getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                .setCategory("Download Fail")
                .setAction("User Name - " + SharedPrefUtils.getUserName(getSharedPreferences("Reach", Context.MODE_PRIVATE)))
                .setLabel("SongBrainz - " + songName)
                .setLabel(reason)
                .setValue(1)
                .build());
    }

    @Override
    public void updateDuration(String formattedDuration) {
        sendMessage(this, Optional.of(formattedDuration), REPLY_DURATION);
    }

    @Override
    public void paused() {
        if (notificationState == NotificationState.Music)
            notificationMusic();
        else if (notificationState == NotificationState.Both)
            notificationBoth();
        sendMessage(this, Optional.absent(), REPLY_PAUSED);
    }

    @Override
    public void unPaused() {
        if (notificationState == NotificationState.Music)
            notificationMusic();
        else if (notificationState == NotificationState.Both)
            notificationBoth();
        sendMessage(this, Optional.absent(), REPLY_UN_PAUSED);
    }

    @Override
    public void musicPlayerDead() {

        musicHandler.close();
        sendMessage(this, Optional.absent(), REPLY_MUSIC_DEAD);
        Log.i("Downloader", "Sent Music player dead");
        switch (notificationState) {

            case Network:
                notificationNetwork();
                break;
            case Music:
                notificationState = NotificationState.Dead;
                stopForeground(true);
                break;
            case Both:
                notificationState = NotificationState.Network;
                notificationNetwork();
                break;
            case Dead:
                notificationState = NotificationState.Dead;
                stopForeground(true);
                break;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        serverId = SharedPrefUtils.getServerId(getSharedPreferences());
        (wakeLock = ((PowerManager) getSystemService(POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag")).acquire();
        // Make sure the media player will acquire a wake-lock while playing. If we don't do
        // that, the CPU might go to sleep while the song is playing, causing playback to stop.
        // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
        // permission in AndroidManifest.xml.
        reference = new WeakReference<>(this);
    }

    @Override
    public void onDestroy() {
        close();
        super.onDestroy();
    }

    private Future<?> submitParent(final ReachTask parent) {

//        Log.i("Downloader", "Fixed pool has " + fixedPool.getActiveCount() + " active threads !");
//        Log.i("Downloader", "Fixed pool has " + fixedPool.getQueue().size() + " queued threads !");

        /**
         * We acquire a permit, signalling that a parent
         * thread is starting. Also prevents duplicate parents
         */
        Log.i("Downloader", "Acquiring permit " + killCheck.availablePermits());
        try {
            killCheck.acquire();
        } catch (InterruptedException e) {
            //This should not happen
            e.printStackTrace();
            close();
            return null;
        }

        return fixedPool.submit(makeParentTask(parent));
    }

    private Runnable makeParentTask(final ReachTask task) {

        final ListenableFutureTask toSubmit = ListenableFutureTask.create(task, null);
        toSubmit.addListener(() -> {

            try {
                toSubmit.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.i("Downloader", "INTERRUPTED EXCEPTION IN REACH TASK");
                close(); //shut down process
            } catch (Throwable e) {
                e.printStackTrace();
                Log.i("Downloader", "EXCEPTION IN REACH TASK " + e.getLocalizedMessage());
            } finally {
                killCheck.release();
                Log.i("Downloader", "DEATH CHECK " + killCheck.availablePermits());
                if (killCheck.availablePermits() == 2)
                    //No parent is active !
                    close();
            }
        }, sameThreadExecutor);
        return toSubmit;
    }

    @Override
    public final IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public final int onStartCommand(Intent intent, int flags, int startId) {
        /**
         * 1) Music requests are immediately serviced
         * 2) lan and network requests are queued
         */
        switch (intent.getAction()) {

            case NetworkHandler.ACTION_NETWORK_MESSAGE: {
                Log.i("Downloader", "ACTION_NETWORK_MESSAGE");
                if (networkFuture == null || networkFuture.isCancelled() || networkFuture.isDone()) {
                    Log.i("Downloader", "Starting network handler");
                    networkFuture = submitParent(networkHandler);
                }
                Log.i("Downloader", "submitting network request " +
                        networkHandler.submitMessage(intent.getStringExtra("message")));
                break;
            }
            case NetworkHandler.ACTION_NETWORK_LOST: {
                Log.i("Downloader", "ACTION_NETWORK_LOST");
                networkHandler.close(); //kill the network handler
                break;
            }
            case MusicHandler.ACTION_NEW_SONG: {
                Log.i("Downloader", "ACTION_NEW_SONG");
                musicHandler.userUnPause();
                MusicData data;
                try {
                    data = intent.getParcelableExtra("message");
                } catch (IllegalStateException | JsonSyntaxException e) {
                    e.printStackTrace();
                    data = null;
                }
                pushNextSong(Optional.fromNullable(data));
                break;
            }
            case MusicHandler.ACTION_NEXT: {
                Log.i("Downloader", "ACTION_NEXT");
                musicHandler.userUnPause();
                final Optional<MusicData> currentSong = musicHandler.getCurrentSong();
                final boolean shuffle = SharedPrefUtils.getShuffle(getSharedPreferences());
                if (shuffle && currentSong.isPresent())
                    pushNextSong(shuffleNext(currentSong.get().getId(), currentSong.get().getType()));
                else pushNextSong(nextSong(musicHandler.getCurrentSong(), false));
                break;
            }
            case MusicHandler.ACTION_PREVIOUS: {
                Log.i("Downloader", "ACTION_PREVIOUS");
                musicHandler.userUnPause();
                final boolean shuffle = SharedPrefUtils.getShuffle(getSharedPreferences());
                if (shuffle)
                    pushNextSong(shufflePrevious());
                else pushNextSong(previousSong(musicHandler.getCurrentSong()));
                break;
            }
            case MusicHandler.ACTION_PLAY_PAUSE: {
                Log.i("Downloader", "ACTION_PLAY_PAUSE");
                if (!musicHandler.processPlayPause())
                    break;
                final Optional<MusicData> history = Optional.fromNullable((MusicData) intent.getParcelableExtra("message"));
                if (history.isPresent())
                    pushNextSong(history);
                else {
                    final Optional<MusicData> currentSong = musicHandler.getCurrentSong();
                    final boolean shuffle = SharedPrefUtils.getShuffle(getSharedPreferences());
                    final boolean repeat = SharedPrefUtils.getRepeat(getSharedPreferences());
                    if (repeat && currentSong.isPresent())
                        pushNextSong(currentSong);
                    else if (shuffle && currentSong.isPresent())
                        pushNextSong(shuffleNext(currentSong.get().getId(), currentSong.get().getType()));
                    else pushNextSong(nextSong(musicHandler.getCurrentSong(), false));
                }
                break;
            }
            case MusicHandler.ACTION_SEEK: {
                Log.i("Downloader", "ACTION_SEEK");
                //if returned true means seek is ok
                if (musicHandler.processSeek(Short.parseShort(intent.getStringExtra("message"))))
                    break;
                final Optional<MusicData> currentSong = musicHandler.getCurrentSong();
                final boolean shuffle = SharedPrefUtils.getShuffle(getSharedPreferences());
                final boolean repeat = SharedPrefUtils.getRepeat(getSharedPreferences());
                if (repeat && currentSong.isPresent())
                    pushNextSong(currentSong);
                else if (shuffle && currentSong.isPresent())
                    pushNextSong(shuffleNext(currentSong.get().getId(), currentSong.get().getType()));
                else pushNextSong(nextSong(musicHandler.getCurrentSong(), false));
                break;
            }
            case MusicHandler.ACTION_KILL: {

                musicStack.clear();
                musicHandler.close();
                musicHandler.sanitize();
                musicPlayerDead();
                break;
            }
            default:
                Log.i("Downloader", "Illegal intent");
        }
        //messenger .send request serviced
        return START_NOT_STICKY;
    }

    //////////////////////////////////
    private Cursor getReachDatabaseCursor() {
        return getContentResolver().query(
                ReachDatabaseProvider.CONTENT_URI,
                StaticData.DOWNLOADED_PARTIAL,
                ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ? and " +
                        ReachDatabaseHelper.COLUMN_PROCESSED + " > ?",
                new String[]{"0", "0"},
                ReachDatabaseHelper.COLUMN_DATE_ADDED + " DESC");
    }

    private Cursor getMyLibraryCursor() {
        return getContentResolver().query(
                ReachSongProvider.CONTENT_URI,
                StaticData.DISK_PARTIAL,
                ReachSongHelper.COLUMN_USER_ID + " = ?",
                new String[]{serverId + ""},
                ReachSongHelper.COLUMN_DISPLAY_NAME + " ASC");
    }

    private Optional<MusicData> playFromCursor(Optional<Cursor> optional, byte type) {

        if (!optional.isPresent())
            return Optional.absent();

        final Cursor cursor = optional.get();

        if (cursor.getCount() == 0 || cursor.getColumnCount() == 0)
            return Optional.absent();

        final MusicData musicData;

        if (type == 0) {

            //if not multiple addition, play the song
            final boolean liked;
            final String temp = cursor.getString(6); //liked
            liked = !TextUtils.isEmpty(temp) && temp.equals("1");

            musicData = new MusicData(
                    cursor.getLong(0), //id
                    cursor.getLong(1), //length
                    cursor.getLong(2), //senderId
                    cursor.getLong(3), //processed
                    cursor.getString(4), //path
                    cursor.getString(5), //displayName
                    cursor.getString(6), //artistName
                    liked,
                    cursor.getLong(9),
                    (byte) 0);

        } else {

            musicData = new MusicData(
                    cursor.getLong(1), //id
                    cursor.getLong(2), //length
                    serverId, //senderId
                    cursor.getLong(2), //processed = length
                    cursor.getString(3), //path
                    cursor.getString(4), //displayName
                    cursor.getString(0), //artistName
                    false, //liked
                    cursor.getLong(6), //duration
                    (byte) 1); //type
        }
        cursor.close();
        return Optional.of(musicData);
    }

    private Optional<Cursor> chooseRandomFromCursor(Cursor cursor) {

        if (cursor == null)
            return Optional.absent();
        final int count = cursor.getCount();
        if (count <= 0) {
            cursor.close();
            return Optional.absent();
        }
        if (cursor.move(random.nextInt(count)) || cursor.moveToFirst())
            return Optional.of(cursor);
        cursor.close();
        return Optional.absent();
    }

    @Override
    public synchronized void pushNextSong(Optional<MusicData> musicData) {

        if (!musicData.isPresent()) {
            Log.i("Downloader", "Music not found");
            return;
        }

        final MusicData data = musicData.get();
        if (data.getProcessed() < 1 || TextUtils.isEmpty(data.getPath()) || data.getPath().equals("hello_world"))
            return;
        if (musicFuture == null || musicFuture.isCancelled() || musicFuture.isDone())
            musicFuture = submitParent(musicHandler);

        musicHandler.passNewSong(musicData.get());
        pushSongToStack(musicData.get());
    }

    private boolean positionCursor(Cursor cursor, long id, int index) {

//        int position = 0;
        while (cursor.moveToNext()) {
            if (cursor.getLong(index) == id) {
//                Log.i("Downloader", "CORRECT position found ! " + position);
//                position++;
                return true;
            }
        }
        return false;
    }

    private Optional<MusicData> previousSong(Optional<MusicData> currentSong) {

        if (!currentSong.isPresent())
            return nextSong(currentSong, false);

        if (currentSong.get().getType() == 0) {

            final Cursor reachDatabaseCursor = getReachDatabaseCursor();
            if (reachDatabaseCursor == null ||
                    !positionCursor(reachDatabaseCursor, currentSong.get().getId(), 0) ||
                    !reachDatabaseCursor.moveToPrevious()) {

                final Cursor myLibraryCursor = getMyLibraryCursor();
                if (myLibraryCursor != null)
                    myLibraryCursor.moveToLast();
                closeCursor(Optional.fromNullable(reachDatabaseCursor));
                return playFromCursor(Optional.fromNullable(myLibraryCursor), (byte) 1);
            }
            return playFromCursor(Optional.of(reachDatabaseCursor), (byte) 0);
        } else {

            final Cursor myLibraryCursor = getMyLibraryCursor();
            if (myLibraryCursor == null ||
                    !positionCursor(myLibraryCursor, currentSong.get().getId(), 1) ||
                    !myLibraryCursor.moveToPrevious()) {

                final Cursor reachDatabaseCursor = getReachDatabaseCursor();
                if (reachDatabaseCursor != null)
                    reachDatabaseCursor.moveToLast();
                closeCursor(Optional.fromNullable(myLibraryCursor));
                return playFromCursor(Optional.fromNullable(reachDatabaseCursor), (byte) 0);
            }
            return playFromCursor(Optional.of(myLibraryCursor), (byte) 1);
        }
    }

    @Override
    public Optional<MusicData> nextSong(Optional<MusicData> currentSong, boolean automatic) {

        if (!currentSong.isPresent())
            return shuffleNext(0, (byte) 0); //if no current song shuffleNext
        if (SharedPrefUtils.getRepeat(getSharedPreferences()) && currentSong.isPresent() && automatic)
            return currentSong;

        if (currentSong.get().getType() == 0) {

            final Cursor reachDatabaseCursor = getReachDatabaseCursor();
            if (reachDatabaseCursor == null ||
                    !positionCursor(reachDatabaseCursor, currentSong.get().getId(), 0) ||
                    !reachDatabaseCursor.moveToNext()) {
                final Cursor myLibraryCursor = getMyLibraryCursor();
                if (myLibraryCursor != null)
                    myLibraryCursor.moveToFirst();
                closeCursor(Optional.fromNullable(reachDatabaseCursor));
                return playFromCursor(Optional.fromNullable(myLibraryCursor), (byte) 1);
            }
            return playFromCursor(Optional.of(reachDatabaseCursor), (byte) 0);
        } else {

            final Cursor myLibraryCursor = getMyLibraryCursor();
            if (myLibraryCursor == null ||
                    !positionCursor(myLibraryCursor, currentSong.get().getId(), 1) ||
                    !myLibraryCursor.moveToNext()) {
                final Cursor reachDatabaseCursor = getReachDatabaseCursor();
                if (reachDatabaseCursor != null)
                    reachDatabaseCursor.moveToFirst();
                closeCursor(Optional.fromNullable(myLibraryCursor));
                return playFromCursor(Optional.fromNullable(reachDatabaseCursor), (byte) 0);
            }
            return playFromCursor(Optional.of(myLibraryCursor), (byte) 1);
        }
    }

    @Override
    public void errorReport(String songName, String missType) {

//        pushNextSong(nextSong(Optional.absent(), false));
        sendMessage(this, Optional.absent(), REPLY_ERROR);
        ((ReachApplication) getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                .setCategory(missType)
                .setAction("User Name - " + SharedPrefUtils.getUserName(getSharedPreferences("Reach", Context.MODE_PRIVATE)))
                .setLabel("SongBrainz - " + songName)
                .setValue(1)
                .build());
    }

    @Override
    public void updateSecondaryProgress(short percent) {
//        Log.i("Downloader", "Progress " + percent);
        updateSecondaryProgress(this, percent, REPLY_SECONDARY_PROGRESS);
    }

    @Override
    public void updatePrimaryProgress(short percent, int progress) {
        updatePrimaryProgress(this, percent, progress, REPLY_PRIMARY_PROGRESS);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public WifiManager getWifiManager() {
        return (WifiManager) getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public SharedPreferences getSharedPreferences() {
        return getSharedPreferences("Reach", MODE_PRIVATE);
    }

    public static InputStream getSERStream(String name) throws IOException {

        final ProcessManager manager = reference.get();
        if (manager == null)
            return null;

        return manager.getAssets().open(name);
    }
}