//package reach.project.reachProcess.reachService;
//
//import android.app.Notification;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.content.pm.PackageManager;
//import android.content.res.AssetManager;
//import android.database.Cursor;
//import android.graphics.Bitmap;
//import android.net.Uri;
//import android.net.wifi.WifiManager;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.IBinder;
//import android.os.Message;
//import android.os.Messenger;
//import android.os.PowerManager;
//import android.os.RemoteException;
//import android.support.annotation.NonNull;
//import android.support.annotation.Nullable;
//import android.support.v4.app.NotificationCompat;
//import android.text.TextUtils;
//import android.util.Log;
//import android.widget.RemoteViews;
//
//import com.facebook.common.executors.UiThreadImmediateExecutorService;
//import com.facebook.common.references.CloseableReference;
//import com.facebook.datasource.DataSource;
//import com.facebook.drawee.backends.pipeline.Fresco;
//import com.facebook.imagepipeline.common.ResizeOptions;
//import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
//import com.facebook.imagepipeline.image.CloseableImage;
//import com.facebook.imagepipeline.request.ImageRequest;
//import com.facebook.imagepipeline.request.ImageRequestBuilder;
//import com.google.android.gms.analytics.HitBuilders;
//import com.google.common.base.Optional;
//import com.google.common.util.concurrent.ListenableFutureTask;
//import com.google.common.util.concurrent.ListeningExecutorService;
//import com.google.common.util.concurrent.MoreExecutors;
//
//import org.json.JSONException;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.lang.ref.WeakReference;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Stack;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//import java.util.concurrent.Semaphore;
//
//import reach.project.R;
//import reach.project.core.ReachActivity;
//import reach.project.core.ReachApplication;
//import reach.project.core.StaticData;
//import reach.project.coreViews.friends.ReachFriendsHelper;
//import reach.project.coreViews.friends.ReachFriendsProvider;
//import reach.project.music.ReachDatabase;
//import reach.project.music.Song;
//import reach.project.music.SongCursorHelper;
//import reach.project.music.SongHelper;
//import reach.project.music.SongProvider;
//import reach.project.player.PlayerActivity;
//import reach.project.reachProcess.auxiliaryClasses.ReachTask;
//import reach.project.usageTracking.PostParams;
//import reach.project.usageTracking.SongMetadata;
//import reach.project.usageTracking.UsageTracker;
//import reach.project.utils.AlbumArtUri;
//import reach.project.utils.MiscUtils;
//import reach.project.utils.SharedPrefUtils;
//import reach.project.utils.ThreadLocalRandom;
//
///**
// * Created by Dexter on 14-05-2015.
// */
//public class ProcessManager extends Service implements
//        MusicHandler.MusicHandlerInterface,
//        NetworkHandler.NetworkHandlerInterface {
//
//    private static final String TAG = ProcessManager.class.getSimpleName();
//
//    private enum NotificationState {
//        Network,
//        Music,
//        Both,
//        Dead
//    }
//
//    //////////////////////////////////
//    private NotificationState notificationState = NotificationState.Dead;
//    //////////////////////////////////
//    //no confirmation
//    public static final String REPLY_PRIMARY_PROGRESS = "reach.project.reachProcess.reachService.ProcessManager.REPLY_PRIMARY_PROGRESS";
//    public static final String REPLY_SECONDARY_PROGRESS = "reach.project.reachProcess.reachService.ProcessManager.REPLY_SECONDARY_PROGRESS";
//    //confirmation
//    public static final String REPLY_LATEST_MUSIC = "reach.project.reachProcess.reachService.ProcessManager.REPLY_LATEST_MUSIC";
//    public static final String REPLY_DURATION = "reach.project.reachProcess.reachService.ProcessManager.REPLY_DURATION";
//    public static final String REPLY_PAUSED = "reach.project.reachProcess.reachService.ProcessManager.REPLY_PAUSED";
//    public static final String REPLY_UN_PAUSED = "reach.project.reachProcess.reachService.ProcessManager.REPLY_UN_PAUSED";
//    public static final String REPLY_MUSIC_DEAD = "reach.project.reachProcess.reachService.ProcessManager.REPLY_MUSIC_DEAD";
//    public static final String REPLY_ERROR = "reach.project.reachProcess.reachService.ProcessManager.REPLY_ERROR";
//
//    public static final String ACTION_NEW_SONG = "reach.project.reachProcess.reachService.MusicHandler.NEW_SONG";
//    public static final String ACTION_NEXT = "reach.project.reachProcess.reachService.MusicHandler.NEXT";
//    public static final String ACTION_PREVIOUS = "reach.project.reachProcess.reachService.MusicHandler.PREVIOUS";
//    //the ones below are directly linked with the player
//    public static final String ACTION_PLAY_PAUSE = "reach.project.reachProcess.reachService.MusicHandler.PLAY_PAUSE";
//    public static final String ACTION_STATUS = "reach.project.reachProcess.reachService.MusicHandler.STATUS";
//    public static final String ACTION_SEEK = "reach.project.reachProcess.reachService.MusicHandler.SEEK";
//    public static final String ACTION_KILL = "reach.project.reachProcess.reachService.MusicHandler.KILL";
//
//    private static synchronized void sendMessage(@NonNull Bundle data) {
//
//        if (musicCallbacks == null)
//            return;
//
//        final Message message = Message.obtain();
//        message.setData(data);
//        try {
//            musicCallbacks.send(message);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void submitNetworkRequest(@NonNull Context context, @NonNull String message) {
//        helper(context, Optional.of(message), NetworkHandler.ACTION_NETWORK_MESSAGE);
//    }
//
//    public static void submitMusicRequest(@NonNull Context context, @NonNull Optional<?> message, @NonNull String action) {
//        helper(context, message, action);
//    }
//
//    public static void installMessenger(@NonNull Messenger messenger) {
//        ProcessManager.musicCallbacks = messenger;
//    }
//
//    public static void unInstallMessenger() {
//        ProcessManager.musicCallbacks = null;
//    }
//
//    private static void helper(@NonNull Context context, @NonNull Optional<?> message, @NonNull String action) {
//
//        final Intent intent = new Intent(context, ProcessManager.class);
//        intent.setAction(action);
//        if (message.isPresent()) {
//            if (message.get() instanceof String)
//                intent.putExtra("message", (String) message.get());
//            else if (message.get() instanceof Song)
//                intent.putExtra("message", (Song) message.get());
//        }
//        context.startService(intent);
//    }
//
//    //////////////////////////////////
//    /**
//     * Pi = ith parent_thread
//     * Cij = jth child_thread of ith parent_thread
//     * *******************************************
//     * P1 : Music_Player //explicitly killed by User
//     * C11 : HttpHandler
//     * C12 : HttpReader
//     * C13 : HttpWriter
//     * P2 : Network_Controller //auto-start, self kill, User can also kill it
//     * C21 : LanHandler
//     * P3 : Communicator
//     */
//
//    /**
//     * Sanitize pattern :
//     * close() : close will signal the 'Runnable', either parent or child to kill itself,
//     * basically stop the infinite while loop. In the next loop cycle, the break will happen.
//     * Once loop breaks sanitize() happens. close() is ALWAYS called by the parent. It basically
//     * toggles an atomic boolean and thread-safety is assured.
//     * <p>
//     * sanitize() : sanitize resets the global data of the current Runnable and calls
//     * 'close()' of any child Runnable. By reset I mean, close all closeables, clear stuff etc.
//     * The complete global space needs to be purged. Cleaning thread-local space might also be necessary.
//     * sanitize() is strictly private. Always called ar start and end of run()
//     */
//
//    @Nullable
//    public static WeakReference<ProcessManager> reference = null;
//    @Nullable
//    private static Messenger musicCallbacks = null;
//
//    private final ExecutorService fixedPool = Executors.newFixedThreadPool(8); //buffer of 1 thread
//    private final ListeningExecutorService sameThreadExecutor = MoreExecutors.sameThreadExecutor();
//    private final Semaphore killCheck = new Semaphore(2, true); //fixed 2 parents
//    private final NetworkHandler networkHandler = new NetworkHandler(this);
//    private final MusicHandler musicHandler = new MusicHandler(this);
//    private final Stack<String> musicStack = new Stack<>(); //id, type, push only when in shuffle mode
//
//    private Future<?> musicFuture, networkFuture;
//    private PowerManager.WakeLock wakeLock = null;
//    private static long serverId;
//
//    private void close() {
//
//        networkHandler.close();
//        musicHandler.close();
//        musicStack.clear();
//        if (musicFuture != null)
//            musicFuture.cancel(true);
//        if (networkFuture != null)
//            networkFuture.cancel(true);
//        if (wakeLock != null && wakeLock.isHeld())
//            wakeLock.release();
//        if (reference != null) {
//            reference.clear();
//            reference = null;
//        }
//
//        musicFuture = networkFuture = null;
//        wakeLock = null;
//
//        fixedPool.shutdownNow();
//        stopSelf();
//        Log.i("Downloader", "KILLING SERVICE !!!");
//    }
//
//    private synchronized int getTotalDownloads() {
//        return getContentResolver().query(
//                SongProvider.CONTENT_URI,
//                new String[]{SongHelper.COLUMN_ID},
//                SongHelper.COLUMN_OPERATION_KIND + " = ? and " +
//                        SongHelper.COLUMN_STATUS + " = ?",
//                new String[]{ReachDatabase.OperationKind.DOWNLOAD_OP.getString(),
//                        ReachDatabase.Status.RELAY.getString()}, null).getCount();
//    }
//
//    //RETURNS TOTAL NUMBER OF PEOPLE WHO ARE UPLOADING
//    private synchronized int getTotalUploads() {
//        return getContentResolver().query(
//                SongProvider.CONTENT_URI,
//                new String[]{SongHelper.COLUMN_ID},
//                SongHelper.COLUMN_OPERATION_KIND + " = ? and " +
//                        SongHelper.COLUMN_STATUS + " = ?",
//                new String[]{ReachDatabase.OperationKind.UPLOAD_OP.getString(),
//                        ReachDatabase.Status.RELAY.getString()}, null).getCount();
//    }
//
//    private String generateNotificationText(int totalDownloads, int totalUploads) {
//
//        if (totalDownloads > 0 && totalUploads == 0) {
//            //only download
//            final Cursor songNameCursor = getContentResolver().query(
//                    SongProvider.CONTENT_URI,
//                    new String[]{SongHelper.COLUMN_DISPLAY_NAME},
//                    SongHelper.COLUMN_OPERATION_KIND + " = ? and " +
//                            SongHelper.COLUMN_STATUS + " = ?",
//                    new String[]{ReachDatabase.OperationKind.DOWNLOAD_OP.getString(), ReachDatabase.Status.RELAY.getString()},
//                    SongHelper.COLUMN_DATE_ADDED + " DESC");
//
//            if (songNameCursor == null)
//                return totalDownloads + " songs are being downloaded";
//            if (!songNameCursor.moveToFirst()) {
//                songNameCursor.close();
//                return totalDownloads + " songs are being downloaded";
//            }
//            final String name = songNameCursor.getString(0);
//            songNameCursor.close();
//            if (totalDownloads == 1)
//                return "Downloading " + name;
//            if (totalDownloads == 2)
//                return "Downloading " + name + " and 1 other song";
//            return "Downloading " + name + " and " + (totalDownloads - 1) + " other songs";
//
//        } else if (totalUploads > 0 && totalDownloads == 0) {
//            //only upload
//            final Cursor receiverIdCursor = getContentResolver().query(
//                    SongProvider.CONTENT_URI,
//                    new String[]{SongHelper.COLUMN_RECEIVER_ID},
//                    SongHelper.COLUMN_OPERATION_KIND + " = ? and " +
//                            SongHelper.COLUMN_STATUS + " = ?",
//                    new String[]{ReachDatabase.OperationKind.UPLOAD_OP.getString(), ReachDatabase.Status.RELAY.getString()}, null);
//
//            if (receiverIdCursor == null)
//                return totalUploads + " songs are being uploaded";
//            final int count = receiverIdCursor.getCount();
//            if (count == 0)
//                return totalUploads + " songs are being uploaded";
//            final HashSet<Long> receiverIds = new HashSet<>(count);
//            while (receiverIdCursor.moveToNext())
//                receiverIds.add(receiverIdCursor.getLong(0));
//            receiverIdCursor.close();
//
//            final Cursor getUserNameCursor = getContentResolver().query(
//                    ReachFriendsProvider.CONTENT_URI,
//                    new String[]{ReachFriendsHelper.COLUMN_USER_NAME},
//                    ReachFriendsHelper.COLUMN_ID + " = ?",
//                    new String[]{receiverIds.iterator().next() + ""}, null);
//            if (getUserNameCursor == null)
//                return totalUploads + " songs are being uploaded";
//            if (!getUserNameCursor.moveToFirst()) {
//                getUserNameCursor.close();
//                return totalUploads + " songs are being uploaded";
//            }
//            final String name = getUserNameCursor.getString(0);
//            final int people = receiverIds.size();
//            receiverIds.clear();
//            getUserNameCursor.close();
//
//            if (people == 1)
//                return name + " is downloading " + totalUploads + " songs from you";
//            if (receiverIds.size() == 2)
//                return name + " and 1 other person are downloading " + totalUploads + " songs from you";
//            else
//                return name + " and " + (people - 1) + " other people are downloading " + totalUploads + " songs from you";
//        } else if (totalDownloads > 0 && totalUploads > 0) {
//            //both
//            return "Downloading " + totalDownloads + " songs and uploading " + totalUploads + " songs";
//        }
//        return null;
//    }
//
//    private void notificationNetwork() {
//
//        final int totalDownloads = getTotalDownloads();
//        final int totalUploads = getTotalUploads();
//        if (totalDownloads == 0 && totalUploads == 0)
//            return;
//
//        Log.i("Downloader", "NOTIFICATION NETWORK !");
//        final RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_updown);
//        remoteViews.setTextViewText(R.id.NupCount, totalUploads + "");
//        remoteViews.setTextViewText(R.id.NdownCount, totalDownloads + "");
//        remoteViews.setTextViewText(R.id.NsongNamePlaying, generateNotificationText(totalDownloads, totalUploads));
//
//        final Intent foreGround = new Intent(this, ReachActivity.class);
//        foreGround.setAction(ReachActivity.OPEN_MANAGER_SONGS_DOWNLOADING);
//        foreGround.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//        final NotificationCompat.Builder note = new NotificationCompat.Builder(this)
//                .setSmallIcon(R.drawable.icon_notification)
//                .setContentTitle("Playing Music")
//                .setPriority(NotificationCompat.PRIORITY_MAX)
//                .setContent(remoteViews)
//                .setOnlyAlertOnce(true)
//                .setContentIntent(PendingIntent.getActivity(this, 0, foreGround, PendingIntent.FLAG_CANCEL_CURRENT));
//        startForeground(StaticData.MUSIC_PLAYER, note.build());
//    }
//
//    private void notificationMusic() {
//
//        final Optional<Song> currentSong = musicHandler.getCurrentSong();
//        final boolean paused = musicHandler.isPaused();
//        if (!currentSong.isPresent())
//            return;
//
//        final RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_player);
//        final Intent foreGround = new Intent(this, PlayerActivity.class);
//        final NotificationCompat.Builder note = new NotificationCompat.Builder(this);
//        final Notification notification;
//
//        remoteViews.setOnClickPendingIntent(R.id.Npause_play, PendingIntent.getService(this, 0, new Intent(ACTION_PLAY_PAUSE, null, this, ProcessManager.class), 0));
//        remoteViews.setOnClickPendingIntent(R.id.NcloseButton, PendingIntent.getService(this, 0, new Intent(ACTION_KILL, null, this, ProcessManager.class), 0));
//        remoteViews.setOnClickPendingIntent(R.id.NprevTrack, PendingIntent.getService(this, 0, new Intent(ACTION_PREVIOUS, null, this, ProcessManager.class), 0));
//        remoteViews.setOnClickPendingIntent(R.id.NnextTrack, PendingIntent.getService(this, 0, new Intent(ACTION_NEXT, null, this, ProcessManager.class), 0));
//        remoteViews.setTextViewText(R.id.NsongNamePlaying, currentSong.get().getDisplayName());
//        remoteViews.setTextViewText(R.id.NartistName, currentSong.get().getArtist());
//        remoteViews.setImageViewResource(R.id.NalbumArt, R.drawable.default_music_icon); //default icon
//
//        if (paused)
//            remoteViews.setImageViewResource(R.id.Npause_play, R.drawable.play_white_selector);
//        else
//            remoteViews.setImageViewResource(R.id.Npause_play, R.drawable.pause_white_selector);
//
//        foreGround.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//        notification = note
//                .setSmallIcon(R.drawable.icon_notification)
//                .setContentTitle("Playing Music")
//                .setPriority(NotificationCompat.PRIORITY_MAX)
//                .setContent(remoteViews)
//                .setOnlyAlertOnce(true)
//                .setContentIntent(PendingIntent.getActivity(this, 0, foreGround, PendingIntent.FLAG_CANCEL_CURRENT))
//                .build();
//        startForeground(StaticData.MUSIC_PLAYER, notification);
//
//        final Optional<Uri> uri = AlbumArtUri.getUri(currentSong.get().getAlbum(),
//                currentSong.get().getArtist(), currentSong.get().getDisplayName(),
//                false);
//
//        //TODO possible memory leak ?
//        final BaseBitmapDataSubscriber dataSubscriber = new BaseBitmapDataSubscriber() {
//            @Override
//            protected void onNewResultImpl(@Nullable Bitmap bitmap) {
//
//                remoteViews.setImageViewBitmap(R.id.NalbumArt, bitmap);
//                startForeground(StaticData.MUSIC_PLAYER, notification); //reload to show notification
//            }
//
//            @Override
//            protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
//                //ignore
//            }
//        };
//
//        if (uri.isPresent()) {
//
//            final ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri.get())
//                    .setResizeOptions(new ResizeOptions(200, 200))
//                    .build();
//
//            final DataSource<CloseableReference<CloseableImage>> dataSource =
//                    Fresco.getImagePipeline().fetchDecodedImage(request, null);
//
//            dataSource.subscribe(dataSubscriber, UiThreadImmediateExecutorService.getInstance());
//        }
//    }
//
//    private void notificationBoth() {
//
//        final Optional<Song> currentSong = musicHandler.getCurrentSong();
//        final int totalDownloads = getTotalDownloads();
//        final int totalUploads = getTotalUploads();
//
//        if (!currentSong.isPresent() && (totalDownloads > 0 || totalUploads > 0)) {
//            notificationNetwork();
//            return;
//        } else if (currentSong.isPresent() && totalDownloads == 0 && totalUploads == 0) {
//            notificationMusic();
//            return;
//        } else if (!currentSong.isPresent() && totalDownloads == 0 && totalUploads == 0)
//            return;
//
//        final boolean paused = musicHandler.isPaused();
//        final RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_both);
//        remoteViews.setOnClickPendingIntent(R.id.Npause_play, PendingIntent.getService(this, 0, new Intent(ACTION_PLAY_PAUSE, null, this, ProcessManager.class), 0));
//        remoteViews.setOnClickPendingIntent(R.id.NcloseButton, PendingIntent.getService(this, 0, new Intent(ACTION_KILL, null, this, ProcessManager.class), 0));
//        remoteViews.setOnClickPendingIntent(R.id.NprevTrack, PendingIntent.getService(this, 0, new Intent(ACTION_PREVIOUS, null, this, ProcessManager.class), 0));
//        remoteViews.setOnClickPendingIntent(R.id.NnextTrack, PendingIntent.getService(this, 0, new Intent(ACTION_NEXT, null, this, ProcessManager.class), 0));
//        remoteViews.setTextViewText(R.id.NsongNamePlaying, currentSong.get().getDisplayName());
//        remoteViews.setTextViewText(R.id.NartistName, currentSong.get().getArtist());
//        remoteViews.setTextViewText(R.id.NupCount, totalUploads + "");
//        remoteViews.setTextViewText(R.id.NdownCount, totalDownloads + "");
//        if (paused)
//            remoteViews.setImageViewResource(R.id.Npause_play, R.drawable.play_white_selector);
//        else
//            remoteViews.setImageViewResource(R.id.Npause_play, R.drawable.pause_white_selector);
//
//        final Intent foreGround = new Intent(this, ReachActivity.class);
//        foreGround.setAction(ReachActivity.OPEN_MANAGER_SONGS_DOWNLOADING);
//        foreGround.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//        final NotificationCompat.Builder note = new NotificationCompat.Builder(this)
//                .setSmallIcon(R.drawable.icon_notification)
//                .setContentTitle("Playing Music")
//                .setPriority(NotificationCompat.PRIORITY_MAX)
//                .setContent(remoteViews)
//                .setOnlyAlertOnce(true)
//                .setContentIntent(PendingIntent.getActivity(this, 0, foreGround, PendingIntent.FLAG_CANCEL_CURRENT));
//        startForeground(StaticData.MUSIC_PLAYER, note.build());
//    }
//
//    @Override
//    public void updateNetworkDetails() {
//
//        switch (notificationState) {
//
//            case Network:
//                notificationNetwork();
//                break;
//            case Music:
//                notificationState = NotificationState.Both;
//                notificationBoth();
//                break;
//            case Both:
//                notificationBoth();
//                break;
//            case Dead:
//                notificationState = NotificationState.Network;
//                notificationNetwork();
//                break;
//        }
//    }
//
//    @Override
//    public void updateSongDetails(Song song) {
//
//        //insert Music player into notification
//        Log.i("Downloader", "UPDATING SONG DETAILS");
//        final Bundle bundle = new Bundle();
//        bundle.putString(PlayerActivity.ACTION, REPLY_LATEST_MUSIC);
//        bundle.putSerializable(PlayerActivity.MUSIC_PARCEL, song);
//        sendMessage(bundle);
//
//        Log.d(TAG, "Song details being stored into shared pref are " + song);
//        SharedPrefUtils.storeLastPlayed(this, song);
//        /**
//         * GA stuff
//         */
//        ((ReachApplication) getApplication()).getTracker().send(new HitBuilders.EventBuilder()
//                .setCategory("Play song")
//                .setAction("User Name - " + SharedPrefUtils.getUserName(getSharedPreferences("Reach", Context.MODE_PRIVATE)))
//                .setLabel("SongBrainz - " + song.getDisplayName())
//                .setValue(1)
//                .build());
//
//        final Map<PostParams, String> simpleParams = MiscUtils.getMap(6);
//        simpleParams.put(PostParams.USER_ID, serverId + "");
//        simpleParams.put(PostParams.DEVICE_ID, MiscUtils.getDeviceId(this));
//        simpleParams.put(PostParams.OS, MiscUtils.getOsName());
//        simpleParams.put(PostParams.OS_VERSION, Build.VERSION.SDK_INT + "");
//        try {
//            simpleParams.put(PostParams.APP_VERSION,
//                    getPackageManager().getPackageInfo(getPackageName(), 0).versionCode + "");
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }
//        simpleParams.put(PostParams.SCREEN_NAME, "unknown");
//
//        final Map<SongMetadata, String> complexParams = MiscUtils.getMap(9);
//        //complexParams.put(SongMetadata.SONG_ID, song.getColumnId() + "");
//        complexParams.put(SongMetadata.META_HASH, song.getFileHash());
//        complexParams.put(SongMetadata.ARTIST, song.getArtist());
//        complexParams.put(SongMetadata.TITLE, song.getDisplayName());
//        complexParams.put(SongMetadata.DURATION, song.getDuration() + "");
//        complexParams.put(SongMetadata.SIZE, song.getSize() + "");
//        complexParams.put(SongMetadata.UPLOADER_ID, song.getSenderId() + "");
//        complexParams.put(SongMetadata.ALBUM, song.getAlbum());
//
//        try {
//            UsageTracker.trackSong(simpleParams, complexParams, UsageTracker.PLAY_SONG);
//        } catch (JSONException ignored) {
//        }
//
//        switch (notificationState) {
//
//            case Network:
//                notificationState = NotificationState.Both;
//                notificationBoth();
//                break;
//            case Music:
//                notificationMusic();
//                break;
//            case Both:
//                notificationBoth();
//                break;
//            case Dead:
//                notificationState = NotificationState.Music;
//                notificationMusic();
//                break;
//        }
//    }
//
//    @Override
//    public void updateDuration(String formattedDuration) {
//
//        Log.d(TAG, "Updating duration, duration = " + formattedDuration);
//        final Bundle bundle = new Bundle();
//        bundle.putString(PlayerActivity.ACTION, REPLY_DURATION);
//        bundle.putString(PlayerActivity.DURATION, formattedDuration);
//        sendMessage(bundle);
//    }
//
//    @Override
//    public void removeNetwork() {
//
//        networkHandler.close();
//        switch (notificationState) {
//
//            case Network:
//                notificationState = NotificationState.Dead;
//                stopForeground(true);
//                break;
//            case Music:
//                notificationMusic();
//                break;
//            case Both:
//                notificationState = NotificationState.Music;
//                notificationMusic();
//                break;
//            case Dead:
//                notificationState = NotificationState.Dead;
//                stopForeground(true);
//                break;
//        }
//    }
//
//    @Override
//    public void downloadFail(String songName, String reason) {
//
//        ((ReachApplication) getApplication()).getTracker().send(new HitBuilders.EventBuilder()
//                .setCategory("Download Fail")
//                .setAction("User Name - " + SharedPrefUtils.getUserName(getSharedPreferences("Reach", Context.MODE_PRIVATE)))
//                .setLabel("SongBrainz - " + songName)
//                .setLabel(reason)
//                .setValue(1)
//                .build());
//    }
//
//    @Override
//    public void paused() {
//        if (notificationState == NotificationState.Music)
//            notificationMusic();
//        else if (notificationState == NotificationState.Both)
//            notificationBoth();
//
//        final Bundle bundle = new Bundle();
//        bundle.putString(PlayerActivity.ACTION, REPLY_PAUSED);
//        sendMessage(bundle);
//    }
//
//    @Override
//    public void unPaused() {
//        if (notificationState == NotificationState.Music)
//            notificationMusic();
//        else if (notificationState == NotificationState.Both)
//            notificationBoth();
//
//        final Bundle bundle = new Bundle();
//        bundle.putString(PlayerActivity.ACTION, REPLY_UN_PAUSED);
//        sendMessage(bundle);
//    }
//
//    @Override
//    public void musicPlayerDead() {
//
//        musicHandler.close();
//
//        final Bundle bundle = new Bundle();
//        bundle.putString(PlayerActivity.ACTION, REPLY_MUSIC_DEAD);
//        sendMessage(bundle);
//
//        Log.i(TAG, "Sent Music player dead");
//
//        switch (notificationState) {
//
//            case Network:
//                notificationNetwork();
//                break;
//            case Music:
//                notificationState = NotificationState.Dead;
//                stopForeground(true);
//                break;
//            case Both:
//                notificationState = NotificationState.Network;
//                notificationNetwork();
//                break;
//            case Dead:
//                notificationState = NotificationState.Dead;
//                stopForeground(true);
//                break;
//        }
//    }
//
//    @Override
//    public void onCreate() {
//
//        super.onCreate();
//        serverId = SharedPrefUtils.getServerId(getSharedPreferences());
//        (wakeLock = ((PowerManager) getSystemService(POWER_SERVICE))
//                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag")).acquire();
//        // Make sure the media player will acquire a wake-lock while playing. If we don't do
//        // that, the CPU might go to sleep while the song is playing, causing playback to stop.
//        // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
//        // permission in AndroidManifest.xml.
//        reference = new WeakReference<>(this);
//    }
//
//    @Override
//    public void onDestroy() {
//        close();
//        super.onDestroy();
//    }
//
//    private Future<?> submitParent(final ReachTask parent) {
//
////        Log.i("Downloader", "Fixed pool has " + fixedPool.getActiveCount() + " active threads !");
////        Log.i("Downloader", "Fixed pool has " + fixedPool.getQueue().size() + " queued threads !");
//
//        /**
//         * We acquire a permit, signalling that a parent
//         * thread is starting. Also prevents duplicate parents
//         */
//        Log.i("Downloader", "Acquiring permit " + killCheck.availablePermits());
//        try {
//            killCheck.acquire();
//        } catch (InterruptedException e) {
//            //This should not happen
//            e.printStackTrace();
//            close();
//            return null;
//        }
//
//        return fixedPool.submit(makeParentTask(parent));
//    }
//
//    private Runnable makeParentTask(final ReachTask task) {
//
//        final ListenableFutureTask toSubmit = ListenableFutureTask.create(task, null);
//        toSubmit.addListener(() -> {
//
//            try {
//                toSubmit.get();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//                Log.i("Downloader", "INTERRUPTED EXCEPTION IN REACH TASK");
//                close(); //shut down process
//            } catch (ExecutionException e) {
//
//                switch (task.getType()) {
//
//                    case MUSIC:
//                        musicHandler.close();
//                        musicStack.clear();
//                        if (musicFuture != null)
//                            musicFuture.cancel(true);
//                        musicFuture = null;
//                        break;
//                    case NETWORK:
//                        networkHandler.close();
//                        if (networkFuture != null)
//                            networkFuture.cancel(true);
//                        networkFuture = null;
//                        break;
//                }
//
//                e.printStackTrace();
//                Log.i("Downloader", "EXCEPTION IN REACH TASK " + e.getLocalizedMessage());
//            } finally {
//                killCheck.release();
//                Log.i("Downloader", "DEATH CHECK " + killCheck.availablePermits());
//                if (killCheck.availablePermits() == 2)
//                    //No parent is active !
//                    close();
//            }
//        }, sameThreadExecutor);
//        return toSubmit;
//    }
//
//    @Override
//    public final IBinder onBind(Intent intent) {
//        throw new UnsupportedOperationException("Not yet implemented");
//    }
//
//    @Override
//    public final int onStartCommand(Intent intent, int flags, int startId) {
//        /**
//         * 1) Music requests are immediately serviced
//         * 2) lan and network requests are queued
//         */
//        switch (intent.getAction()) {
//
//            case NetworkHandler.ACTION_NETWORK_MESSAGE: {
//                Log.i("Downloader", "ACTION_NETWORK_MESSAGE");
//                if (networkFuture == null || networkFuture.isCancelled() || networkFuture.isDone()) {
//                    Log.i("Downloader", "Starting network handler");
//                    networkFuture = submitParent(networkHandler);
//                }
//                Log.i("Downloader", "submitting network request " +
//                        networkHandler.submitMessage(intent.getStringExtra("message")));
//                break;
//            }
//            case NetworkHandler.ACTION_NETWORK_LOST: {
//                Log.i("Downloader", "ACTION_NETWORK_LOST");
//                networkHandler.close(); //kill the network handler
//                break;
//            }
//            case ACTION_NEW_SONG: {
//
//                Log.i("Downloader", "ACTION_NEW_SONG");
//                musicHandler.userUnPause();
//                final Song data = (Song) intent.getSerializableExtra("message");
//                pushNextSong(Optional.fromNullable(data));
//                break;
//            }
//            case ACTION_NEXT: {
//
//                Log.i("Downloader", "ACTION_NEXT");
//                musicHandler.userUnPause();
//                final Optional<Song> currentSong = musicHandler.getCurrentSong();
//
//                if (SharedPrefUtils.getShuffle(this) && currentSong.isPresent())
//                    pushNextSong(shuffleNext(currentSong.get().getFileHash()));
//                else
//                    pushNextSong(nextSong(currentSong, false));
//                break;
//            }
//            case ACTION_PREVIOUS: {
//
//                Log.i("Downloader", "ACTION_PREVIOUS");
//                musicHandler.userUnPause();
//                if (SharedPrefUtils.getShuffle(this))
//                    pushNextSong(shufflePrevious());
//                else
//                    pushNextSong(previousSong(musicHandler.getCurrentSong()));
//                break;
//            }
//
//            case ACTION_STATUS: {
//
//                final Bundle bundle = new Bundle();
//                if (musicHandler.isPaused())
//                    bundle.putString(PlayerActivity.ACTION, REPLY_PAUSED);
//                else
//                    bundle.putString(PlayerActivity.ACTION, REPLY_UN_PAUSED);
//                sendMessage(bundle);
//                break;
//            }
//
//            case ACTION_PLAY_PAUSE: {
//
//                Log.i("Downloader", "ACTION_PLAY_PAUSE");
//                if (!musicHandler.processPlayPause())
//                    break;
//
//                final Optional<Song> history = SharedPrefUtils.getLastPlayed(this);
//                if (history.isPresent())
//                    pushNextSong(history);
//                else {
//
//                    final Optional<Song> currentSong = musicHandler.getCurrentSong();
//                    final boolean shuffle = SharedPrefUtils.getShuffle(this);
//                    final boolean repeat = SharedPrefUtils.getRepeat(this);
//                    if (repeat && currentSong.isPresent())
//                        pushNextSong(currentSong);
//                    else if (shuffle && currentSong.isPresent())
//                        pushNextSong(shuffleNext(currentSong.get().getFileHash()));
//                    else
//                        pushNextSong(nextSong(currentSong, false));
//                }
//                break;
//            }
//            case ACTION_SEEK: {
//                Log.i("Downloader", "ACTION_SEEK");
//                //if returned true means seek is ok
//                if (musicHandler.processSeek(Short.parseShort(intent.getStringExtra("message"))))
//                    break;
//                final Optional<Song> currentSong = musicHandler.getCurrentSong();
//                final boolean shuffle = SharedPrefUtils.getShuffle(this);
//                final boolean repeat = SharedPrefUtils.getRepeat(this);
//                if (repeat && currentSong.isPresent())
//                    pushNextSong(currentSong);
//                else if (shuffle && currentSong.isPresent())
//                    pushNextSong(shuffleNext(currentSong.get().getFileHash()));
//                else pushNextSong(nextSong(musicHandler.getCurrentSong(), false));
//                break;
//            }
//            case ACTION_KILL: {
//
//                musicStack.clear();
//                musicHandler.close();
//                musicHandler.sanitize();
//                musicPlayerDead();
//                break;
//            }
//            default:
//                Log.i("Downloader", "Illegal intent");
//        }
//        //messenger .send request serviced
//        return START_NOT_STICKY;
//    }
//
//    //////////////////////////////////
//
//    private Optional<Song> shufflePrevious() {
//
//        if (musicStack.size() < 2)
//            return previousSong(musicHandler.getCurrentSong());
//
//        musicStack.pop();//ignore current song !
//        final String currentSongHash = musicStack.pop();
//
//        //TODO not done meta
//        final Cursor cursor = getContentResolver().query(
//                SongProvider.CONTENT_URI,
//                SongCursorHelper.SONG_HELPER.getProjection(),
//                SongHelper.COLUMN_META_HASH + " = ?",
//                new String[]{currentSongHash}, null);
//
//        if (cursor == null)
//            return previousSong(musicHandler.getCurrentSong());
//
//        if (!cursor.moveToFirst()) {
//
//            cursor.close();
//            return previousSong(musicHandler.getCurrentSong());
//        }
//
//        return playFromCursor(Optional.of(cursor));
//    }
//
//    private Optional<Song> shuffleNext(String fileHash) {
//
//        //TODO not done meta
//        final Cursor myLibraryCursor = getContentResolver().query(
//                SongProvider.CONTENT_URI,
//                SongCursorHelper.SONG_HELPER.getProjection(),
//                SongHelper.COLUMN_META_HASH + " != ? and " +
//                        SongHelper.COLUMN_PROCESSED + " > ?", //all except that id
//                new String[]{fileHash, "0"}, null);
//
//        return playFromCursor(chooseRandomFromCursor(myLibraryCursor));
//    }
//
//    private Cursor getMyLibraryCursor() {
//
//        return getContentResolver().query(
//                SongProvider.CONTENT_URI,
//                SongCursorHelper.SONG_HELPER.getProjection(),
//                "(" + SongHelper.COLUMN_OPERATION_KIND + " = ? and " + SongHelper.COLUMN_STATUS + " = ?) or " +
//                        SongHelper.COLUMN_OPERATION_KIND + " = ?",
//                new String[]{
//                        ReachDatabase.OperationKind.DOWNLOAD_OP.getString(),
//                        ReachDatabase.Status.FINISHED.getString(),
//                        ReachDatabase.OperationKind.OWN.getString()},
//                SongHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE");
//    }
//
//    private Optional<Song> playFromCursor(Optional<Cursor> optional) {
//
//        if (!optional.isPresent())
//            return Optional.absent();
//
//        final Cursor cursor = optional.get();
//        final Song song = SongCursorHelper.SONG_HELPER.parse(cursor);
//        cursor.close();
//
////        Log.d("ProcessManager", "playFromCursor, music = " + song.displayName);
//        return Optional.of(song);
//    }
//
//    private Optional<Cursor> chooseRandomFromCursor(Cursor cursor) {
//
//        if (cursor == null)
//            return Optional.absent();
//
//        final int count = cursor.getCount();
//        if (count <= 0) {
//            cursor.close();
//            return Optional.absent();
//        }
//
//        if (cursor.move(ThreadLocalRandom.current().nextInt(count)) || cursor.moveToFirst())
//            return Optional.of(cursor);
//        cursor.close();
//        return Optional.absent();
//    }
//
//    @Override
//    public synchronized void pushNextSong(Optional<Song> songOptional) {
//
//        if (!songOptional.isPresent()) {
//            Log.i("Downloader", "Music not found");
//            return;
//        }
//
//        final Song song = songOptional.get();
//        if (song.getProcessed() < 1 || TextUtils.isEmpty(song.path) || song.path.equals("hello_world"))
//            return;
//
//        if (musicFuture == null || musicFuture.isCancelled() || musicFuture.isDone())
//            musicFuture = submitParent(musicHandler);
//
//        musicHandler.passNewSong(song);
//
//        if (musicStack.isEmpty())
//            musicStack.push(song.fileHash);
//        else if (!musicStack.peek().equals(song.fileHash))
//            musicStack.push(song.fileHash);
//    }
//
//    private boolean positionCursor(Cursor cursor, String id) {
//
//        do {
//            final String hashCode = cursor.getString(2);
//            if (TextUtils.isEmpty(hashCode))
//                continue;
//            if (hashCode.equals(id)) {
////                Log.i("Downloader", "CORRECT position found ! " + position);
////                position++;
//                return true;
//            }
//        } while (cursor.moveToNext());
//        return false;
//    }
//
//    private Optional<Song> previousSong(Optional<Song> currentSong) {
//
//        if (!currentSong.isPresent())
//            return nextSong(currentSong, false);
//
//        final Cursor cursor = getMyLibraryCursor();
//
//        if (cursor == null)
//            return Optional.absent();
//        if (!cursor.moveToFirst()) {
//
//            cursor.close();
//            return Optional.absent();
//        }
//
//        if (!positionCursor(cursor, currentSong.get().fileHash)) {
//
//            cursor.close();
//            return Optional.absent();
//        }
//
//        if (cursor.moveToPrevious())
//            return playFromCursor(Optional.of(cursor));
//        else if (cursor.moveToLast())
//            return playFromCursor(Optional.of(cursor));
//        else
//            return Optional.absent();
//    }
//
//    @Override
//    public Optional<Song> nextSong(Optional<Song> currentSong, boolean automatic) {
//
//        if (!currentSong.isPresent())
//            return shuffleNext("0"); //if no current song shuffleNext
//
//        if (SharedPrefUtils.getRepeat(this) && currentSong.isPresent() && automatic)
//            return currentSong;
//
//        final Cursor cursor = getMyLibraryCursor();
//
//        if (cursor == null)
//            return Optional.absent();
//        if (!cursor.moveToFirst()) {
//
//            cursor.close();
//            return Optional.absent();
//        }
//
//        if (!positionCursor(cursor, currentSong.get().fileHash)) {
//
//            cursor.close();
//            return Optional.absent();
//        }
//
//        if (cursor.moveToNext())
//            return playFromCursor(Optional.of(cursor));
//        else if (cursor.moveToFirst())
//            return playFromCursor(Optional.of(cursor));
//        else
//            return Optional.absent();
//    }
//
//    @Override
//    public void errorReport(String songName, String missType) {
//
////        pushNextSong(nextSong(Optional.absent(), false));
//        Log.d(TAG, "Replying error to the player activity");
//        final Bundle bundle = new Bundle();
//        bundle.putString(PlayerActivity.ACTION, REPLY_ERROR);
//        sendMessage(bundle);
//
//        ((ReachApplication) getApplication()).getTracker().send(new HitBuilders.EventBuilder()
//                .setCategory(missType)
//                .setAction("User Name - " + SharedPrefUtils.getUserName(getSharedPreferences("Reach", Context.MODE_PRIVATE)))
//                .setLabel("SongBrainz - " + songName)
//                .setValue(1)
//                .build());
//    }
//
//
//    @Override
//    public Context getContext() {
//        return this;
//    }
//
//    @Override
//    public void updateSecondaryProgress(short percent) {
//
//        Log.i(TAG, "Updating secondary progress " + percent);
//        final Bundle bundle = new Bundle();
//        bundle.putString(PlayerActivity.ACTION, REPLY_SECONDARY_PROGRESS);
//        bundle.putShort(PlayerActivity.SECONDARY_PROGRESS, percent);
//        sendMessage(bundle);
//    }
//
//    @Override
//    public void updatePrimaryProgress(short percent, int position) {
//
//        final Bundle bundle = new Bundle();
//        bundle.putString(PlayerActivity.ACTION, REPLY_PRIMARY_PROGRESS);
//        bundle.putShort(PlayerActivity.PRIMARY_PROGRESS, percent);
//        bundle.putInt(PlayerActivity.PLAYER_POSITION, position * 1000); //convert to millisecond
//        sendMessage(bundle);
//    }
//
//    @Override
//    public WifiManager getWifiManager() {
//        return (WifiManager) getSystemService(Context.WIFI_SERVICE);
//    }
//
//    @Override
//    public SharedPreferences getSharedPreferences() {
//        return getSharedPreferences("Reach", MODE_PRIVATE);
//    }
//
//    public static InputStream getSERStream(String name) throws IOException {
//
//        final ProcessManager manager;
//        if (reference == null || (manager = reference.get()) == null)
//            return null;
//
//        return manager.getAssets().open(name, AssetManager.ACCESS_RANDOM);
//    }
//}