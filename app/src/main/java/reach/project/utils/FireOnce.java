package reach.project.utils;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.gson.Gson;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import reach.backend.entities.messaging.model.MyBoolean;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.reachProcess.auxiliaryClasses.Connection;
import reach.project.reachProcess.reachService.ProcessManager;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 31/12/15.
 */
public enum FireOnce {
    ;

    private static final ExecutorService contactSyncService = MiscUtils.getRejectionExecutor();
    private static final ExecutorService pingService = MiscUtils.getRejectionExecutor();
    private static final ExecutorService refreshDownloadOpsService = MiscUtils.getRejectionExecutor();

    @Nullable
    private static Future syncingContacts = null;

    public static synchronized void contactSync(Context context) {

        final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        final ContentResolver resolver = context.getContentResolver();

        final long serverId = SharedPrefUtils.getServerId(preferences);
        final String phoneNumber = SharedPrefUtils.getPhoneNumber(preferences);

        final Cursor verify = resolver.query(
                ReachFriendsProvider.CONTENT_URI,
                new String[]{ReachFriendsHelper.COLUMN_ID}, null, null, null);

        if (verify == null || verify.getCount() == 0) { //run full sync if no friends

            syncingContacts = contactSyncService.submit(new ForceSyncFriends(
                    new WeakReference<>(context),
                    serverId,
                    phoneNumber));
        } else { //run quick sync

            syncingContacts = contactSyncService.submit(new QuickSyncFriends(
                    new WeakReference<>(context),
                    serverId,
                    phoneNumber));

            /**
             * Invalidate everyone
             */
            final ContentValues contentValues = new ContentValues();
            contentValues.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.OFFLINE_REQUEST_GRANTED);
            contentValues.put(ReachFriendsHelper.COLUMN_NETWORK_TYPE, (short) 0);

            resolver.update(
                    ReachFriendsProvider.CONTENT_URI,
                    contentValues,
                    ReachFriendsHelper.COLUMN_STATUS + " = ? and " +
                            ReachFriendsHelper.COLUMN_LAST_SEEN + " < ?",
                    new String[]{ReachFriendsHelper.ONLINE_REQUEST_GRANTED + "",
                            (System.currentTimeMillis() - (60 * 1000)) + ""});
            contentValues.clear();
        }

        if (verify != null)
            verify.close();
        StaticData.NETWORK_CACHE.clear(); //clear network cache

