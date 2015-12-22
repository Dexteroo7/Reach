package reach.project.coreViews.friends.friendsAdapters;

import android.database.Cursor;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;
import reach.project.utils.ReachCursorAdapter;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;

/**
 * Created by dexter on 18/11/15.
 */
final class LockedFriendsAdapter extends ReachCursorAdapter<FriendsViewHolder> implements MoreQualifier {

    public LockedFriendsAdapter(HandOverMessage<Cursor> handOverMessage, int resourceId) {
        super(handOverMessage, resourceId);
    }

    @Nullable
    private WeakReference<RecyclerView.Adapter> reference = null;

    @Override
    public void setCursor(@Nullable Cursor newCursor) {
        super.setCursor(newCursor);

        final RecyclerView.Adapter adapter;
        if (reference != null && (adapter = reference.get()) != null)
            adapter.notifyDataSetChanged();
    }

    @Override
    public int getItemId(@Nonnull Cursor cursor) {
        return cursor.getInt(0); //TODO shift to dirtyHash
    }

    @Override
    public int getItemCount() {

        final int count = super.getItemCount();
        return count > 4 ? 4 : count;
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

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        reference = adapterWeakReference;
    }
}
