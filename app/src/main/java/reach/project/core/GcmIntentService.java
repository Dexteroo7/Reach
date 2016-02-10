package reach.project.core;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.common.base.Optional;
import com.google.gson.Gson;

import reach.project.R;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.notificationCentre.NotificationActivity;
import reach.project.reachProcess.auxiliaryClasses.Connection;
import reach.project.reachProcess.reachService.ProcessManager;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

/**
 * Created by dexter on 21/6/14.
 */

public class GcmIntentService extends IntentService {

    public GcmIntentService() {
        // Setting project ID
        super("able-door-616");
    }

    public static long lastPong = 0;
    public static final int NOTIFICATION_ID_FRIEND = 498274254;
    public static final int NOTIFICATION_ID_SYNC = 565128993;
    public static final int NOTIFICATION_ID_CHAT = 865910077;
    public static final int NOTIFICATION_ID_MANUAL = 884587848;
    public static final int NOTIFICATION_ID_CURATED = 854643264;

    /**
     * Handle GCM receipt intent
     *
     * @param intent
     */
    @Override
    protected void onHandleIntent(final Intent intent) {

        final Bundle extras = intent.getExtras();
        final String messageType = GoogleCloudMessaging.getInstance(this).getMessageType(intent);

        if (extras == null || extras.isEmpty() || TextUtils.isEmpty(messageType) ||
                !GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            GcmBroadcastReceiver.completeWakefulIntent(intent);
            return;
        }

        final String type = extras.getString("type");
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (TextUtils.isEmpty(type)) {
            final String message = extras.getString("message");
            if (TextUtils.isEmpty(message)) {
                GcmBroadcastReceiver.completeWakefulIntent(intent);
                return;
            }
            /**
             * Service a permission request
             */
            if (message.startsWith("PERMISSION_REQUEST")) {

                final String[] splitter = message.split("`");
                final String userName = splitter[2].trim();

                final PendingIntent viewPendingIntent = PendingIntent.getActivity(
                        this,
                        NOTIFICATION_ID_FRIEND,
                        NotificationActivity.getIntent(this),
                        PendingIntent.FLAG_CANCEL_CURRENT);

                final NotificationCompat.Builder notificationBuilder =
                        new NotificationCompat.Builder(this)
                                .setAutoCancel(true)
                                .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND)
                                .setSmallIcon(R.drawable.icon_notification)
                                .setContentTitle(userName)
                                .setTicker(userName + " wants to access your Music")
                                .setContentText("wants to access your Music")
                                .setContentIntent(viewPendingIntent)
                                .setPriority(NotificationCompat.PRIORITY_MAX)
                                .setWhen(System.currentTimeMillis());

                notificationManager.notify(NOTIFICATION_ID_FRIEND, notificationBuilder.build());
            }

            /**
             * Service permission granted and rejected Notifications
             */
            if (message.contains("PERMISSION_GRANTED") ||
                    message.contains("PERMISSION_REJECTED")) {

                final String[] splitter = message.split("`");
                final String mType = splitter[0];
                final String hostId = splitter[1];
                final String hostName = splitter[2];

                final PendingIntent viewPendingIntent = PendingIntent.getActivity(
                        this,
                        NOTIFICATION_ID_FRIEND,
                        NotificationActivity.getIntent(this),
                        PendingIntent.FLAG_CANCEL_CURRENT);

                /*MiscUtils.useFragment(NotificationFragment.getReference(), fragment -> {
                    fragment.refresh();
                    GcmBroadcastReceiver.completeWakefulIntent(intent);
                    return null;
                });*/

                final NotificationCompat.Builder notificationBuilder =
                        new NotificationCompat.Builder(this)
                                .setAutoCancel(true)
                                .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND)
                                .setSmallIcon(R.drawable.icon_notification)
                                .setContentTitle(hostName)
                                .setContentIntent(viewPendingIntent)
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setWhen(System.currentTimeMillis());

                final ContentValues values = new ContentValues();
                if (mType.equals("PERMISSION_GRANTED")) {
                    notificationBuilder.setTicker(hostName + " accepted your request");
                    notificationBuilder.setContentText("accepted your request");
                    values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.ONLINE_REQUEST_GRANTED);
                } else if (mType.equals("PERMISSION_REJECTED")) {
                    notificationBuilder.setTicker(hostName + " rejected your request");
                    notificationBuilder.setContentText("rejected your request");
                    values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.REQUEST_NOT_SENT);
                }
                /**
                 * It is important to only update the required data
                 */
                values.put(ReachFriendsHelper.COLUMN_LAST_SEEN, System.currentTimeMillis());
                getContentResolver().update(
                        Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + hostId),
                        values,
                        ReachFriendsHelper.COLUMN_ID + " = ?",
                        new String[]{hostId + ""});
                notificationManager.notify(NOTIFICATION_ID_FRIEND, notificationBuilder.build());
            }

            /**
             * Service new notification
             */
            else if (message.startsWith("SYNC")) {

                final String count = message.substring(4);
                final PendingIntent viewPendingIntent = PendingIntent.getActivity(
                        this,
                        NOTIFICATION_ID_SYNC,
                        NotificationActivity.getIntent(this),
                        PendingIntent.FLAG_CANCEL_CURRENT);

                /*MiscUtils.useFragment(NotificationFragment.getReference(), fragment -> {
                    fragment.refresh();
                    GcmBroadcastReceiver.completeWakefulIntent(intent);
                    return null;
                });*/
                final NotificationCompat.Builder notificationBuilder =
                        new NotificationCompat.Builder(this)
                                .setAutoCancel(true)
                                .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND)
                                .setSmallIcon(R.drawable.icon_notification)
                                .setContentTitle("You have " + count + " new notification")
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setContentIntent(viewPendingIntent)
                                .setWhen(System.currentTimeMillis());
                notificationManager.notify(NOTIFICATION_ID_SYNC, notificationBuilder.build());
            }

            /**
             * Service manual notification
             */
            else if (message.startsWith("MANUAL")) {

                final String[] splitter = message.split("`");

                final Intent viewIntent;

                if (splitter[3].split(" ")[0].equals("likes"))
                    viewIntent = new Intent(this, ReachActivity.class);
                else {
                    viewIntent = new Intent(this, DialogActivity.class);
                    viewIntent.putExtra("type", 3);
                    viewIntent.putExtra("manual_title", splitter[2].trim());
                    viewIntent.putExtra("manual_text", splitter[3].trim());
                }

                viewIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

                final PendingIntent viewPendingIntent = PendingIntent.getActivity(this, NOTIFICATION_ID_MANUAL, viewIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                final NotificationCompat.Builder notificationBuilder =
                        new NotificationCompat.Builder(this)
                                .setAutoCancel(true)
                                .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND)
                                .setSmallIcon(R.drawable.icon_notification)
                                .setContentTitle(splitter[2].trim())
                                .setContentText(splitter[3].trim())
                                .setContentIntent(viewPendingIntent)
                                .setPriority(NotificationCompat.PRIORITY_MAX)
                                .setStyle(new NotificationCompat.BigTextStyle()
                                        .bigText(splitter[3].trim()))
                                .setWhen(System.currentTimeMillis());
                notificationManager.notify(NOTIFICATION_ID_MANUAL, notificationBuilder.build());
            }

            /**
             * Service chat notification, IF, chat is not open
             */
            /*else if (message.startsWith("CHAT") && !ChatActivityFragment.connected.get()) {

                final Intent viewIntent = new Intent(this, ChatActivity.class);
                final PendingIntent viewPendingIntent = PendingIntent.getActivity(this, NOTIFICATION_ID_CHAT, viewIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                final NotificationCompat.Builder notificationBuilder =
                        new NotificationCompat.Builder(this)
                                .setAutoCancel(true)
                                .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND)
                                .setSmallIcon(R.drawable.icon_notification)
                                .setContentTitle("Devika sent you a message")
                                .setContentIntent(viewPendingIntent)
                                .setPriority(NotificationCompat.PRIORITY_MAX)
                                .setWhen(System.currentTimeMillis());
                notificationManager.notify(NOTIFICATION_ID_CHAT, notificationBuilder.build());
            }*/

            /**
             * Service PONG
             */
            else if (message.startsWith("PONG")) {

                /**
                 * 0 - PONG
                 * 1 - ID
                 * 2 - Network Type
                 */
                final String[] splitter = message.split(" ");
                final long hostId = Long.parseLong(splitter[1]);

                if (hostId == StaticData.DEVIKA) {

                    GcmBroadcastReceiver.completeWakefulIntent(intent);
                    return;
                }

                final ContentValues friend = new ContentValues();
                final ContentValues database = new ContentValues();
                StaticData.NETWORK_CACHE.put(hostId, splitter[2]);

//                Log.i("Ayush", hostId + " Got PONG");

                friend.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.ONLINE_REQUEST_GRANTED);
                friend.put(ReachFriendsHelper.COLUMN_NETWORK_TYPE, splitter[2]);
                friend.put(ReachFriendsHelper.COLUMN_LAST_SEEN, System.currentTimeMillis()); //online

                database.put(ReachDatabaseHelper.COLUMN_ONLINE_STATUS, ReachFriendsHelper.ONLINE_REQUEST_GRANTED);
                /**
                 * It is important to only update the required data
                 */
                getContentResolver().update(
                        Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + hostId),
                        friend,
                        ReachFriendsHelper.COLUMN_ID + " = ? and " +
                                ReachFriendsHelper.COLUMN_STATUS + " < ?",
                        new String[]{hostId + "", ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED + ""});
                getContentResolver().update(
                        ReachDatabaseProvider.CONTENT_URI,
                        database,
                        "(" + ReachDatabaseHelper.COLUMN_SENDER_ID + " = ? or " +
                                ReachDatabaseHelper.COLUMN_RECEIVER_ID + " = ?) and " +
                                ReachDatabaseHelper.COLUMN_STATUS + " != ?",
                        new String[]{hostId + "", hostId + "", ReachDatabase.FINISHED + ""});
            }

            /**
             * Service PING
             */
            else if (message.startsWith("PING")) {

                //Handle announce
                final long id = SharedPrefUtils.getServerId(getSharedPreferences("Reach", MODE_PRIVATE));
                if (id == 0) {

                    GcmBroadcastReceiver.completeWakefulIntent(intent);
                    return;
                }
                final short[] networkType = new short[]{getNetworkType(this)};
                if (networkType[0] > 1 && !SharedPrefUtils.getMobileData(getSharedPreferences("Reach", MODE_PRIVATE)))
                    networkType[0] = 5;

                final long currentTime = System.currentTimeMillis();
                if (currentTime - lastPong < StaticData.MINIMUM_PONG_GAP) {

                    Log.i("Ayush", "Ignoring PING " + (currentTime - lastPong));
                    GcmBroadcastReceiver.completeWakefulIntent(intent);
                    return;
                }
                lastPong = currentTime;
                MiscUtils.autoRetry(() -> StaticData.MESSAGING_API
                        .handleAnnounce(id, networkType[0] + "")
                        .execute(), Optional.absent()).orNull();
            }

            /**
             * Service CONNECT
             */
            else if (message.startsWith("CONNECT")) {

                //Verify
                final String actualMessage = message.substring(7);
                final Connection connection = new Gson().fromJson(actualMessage, Connection.class);
                if (connection == null || TextUtils.isEmpty(connection.getMessageType())) {
                    Log.i("Downloader", "illegal network request");
                    GcmBroadcastReceiver.completeWakefulIntent(intent);
                    return;
                }

                final Cursor isPaused = getContentResolver().query(
                        ReachDatabaseProvider.CONTENT_URI,
                        new String[]{ReachDatabaseHelper.COLUMN_STATUS},
                        ReachDatabaseHelper.COLUMN_SENDER_ID + " = ? and " +
                                ReachDatabaseHelper.COLUMN_RECEIVER_ID + " = ? and " +
                                ReachDatabaseHelper.COLUMN_SONG_ID + " = ?",
                        new String[]{connection.getSenderId() + "",
                                connection.getReceiverId() + "",
                                connection.getSongId() + ""}, null);

                if (isPaused != null && isPaused.moveToFirst() && isPaused.getShort(0) == ReachDatabase.PAUSED_BY_USER) {
                    isPaused.close();
                    GcmBroadcastReceiver.completeWakefulIntent(intent);
                    return;
                } else if (isPaused != null)
                    isPaused.close();

                Log.i("Downloader", message + " Received");
                if (!SharedPrefUtils.getMobileData(getSharedPreferences("Reach", MODE_PRIVATE)) && getNetworkType(this) != 1 && message.contains("REQ"))
                    Log.i("Downloader", "Dropping request on mobile network");
                else
                    ProcessManager.submitNetworkRequest(this, actualMessage);
            }