        pingService.submit(new SendPing( //send ping
                null,
                new WeakReference<>(resolver),
                serverId));
    }

    public static synchronized void contactSync(@Nullable WeakReference<Context> reference,
                                                long serverId,
                                                String phoneNumber) {

        final ContentResolver resolver = MiscUtils.useContextFromContext(reference, Context::getContentResolver).orNull();
        if (resolver == null)
            return;

        final Cursor verify = resolver.query(
                ReachFriendsProvider.CONTENT_URI,
                new String[]{ReachFriendsHelper.COLUMN_ID}, null, null, null);

        if (verify == null || verify.getCount() == 0) { //run full sync if no friends

            syncingContacts = contactSyncService.submit(new ForceSyncFriends(
                    reference,
                    serverId,
                    phoneNumber));
        } else { //run quick sync

            syncingContacts = contactSyncService.submit(new QuickSyncFriends(
                    reference,
                    serverId,
                    phoneNumber));

            /**
             * Invalidate everyone
             */
            final ContentValues contentValues = new ContentValues();
            contentValues.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.OFFLINE_REQUEST_GRANTED);
            contentValues.put(ReachFriendsHelper.COLUMN_NETWORK_TYPE, (short) 0);

            resolver.update(
                    ReachFriendsProvider.CONTENT_URI,
                    contentValues,
                    ReachFriendsHelper.COLUMN_STATUS + " = ? and " +
                            ReachFriendsHelper.COLUMN_LAST_SEEN + " < ?",
                    new String[]{ReachFriendsHelper.ONLINE_REQUEST_GRANTED + "",
                            (System.currentTimeMillis() - (60 * 1000)) + ""});
            contentValues.clear();
        }

        if (verify != null)
            verify.close();
        StaticData.NETWORK_CACHE.clear(); //clear network cache

        pingService.submit(new SendPing( //send ping
                null,
                new WeakReference<>(resolver),
                serverId));
    }

    public static void sendPing(@Nullable WeakReference<HandOverMessage<Void>> handOverMessageWeakReference,
                                WeakReference<ContentResolver> resolverWeakReference,
                                long serverId) {

        pingService.submit(new SendPing( //send ping
                handOverMessageWeakReference,
                resolverWeakReference,
                serverId));
    }



    private static final class SendPing implements Runnable {

        @Nullable
        private final WeakReference<HandOverMessage<Void>> handOverMessageWeakReference;

        private final WeakReference<ContentResolver> resolverWeakReference;
        private final long serverId;

        public SendPing(@Nullable WeakReference<HandOverMessage<Void>> handOverMessageWeakReference,
                        WeakReference<ContentResolver> resolverWeakReference,
                        long serverId) {

            this.handOverMessageWeakReference = handOverMessageWeakReference;
            this.resolverWeakReference = resolverWeakReference;
            this.serverId = serverId;
        }

        @Override
        public void run() {

            if (syncingContacts != null)
                try {
                    syncingContacts.get(); //wait for contacts sync
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return; //quit
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

            MiscUtils.autoRetry(() -> StaticData.USER_API.pingMyReach(serverId).execute(), Optional.absent()).orNull();
            StaticData.NETWORK_CACHE.clear();

            /**
             * Invalidate those who were online 60 secs ago
             * and send PING
             */
            final long currentTime = System.currentTimeMillis();
            final ContentValues contentValues = new ContentValues();
            contentValues.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.OFFLINE_REQUEST_GRANTED);
            contentValues.put(ReachFriendsHelper.COLUMN_NETWORK_TYPE, (short) 0);

            final ContentResolver resolver = resolverWeakReference.get();
            if (resolver == null)
                return;

            resolver.update(
                    ReachFriendsProvider.CONTENT_URI,
                    contentValues,
                    ReachFriendsHelper.COLUMN_STATUS + " = ? and " +
                            ReachFriendsHelper.COLUMN_LAST_SEEN + " < ? and " +
                            ReachFriendsHelper.COLUMN_ID + " != ?",
                    new String[]{ReachFriendsHelper.ONLINE_REQUEST_GRANTED + "", (currentTime - 60 * 1000) + "", StaticData.DEVIKA + ""});
            contentValues.clear();

            final HandOverMessage handOverMessage;
            if (handOverMessageWeakReference != null && (handOverMessage = handOverMessageWeakReference.get()) != null)
                handOverMessage.handOverMessage(true); //done
        }
    }

    public static void refreshOperations(WeakReference<? extends Context> reference) {
        new RefreshOperations(reference).executeOnExecutor(refreshDownloadOpsService);
    }

    //TODO optimize database fetch !
    private static class RefreshOperations extends AsyncTask<Void, Void, Void> {

        private final WeakReference<? extends Context> reference;

        public RefreshOperations(WeakReference<? extends Context> reference) {
            this.reference = reference;
        }

        /**
         * Create a contentProviderOperation, we do not update
         * if the operation is paused.
         * We do not mess with the status if it was paused, working, relay or finished !
         *
         * @param contentValues the values to use
         * @param id            the id of the entry
         * @return the contentProviderOperation
         */
        private ContentProviderOperation getUpdateOperation(ContentValues contentValues, long id) {
            return ContentProviderOperation
                    .newUpdate(Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + id))
                    .withValues(contentValues)
                    .withSelection(ReachDatabaseHelper.COLUMN_ID + " = ? and " +
                                    ReachDatabaseHelper.COLUMN_STATUS + " != ? and " +
                                    ReachDatabaseHelper.COLUMN_STATUS + " != ? and " +
                                    ReachDatabaseHelper.COLUMN_STATUS + " != ? and " +
                                    ReachDatabaseHelper.COLUMN_STATUS + " != ?",
                            new String[]{
                                    id + "",
                                    ReachDatabase.PAUSED_BY_USER + "",
                                    ReachDatabase.WORKING + "",
                                    ReachDatabase.RELAY + "",
                                    ReachDatabase.FINISHED + ""})
                    .build();
        }

        private ContentProviderOperation getForceUpdateOperation(ContentValues contentValues, long id) {
            return ContentProviderOperation
                    .newUpdate(Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + id))
                    .withValues(contentValues)
                    .withSelection(ReachDatabaseHelper.COLUMN_ID + " = ?",
                            new String[]{id + ""})
                    .build();
        }

        private String generateRequest(ReachDatabase reachDatabase) {

            return "CONNECT" + new Gson().toJson
                    (new Connection(
                            ////Constructing connection object
                            "REQ",
                            reachDatabase.getSenderId(),
                            reachDatabase.getReceiverId(),
                            reachDatabase.getSongId(),
                            reachDatabase.getProcessed(),
                            reachDatabase.getLength(),
                            UUID.randomUUID().getMostSignificantBits(),
                            UUID.randomUUID().getMostSignificantBits(),
                            reachDatabase.getLogicalClock(), ""));
        }

        private String fakeResponse(ReachDatabase reachDatabase) {

            return new Gson().toJson
                    (new Connection(
                            ////Constructing connection object
                            "RELAY",
                            reachDatabase.getSenderId(),
                            reachDatabase.getReceiverId(),
                            reachDatabase.getSongId(),
                            reachDatabase.getProcessed(),
                            reachDatabase.getLength(),
                            UUID.randomUUID().getMostSignificantBits(),
                            UUID.randomUUID().getMostSignificantBits(),
                            reachDatabase.getLogicalClock(), ""));
        }

        private ArrayList<ContentProviderOperation> bulkStartDownloads(List<ReachDatabase> reachDatabases) {

            final ArrayList<ContentProviderOperation> operations = new ArrayList<>();

            for (ReachDatabase reachDatabase : reachDatabases) {

                final ContentValues values = new ContentValues();
                if (reachDatabase.getProcessed() >= reachDatabase.getLength()) {

                    //mark finished
                    if (reachDatabase.getStatus() != ReachDatabase.FINISHED) {

                        values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.FINISHED);
                        values.put(ReachDatabaseHelper.COLUMN_PROCESSED, reachDatabase.getLength());
                        operations.add(getForceUpdateOperation(values, reachDatabase.getId()));
                    }
                    continue;
                }

                final MyBoolean myBoolean;
                if (reachDatabase.getSenderId() == StaticData.DEVIKA) {

                    //hit cloud
                    MiscUtils.useContextFromContext(reference, context -> {
                        ProcessManager.submitNetworkRequest(context, fakeResponse(reachDatabase));
                        return null;
                    });

                    myBoolean = new MyBoolean();
                    myBoolean.setGcmexpired(false);
                    myBoolean.setOtherGCMExpired(false);
                } else {
                    //sending REQ to senderId
                    myBoolean = MiscUtils.sendGCM(
                            generateRequest(reachDatabase),
                            reachDatabase.getSenderId(),
                            reachDatabase.getReceiverId());
                }

                if (myBoolean == null) {
                    Log.i("Ayush", "GCM sending resulted in shit");
                    values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.GCM_FAILED);
                } else if (myBoolean.getGcmexpired()) {
                    Log.i("Ayush", "GCM re-registry needed");
                    values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.GCM_FAILED);
                } else if (myBoolean.getOtherGCMExpired()) {
                    Log.i("Downloader", "SENDING GCM FAILED " + reachDatabase.getSenderId());
                    values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.GCM_FAILED);
                } else {
                    Log.i("Downloader", "GCM SENT " + reachDatabase.getSenderId());
                    values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.NOT_WORKING);
                }
                operations.add(getUpdateOperation(values, reachDatabase.getId()));
            }
            return operations;
        }

        @Override
        protected Void doInBackground(Void... params) {

            final Cursor cursor = MiscUtils.useContextFromContext(reference, activity -> {

                return activity.getContentResolver().query(
                        ReachDatabaseProvider.CONTENT_URI,
                        ReachDatabaseHelper.projection,
                        ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ? and " +
                                ReachDatabaseHelper.COLUMN_STATUS + " != ?",
                        new String[]{
                                "0", //only downloads
                                ReachDatabase.PAUSED_BY_USER + ""}, null); //should not be paused
            }).orNull();

            if (cursor == null)
                return null;

            final List<ReachDatabase> reachDatabaseList = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext())
                reachDatabaseList.add(ReachDatabaseHelper.cursorToProcess(cursor));
            cursor.close();

            if (reachDatabaseList.size() > 0) {

                final ArrayList<ContentProviderOperation> operations = bulkStartDownloads(reachDatabaseList);
                if (operations.size() > 0) {

                    MiscUtils.useContextFromContext(reference, activity -> {

                        try {
                            Log.i("Downloader", "Starting Download op " + operations.size());
                            activity.getContentResolver().applyBatch(ReachDatabaseProvider.AUTHORITY, operations);
                        } catch (RemoteException | OperationApplicationException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }

            return null;
        }
    }
}
