package reach.project.coreViews.friends.friendsAdapters;

import android.database.Cursor;
import android.net.Uri;
import android.view.View;

import javax.annotation.Nonnull;

import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;
import reach.project.utils.ReachCursorAdapter;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 18/11/15.
 */
final class LockedFriendsAdapter extends ReachCursorAdapter<FriendsViewHolder> {

    public LockedFriendsAdapter(HandOverMessage<Cursor> handOverMessage, int resourceId) {
        super(handOverMessage, resourceId);
    }

    @Override
    public int getItemId(@Nonnull Cursor cursor) {
        return cursor.getInt(0); //TODO shift to dirtyHash
    }

    @Override
    public void onBindViewHolder(FriendsViewHolder friendsViewHolder, Cursor cursor) {

        friendsViewHolder.userNameList.setText(cursor.getString(2));
        friendsViewHolder.telephoneNumberList.setText(cursor.getInt(6) + "");
        friendsViewHolder.coverPic.setImageURI(Uri.parse("res:///" + MiscUtils.getRandomPic()));
        friendsViewHolder.profilePhotoList.setImageURI(Uri.parse(StaticData.cloudStorageImageBaseUrl + cursor.getString(3)));
        friendsViewHolder.lockIcon.setVisibility(View.VISIBLE);
    }

    @Override
    public FriendsViewHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        return new FriendsViewHolder(itemView, handOverMessage);
    }
}
