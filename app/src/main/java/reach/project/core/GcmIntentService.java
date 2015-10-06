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
import reach.project.devikaChat.ChatActivity;
import reach.project.devikaChat.ChatActivityFragment;
import reach.project.notificationCentre.FriendRequestFragment;
import reach.project.notificationCentre.NotificationFragment;
import reach.project.uploadDownload.ReachDatabaseProvider;
import reach.project.friends.ReachFriendsProvider;
import reach.project.uploadDownload.ReachDatabaseHelper;
import reach.project.friends.ReachFriendsHelper;
import reach.project.reachProcess.auxiliaryClasses.Connection;
import reach.project.reachProcess.reachService.ProcessManager;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.uploadDownload.ReachDatabase;

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

    /**
     * Handle GCM receipt intent
     *
     * @param intent
     */
    @Override
    protected void onHandleIntent(final Intent intent) {

        final Bundle extras = intent.getExtras();
        final String messageType = GoogleCloudMessaging.getInstance(this).getMessageType(intent);
        final String message;

        if (extras == null || extras.isEmpty() || TextUtils.isEmpty(messageType) ||
                !GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType) ||
                TextUtils.isEmpty(message = extras.get("message") + "")) {

            GcmBroadcastReceiver.completeWakefulIntent(intent);
            return;
        }

        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        /**
         * Service a permission request
         */
        if (message.startsWith("PERMISSION_REQUEST")) {

            final String[] splitter = message.split("`");
            final String userName = splitter[2].trim();

            final Intent viewIntent = new Intent(this, ReachActivity.class);
            viewIntent.putExtra("openFriendRequests", true);
            final PendingIntent viewPendingIntent = PendingIntent.getActivity(this, NOTIFICATION_ID_FRIEND, viewIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            MiscUtils.useFragment(FriendRequestFragment.getReference(), fragment -> {
                fragment.refresh();
                GcmBroadcastReceiver.completeWakefulIntent(intent);
                return null;
            });
            final NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this)
                            .setAutoCancel(true)
                            .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND)
                            .setSmallIcon(R.drawable.ic_icon_notif)
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
            final String type = splitter[0];
            final String hostId = splitter[1];
            final String hostName = splitter[2];

            final Intent viewIntent = new Intent(this, ReachActivity.class);
            viewIntent.putExtra("openNotifications", true);
            final PendingIntent viewPendingIntent = PendingIntent.getActivity(this, NOTIFICATION_ID_FRIEND, viewIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            MiscUtils.useFragment(NotificationFragment.getReference(), fragment -> {
                fragment.refresh();
                GcmBroadcastReceiver.completeWakefulIntent(intent);
                return null;
            });

            final NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this)
                            .setAutoCancel(true)
                            .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND)
                            .setSmallIcon(R.drawable.ic_icon_notif)
                            .setContentTitle(hostName)
                            .setContentIntent(viewPendingIntent)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setWhen(System.currentTimeMillis());

            final ContentValues values = new ContentValues();
            if (type.equals("PERMISSION_GRANTED")) {
                notificationBuilder.setTicker(hostName + " accepted your request");
                notificationBuilder.setContentText("accepted your request");
                values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.ONLINE_REQUEST_GRANTED);
            } else if (type.equals("PERMISSION_REJECTED")) {
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
            final Intent viewIntent = new Intent(this, ReachActivity.class);
            viewIntent.putExtra("openNotifications", true);
            final PendingIntent viewPendingIntent = PendingIntent.getActivity(this, NOTIFICATION_ID_SYNC, viewIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            MiscUtils.useFragment(NotificationFragment.getReference(), fragment -> {
                fragment.refresh();
                GcmBroadcastReceiver.completeWakefulIntent(intent);
                return null;
            });
            final NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this)
                            .setAutoCancel(true)
                            .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND)
                            .setSmallIcon(R.drawable.ic_icon_notif)
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

            if (splitter[3].split(" ")[0].equals("likes")) {
                viewIntent = new Intent(this, ReachActivity.class);
                viewIntent.putExtra("openPlayer", true);
            } else {
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
                            .setSmallIcon(R.drawable.ic_icon_notif)
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
        else if (message.startsWith("CHAT") && !ChatActivityFragment.connected.get()) {

            final Intent viewIntent = new Intent(this, ChatActivity.class);
            final PendingIntent viewPendingIntent = PendingIntent.getActivity(this, NOTIFICATION_ID_CHAT, viewIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            final NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this)
                            .setAutoCancel(true)
                            .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND)
                            .setSmallIcon(R.drawable.ic_icon_notif)
                            .setContentTitle("Bitch you got a chat")
                            .setContentIntent(viewPendingIntent)
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setWhen(System.currentTimeMillis());
            notificationManager.notify(NOTIFICATION_ID_CHAT, notificationBuilder.build());
        }

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

            if (hostId == StaticData.devika) {

                GcmBroadcastReceiver.completeWakefulIntent(intent);
                return;
            }

            final ContentValues friend = new ContentValues();
            final ContentValues database = new ContentValues();
            StaticData.networkCache.put(hostId, splitter[2]);

            Log.i("Ayush", hostId + " Got PONG");

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
                    ReachFriendsHelper.COLUMN_ID + " = ?",
                    new String[]{hostId + ""});
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
            MiscUtils.autoRetry(() -> StaticData.messagingEndpoint
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
        } else
            Log.i("Downloader", "Received unexpected GCM " + message);

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

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
