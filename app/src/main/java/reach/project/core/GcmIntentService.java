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
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.gson.Gson;

import java.io.IOException;

import reach.project.R;
import reach.project.utils.auxiliaryClasses.ReachDatabase;
import reach.project.database.contentProvider.ReachDatabaseProvider;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.sql.ReachDatabaseHelper;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.reachProcess.auxiliaryClasses.Connection;
import reach.project.reachProcess.reachService.ProcessManager;
import reach.project.utils.auxiliaryClasses.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.auxiliaryClasses.PushContainer;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.StringCompress;

/**
 * Created by dexter on 21/6/14.
 */
public class GcmIntentService extends IntentService {

    public GcmIntentService() {
        super("able-door-616");
    }

    private static long lastPong = 0;

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

        final NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);
        /**
         * Service a permission request
         */
//        if (message.startsWith("PERMISSION_REQUEST")) {
//
//            final String[] splitter = message.split("`");
//            final String userId = splitter[1].trim();
//            final String userName = splitter[2].trim();
//            final Cursor getFriend = getContentResolver().query(
//                    Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
//                    ReachFriendsHelper.projection,
//                    ReachFriendsHelper.COLUMN_ID + " = ?",
//                    new String[]{userId}, null);
//            final boolean shouldInsert;
//            ReachFriend reachFriend;
//            if (getFriend == null ||
//                    !getFriend.moveToFirst() ||
//                    (reachFriend = ReachFriendsHelper.cursorToProcess(getFriend)) == null) {
//
//                final long myId = SharedPrefUtils.getServerId(getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS));
//                reachFriend = MiscUtils.autoRetry(new DoWork<ReachFriend>() {
//                    @Override
//                    protected ReachFriend doWork() throws IOException {
//                        return null;
////                        return StaticData.userEndpoint.getReachFriend(Long.parseLong(userId), myId).execute();
//                    }
//                }, Optional.<Predicate<ReachFriend>>absent()).orNull();
//                shouldInsert = !(reachFriend == null);
//            } else
//                shouldInsert = false;
//
//            if (getFriend != null)
//                getFriend.close();
//            if (reachFriend == null) {
//                GcmBroadcastReceiver.completeWakefulIntent(intent);
//                return;
//            }
//            if (shouldInsert)
//                getContentResolver().insert(ReachFriendsProvider.CONTENT_URI, ReachFriendsHelper.contentValuesCreator(reachFriend));
//
//            final int notification_id = message.hashCode();
//            final Intent viewIntent = new Intent(this, ReachNotificationActivity.class);
//            final Bundle bundle = new Bundle();
//            bundle.putLong("number_of_songs", reachFriend.getNumberofSongs());
//            bundle.putLong("host_id", reachFriend.getId());
//            bundle.putString("image_id", reachFriend.getImageId());
//            bundle.putString("user_name", userName);
//            bundle.putInt("notification_id", notification_id);
//            viewIntent.putExtras(bundle);
//
//            final PendingIntent viewPendingIntent = PendingIntent.getActivity(this, notification_id, viewIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//            final NotificationCompat.Builder notificationBuilder =
//                    new NotificationCompat.Builder(this)
//                            .setAutoCancel(true)
//                            .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND)
//                            .setSmallIcon(R.drawable.ic_icon_notif)
//                            .setContentTitle(userName)
//                            .setTicker(userName + " wants to access your Music")
//                            .setContentText("wants to access your Music")
//                            .setContentIntent(viewPendingIntent)
//                            .setPriority(NotificationCompat.PRIORITY_MAX)
//                            .setWhen(System.currentTimeMillis());
//
//            notificationManager.notify(notification_id, notificationBuilder.build());
//        }
        /**
         * Service permission granted and rejected Notifications
         */
        if (message.contains("PERMISSION_GRANTED") ||
                message.contains("PERMISSION_REJECTED")) {

            final String[] splitter = message.split("`");
            final String type = splitter[0];
            final String hostId = splitter[1];
            final String hostName = splitter[2];
            final int notification_id = message.hashCode();

            final Intent viewIntent = new Intent(this, ReachActivity.class);
            Bundle bundle = new Bundle();
            bundle.putBoolean("oP", true);
            viewIntent.putExtras(bundle);
            final PendingIntent viewPendingIntent = PendingIntent.getActivity(this, notification_id, viewIntent, PendingIntent.FLAG_UPDATE_CURRENT);

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
            notificationManager.notify(notification_id, notificationBuilder.build());

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
            }
            else {
                viewIntent = new Intent(this, PushActivity.class);
                viewIntent.putExtra("type", 3);
                viewIntent.putExtra("manual_title", splitter[2].trim());
                viewIntent.putExtra("manual_text", splitter[3].trim());
            }

            viewIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

            final PendingIntent viewPendingIntent = PendingIntent.getActivity(this, message.hashCode(), viewIntent, PendingIntent.FLAG_CANCEL_CURRENT);
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
            notificationManager.notify(message.hashCode(), notificationBuilder.build());
        }
        /**
         * Service push request
         */
        else if (message.startsWith("PUSH")) {

            final int notification_id = message.hashCode();
            final Intent viewIntent = new Intent(this, PushActivity.class);
            viewIntent.putExtra("type", 0);
            viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            final String payLoad = message.substring(4);

            final String unCompressed;
            try {
                unCompressed = StringCompress.decompress(Base64.decode(payLoad, Base64.DEFAULT));
            } catch (IOException e) {
                e.printStackTrace();
                GcmBroadcastReceiver.completeWakefulIntent(intent);
                return;
            }
            final PushContainer pushContainer = new Gson().fromJson(unCompressed, PushContainer.class);
            viewIntent.putExtra("data", unCompressed);
            viewIntent.putExtra("song_count", pushContainer.getSongCount());
            viewIntent.putExtra("user_name", pushContainer.getUserName());
            viewIntent.putExtra("receiver_id", pushContainer.getReceiverId());
            viewIntent.putExtra("sender_id", pushContainer.getSenderId());
            viewIntent.putExtra("user_image", pushContainer.getUserImage());
            viewIntent.putExtra("first_song", pushContainer.getFirstSongName());
            viewIntent.putExtra("hash", payLoad.hashCode()); //hash of compressed String
            viewIntent.putExtra("custom_message", pushContainer.getCustomMessage());

            String cMsg = pushContainer.getCustomMessage();
            String count;
            if (cMsg != null && cMsg.length() > 0)
                count = cMsg + ". Start listening to ";
            else
                count = "wants you to listen to ";

            count = count + pushContainer.getFirstSongName();

            if (pushContainer.getSongCount() == 2)
                count = count + " and 1 other song";
            else if (pushContainer.getSongCount() > 2)
                count = count + " and " + (pushContainer.getSongCount() - 1) + " other songs";

            final PendingIntent viewPendingIntent = PendingIntent.getActivity(this, notification_id, viewIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            final NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this)
                            .setAutoCancel(true)
                            .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND)
                            .setSmallIcon(R.drawable.ic_icon_notif)
                            .setContentTitle(pushContainer.getUserName())
                            .setTicker(pushContainer.getUserName() + " " + count)
                            .setContentText(count)
                            .setContentIntent(viewPendingIntent)
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setWhen(System.currentTimeMillis());
            notificationManager.notify(notification_id, notificationBuilder.build());
        }
