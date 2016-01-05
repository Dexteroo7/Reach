package reach.project.utils;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Optional;

import java.io.Closeable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Created by dexter on 18/11/15.
 */
public abstract class ReachCursorAdapter<T extends SingleItemViewHolder> extends RecyclerView.Adapter<T> implements HandOverMessage<Integer>, Closeable {

    private final int resourceId;
    private final HandOverMessage<Cursor> handOverMessage;

    @Nullable
    public Cursor getCursor() {
        return cursor;
    }

    @Nullable
    private Cursor cursor = null;
    private int oldCount = 0;

    @Override
    public void close() {
        MiscUtils.closeQuietly(cursor);
        cursor = null;
        oldCount = 0;
        notifyDataSetChanged();
    }

    public ReachCursorAdapter(HandOverMessage<Cursor> handOverMessage, int resourceId) {

        this.handOverMessage = handOverMessage;
        this.resourceId = resourceId;
        setHasStableIds(true);
    }

    public ReachCursorAdapter(HandOverMessage<Cursor> handOverMessage, int resourceId, Cursor cursor) {

        this.handOverMessage = handOverMessage;
        this.resourceId = resourceId;
        this.cursor = cursor;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {

        if (cursor != null && cursor.moveToPosition(position))
            return getItemId(cursor);
        else
            return super.getItemId(position);
    }

    @Override
    public T onCreateViewHolder(ViewGroup parent, int viewType) {
        return getViewHolder(LayoutInflater.from(parent.getContext()).inflate(resourceId, parent, false), this);
    }

    public void setCursor(@Nullable Cursor newCursor) {

        if (cursor != null)
            cursor.close();
        cursor = newCursor;

        if (cursor != null)
            notifyDataSetChanged();
        else
            notifyItemRangeRemoved(0, oldCount);
    }

    public abstract T getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage);
    public abstract void onBindViewHolder(T holder, Cursor item);

    @Override
    public void onBindViewHolder(T holder, int position) {

        final Optional<Cursor> cursorOptional = getItem(position);
        holder.bindPosition(position);

        if (!cursorOptional.isPresent())
            return;

        onBindViewHolder(holder, cursorOptional.get());
    }

    public abstract int getItemId(@Nonnull Cursor cursor);

    @Nonnull
    public Optional<Cursor> getItem(int position) {

//        Log.i("Ayush", "Moving cursor to " + position);
        if (cursor != null && cursor.moveToPosition(position))
            return Optional.of(cursor);
        return Optional.absent();
    }

    @Override
    public int getItemCount() {

//        final int count = oldCount = cursor != null ? cursor.getCount() : 1;
//        Log.i("Ayush", "New Count " + count);
        return oldCount = cursor != null ? cursor.getCount() : 1;
    }

    @Override
    public void handOverMessage(@NonNull Integer position) {

        if (cursor == null || !cursor.moveToPosition(position))
            throw new IllegalStateException("Resource cursor has been computed");

        handOverMessage.handOverMessage(cursor);
    }

    public int getResourceId() {
        return resourceId;
    }

    public HandOverMessage<Cursor> getHandOverMessage() {
        return handOverMessage;
    }
}