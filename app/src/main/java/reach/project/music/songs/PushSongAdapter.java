package reach.project.music.songs;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import reach.project.R;
import reach.project.utils.MiscUtils;

/**
 * Created by dexter on 7/8/14.
 */
public class PushSongAdapter extends ResourceCursorAdapter {

    private final SparseBooleanArray booleanArray = new SparseBooleanArray();
    private final int pad = MiscUtils.dpToPx(5);

    public PushSongAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
    }

    public String[] getProjection() {
        return projection;
    }

    private final class ViewHolder{

        private final ImageView listToggle;
        private final TextView listTitle,listSubTitle,songDuration;

        private ViewHolder(ImageView listToggle, TextView listTitle, TextView listSubTitle, TextView songDuration) {
            this.listToggle = listToggle;
            this.listTitle = listTitle;
            this.listSubTitle = listSubTitle;
            this.songDuration = songDuration;
        }
    }

    private final String [] projection = new String[] {

            ReachSongHelper.COLUMN_ID,

            ReachSongHelper.COLUMN_SONG_ID,
            ReachSongHelper.COLUMN_DISPLAY_NAME,
            ReachSongHelper.COLUMN_ACTUAL_NAME,

            ReachSongHelper.COLUMN_ARTIST,
            ReachSongHelper.COLUMN_DURATION,
            ReachSongHelper.COLUMN_ALBUM,
            ReachSongHelper.COLUMN_SIZE,

    };

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        if(cursor == null) return;

        final ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.listTitle.setText(cursor.getString(2)); //displayName
        viewHolder.songDuration.setText(MiscUtils.combinationFormatter(cursor.getLong(5))); //duration
        /////////////
        viewHolder.listSubTitle.setText(cursor.getString(4)); //artist
        if(TextUtils.isEmpty(viewHolder.listSubTitle.getText()))
            viewHolder.listSubTitle.setText(cursor.getString(6)); //album
        /**
         * Using the sparse-array we are able to retain the
         * check marks when view is re-made
         */
        viewHolder.listToggle.setBackgroundResource(0);
        viewHolder.listToggle.setImageBitmap(null);

        if (booleanArray.get(getHashCode(cursor.getLong(7), cursor.getLong(1), cursor.getString(2), cursor.getString(3)), false)) {

            viewHolder.listToggle.setPadding(pad, pad, pad, pad);
            viewHolder.listToggle.setBackgroundResource(R.drawable.circular_background_dark);
            Picasso.with(context).load(R.drawable.check_white).noFade().into(viewHolder.listToggle);
        } else {

            viewHolder.listToggle.setPadding(0, 0, 0, 0);
            viewHolder.listToggle.setBackgroundResource(0);
            Picasso.with(context).load(R.drawable.add_grey).noFade().into(viewHolder.listToggle);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        final View view = super.newView(context, cursor, parent);
        final ViewHolder viewHolder = new ViewHolder(
                (ImageView) view.findViewById(R.id.listToggle),
                (TextView) view.findViewById(R.id.listTitle),
                (TextView) view.findViewById(R.id.listSubTitle),
                (TextView) view.findViewById(R.id.songDuration));

        view.setTag(viewHolder);
        return view;
    }

    public void cleanUp() {
        booleanArray.clear();
    }

    public boolean getCheck(int hashCode) {
        return booleanArray.get(hashCode, false);
    }

    public void setCheck(int hashCode, boolean status) {
        booleanArray.put(hashCode, status);
    }

    public int getHashCode(long size, long songId, String displayName, String actualName) {
        int result = (int) (size ^ (size >>> 32));
        result = 31 * result + (int) (songId ^ (songId >>> 32));
        result = 31 * result + displayName.hashCode();
        result = 31 * result + actualName.hashCode();
        return result;
    }
}
