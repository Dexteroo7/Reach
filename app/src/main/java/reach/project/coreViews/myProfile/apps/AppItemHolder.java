package reach.project.coreViews.myProfile.apps;

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
    public final ImageView toggleButton;
    public final ImageView toggleButton2;
    public final TextView toggleText;
    public final TextView appName;

    public AppItemHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        super(itemView, handOverMessage);

        this.appIcon = (ImageView) itemView.findViewById(R.id.appIcon);
        this.toggleButton = (ImageView) itemView.findViewById(R.id.toggleButton);
        this.toggleButton2 = (ImageView) itemView.findViewById(R.id.toggleButton2);
        this.toggleText = (TextView) itemView.findViewById(R.id.toggleText);
        this.appName = (TextView) itemView.findViewById(R.id.appName);
    }
}
