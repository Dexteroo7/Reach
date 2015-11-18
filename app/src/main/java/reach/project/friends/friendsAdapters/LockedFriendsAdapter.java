package reach.project.friends.friendsAdapters;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import javax.annotation.Nonnull;

import reach.project.utils.ReachCursorAdapter;

/**
 * Created by dexter on 18/11/15.
 */
public final class LockedFriendsAdapter extends ReachCursorAdapter {

    @Override
    public int getItemId(@Nonnull Cursor cursor) {
        return cursor.getInt(0); //TODO shift to dirtyHash
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //create HorizontalViewHolder here
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        //utilize HorizontalViewHolder here
    }
}
