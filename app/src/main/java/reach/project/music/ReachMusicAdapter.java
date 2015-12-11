package reach.project.music;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.squareup.picasso.Picasso;

import reach.project.R;
import reach.project.uploadDownload.ReachDatabaseHelper;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;

/**
 * Created by dexter on 7/8/14.
 */
public class ReachMusicAdapter extends ResourceCursorAdapter {

    public String[] getProjectionDownloaded() {
        return projectionDownloaded;
    }

    public String[] getProjectionMyLibrary() {
        return projectionMyLibrary;
    }

    private final String[] projectionMyLibrary =
            {
                    MySongsHelper.COLUMN_ID, //0

                    MySongsHelper.COLUMN_SONG_ID, //1
                    MySongsHelper.COLUMN_USER_ID, //2

                    MySongsHelper.COLUMN_DISPLAY_NAME, //3
                    MySongsHelper.COLUMN_ACTUAL_NAME, //4

                    MySongsHelper.COLUMN_ARTIST, //5
                    MySongsHelper.COLUMN_ALBUM, //6

                    MySongsHelper.COLUMN_DURATION, //7
                    MySongsHelper.COLUMN_SIZE, //8

                    MySongsHelper.COLUMN_VISIBILITY, //9
                    MySongsHelper.COLUMN_GENRE //10
            };

    private final String[] projectionDownloaded =
            {
                    ReachDatabaseHelper.COLUMN_ID, //0

                    ReachDatabaseHelper.COLUMN_UNIQUE_ID, //1
                    ReachDatabaseHelper.COLUMN_RECEIVER_ID, //2

                    ReachDatabaseHelper.COLUMN_DISPLAY_NAME, //3
                    ReachDatabaseHelper.COLUMN_ACTUAL_NAME, //4

                    ReachDatabaseHelper.COLUMN_ARTIST, //5
                    ReachDatabaseHelper.COLUMN_ALBUM, //6

                    ReachDatabaseHelper.COLUMN_DURATION, //7
                    ReachDatabaseHelper.COLUMN_SIZE, //8

                    ReachDatabaseHelper.COLUMN_VISIBILITY, //9
                    ReachDatabaseHelper.COLUMN_GENRE //10
            };

//    private static final String EMPTY_STRING = "empty_string";

    public static final byte PLAYER = 0;
    public static final byte LIST = 1;

    private final byte type;
    private final int width = 50, height = 50; //fixed dimensions
    private final Picasso picasso;

    public ReachMusicAdapter(Context context, int layout, Cursor c, int flags, byte type) {
        super(context, layout, c, flags);
        this.type = type;
        this.picasso = Picasso.with(context);
    }

    private final class ViewHolder {

        private final ImageView albumArt;
        private final ImageView listToggle;
        private final TextView listTitle, listSubTitle, songDuration;

        private ViewHolder(ImageView listToggle, ImageView albumArt, TextView listTitle, TextView listSubTitle, TextView songDuration) {

            this.listToggle = listToggle;
            this.albumArt = albumArt;
            this.listTitle = listTitle;
            this.listSubTitle = listSubTitle;
            this.songDuration = songDuration;
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        final ViewHolder viewHolder = (ViewHolder) view.getTag();

        final String displayName;
        final String artist;
        final String album;
        final long duration;
        final short visibility;

        switch (type) {
            case PLAYER: {
                displayName = cursor.getString(3);
                artist = cursor.getString(4);
                duration = cursor.getLong(5);
                album = cursor.getString(6);
                visibility = 1;
                break;
            }
            case LIST: {
                displayName = cursor.getString(3);
                artist = cursor.getString(5);
                album = cursor.getString(6);
                duration = cursor.getLong(7);
                visibility = cursor.getShort(9);
                break;
            }
            default:
                return;
        }

        final Optional<Uri> uriOptional = AlbumArtUri.getUri(album, artist, displayName);

        if (uriOptional.isPresent()) {

            Log.i("Ayush", "Url found = " + uriOptional.get().toString());
            picasso.load(uriOptional.get()).fit().centerCrop().into(viewHolder.albumArt);
        } else
            viewHolder.albumArt.setImageBitmap(null);

        viewHolder.listTitle.setText(displayName); //displayName
        viewHolder.songDuration.setText(MiscUtils.combinationFormatter(duration)); //duration
        /////////////
        viewHolder.listSubTitle.setText(artist); //artist
        if (TextUtils.isEmpty(viewHolder.listSubTitle.getText()))
            viewHolder.listSubTitle.setText(album); //album
        if (viewHolder.listToggle != null) {

            //Privacy Fragment stuff
            if (visibility == 0) { //visibility
                viewHolder.songDuration.setText("Invisible");
                viewHolder.listToggle.setImageResource(R.drawable.ic_action_visibility_off);
                view.setAlpha(0.4f);
            } else {
                viewHolder.songDuration.setText(" Visible ");
                viewHolder.listToggle.setImageResource(R.drawable.ic_action_visibility_on);
                view.setAlpha(1f);
            }
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        final View view = super.newView(context, cursor, parent);
        final ViewHolder viewHolder = new ViewHolder(
                (ImageView) view.findViewById(R.id.listToggle),
                (ImageView) view.findViewById(R.id.albumArt),
                (TextView) view.findViewById(R.id.listTitle),
                (TextView) view.findViewById(R.id.listSubTitle),
                (TextView) view.findViewById(R.id.songDuration));

        view.setTag(viewHolder);
        return view;
    }
}