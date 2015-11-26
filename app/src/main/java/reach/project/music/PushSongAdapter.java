package reach.project.music;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import reach.project.R;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.utils.MiscUtils;

/**
 * Created by dexter on 7/8/14.
 */
public class PushSongAdapter extends ResourceCursorAdapter {

    private final IsItemSelected itemSelected;
    private final int pad = MiscUtils.dpToPx(5);

    public PushSongAdapter(Context context, int layout, Cursor c, int flags, IsItemSelected itemSelected) {
        super(context, layout, c, flags);
        this.itemSelected = itemSelected;
    }

    public String[] getProjectionMyLibrary() {
        return projectionMyLibrary;
    }

    public String[] getProjectionDownloaded() {
        return projectionDownloaded;
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

    private final String [] projectionMyLibrary = new String[] {

            MySongsHelper.COLUMN_ID, //0

            MySongsHelper.COLUMN_SONG_ID, //1
            MySongsHelper.COLUMN_DISPLAY_NAME, //2
            MySongsHelper.COLUMN_ACTUAL_NAME, //3

            MySongsHelper.COLUMN_ARTIST, //4
            MySongsHelper.COLUMN_DURATION, //5
            MySongsHelper.COLUMN_ALBUM, //6
            MySongsHelper.COLUMN_SIZE, //7

            MySongsHelper.COLUMN_GENRE, //8
            MySongsHelper.COLUMN_ALBUM_ART_DATA, //9
    };

    private final String [] projectionDownloaded = new String[] {

            ReachDatabaseHelper.COLUMN_ID, //0

            ReachDatabaseHelper.COLUMN_UNIQUE_ID, //1
            ReachDatabaseHelper.COLUMN_DISPLAY_NAME, //2
            ReachDatabaseHelper.COLUMN_ACTUAL_NAME, //3

            ReachDatabaseHelper.COLUMN_ARTIST, //4
            ReachDatabaseHelper.COLUMN_DURATION, //5
            ReachDatabaseHelper.COLUMN_ALBUM, //6
            ReachDatabaseHelper.COLUMN_SIZE, //7

            ReachDatabaseHelper.COLUMN_GENRE, //8
            ReachDatabaseHelper.COLUMN_ALBUM_ART_DATA, //9
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

        if (itemSelected.isSelected(getHashCode(cursor.getLong(7), cursor.getLong(1), cursor.getString(2), cursor.getString(3)))) {

            viewHolder.listToggle.setPadding(pad, pad, pad, pad);
            viewHolder.listToggle.setBackgroundResource(R.drawable.circular_background_dark);
//            Picasso.with(context).load(R.drawable.check_white).noFade().into(viewHolder.listToggle);
        } else {

            viewHolder.listToggle.setPadding(0, 0, 0, 0);
            viewHolder.listToggle.setBackgroundResource(0);
//            Picasso.with(context).load(R.drawable.add_grey).noFade().into(viewHolder.listToggle);
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

    public int getHashCode(long size, long songId, String displayName, String actualName) {
        int result = (int) (size ^ (size >>> 32));
        result = 31 * result + (int) (songId ^ (songId >>> 32));
        result = 31 * result + displayName.hashCode();
        result = 31 * result + actualName.hashCode();
        return result;
    }

    public interface IsItemSelected {
        boolean isSelected(int hashCode);
    }
}
