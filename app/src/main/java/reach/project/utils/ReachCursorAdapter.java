package reach.project.utils;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;

import com.google.common.base.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Created by dexter on 18/11/15.
 */
public abstract class ReachCursorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    @Nullable
    private Cursor cursor = null;
    private int oldCount = 0;

    @Override
    public long getItemId(int position) {

        if (cursor != null && cursor.moveToPosition(position))
            return getItemId(cursor);
        else
            return 0;
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
}
