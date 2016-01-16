package reach.project.coreViews.friends.friendsAdapters;

import android.support.v7.widget.PopupMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import reach.project.R;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

final class FriendsViewHolder extends SingleItemViewHolder implements View.OnClickListener {

    public final TextView userNameList, telephoneNumberList, appCount, lockText;
    public final ImageView lockIcon, optionsIcon;
    public final SimpleDraweeView profilePhotoList, coverPic;

    protected FriendsViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        super(itemView, handOverMessage);
        this.itemView.setTag(1);
        this.itemView.setOnClickListener(this);

        this.userNameList = (TextView) itemView.findViewById(R.id.userNameList);
        this.telephoneNumberList = (TextView) itemView.findViewById(R.id.telephoneNumberList);
        this.appCount = (TextView) itemView.findViewById(R.id.appCount);
        //this.newSongs = (TextView) itemView.findViewById(R.id.newSongs);
        this.profilePhotoList = (SimpleDraweeView) itemView.findViewById(R.id.profilePhotoList);
        this.coverPic = (SimpleDraweeView) itemView.findViewById(R.id.coverPic);
        this.lockIcon = (ImageView) itemView.findViewById(R.id.lockIcon);
        this.lockText = (TextView) itemView.findViewById(R.id.lockText);

        this.optionsIcon = (ImageView) itemView.findViewById(R.id.optionsIcon);
        this.optionsIcon.setTag(2);
        this.optionsIcon.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        if ((int) v.getTag() == 1)
            super.onClick(v);
        else {
            final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
            popupMenu.inflate(R.menu.friends_popup_menu);
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.friends_menu_1:
                        return true;
                    case R.id.friends_menu_2:
                        return true;
                    default:
                        return false;
                }
            });
            popupMenu.show();
        }
    }
}

