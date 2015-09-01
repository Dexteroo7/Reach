package reach.project.music.playLists;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import reach.project.R;

public class ReachPlayListsAdapter extends ResourceCursorAdapter {

    public ReachPlayListsAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
    }

    private final class ViewHolder{
        private final TextView listTitle,songs;
        private ViewHolder(TextView listTitle, TextView songs) {
            this.listTitle = listTitle;
            this.songs = songs;
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        if(cursor == null) return;

        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.songs.setText(cursor.getInt(0) + "");
        viewHolder.listTitle.setText(cursor.getString(1));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        final View view = super.newView(context, cursor, parent);
        final ViewHolder viewHolder = new ViewHolder(
                (TextView) view.findViewById(R.id.listTitle),
                (TextView) view.findViewById(R.id.songs));
        view.setTag(viewHolder);
        return view;
    }
}