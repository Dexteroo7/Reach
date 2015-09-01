package reach.project.utils;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import reach.backend.entities.userApi.model.ContactsWrapper;
import reach.backend.entities.userApi.model.Friend;
import reach.backend.entities.userApi.model.FriendCollection;
import reach.project.core.StaticData;
import reach.project.friends.ReachFriendsProvider;
import reach.project.friends.ReachFriendsHelper;

/**
 * Created by dexter on 19/07/15.
 */
public class ForceSyncFriends implements Runnable {

    private final WeakReference<Context> reference;
    private final long serverId;
    private final String myNumber;

    public ForceSyncFriends(Activity activity, long serverId, String myNumber) {
        this.reference = new WeakReference<>(activity);
        this.serverId = serverId;
        this.myNumber = myNumber;
    }

    public ForceSyncFriends(WeakReference<Context> reference, long serverId, String myNumber) {
        this.reference = reference;
        this.serverId = serverId;
        this.myNumber = myNumber;
    }

    @Override
    public void run() {

        //First we fetch the list of 'KNOWN' friends
        final List<Friend> fullSync = serverId == 0 ? null : MiscUtils.autoRetry(
                () -> {
                    final FriendCollection collection = StaticData.userEndpoint.longSync(serverId).execute();
                    if (collection != null && collection.size() > 0)
                        return collection.getItems();
                    return null;
                }, Optional.<Predicate<List<Friend>>>absent()).orNull();

        //Now we collect the phoneNumbers on device
        final HashSet<String> numbers = new HashSet<>();
        numbers.add("8860872102"); //Devika
        //scan phoneBook, add if found
        MiscUtils.useContextFromContext(reference, context -> {
            numbers.addAll(MiscUtils.scanPhoneBook(context.getContentResolver()));
            return null;
        });
        Log.i("Ayush", "Prepared numbers" + numbers.size());

        //Now we remove the duplicate numbers and sync phoneBook
        if (fullSync != null)
            for (Friend friend : fullSync)
                numbers.remove(friend.getPhoneNumber());
        numbers.remove(myNumber); //sanity check

        final List<Friend> phoneBookSync;
        if (numbers.isEmpty())
            phoneBookSync = null;
        else {

            final ContactsWrapper wrapper = new ContactsWrapper();
            Log.i("Ayush", "Prepared callData phoneBookSync" + numbers.size());
            wrapper.setContacts(ImmutableList.copyOf(numbers));
            phoneBookSync = MiscUtils.autoRetry(() -> StaticData.userEndpoint.phoneBookSync(wrapper).execute().getItems(), Optional.absent()).orNull();
        }

        //Finally we insert the received contacts
        final int size1 = (fullSync == null) ? 0 : fullSync.size();
        final int size2 = (phoneBookSync == null) ? 0 : phoneBookSync.size();
        Log.i("Ayush", "Sizes " + size1 + " " + size2);
        if (size1 + size2 == 0)
            return;

        final List<ContentValues> values;
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

        MiscUtils.useContextFromContext(reference, context -> context.getContentResolver().bulkInsert(ReachFriendsProvider.CONTENT_URI,
                values.toArray(new ContentValues[size1 + size2])) > 0).or(false);

        MiscUtils.closeQuietly(fullSync, numbers, values);
    }
}