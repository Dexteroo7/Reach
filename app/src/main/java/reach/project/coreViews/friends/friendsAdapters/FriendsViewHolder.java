package reach.project.coreViews.friends.friendsAdapters;

import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import reach.project.R;
import reach.project.utils.viewHelpers.HandOverMessage;

final class FriendsViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener {

    public final TextView userNameList, telephoneNumberList, newSongs;
    public final ImageView lockIcon, optionsIcon;
    public final SimpleDraweeView profilePhotoList, coverPic;
    private final HandOverMessage<Integer> handOverMessage;
    private int position;

    protected FriendsViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        super(itemView);
        this.handOverMessage = handOverMessage;
        this.itemView.setTag(1);
        this.itemView.setOnClickListener(this);

        this.userNameList = (TextView) itemView.findViewById(R.id.userNameList);
        this.telephoneNumberList = (TextView) itemView.findViewById(R.id.telephoneNumberList);
        this.newSongs = (TextView) itemView.findViewById(R.id.newSongs);
        this.profilePhotoList = (SimpleDraweeView) itemView.findViewById(R.id.profilePhotoList);
        this.coverPic = (SimpleDraweeView) itemView.findViewById(R.id.coverPic);
        this.lockIcon = (ImageView) itemView.findViewById(R.id.lockIcon);
        this.optionsIcon = (ImageView) itemView.findViewById(R.id.optionsIcon);
        this.optionsIcon.setTag(2);
        this.optionsIcon.setOnClickListener(this);
    }

    public void bindPosition(int position) {
        this.position = position;
    }

    @Override
    public void onClick(View v) {
        if ((int) v.getTag() == 1)
            handOverMessage.handOverMessage(position);
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

