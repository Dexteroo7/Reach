package reach.project.coreViews.friends.invite;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.View;

import javax.annotation.Nonnull;

import reach.project.utils.MiscUtils;
import reach.project.utils.ReachCursorAdapter;
import reach.project.utils.viewHelpers.HandOverMessage;

final class AllContactsAdapter extends ReachCursorAdapter<AllContactsViewHolder> {

    public AllContactsAdapter(HandOverMessage<Cursor> handOverMessage, int resourceId) {
        super(handOverMessage, resourceId);
    }

    @Override
    public long getItemId(@Nonnull Cursor cursor) {
        return cursor.getLong(2); //CONTACT_ID
    }

    @Override
    public void onBindViewHolder(AllContactsViewHolder friendsViewHolder, Cursor cursor) {
        friendsViewHolder.userNameList.setText(cursor.getString(1));
        friendsViewHolder.subTitle.setText(cursor.getString(0));
        friendsViewHolder.userInitials.setText(MiscUtils.generateInitials(cursor.getString(1)));
        friendsViewHolder.profilePhotoList.setController(MiscUtils.getControllerResize(friendsViewHolder.profilePhotoList.getController(),
                Uri.withAppendedPath(ContentUris.
                                withAppendedId(ContactsContract.Contacts.CONTENT_URI, cursor.getLong(2)),
                        ContactsContract.Contacts.Photo.CONTENT_DIRECTORY), 100, 100));
    }

    @Override
    public AllContactsViewHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        return new AllContactsViewHolder(itemView, handOverMessage);
    }
}