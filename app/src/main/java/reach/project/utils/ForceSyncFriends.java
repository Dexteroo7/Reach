package reach.project.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.text.TextUtils;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
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

    public ForceSyncFriends(Activity activity, long serverId) {
        this.activityWeakReference = new WeakReference<>(activity);
        this.serverId = serverId;
    }

    private boolean checkDead(Activity activity) {
        return (activity == null || activity.isFinishing());
    }

    @Override
    public void run() {

        Activity activity = activityWeakReference.get();
        if (checkDead(activity))
            return;
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
        /////phone numbers prepared
        final Cursor currentIds = resolver.query(
                ReachFriendsProvider.CONTENT_URI,
                new String[]{
                        ReachFriendsHelper.COLUMN_PHONE_NUMBER //String
                }, null, null, null);

        if (currentIds == null || currentIds.getCount() == 0)
            return;

        final List<String> presentNumbers = new ArrayList<>();
        while (currentIds.moveToNext())
            presentNumbers.add(currentIds.getString(0));
        currentIds.close();

        final Optional<List<Friend>> fullSync = MiscUtils.autoRetry(
                new DoWork<List<Friend>>() {
                    @Override
                    protected List<Friend> doWork() throws IOException {
                        return StaticData.userEndpoint.longSync(serverId).execute().getItems();
                    }
                }, Optional.<Predicate<List<Friend>>>absent());

        activity = activityWeakReference.get();
        if (checkDead(activity))
            return;
        if (!fullSync.isPresent())
            return; //nothing to do here

        final List<Friend> newFriends = fullSync.get();
        if (newFriends != null && newFriends.size() > 0)
            for (Friend friend : newFriends)
                presentNumbers.add(friend.getPhoneNumber());

        //remove all present phoneNumbers and sync phoneBook
        numbers.removeAll(presentNumbers);
        final Optional<List<Friend>> phoneBookSync = MiscUtils.autoRetry(
                new DoWork<List<Friend>>() {
                    @Override
                    protected List<Friend> doWork() throws IOException {
                        return StaticData.userEndpoint.phoneBookSync(ImmutableList.copyOf(numbers)).execute().getItems();
                    }
                }, Optional.<Predicate<List<Friend>>>absent());

        activity = activityWeakReference.get();
        if (checkDead(activity))
            return;

        final int size1 = (newFriends == null) ? 0 : newFriends.size();
        final int size2 = (phoneBookSync.isPresent()) ? phoneBookSync.get().size() : 0;
        final ContentValues[] values = new ContentValues[size1 + size2];

        if (size1 > 0)
            for (Friend friend : newFriends)
                values[0] = ReachFriendsHelper.contentValuesCreator(friend);
        if (size2 > 0)
            for (Friend friend : phoneBookSync.get())
                values[0] = ReachFriendsHelper.contentValuesCreator(friend);
        resolver.bulkInsert(ReachFriendsProvider.CONTENT_URI, values);
    }
}
