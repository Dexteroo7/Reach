package reach.project.coreViews.friends.friendsAdapters;

import android.database.Cursor;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import reach.project.utils.AlbumArtUri;
import reach.project.utils.ReachCursorAdapter;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;

/**
 * Created by dexter on 18/11/15.
 */
final class LockedFriendsAdapter extends ReachCursorAdapter<LockedFriendsViewHolder> implements MoreQualifier {

    public LockedFriendsAdapter(HandOverMessage<Cursor> handOverMessage, int resourceId) {
        super(handOverMessage, resourceId);
    }

    @Nullable
    private WeakReference<RecyclerView.Adapter> reference = null;

    @Override
    public void setCursor(@Nullable Cursor newCursor) {
        super.setCursor(newCursor);

        final RecyclerView.Adapter adapter;
        if (reference != null && (adapter = reference.get()) != null) {

            //must set the new cursor
            ((ReachCursorAdapter) adapter).setCursor(newCursor);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public long getItemId(@Nonnull Cursor cursor) {
        return cursor.getLong(0); //_id
    }

    @Override
    public int getItemCount() {

        final int count = super.getItemCount();
        return count > 4 ? 4 : count;
    }

    public void onBindViewHolder(LockedFriendsViewHolder friendsViewHolder, Cursor cursor) {

        friendsViewHolder.userNameList.setText(cursor.getString(2));
        friendsViewHolder.telephoneNumberList.setText(cursor.getInt(7) + "");
        friendsViewHolder.appCount.setText(cursor.getInt(8) + "");
        friendsViewHolder.coverPic.setImageURI(Uri.parse(cursor.getString(5)));
        //friendsViewHolder.coverPic.setController(MiscUtils.getControllerResize(friendsViewHolder.coverPic.getController(), Uri.parse(MiscUtils.getRandomPic()), 200, 200));

        friendsViewHolder.profilePhotoList.setImageURI(AlbumArtUri.getUserImageUri(
                cursor.getLong(0),
                "imageId",
                "rw",
                true,
                200,
                200));

        friendsViewHolder.lockIcon.setVisibility(View.VISIBLE);
    }

    @Override
    public LockedFriendsViewHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        return new LockedFriendsViewHolder(itemView, handOverMessage);
    }

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        reference = adapterWeakReference;
    }
}
