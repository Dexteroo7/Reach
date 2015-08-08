package reach.project.utils;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
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

import reach.backend.entities.userApi.model.ContactsWrapper;
import reach.backend.entities.userApi.model.Friend;
import reach.backend.entities.userApi.model.FriendCollection;
import reach.project.core.StaticData;
import reach.project.coreViews.ContactsListFragment;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.utils.auxiliaryClasses.DoWork;

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

    public ForceSyncFriends(WeakReference<Activity> activity, long serverId, String myNumber) {
        this.activityWeakReference = activity;
        this.serverId = serverId;
        this.myNumber = myNumber;
    }

    private boolean checkDead(Activity activity) {
        return (activity == null || activity.isFinishing());
    }

    @Override
    public void run() {

        final List<Friend> fullSync;
        /**
         * First we fetch the list of 'KNOWN' friends
         */
         fullSync = serverId == 0 ? null : MiscUtils.autoRetry(
                new DoWork<List<Friend>>() {
                    @Override
                    public List<Friend> doWork() throws IOException {
                        final FriendCollection collection = StaticData.userEndpoint.longSync(serverId).execute();
                        if(collection != null && collection.size() > 0)
                            return collection.getItems();
                        return null;
                    }
                }, Optional.<Predicate<List<Friend>>>absent()).orNull();


        Activity activity = activityWeakReference.get();
        if (checkDead(activity))
            return;

        /**
         * Now we collect the phoneNumbers on device
         */
        final HashSet<String> numbers = new HashSet<>();
        final List<String> presentNumbers;
        final List<ContentValues> values;

        //TODO get all global profiles !
        numbers.add("8860872102");

        final Cursor phoneNumbers = activity.getContentResolver().query(ContactsContract.
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

        /**
         * Now we remove the duplicate numbers and sync phoneBook
         */
        presentNumbers = new ArrayList<>();
        if(fullSync != null)
            for (Friend friend : fullSync)
                presentNumbers.add(friend.getPhoneNumber());

        //remove all present phoneNumbers and sync phoneBook
        numbers.removeAll(presentNumbers);
        numbers.remove(myNumber);

        final List<Friend> phoneBookSync = numbers.isEmpty() ? null : MiscUtils.autoRetry(

                new DoWork<List<Friend>>() {
                    final ContactsWrapper wrapper = new ContactsWrapper();
                    {
                        Log.i("Ayush", "Prepared callData phoneBookSync" + numbers.size());
                        wrapper.setContacts(ImmutableList.copyOf(numbers));
                    }
                    public List<Friend> doWork() throws IOException {
                        return StaticData.userEndpoint.phoneBookSync(wrapper).execute().getItems();
                    }
                }, Optional.<Predicate<List<Friend>>>absent()).orNull();

        activity = activityWeakReference.get();
        if (checkDead(activity))
            return;

        /**
         * Finally we insert the received contacts
         */
        final int size1 = (fullSync == null) ? 0 : fullSync.size();
        final int size2 = (phoneBookSync == null) ? 0 : phoneBookSync.size();
        Log.i("Ayush", "Sizes " + size1 + " " + size2);
        if (size1 + size2 == 0)
            return;

        values = new ArrayList<>();
        if (size1 > 0)
            for (Friend friend : fullSync) {
                Log.i("Ayush", friend.getUserName() + friend.getStatus() + " " + friend.getId());
                values.add(ReachFriendsHelper.contentValuesCreator(friend));
            }
        if (size2 > 0)
            for (Friend friend : phoneBookSync) {
                Log.i("Ayush", friend.getUserName() + friend.getStatus() + " " + friend.getId());
                values.add(ReachFriendsHelper.contentValuesCreator(friend));
            }

        Log.i("Ayush", "Starting insertion");
        Log.i("Ayush", "Inserting " + activity.getContentResolver().bulkInsert(ReachFriendsProvider.CONTENT_URI,
                values.toArray(new ContentValues[size1 + size2])));

        MiscUtils.closeAndIgnore(fullSync, numbers, values, presentNumbers);
    }
}
