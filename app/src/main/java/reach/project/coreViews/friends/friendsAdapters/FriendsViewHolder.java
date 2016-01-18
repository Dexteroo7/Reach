package reach.project.coreViews.friends.friendsAdapters;

import android.content.Context;
import android.support.v4.util.Pair;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import reach.project.R;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.HandOverMessage;

final class FriendsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    public final TextView userNameList, telephoneNumberList, appCount, lockText;
    public final ImageView lockIcon, optionsIcon;
    public final SimpleDraweeView profilePhotoList, coverPic;
    public final PopupMenu popupMenu;
    private final HandOverMessage<Object> handOverMessage;
    private int position;
    private Pair<Integer, Long> pair;

    protected FriendsViewHolder(View itemView, HandOverMessage<Object> handOverMessage) {

        super(itemView);
        this.handOverMessage = handOverMessage;
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

        this.popupMenu = new PopupMenu(itemView.getContext(), optionsIcon);
        this.popupMenu.inflate(R.menu.friends_popup_menu);
        this.popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.friends_menu_1:
                    handOverMessage.handOverMessage(position);
                    return true;
                case R.id.friends_menu_2:
                    pair = new Pair<>(position, SharedPrefUtils.getServerId(itemView.getContext()
                            .getSharedPreferences("Reach", Context.MODE_PRIVATE)));
                    handOverMessage.handOverMessage(pair);
                    return true;
                default:
                    return false;
            }
        });
    }

    public void bindPosition(int position) {
        this.position = position;
    }

    @Override
    public void onClick(View v) {

        if ((int) v.getTag() == 1)
            handOverMessage.handOverMessage(position);
        else
            popupMenu.show();
    }
}

