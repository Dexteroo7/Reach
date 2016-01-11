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

final class ReachAllContactsAdapter extends ReachCursorAdapter<InviteViewHolder> {


    public ReachAllContactsAdapter(HandOverMessage<Cursor> handOverMessage, int resourceId) {
        super(handOverMessage, resourceId);
    }

    @Override
    public long getItemId(@Nonnull Cursor cursor) {

        return cursor.getLong(2); //CONTACT_ID
    }

    @Override
    public void onBindViewHolder(InviteViewHolder friendsViewHolder, Cursor cursor) {

        friendsViewHolder.userNameList.setText(cursor.getString(1));
        friendsViewHolder.subTitle.setText(cursor.getString(0));
        friendsViewHolder.userInitials.setText(MiscUtils.generateInitials(cursor.getString(1)));
        friendsViewHolder.profilePhotoList.setController(MiscUtils.getControllerwithResize(friendsViewHolder.profilePhotoList.getController(),
                Uri.withAppendedPath(ContentUris.
                                withAppendedId(ContactsContract.Contacts.CONTENT_URI, cursor.getLong(2)),
                        ContactsContract.Contacts.Photo.CONTENT_DIRECTORY), 100, 100));

        //TODO check from SharedPrefs
        /*if (contact.isInviteSent())
            friendsViewHolder.listToggle.setImageResource(R.drawable.icon_organize_tick_white);
        else
            friendsViewHolder.listToggle.setImageResource(R.drawable.add_pink);*/
    }

    @Override
    public InviteViewHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        return new InviteViewHolder(itemView, handOverMessage);
    }
}