package reach.project.coreViews.friends.friendsAdapters;

import android.support.v7.widget.PopupMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import reach.project.R;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Created by dexter on 18/11/15.
 */
final class LockedFriendsViewHolder extends SingleItemViewHolder {

    public final TextView userNameList, telephoneNumberList, newSongs, appCount;
    public final ImageView lockIcon, optionsIcon;
    public final SimpleDraweeView profilePhotoList, coverPic;
    public final PopupMenu popupMenu;

    protected LockedFriendsViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        super(itemView, handOverMessage);

        this.userNameList = (TextView) itemView.findViewById(R.id.userNameList);
        this.telephoneNumberList = (TextView) itemView.findViewById(R.id.telephoneNumberList);
        this.appCount = (TextView) itemView.findViewById(R.id.appCount);
        this.newSongs = (TextView) itemView.findViewById(R.id.newSongs);
        this.profilePhotoList = (SimpleDraweeView) itemView.findViewById(R.id.profilePhotoList);
        this.coverPic = (SimpleDraweeView) itemView.findViewById(R.id.coverPic);
        this.lockIcon = (ImageView) itemView.findViewById(R.id.lockIcon);
        this.optionsIcon = (ImageView) itemView.findViewById(R.id.optionsIcon);
        this.optionsIcon.setOnClickListener(this);

        this.popupMenu = new PopupMenu(itemView.getContext(), optionsIcon);
        this.popupMenu.inflate(R.menu.friends_locked_popup_menu);
        this.popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.friends_menu_1:
                    super.onClick(itemView);
                    return true;
                case R.id.friends_menu_2:
                    //send request
                    return true;
                default:
                    return false;
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v instanceof ImageView)
            popupMenu.show();
        else
            super.onClick(null);
    }
}