package reach.project.coreViews.friends.invite;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import reach.project.R;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Created by dexter on 18/11/15.
 */
final class InviteViewHolder extends SingleItemViewHolder {

    public final TextView userNameList, userInitials, subTitle;
    public final ImageView profilePhotoList, listToggle;

    protected InviteViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        super(itemView, handOverMessage);

        this.userNameList = (TextView) itemView.findViewById(R.id.userNameList);
        this.userInitials = (TextView) itemView.findViewById(R.id.userInitials);
        this.subTitle = (TextView) itemView.findViewById(R.id.listSubTitle);
        this.profilePhotoList = (ImageView) itemView.findViewById(R.id.profilePhotoList);
        this.listToggle = (ImageView) itemView.findViewById(R.id.listToggle);
    }
}

