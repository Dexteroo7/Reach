package reach.project.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import reach.project.R;

/**
 * Created by dexter on 15/07/15.
 */
public class DrawerListAdapter extends ArrayAdapter<String> {

    private final Context mContext;
    private final int layoutResourceId;
    private final String txt[];
    private final int imageRes[] = {
            R.drawable.icon_grey,
            R.drawable.audio_grey,
            R.drawable.promo,
            R.drawable.add_icon_grey,
            R.drawable.clock,
            R.drawable.sheet_grey
    };

    public DrawerListAdapter(Context context, int resource, String[] text) {

        super(context, resource, text);
        this.layoutResourceId = resource;
        this.mContext = context;
        this.txt = text;
    }

    private final class ViewHolder {

        private final ImageView listImage;
        private final TextView textViewItem;

        private ViewHolder(ImageView listImage, TextView textViewItem) {
            this.listImage = listImage;
            this.textViewItem = textViewItem;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final ViewHolder viewHolder;

        if (convertView == null) {

            convertView = ((Activity) mContext).getLayoutInflater().inflate(layoutResourceId, parent, false);
            viewHolder = new ViewHolder(
                    (ImageView) convertView.findViewById(R.id.listImage),
                    (TextView) convertView.findViewById(R.id.listTitle));
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        Picasso.with(parent.getContext()).load(imageRes[position]).into(viewHolder.listImage);
        viewHolder.textViewItem.setText(txt[position]);
        return convertView;
    }
}

