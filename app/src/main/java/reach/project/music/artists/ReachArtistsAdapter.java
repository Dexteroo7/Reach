package reach.project.music.artists;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import reach.project.R;

/**
 * Created by dexter on 8/8/14.
 */
public class ReachArtistsAdapter extends ResourceCursorAdapter {

    public ReachArtistsAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
    }

    private final class ViewHolder{
//        private final ImageView listImage;
        private final TextView listTitle,songs,listSubTitle;

        private ViewHolder(TextView listTitle, TextView songs, TextView listSubTitle) {
//            this.listImage = listImage;
            this.listTitle = listTitle;
            this.songs = songs;
            this.listSubTitle = listSubTitle;
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        if(cursor == null) return;
        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.listSubTitle.setText(cursor.getString(1));
        viewHolder.listTitle.setText(cursor.getString(2));
        viewHolder.songs.setText(cursor.getInt(4) + "");
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        final View view = super.newView(context, cursor, parent);
        final ViewHolder viewHolder = new ViewHolder(
//                (ImageView) view.findViewById(R.id.listImage),
                (TextView) view.findViewById(R.id.listTitle),
                (TextView) view.findViewById(R.id.songs),
                (TextView) view.findViewById(R.id.listSubTitle));
        view.setTag(viewHolder);
        return view;
    }
}