//                Log.i("Downloader", "Received unexpected GCM " + message);
        } else {
            switch (type) {
                case "CURATED":
                    final String heading = extras.getString("heading");
                    final String message = extras.getString("message");

                    final NotificationCompat.Builder notificationBuilder =
                            new NotificationCompat.Builder(this)
                                    .setAutoCancel(true)
                                    .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND)
                                    .setContentTitle(heading)
                                    .setTicker(heading + " " + message)
                                    .setContentText(message)
                                    .setSmallIcon(R.drawable.icon_notification)
                                    .setPriority(NotificationCompat.PRIORITY_MAX)
                                    .setWhen(System.currentTimeMillis());

//                    final String imageURL = extras.getString("imageURL");
//                    setBitmap(imageURL, notificationBuilder);

                    final Intent mIntent;

                    final String activityName = extras.getString("activityName");
                    if (!TextUtils.isEmpty(activityName)) {
                        Class<?> mClass = null;
                        try {
                            mClass = Class.forName(activityName);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        mIntent = new Intent(this, mClass);

                        if (!TextUtils.isEmpty(extras.getString("userId"))) {
                            final long userId = Long.parseLong(extras.getString("userId"));
                            if (userId > 0)
                                mIntent.putExtra("userId", userId);
                        }

                        final String tab = extras.getString("tab");
                        if (!TextUtils.isEmpty(tab))
                            mIntent.setAction(tab);
                    }
                    else
                        mIntent = ReachActivity.getIntent(this);

                    final PendingIntent viewPendingIntent = PendingIntent.getActivity(
                            this,
                            NOTIFICATION_ID_CURATED,
                            mIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);
                    notificationBuilder.setContentIntent(viewPendingIntent);

                    notificationManager.notify(NOTIFICATION_ID_FRIEND, notificationBuilder.build());

                    break;
                default:
                    GcmBroadcastReceiver.completeWakefulIntent(intent);
                    throw new IllegalArgumentException("Invalid notification type : " + type);
            }
        }

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    /*private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private void setBitmap(String url, NotificationCompat.Builder notificationBuilder) {
        if (notificationBuilder == null)
            return;
        DataSubscriber dataSubscriber = new BaseDataSubscriber<CloseableReference<CloseableBitmap>>() {
            @Override
            public void onNewResultImpl(
                    DataSource<CloseableReference<CloseableBitmap>> dataSource) {
                if (!dataSource.isFinished()) {
                    return;
                }
                CloseableReference<CloseableBitmap> imageReference = dataSource.getResult();
                if (imageReference != null) {
                    final CloseableReference<CloseableBitmap> closeableReference = imageReference.clone();
                    try {
                        CloseableBitmap closeableBitmap = closeableReference.get();
                        Bitmap bitmap  = closeableBitmap.getUnderlyingBitmap();
                        if(bitmap != null && !bitmap.isRecycled()) {
                            notificationBuilder.setLargeIcon(bitmap);
                        }
                    } finally {
                        imageReference.close();
                        closeableReference.close();
                    }
                }
            }
            @Override
            public void onFailureImpl(DataSource dataSource) {
                Throwable throwable = dataSource.getFailureCause();
                // handle failure
            }
        };
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        ImageRequestBuilder builder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url));
        final int size = MiscUtils.dpToPx(100);
        builder.setResizeOptions(new ResizeOptions(size, size));
        ImageRequest request = builder.build();
        DataSource<CloseableReference<CloseableImage>>
                dataSource = imagePipeline.fetchDecodedImage(request, this);
        dataSource.subscribe(dataSubscriber, executorService);
    }*/

    private short getNetworkType(Context context) {

        if (context == null)
            return 0;
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo info = cm.getActiveNetworkInfo();
        final short netType;

        if (info != null && info.isConnected()) {

            final int type = info.getType();
            /**
             * WIFI
             */
            if (type == ConnectivityManager.TYPE_WIFI)
                netType = 1;
            /**
             * MOBILE DATA
             */
            else if (type == ConnectivityManager.TYPE_MOBILE) {

                final int subtype = info.getSubtype();
                if (subtype == TelephonyManager.NETWORK_TYPE_1xRTT ||
                        subtype == TelephonyManager.NETWORK_TYPE_CDMA ||
                        subtype == TelephonyManager.NETWORK_TYPE_EDGE ||
                        subtype == TelephonyManager.NETWORK_TYPE_GPRS ||
                        subtype == TelephonyManager.NETWORK_TYPE_IDEN) {
                    netType = 2;
                } else if (subtype == TelephonyManager.NETWORK_TYPE_EVDO_0 ||
                        subtype == TelephonyManager.NETWORK_TYPE_EVDO_A ||
                        subtype == TelephonyManager.NETWORK_TYPE_HSDPA ||
                        subtype == TelephonyManager.NETWORK_TYPE_HSPA ||
                        subtype == TelephonyManager.NETWORK_TYPE_HSUPA ||
                        subtype == TelephonyManager.NETWORK_TYPE_UMTS ||
                        subtype == TelephonyManager.NETWORK_TYPE_EHRPD ||
                        subtype == TelephonyManager.NETWORK_TYPE_EVDO_B ||
                        subtype == TelephonyManager.NETWORK_TYPE_HSPAP) {
                    netType = 3;
                } else if (subtype == TelephonyManager.NETWORK_TYPE_LTE) {
                    netType = 4;
                } else netType = 0;
            } else netType = 0;
        } else netType = 0;

        return netType;
    }
}
