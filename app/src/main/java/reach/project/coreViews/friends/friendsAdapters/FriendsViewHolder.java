package reach.project.coreViews.friends.friendsAdapters;

import android.support.v7.widget.PopupMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import reach.project.R;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

final class FriendsViewHolder extends SingleItemViewHolder {

    public final TextView userNameList, telephoneNumberList, newSongs, appCount;
    public final ImageView lockIcon, optionsIcon;
    public final SimpleDraweeView profilePhotoList, coverPic;

    protected FriendsViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        super(itemView, handOverMessage);

        this.userNameList = (TextView) itemView.findViewById(R.id.userNameList);
        this.telephoneNumberList = (TextView) itemView.findViewById(R.id.telephoneNumberList);
        this.appCount = (TextView) itemView.findViewById(R.id.appCount);
        this.newSongs = (TextView) itemView.findViewById(R.id.newSongs);
        this.profilePhotoList = (SimpleDraweeView) itemView.findViewById(R.id.profilePhotoList);
        this.coverPic = (SimpleDraweeView) itemView.findViewById(R.id.coverPic);
        this.lockIcon = (ImageView) itemView.findViewById(R.id.lockIcon);

        this.optionsIcon = (ImageView) itemView.findViewById(R.id.optionsIcon);
        this.optionsIcon.setOnClickListener(optionsMenuClick);
    }

    private final View.OnClickListener optionsMenuClick = view -> {

        final PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        popupMenu.inflate(R.menu.friends_popup_menu);
        popupMenu.setOnMenuItemClickListener(item -> {

            switch (item.getItemId()) {
                case R.id.friends_menu_1:
                    handOverMessage.handOverMessage(position);
                    return true;
                case R.id.friends_menu_2:
                    return true;
                default:
                    return false;
            }
        });
        popupMenu.show();
    };
}

