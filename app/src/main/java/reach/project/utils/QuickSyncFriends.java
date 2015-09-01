package reach.project.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.util.LongSparseArray;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import reach.backend.entities.userApi.model.ContactsWrapper;
import reach.backend.entities.userApi.model.Friend;
import reach.backend.entities.userApi.model.JsonMap;
import reach.backend.entities.userApi.model.QuickSync;
import reach.project.core.StaticData;
import reach.project.friends.ReachFriendsProvider;
import reach.project.friends.ReachFriendsHelper;

/**
 * Created by dexter on 19/07/15.
 */
public class QuickSyncFriends implements Callable<QuickSyncFriends.Status> {

    public enum Status {
        DEAD,
        OK,
        FULL_SYNC
    }

    private final WeakReference<Context> reference;
    private final long serverId;
    private final String myNumber;

    public QuickSyncFriends(Activity activity, long serverId, String myNumber) {
        this.reference = new WeakReference<>(activity);
        this.serverId = serverId;
        this.myNumber = myNumber;
    }

    @Override
    public QuickSyncFriends.Status call() {

        //Now we collect the phoneNumbers on device
        final HashSet<String> numbers = new HashSet<>();
        numbers.add("8860872102"); //Devika
        //scan phoneBook, add if found
        MiscUtils.useContextFromContext(reference, context -> {
            numbers.addAll(MiscUtils.scanPhoneBook(context.getContentResolver()));
            return null;
        });
        Log.i("Ayush", "Prepared numbers" + numbers.size());

        //prepare data for quickSync call
        final Cursor currentIds = MiscUtils.useContextFromContext(reference, context -> context.getContentResolver().query(
                ReachFriendsProvider.CONTENT_URI,
                new String[]{
                        ReachFriendsHelper.COLUMN_ID, //long
                        ReachFriendsHelper.COLUMN_HASH, //int
                        ReachFriendsHelper.COLUMN_PHONE_NUMBER, //int
                        ReachFriendsHelper.COLUMN_NUMBER_OF_SONGS, //old number of songs
                }, null, null, null)).orNull();

        //if no previous data found run full sync
        if (currentIds == null || currentIds.getCount() == 0) {
            new ForceSyncFriends(reference, serverId, myNumber).run();
            return Status.FULL_SYNC;
        }

        //add data for quickSync
        final List<Long> ids = new ArrayList<>();
        final List<Integer> hashes = new ArrayList<>();
        final LongSparseArray<Integer> oldNumberOfSongs = new LongSparseArray<>();

        while (currentIds.moveToNext()) {

            final long id = currentIds.getLong(0); //id
            ids.add(id);
            hashes.add(currentIds.getInt(1)); //hash
            numbers.remove(currentIds.getString(2)); //phoneNumber
            oldNumberOfSongs.append(id, currentIds.getInt(3)); //old numberOfSongs
        }
        currentIds.close();

        //run Quick sync
        final HashSet<Friend> newFriends = new HashSet<>();
        final HashSet<Long> toDelete = new HashSet<>();
        Log.i("Ayush", "Prepared callData quickSync" + ids.size() + " " + hashes.size());
        final QuickSync quickSync = MiscUtils.autoRetry(() -> StaticData.userEndpoint.quickSync(serverId, hashes, ids).execute(), Optional.<Predicate<QuickSync>>absent()).orNull();
        if (quickSync != null) {

            Log.i("Ayush", "Found quick sync");

            if (quickSync.getNewFriends() != null)
                for (Friend friend : quickSync.getNewFriends()) {

                    numbers.remove(friend.getPhoneNumber());
                    newFriends.add(friend);
                }

            if (quickSync.getToUpdate() != null) {
                /**
                 * We update by deletion followed by insertion.
                 * Friends with hash = 0 are meant for removal
                 */
                for (Friend friend : quickSync.getToUpdate()) {

                    numbers.remove(friend.getPhoneNumber());
                    toDelete.add(friend.getId());

                    if (friend.getHash() != 0)
                        newFriends.add(friend); //its an update
                    //else it was a deletion
                }
            }
        }

        //removed all present phoneNumbers, now sync phoneBook
        numbers.remove(myNumber);
        if (!numbers.isEmpty()) {

            final ContactsWrapper wrapper = new ContactsWrapper();
            Log.i("Ayush", "Prepared callData phoneBookSync" + numbers.size());
            wrapper.setContacts(ImmutableList.copyOf(numbers));
            //TODO test
            newFriends.addAll(MiscUtils.autoRetry(() -> StaticData.userEndpoint.phoneBookSync(wrapper).execute().getItems(), Optional.absent()).or(Collections.EMPTY_LIST));
        }

        //START DB COMMITS
        MiscUtils.useContextFromContext(reference, context -> {

            bulkInsert(context,
                    context.getContentResolver(),
                    newFriends,
                    toDelete,
                    quickSync != null ? quickSync.getNewStatus() : null,
                    oldNumberOfSongs);
            return null;
        });

        MiscUtils.closeQuietly(numbers, ids, hashes, newFriends, toDelete);
        return Status.OK;
    }

    private void bulkInsert(Context context,
                            ContentResolver resolver,
                            Iterable<Friend> toInsert,
                            Iterable<Long> toDelete,
                            JsonMap statusChange,
                            LongSparseArray<Integer> oldNumberOfSongs) {

        final ReachFriendsHelper reachFriendsHelper = new ReachFriendsHelper(context);
        final SQLiteDatabase sqlDB = reachFriendsHelper.getWritableDatabase();
        sqlDB.beginTransaction();
        try {

            if (statusChange != null && statusChange.size() > 0) {

                ContentValues values = new ContentValues();
                for (Map.Entry<String, Object> newStatus : statusChange.entrySet()) {

                    values.put(ReachFriendsHelper.COLUMN_ID, newStatus.getKey());
                    values.put(ReachFriendsHelper.COLUMN_STATUS, newStatus.getValue() + "");
//                    Log.i("Ayush", "Updating status " + newStatus.getKey() + " " + newStatus.getValue());
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
                final ContentValues values = ReachFriendsHelper.contentValuesCreator(friend, oldNumberOfSongs.get(friend.getId(), 0));
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