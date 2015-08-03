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

import reach.backend.entities.userApi.model.ContactsWrapper;
import reach.backend.entities.userApi.model.Friend;
import reach.backend.entities.userApi.model.JsonMap;
import reach.backend.entities.userApi.model.QuickSync;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.utils.auxiliaryClasses.DoWork;

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

    public QuickSyncFriends(WeakReference<Activity> activity, long serverId, String myNumber) {
        this.activityWeakReference = activity;
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
        final List<Long> ids = new ArrayList<>();
        final List<Integer> hashes = new ArrayList<>();
        final List<String> presentNumbers = new ArrayList<>();

        final HashSet<Friend> newFriends = new HashSet<>();
        final HashSet<Long> toDelete = new HashSet<>();

        //TODO get all global profiles !
        numbers.add("8860872102");
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

        if (currentIds == null || currentIds.getCount() == 0) {
            new ForceSyncFriends(activityWeakReference, serverId, myNumber).run();
            return Status.FULL_SYNC;
        }


        while (currentIds.moveToNext()) {
            ids.add(currentIds.getLong(0));
            hashes.add(currentIds.getInt(1));
            presentNumbers.add(currentIds.getString(2));
        }
        currentIds.close();
        Log.i("Ayush", "Prepared callData quickSync" + ids.size() + " " + hashes.size());

        final QuickSync quickSync = MiscUtils.autoRetry(
                new DoWork<QuickSync>() {
                    @Override
                    public QuickSync doWork() throws IOException {
                        return StaticData.userEndpoint.quickSync(serverId, hashes, ids).execute();
                    }
                }, Optional.<Predicate<QuickSync>>absent()).orNull();

        activity = activityWeakReference.get();
        if (checkDead(activity))
            return Status.DEAD;

        if(quickSync != null) {

            if(quickSync.getNewFriends() != null)
                for (Friend friend : quickSync.getNewFriends()) {
                    presentNumbers.add(friend.getPhoneNumber());
                    newFriends.add(friend);
                }
            if (quickSync.getToUpdate() != null) {
                /**
                 * We update by deletion followed by insertion.
                 * Friends with hash = 0 are meant for removal
                 */
                for (Friend friend : quickSync.getToUpdate()) {

                    presentNumbers.add(friend.getPhoneNumber());
                    toDelete.add(friend.getId());

                    if (friend.getHash() != 0)
                        newFriends.add(friend); //its an update
                    //else it was a deletion
                }
            }
        }

        //remove all present phoneNumbers and sync phoneBook
        numbers.removeAll(presentNumbers);
        numbers.remove(myNumber);

        final Optional<List<Friend>> phoneBookSync = MiscUtils.autoRetry(
                new DoWork<List<Friend>>() {
                    final ContactsWrapper wrapper = new ContactsWrapper();
                    {
                        Log.i("Ayush", "Prepared callData phoneBookSync" + numbers.size());
                        wrapper.setContacts(ImmutableList.copyOf(numbers));
                    }
                    @Override
                    public List<Friend> doWork() throws IOException {
                        return StaticData.userEndpoint.phoneBookSync(wrapper).execute().getItems();
                    }
                }, Optional.<Predicate<List<Friend>>>absent());

        activity = activityWeakReference.get();
        if (checkDead(activity))
            return Status.DEAD;

        if (phoneBookSync.isPresent())
            newFriends.addAll(phoneBookSync.get());

        //START DB COMMITS
        bulkInsert(activity,
                resolver,
                newFriends,
                toDelete,
                quickSync != null ? quickSync.getNewStatus() : null);

        MiscUtils.closeAndIgnore(numbers, ids, presentNumbers, hashes, newFriends, toDelete);
        return Status.OK;
    }

    private void bulkInsert(Context context,
                           ContentResolver resolver,
                           Iterable<Friend> toInsert,
                           Iterable<Long> toDelete,
                           JsonMap statusChange) {

        final ReachFriendsHelper reachFriendsHelper = new ReachFriendsHelper(context);
        final SQLiteDatabase sqlDB = reachFriendsHelper.getWritableDatabase();
        sqlDB.beginTransaction();
        try {

            if (statusChange != null && statusChange.size() > 0) {

                ContentValues values = new ContentValues();
                for (Map.Entry<String, Object> newStatus : statusChange.entrySet()) {

                    values.put(ReachFriendsHelper.COLUMN_ID, newStatus.getKey());
                    values.put(ReachFriendsHelper.COLUMN_STATUS, newStatus.getValue() + "");
                    Log.i("Ayush", "Updating status " + newStatus.getKey() + " " + newStatus.getValue());
                    sqlDB.update(ReachFriendsHelper.FRIENDS_TABLE,
                            values,
                            ReachFriendsHelper.COLUMN_ID + " = " + newStatus.getKey(), null);
                }
                statusChange.clear();
            }

            for (Long id : toDelete) {
                Log.i("Ayush", "Deleting " + id);
                sqlDB.delete(ReachFriendsHelper.FRIENDS_TABLE,
                        ReachFriendsHelper.COLUMN_ID + "=" + id, null);
            }

            for (Friend friend : toInsert) {
                Log.i("Ayush", "Inserting " + friend.getUserName());
                final ContentValues values = ReachFriendsHelper.contentValuesCreator(friend);
                sqlDB.insert(ReachFriendsHelper.FRIENDS_TABLE, null, values);
            }
            sqlDB.setTransactionSuccessful();

        } finally {

            sqlDB.endTransaction();
            reachFriendsHelper.close();
        }
        resolver.notifyChange(ReachFriendsProvider.CONTENT_URI, null);
    }
}