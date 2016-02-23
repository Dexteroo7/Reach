package reach.project.utils;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.common.base.Optional;
import com.google.gson.Gson;

import java.io.Closeable;
import java.io.DataInputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import reach.backend.entities.messaging.model.MyBoolean;
import reach.backend.entities.userApi.model.MyString;
import reach.project.ancillaryViews.UpdateFragment;
import reach.project.core.StaticData;
import reach.project.music.ReachDatabase;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.reachProcess.auxiliaryClasses.Connection;
import reach.project.reachProcess.reachService.ProcessManager;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 31/12/15.
 */
public enum FireOnce implements Closeable {

    INSTANCE;

    private static final ExecutorService contactSyncService = MiscUtils.getRejectionExecutor();
    private static final ExecutorService pingService = MiscUtils.getRejectionExecutor();
    private static final ExecutorService refreshDownloadOpsService = MiscUtils.getRejectionExecutor();
    private static final ExecutorService checkGCMService = MiscUtils.getRejectionExecutor();
    private static final ExecutorService checkUpdateService = MiscUtils.getRejectionExecutor();

    @Override
    public void close() {

        if (syncingContacts != null)
            syncingContacts.cancel(true);
        syncingContacts = null;

        contactSyncService.shutdownNow();
        pingService.shutdownNow();
        refreshDownloadOpsService.shutdownNow();
        checkGCMService.shutdownNow();
        checkUpdateService.shutdownNow();
    }

