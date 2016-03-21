package reach.project.coreViews.fileManager.apps;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicReference;

import reach.project.R;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Created by dexter on 26/11/15.
 */
class AppItemHolder extends SingleItemViewHolder {

    final ImageView appIcon, extraButton;
    final TextView appName;
    final ImageView toggleImage;

    final AtomicReference<String> packageName = new AtomicReference<>("");

    public AppItemHolder(View itemView,
                         HandOverMessage<Integer> handOverMessage,
                         View.OnClickListener extraClick) {

        super(itemView, handOverMessage);

        this.appIcon = (ImageView) itemView.findViewById(R.id.appIcon);
        this.appName = (TextView) itemView.findViewById(R.id.appName);
        this.toggleImage = (ImageView) itemView.findViewById(R.id.toggleImage);
        this.extraButton = (ImageView) itemView.findViewById(R.id.extraButton);
        this.extraButton.setTag(packageName);
        this.extraButton.setOnClickListener(extraClick);
    }
}