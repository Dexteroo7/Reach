package reach.project.adapter;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
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
public class ReachMusicAdapter extends ResourceCursorAdapter {

    public static final byte PLAYER = 0;
    public static final byte LIST = 1;
    private final byte type;
    public ReachMusicAdapter(Context context, int layout, Cursor c, int flags, byte type) {
        super(context, layout, c, flags);
        this.type = type;
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

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        final ViewHolder viewHolder = (ViewHolder) view.getTag();

        final String displayName;
        final String artist;
        final String album;
        final long duration;
        final short visibility;
        switch (type) {
            case PLAYER : {
                displayName = cursor.getString(3);
                artist = cursor.getString(4);
                duration = cursor.getLong(5);
                album = cursor.getString(6);
                visibility = 1;
                break;
            }
            case LIST : {
                displayName = cursor.getString(3);
                artist = cursor.getString(5);
                album = cursor.getString(6);
                duration = cursor.getLong(7);
                visibility = cursor.getShort(9);
                break;
            }
            default:return;
        }

        viewHolder.listTitle.setText(displayName); //displayName
        viewHolder.songDuration.setText(MiscUtils.combinationFormatter(duration)); //duration
        /////////////
        viewHolder.listSubTitle.setText(artist); //artist
        if(TextUtils.isEmpty(viewHolder.listSubTitle.getText()))
            viewHolder.listSubTitle.setText(album); //album
        if (viewHolder.listToggle != null) {
            //Privacy Fragment stuff
            if (visibility == 0) { //visibility
                viewHolder.songDuration.setText("Invisible");
                Picasso.with(context).load(R.drawable.ic_action_visibility_off).noFade().into(viewHolder.listToggle);
                view.setAlpha(0.4f);
            }
            else {
                viewHolder.songDuration.setText(" Visible ");
                Picasso.with(context).load(R.drawable.ic_action_visibility_on).noFade().into(viewHolder.listToggle);
                view.setAlpha(1f);
            }
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
}