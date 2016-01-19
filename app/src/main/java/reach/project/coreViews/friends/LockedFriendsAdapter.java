package reach.project.coreViews.friends;

import android.database.Cursor;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.facebook.imagepipeline.common.ResizeOptions;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.ReachCursorAdapter;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;

/**
 * Created by dexter on 18/11/15.
 */
final class LockedFriendsAdapter extends ReachCursorAdapter<FriendsViewHolder> implements MoreQualifier {

    private final ResizeOptions resizeOptions = new ResizeOptions(150, 150);

    public LockedFriendsAdapter(HandOverMessage<Cursor> handOverMessage, int resourceId) {
        super(handOverMessage, resourceId);
        setHasStableIds(true);
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

        Log.i(LockedFriendsAdapter.class.getName(), "Getting locked cursor id " + cursor.getLong(0));
        return cursor.getLong(0); //_id
    }

    @Override
    public int getItemCount() {

        final int count = super.getItemCount();
        return count > 4 ? 4 : count;
    }

    @Override
    public void onViewRecycled(FriendsViewHolder holder) {

        Log.i(LockedFriendsAdapter.class.getName(), "Recycling " + holder.getPositionItem());
        super.onViewRecycled(holder);
    }

    @Override
    public boolean onFailedToRecycleView(FriendsViewHolder holder) {

        Log.i(LockedFriendsAdapter.class.getName(), "onFailedToRecycleView " + holder.getPositionItem());
        return true;
    }

    public void onBindViewHolder(FriendsViewHolder friendsViewHolder, Cursor cursor) {

        friendsViewHolder.userNameList.setText(cursor.getString(2));
        friendsViewHolder.telephoneNumberList.setText(cursor.getInt(7) + "");
        friendsViewHolder.appCount.setText(cursor.getInt(8) + "");
        friendsViewHolder.coverPic.setController(MiscUtils.getControllerResize(friendsViewHolder.coverPic.getController(), Uri.parse(MiscUtils.getRandomPic()), resizeOptions));
        friendsViewHolder.profilePhotoList.setImageURI(AlbumArtUri.getUserImageUri(
                cursor.getLong(0),
                "imageId",
                "rw",
                true,
                150,
                150));

        friendsViewHolder.lockIcon.setVisibility(View.VISIBLE);
        
        Log.i(LockedFriendsAdapter.class.getName(), "Binding view holder " + friendsViewHolder.getPositionItem());
    }

    @Override
    public FriendsViewHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        final FriendsViewHolder friendsViewHolder = new FriendsViewHolder(itemView, handOverMessage);
        Log.i(LockedFriendsAdapter.class.getName(), "Creating view holder ");
        return friendsViewHolder;
    }

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        reference = adapterWeakReference;
    }
}
