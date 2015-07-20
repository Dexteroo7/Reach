package reach.project.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import reach.backend.entities.userApi.model.Friend;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.sql.ReachFriendsHelper;

/**
 * Created by dexter on 19/07/15.
 */
public class ForceSyncFriends implements Runnable {

    private final WeakReference<Activity> activityWeakReference;
    private final long serverId;
    private final String myNumber;

    public ForceSyncFriends(Activity activity, long serverId, String myNumber) {
        this.activityWeakReference = new WeakReference<>(activity);
        this.serverId = serverId;
        this.myNumber = myNumber;
    }

    private boolean checkDead(Activity activity) {
        return (activity == null || activity.isFinishing());
    }

    @Override
    public void run() {

        Activity activity = activityWeakReference.get();
        if (checkDead(activity))
            return;
        ContentResolver resolver = activity.getContentResolver();

        final List<String> numbers = new ArrayList<>();
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
        final List<Friend> fullSync = serverId == 0 ? null : MiscUtils.autoRetry(
                new DoWork<List<Friend>>() {
                    @Override
                    protected List<Friend> doWork() throws IOException {
                        return StaticData.userEndpoint.longSync(serverId).execute().getItems();
                    }
                }, Optional.<Predicate<List<Friend>>>absent()).orNull();

        activity = activityWeakReference.get();
        if (checkDead(activity))
            return;

        final List<Friend> newFriends = new ArrayList<>();
        final List<String> presentNumbers = new ArrayList<>();
        if(fullSync != null)
            newFriends.addAll(fullSync);

        if (newFriends.size() > 0)
            for (Friend friend : newFriends)
                presentNumbers.add(friend.getPhoneNumber());

        Log.i("Ayush", "Duplicate numbers removed");
        //remove all present phoneNumbers and sync phoneBook
        numbers.removeAll(presentNumbers);
        numbers.remove(myNumber);
        final List<Friend> phoneBookSync = MiscUtils.autoRetry(

                new DoWork<List<Friend>>() {

                    protected List<Friend> doWork() throws IOException {
                        return StaticData.userEndpoint.phoneBookSync(numbers).execute().getItems();
                    }
                }, Optional.<Predicate<List<Friend>>>absent()).orNull();

        activity = activityWeakReference.get();
        if (checkDead(activity))
            return;
        resolver = activity.getContentResolver();

        final int size1 = newFriends.size();
        final int size2 = (phoneBookSync == null) ? 0 : phoneBookSync.size();
        Log.i("Ayush", "Sizes " + size1 + " " + size2);
        if (size1 + size2 == 0)
            return;

        final List<ContentValues> values = new ArrayList<>();
        if (size1 > 0)
            for (Friend friend : newFriends) {
                Log.i("Ayush", friend.getUserName() + friend.getStatus());
                values.add(ReachFriendsHelper.contentValuesCreator(friend));
            }
        if (size2 > 0)
            for (Friend friend : phoneBookSync) {
                Log.i("Ayush", friend.getUserName() + friend.getStatus() + " phone book");
                values.add(ReachFriendsHelper.contentValuesCreator(friend));
            }
        Log.i("Ayush", "Starting insertion");
        Log.i("Ayush", "Inserting " + resolver.bulkInsert(ReachFriendsProvider.CONTENT_URI,
                values.toArray(new ContentValues[size1 + size2])));
    }
}
