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
final class FriendsViewHolder extends SingleItemViewHolder {

    public final TextView userNameList, telephoneNumberList, newSongs, appCount;
    public final CheckBox checkBox;
    public final View mask;
    public final SimpleDraweeView profilePhotoList, coverPic;

    protected FriendsViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        super(itemView, handOverMessage);

        this.userNameList = (TextView) itemView.findViewById(R.id.userNameList);
        this.telephoneNumberList = (TextView) itemView.findViewById(R.id.telephoneNumberList);
        this.appCount = (TextView) itemView.findViewById(R.id.appCount);
        this.newSongs = (TextView) itemView.findViewById(R.id.newSongs);
        this.profilePhotoList = (SimpleDraweeView) itemView.findViewById(R.id.profilePhotoList);
        this.coverPic = (SimpleDraweeView) itemView.findViewById(R.id.coverPic);
        this.checkBox = (CheckBox) itemView.findViewById(R.id.checkBox);
        this.mask = itemView.findViewById(R.id.mask);
    }
}

