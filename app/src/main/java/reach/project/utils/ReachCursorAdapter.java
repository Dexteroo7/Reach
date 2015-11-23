package reach.project.utils;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import reach.project.utils.viewHelpers.SingleItemViewHolder;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 18/11/15.
 */
public abstract class ReachCursorAdapter<T extends SingleItemViewHolder> extends RecyclerView.Adapter<T> implements HandOverMessage<Integer> {

    private final int resourceId;
    private final HandOverMessage<Cursor> handOverMessage;

    @Nullable
    private Cursor cursor = null;
    private int oldCount = 0;

    public ReachCursorAdapter(HandOverMessage<Cursor> handOverMessage, int resourceId) {

        this.handOverMessage = handOverMessage;
        this.resourceId = resourceId;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {

        if (cursor != null && cursor.moveToPosition(position))
            return getItemId(cursor);
        else
            return 0;
    }

    @Override
    public T onCreateViewHolder(ViewGroup parent, int viewType) {
        return getViewHolder(LayoutInflater.from(parent.getContext()).inflate(resourceId, parent, false), this);
    }

    public void setCursor(@Nullable Cursor cursor) {

        if (this.cursor != null)
            this.cursor.close();
        this.cursor = cursor;

        if (cursor != null)
            notifyDataSetChanged();
        else
            notifyItemRangeRemoved(0, oldCount);

    }

    public abstract T getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage);

    public abstract int getItemId(@Nonnull Cursor cursor);

    @Nonnull
    public Optional<Cursor> getItem(int position) {

        if (cursor == null || !cursor.moveToPosition(position))
            return Optional.absent();
        return Optional.of(cursor);
    }

    @Override
    public int getItemCount() {
        return oldCount = cursor != null ? cursor.getCount() : 1;
    }

    @Override
    public void handOverMessage(@NonNull Integer position) {

        if (cursor == null || !cursor.moveToPosition(position))
            throw new IllegalStateException("Resource cursor has been computed");

        handOverMessage.handOverMessage(cursor);
    }
}
