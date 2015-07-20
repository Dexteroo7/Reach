package reach.project.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import reach.backend.entities.userApi.model.Friend;
import reach.backend.entities.userApi.model.JsonMap;
import reach.backend.entities.userApi.model.QuickSync;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.sql.ReachFriendsHelper;

/**
 * Created by dexter on 19/07/15.
 */
public class QuickSyncFriends implements Callable<QuickSyncFriends.Status> {

    public enum Status {
        DEAD,
        OK,
        FULL_SYNC
    }

    private final WeakReference<Activity> activityWeakReference;
    private final long serverId;
    private final String myNumber;

    public QuickSyncFriends(Activity activity, long serverId, String myNumber) {
        this.activityWeakReference = new WeakReference<>(activity);
        this.serverId = serverId;
        this.myNumber = myNumber;
    }

    private boolean checkDead(Activity activity) {
        return (activity == null || activity.isFinishing());
    }

    @Override
    public QuickSyncFriends.Status call() {

        Activity activity = activityWeakReference.get();
        if (checkDead(activity))
            return Status.DEAD;
        final ContentResolver resolver = activity.getContentResolver();

        final HashSet<String> numbers = new HashSet<>();
        numbers.add("000000001");
        final Cursor phoneNumbers = resolver.query(ContactsContract.
                CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        if (phoneNumbers != null) {

            while (phoneNumbers.moveToNext()) {

                final int columnIndex = phoneNumbers.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                if (columnIndex == -1)
                    continue;
                String phoneNumber = phoneNumbers.getString(columnIndex);
                if (TextUtils.isEmpty(phoneNumber))
                    continue;
                phoneNumber = phoneNumber.replaceAll("[^0-9]", "");
                final int length;
                if (TextUtils.isEmpty(phoneNumber) || (length = phoneNumber.length()) < 10)
                    continue;
                numbers.add(phoneNumber.substring(length - 10, length)); //take last 10
            }
            phoneNumbers.close();
        }
        Log.i("Ayush", "Prepared numbers" + numbers.size());
        /////phone numbers prepared
        final Cursor currentIds = resolver.query(
                ReachFriendsProvider.CONTENT_URI,
                new String[]{
                        ReachFriendsHelper.COLUMN_ID, //long
                        ReachFriendsHelper.COLUMN_HASH, //int
                        ReachFriendsHelper.COLUMN_PHONE_NUMBER //int
                }, null, null, null);

        if (currentIds == null || currentIds.getCount() == 0)
            return Status.FULL_SYNC;

        final List<Long> ids = new ArrayList<>();
        final List<Integer> hashes = new ArrayList<>();
        final List<String> presentNumbers = new ArrayList<>();
        while (currentIds.moveToNext()) {
            ids.add(currentIds.getLong(0));
            hashes.add(currentIds.getInt(1));
            presentNumbers.add(currentIds.getString(2));
        }
        currentIds.close();
        Log.i("Ayush", "Prepared callData" + ids.size() + " " + hashes.size() + " | " + presentNumbers.size());

        final QuickSync quickSync = MiscUtils.autoRetry(
                new DoWork<QuickSync>() {
                    @Override
                    protected QuickSync doWork() throws IOException {
                        return StaticData.userEndpoint.quickSync(serverId, hashes, ids).execute();
                    }
                }, Optional.<Predicate<QuickSync>>absent()).orNull();

        activity = activityWeakReference.get();
        if (checkDead(activity))
            return Status.DEAD;

        final List<Friend> newFriends = new ArrayList<>();
        if(quickSync != null && quickSync.getNewFriends() != null)
            newFriends.addAll(quickSync.getNewFriends());

        if (newFriends.size() > 0) {
            Log.i("Ayush", "Found new friends " + newFriends.size());
            for (Friend friend : newFriends)
                presentNumbers.add(friend.getPhoneNumber());
        }

        //remove all present phoneNumbers and sync phoneBook
        numbers.removeAll(presentNumbers);
        numbers.remove(myNumber);
        final Optional<List<Friend>> phoneBookSync = MiscUtils.autoRetry(
                new DoWork<List<Friend>>() {
                    @Override
                    protected List<Friend> doWork() throws IOException {
                        return StaticData.userEndpoint.phoneBookSync(ImmutableList.copyOf(numbers)).execute().getItems();
                    }
                }, Optional.<Predicate<List<Friend>>>absent());

        activity = activityWeakReference.get();
        if (checkDead(activity))
            return Status.DEAD;

        final List<Friend> toInsert = new ArrayList<>();
        final List<Long> toDelete = new ArrayList<>();

        if (phoneBookSync.isPresent())
            toInsert.addAll(phoneBookSync.get());
        if (quickSync != null && quickSync.getToUpdate() != null)
            for (Friend friend : quickSync.getToUpdate()) {
                toDelete.add(friend.getId());
                toInsert.add(friend);
            }

        //START DB COMMITS
        bulkInsert(activity,
                resolver,
                toInsert,
                toDelete,
                quickSync != null ? quickSync.getNewStatus() : null);

        return Status.OK;
    }

    public void bulkInsert(Context context,
                           ContentResolver resolver,
                           List<Friend> toInsert,
                           List<Long> toDelete,
                           JsonMap statusChange) {

        final ReachFriendsHelper reachFriendsHelper = new ReachFriendsHelper(context);
        final SQLiteDatabase sqlDB = reachFriendsHelper.getWritableDatabase();
        sqlDB.beginTransaction();
        try {

            if (toInsert != null && toInsert.size() > 0)
                for (Friend friend : toInsert) {
                    Log.i("Ayush", "Inserting " + friend.getUserName());
                    final ContentValues values = ReachFriendsHelper.contentValuesCreator(friend);
                    sqlDB.insert(ReachFriendsHelper.FRIENDS_TABLE, null, values);
                }

            if (toDelete != null && toDelete.size() > 0)
                for (Long id : toDelete) {
                    Log.i("Ayush", "Deleting " + id);
                    sqlDB.delete(ReachFriendsHelper.FRIENDS_TABLE,
                            ReachFriendsHelper.COLUMN_ID + "=" + id, null);
                }

            if (statusChange != null && statusChange.size() > 0)
                for (Map.Entry<String, Object> newStatus : statusChange.entrySet()) {
                    final ContentValues values = new ContentValues();
                    values.put(newStatus.getKey(), (Short) newStatus.getValue());
                    Log.i("Ayush", "Updating status " + newStatus.getKey() + " " + newStatus.getValue());
                    sqlDB.update(ReachFriendsHelper.FRIENDS_TABLE,
                            values,
                            ReachFriendsHelper.COLUMN_ID + "=" + newStatus.getKey(), null);
                }
            sqlDB.setTransactionSuccessful();
        } finally {
            sqlDB.endTransaction();
        }
        resolver.notifyChange(ReachFriendsProvider.CONTENT_URI, null);
    }
}