//        /**
//         * Service LIKE
//         */
//        else if(message.startsWith("LIKE")) {
//            final int notification_id = message.hashCode();
//            final Intent viewIntent = new Intent(this, ReachActivity.class);
//            Bundle bundle = new Bundle();
//            bundle.putBoolean("oP",true);
//            viewIntent.putExtras(bundle);
//            viewIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//            final String [] splitter = message.split("`");
//            if(splitter.length < 4) {
//                GcmBroadcastReceiver.completeWakefulIntent(intent);
//                return;
//            }
//            Bitmap image;
//            try {
//                image = Picasso.with(this).load(splitter[2]).get();
//            } catch (IOException | IllegalStateException e) {
//                e.printStackTrace();
//                image = null;
//            }
//            final PendingIntent viewPendingIntent = PendingIntent.getActivity(this, notification_id, viewIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//            final NotificationCompat.Builder notificationBuilder =
//                    new NotificationCompat.Builder(this)
//                            .setAutoCancel(true)
//                            .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND)
//                            .setSmallIcon(R.drawable.ic_icon_notif)
//                            .setContentTitle(splitter[1])
//                            .setContentText(splitter[1] + " likes your song " + splitter[3])
//                            .setLargeIcon(image)
//                            .setContentIntent(viewPendingIntent)
//                            .setPriority(NotificationCompat.PRIORITY_MAX)
//                            .setWhen(System.currentTimeMillis());
//            notificationManager.notify(notification_id, notificationBuilder.build());
//        }
        /*if( message.startsWith("SYNC_NOTIFICATIONS")) {

            //Pull Notifications from server and insert new ones. Accordingly notify the User
            final long id = SharedPrefUtils.getServerId(getSharedPreferences("Reach", MODE_MULTI_PROCESS));
            final Optional<NotificationCollection> collection = MiscUtils.autoRetry(new DoWork<NotificationCollection>() {
                @Override
                protected NotificationCollection doWork() throws IOException {
                    return StaticData.userEndpoint.getNotification(id).execute();
                }
            }, Optional.<Predicate<NotificationCollection>>absent());
            if(!collection.isPresent()) {
                GcmBroadcastReceiver.completeWakefulIntent(intent);
                return;
            }
            final List<Notification> Notifications = collection.get().getItems();
            if(Notifications == null || Notifications.isEmpty()) {
                GcmBroadcastReceiver.completeWakefulIntent(intent);
                return;
            }


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
            final ContentValues friend = new ContentValues();
            final ContentValues database = new ContentValues();
            StaticData.networkCache.put(hostId, splitter[2]);

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
            final long id = SharedPrefUtils.getServerId(getSharedPreferences("Reach", MODE_MULTI_PROCESS));
            if (id == 0)
                return;
            final short[] networkType = new short[]{getNetworkType(this)};
            if (networkType[0] > 1 && !SharedPrefUtils.getMobileData(getSharedPreferences("Reach", MODE_MULTI_PROCESS)))
                networkType[0] = 5;

            final long currentTime = System.currentTimeMillis();
            if (currentTime - lastPong < StaticData.MINIMUM_PONG_GAP) {
                Log.i("Ayush", "Ignoring PING " + (currentTime - lastPong));
                return;
            }
            lastPong = currentTime;
            MiscUtils.autoRetry(new DoWork<Void>() {
                @Override
                public Void doWork() throws IOException {

                    return StaticData.messagingEndpoint.messagingEndpoint()
                            .handleAnnounce(id, networkType[0] + "")
                            .execute();
                }
            }, Optional.<Predicate<Void>>absent()).orNull();
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

            if(isPaused != null && isPaused.moveToFirst() && isPaused.getShort(0) == ReachDatabase.PAUSED_BY_USER) {
                isPaused.close();
                GcmBroadcastReceiver.completeWakefulIntent(intent);
                return;
            } else if(isPaused != null)
                isPaused.close();

            Log.i("Downloader", message + " Received");
            if (!SharedPrefUtils.getMobileData(getSharedPreferences("Reach", MODE_MULTI_PROCESS)) && getNetworkType(this) != 1 && message.contains("REQ"))
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
