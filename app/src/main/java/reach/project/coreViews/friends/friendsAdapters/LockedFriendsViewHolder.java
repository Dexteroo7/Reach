package reach.project.coreViews.friends.friendsAdapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import reach.project.R;
import reach.project.utils.viewHelpers.SingleItemViewHolder;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 18/11/15.
 */
final class LockedFriendsViewHolder extends SingleItemViewHolder {

    public final TextView userNameList, telephoneNumberList, newSongs, appCount;
    public final ImageView lockIcon;
    public final SimpleDraweeView profilePhotoList, coverPic;

    protected LockedFriendsViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        super(itemView, handOverMessage);

        this.userNameList = (TextView) itemView.findViewById(R.id.userNameList);
        this.telephoneNumberList = (TextView) itemView.findViewById(R.id.telephoneNumberList);
        this.appCount = (TextView) itemView.findViewById(R.id.appCount);
        this.newSongs = (TextView) itemView.findViewById(R.id.newSongs);
        this.profilePhotoList = (SimpleDraweeView) itemView.findViewById(R.id.profilePhotoList);
        this.coverPic = (SimpleDraweeView) itemView.findViewById(R.id.coverPic);
        this.lockIcon = (ImageView) itemView.findViewById(R.id.lockIcon);
    }
}