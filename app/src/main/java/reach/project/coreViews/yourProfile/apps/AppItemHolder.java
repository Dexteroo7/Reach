package reach.project.coreViews.yourProfile.apps;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import reach.project.R;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Created by dexter on 26/11/15.
 */
class AppItemHolder extends SingleItemViewHolder {

    public final ImageView appIcon;
    public final TextView appName;

    public AppItemHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        super(itemView, handOverMessage);

        this.appIcon = (ImageView) itemView.findViewById(R.id.appIcon);
        this.appName = (TextView) itemView.findViewById(R.id.appName);
    }
}
