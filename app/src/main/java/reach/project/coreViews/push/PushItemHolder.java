package reach.project.coreViews.push;

import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import reach.project.R;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Created by dexter on 18/11/15.
 */
class PushItemHolder extends SingleItemViewHolder {

    public final TextView text;
    public final SimpleDraweeView image;
    public final CheckBox checkBox;
    public final View mask;

    protected PushItemHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        super(itemView, handOverMessage);

        this.text = (TextView) itemView.findViewById(R.id.text);
        this.image = (SimpleDraweeView) itemView.findViewById(R.id.image);
        this.checkBox = (CheckBox) itemView.findViewById(R.id.checkBox);
        this.mask = itemView.findViewById(R.id.mask);
    }
}