    @Nullable
    private static Future syncingContacts = null;

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
                            (System.currentTimeMillis() - StaticData.ONLINE_LIMIT) + ""});
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

    public static void refreshOperations(WeakReference<? extends Context> reference) {
        new RefreshOperations(reference).executeOnExecutor(refreshDownloadOpsService);
    }

    public static void checkGCM(WeakReference<Context> reference, long serverId) {
        checkGCMService.submit(new CheckGCM(serverId, reference));
    }

    public static void checkUpdate(WeakReference<? extends AppCompatActivity> activityWeakReference) {
        new CheckUpdate().executeOnExecutor(checkUpdateService, activityWeakReference);
    }

    //////////////////////////////

    private static class CheckUpdate extends AsyncTask<WeakReference, Void, WeakReference<AppCompatActivity>> {

        @Override
        protected WeakReference<AppCompatActivity> doInBackground(WeakReference... params) {

            int latestVersion = 0;
            DataInputStream inputStream = null;
            try {
                inputStream = new DataInputStream(new URL(StaticData.DROP_BOX).openStream());
                latestVersion = inputStream.readInt();
            } catch (Exception ignored) {
            } finally {
                MiscUtils.closeQuietly(inputStream);
            }

            //noinspection unchecked
            final WeakReference<AppCompatActivity> reference = params[0];

            final Integer currentVersion = MiscUtils.useActivityWithResult(reference, activity -> {
                try {
                    return activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                return Integer.MAX_VALUE; //prevent accidental update dialog
            }).or(Integer.MAX_VALUE); //prevent accidental update dialog

            if (latestVersion > currentVersion)
                return reference;
            return null; //don't show dialog
        }

        @Override
        protected void onPostExecute(WeakReference<AppCompatActivity> activityWeakReference) {

            super.onPostExecute(activityWeakReference);

            if (activityWeakReference == null) //no need to show dialog
                return;

            MiscUtils.useActivity(activityWeakReference, activity -> {

                final UpdateFragment updateFragment = new UpdateFragment();
                updateFragment.setCancelable(false);
                try {
                    updateFragment.show(activity.getSupportFragmentManager(), "update");
                } catch (IllegalStateException | WindowManager.BadTokenException ignored) {
                    activity.finish();
                }
            });
        }
    }

    private static final class CheckGCM implements Runnable {

        private final long serverId;
        private final WeakReference<Context> contextWeakReference;

        private CheckGCM(long serverId, WeakReference<Context> contextWeakReference) {
            this.serverId = serverId;
            this.contextWeakReference = contextWeakReference;
        }

        @Override
        public void run() {

            if (serverId == 0)
                return;

            final int version = MiscUtils.useContextFromContext(contextWeakReference, context -> {
                try {
                    return context.getPackageManager()
                            .getPackageInfo(context.getPackageName(), 0).versionCode;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                return 0;
            }).or(0);

            final MyString dataToReturn = MiscUtils.autoRetry(() ->
                    StaticData.USER_API.getGcmIdAndUpdateAppVersion(serverId, version + "").execute(), Optional.absent()).orNull();

            //check returned gcm
            final String gcmId;
            if (dataToReturn == null || //fetch failed
                    TextUtils.isEmpty(gcmId = dataToReturn.getString()) || //null gcm
                    gcmId.equals("hello_world")) { //bad gcm

                //network operation
                if (updateGCM(serverId, contextWeakReference))
                    Log.i("Ayush", "GCM updated !");
                else
                    Log.i("Ayush", "GCM check failed");
            }
        }
    }

    /**
     * @param id        id of the person to update gcm of
     * @param reference the context reference
     * @param <T>       something which extends context
     * @return false : failed, true : OK
     */
    private static <T extends Context> boolean updateGCM(final long id, final WeakReference<T> reference) {

        final String regId = MiscUtils.autoRetry(() -> {

            final Context context;
            if (reference == null || (context = reference.get()) == null)
                return "QUIT";
            return GoogleCloudMessaging.getInstance(context)
                    .register("528178870551");
        }, Optional.of(TextUtils::isEmpty)).orNull();

        if (TextUtils.isEmpty(regId) || regId.equals("QUIT"))
            return false;
        //if everything is fine, send to server
        Log.i("Ayush", "Uploading newGcmId to server");
        final Boolean result = MiscUtils.autoRetry(() -> {

            StaticData.USER_API.setGCMId(id, regId).execute();
            Log.i("Ayush", regId.substring(0, 5) + "NEW GCM ID AFTER CHECK");
            return true;
        }, Optional.absent()).orNull();
        //set locally
        return !(result == null || !result);
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
                    new String[]{ReachFriendsHelper.ONLINE_REQUEST_GRANTED + "",
                            (System.currentTimeMillis() - StaticData.ONLINE_LIMIT) + "", StaticData.DEVIKA + ""});
            contentValues.clear();

            final HandOverMessage handOverMessage;
            if (handOverMessageWeakReference != null && (handOverMessage = handOverMessageWeakReference.get()) != null)
                handOverMessage.handOverMessage(true); //done
        }
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
                    .newUpdate(Uri.parse(SongProvider.CONTENT_URI + "/" + id))
                    .withValues(contentValues)
                    .withSelection(SongHelper.COLUMN_ID + " = ? and " +
                                    SongHelper.COLUMN_STATUS + " != ? and " +
                                    SongHelper.COLUMN_STATUS + " != ? and " +
                                    SongHelper.COLUMN_STATUS + " != ? and " +
                                    SongHelper.COLUMN_STATUS + " != ?",
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
                    .newUpdate(Uri.parse(SongProvider.CONTENT_URI + "/" + id))
                    .withValues(contentValues)
                    .withSelection(SongHelper.COLUMN_ID + " = ?",
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

                        values.put(SongHelper.COLUMN_STATUS, ReachDatabase.FINISHED);
                        values.put(SongHelper.COLUMN_PROCESSED, reachDatabase.getLength());
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
                    values.put(SongHelper.COLUMN_STATUS, ReachDatabase.GCM_FAILED);
                } else if (myBoolean.getGcmexpired()) {
                    Log.i("Ayush", "GCM re-registry needed");
                    values.put(SongHelper.COLUMN_STATUS, ReachDatabase.GCM_FAILED);
                } else if (myBoolean.getOtherGCMExpired()) {
                    Log.i("Downloader", "SENDING GCM FAILED " + reachDatabase.getSenderId());
                    values.put(SongHelper.COLUMN_STATUS, ReachDatabase.GCM_FAILED);
                } else {
                    Log.i("Downloader", "GCM SENT " + reachDatabase.getSenderId());
                    values.put(SongHelper.COLUMN_STATUS, ReachDatabase.NOT_WORKING);
                }
                operations.add(getUpdateOperation(values, reachDatabase.getId()));
            }
            return operations;
        }

        @Override
        protected Void doInBackground(Void... params) {

            final Cursor cursor = MiscUtils.useContextFromContext(reference, activity -> {

                return activity.getContentResolver().query(
                        SongProvider.CONTENT_URI,
                        SongHelper.projection,
                        SongHelper.COLUMN_OPERATION_KIND + " = ? and " +
                                SongHelper.COLUMN_STATUS + " != ?",
                        new String[]{
                                "0", //only downloads
                                ReachDatabase.PAUSED_BY_USER + ""}, null); //should not be paused
            }).orNull();

            if (cursor == null)
                return null;

            final List<ReachDatabase> reachDatabaseList = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext())
                reachDatabaseList.add(SongHelper.cursorToProcess(cursor));
            cursor.close();

            if (reachDatabaseList.size() > 0) {

                final ArrayList<ContentProviderOperation> operations = bulkStartDownloads(reachDatabaseList);
                if (operations.size() > 0) {

                    MiscUtils.useContextFromContext(reference, context -> {

                        try {
                            Log.i("Downloader", "Starting Download op " + operations.size());
                            context.getContentResolver().applyBatch(SongProvider.AUTHORITY, operations);
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
