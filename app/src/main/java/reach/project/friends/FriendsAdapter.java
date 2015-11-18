package reach.project.friends;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import reach.backend.entities.userApi.model.Friend;
import reach.project.utils.ReachCursorAdapter;

/**
 * Can not use ReachCursor adapter as item type is Object not cursor
 * Created by dexter on 18/11/15.
 */
public class FriendsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    ///////////Vertical Cursor (parent)
    private static final byte VIEW_TYPE_FRIEND = 0;
    private static final byte VIEW_TYPE_LOCKED = 1;

    @Nullable
    private Cursor verticalCursor = null;
    private int oldParentCount = 0;

    public void setVerticalCursor(@Nullable Cursor cursor) {

        if (this.verticalCursor != null)
            this.verticalCursor.close();
        this.verticalCursor = cursor;

        if (cursor != null)
            notifyDataSetChanged();
        else
            notifyItemRangeRemoved(0, oldParentCount);
    }
    ///////////Vertical Cursor (parent)

    ///////////Horizontal Cursor
    private final LockedFriendsAdapter lockedFriendsAdapter = new LockedFriendsAdapter();

    public void setHorizontalCursor(@Nullable Cursor cursor) {
        lockedFriendsAdapter.setCursor(cursor);
    }
    ///////////Horizontal Cursor

    /**
     * Will either return Friend object OR flag for horizontal list
     *
     * @param position position to load
     * @return object
     */
    @Nonnull
    private Object getItem(int position) {

        if (position == 10 || verticalCursor == null)
            return false;

        else if (position < 10) {

            if (position == oldParentCount)
                return false;

            if (verticalCursor.moveToPosition(position))
                return verticalCursor;
            else
                return false;
        } else {

            final int relativePosition = position - 1;

            if (verticalCursor.moveToPosition(relativePosition))
                return verticalCursor;
            else
                return false;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case VIEW_TYPE_FRIEND: {

                break;
            }

            case VIEW_TYPE_LOCKED: {

                break;
            }
        }

        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final Object friend = getItem(position);
        if (friend instanceof Friend) {

            final Friend exactType = (Friend) friend;
            //use
        } else {

            //use horizontal adapter
        }
    }

    @Override
    public int getItemViewType(int position) {

        final Object item = getItem(position);
        if (item instanceof Friend)
            return VIEW_TYPE_FRIEND;
        else
            return VIEW_TYPE_LOCKED;
    }

    @Override
    public long getItemId(int position) {

        final Object item = getItem(position);
        if (item instanceof Friend)
            return ((Friend) item).getHash();
        else
            return 0;
    }

    @Override
    public int getItemCount() {

        if (verticalCursor != null)
            oldParentCount = verticalCursor.getCount();
        return oldParentCount + 1;
    }

    private final class LockedFriendsAdapter extends ReachCursorAdapter {

        @Override
        public int getItemId(@Nonnull Cursor cursor) {
            return cursor.getInt(0);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        }
    }
}
