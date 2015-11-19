package reach.project.friends.friendsAdapters;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import javax.annotation.Nonnull;

import reach.project.R;

/**
 * Created by dexter on 18/11/15.
 */
public final class FriendsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    public final TextView userNameList, telephoneNumberList, newSongs;
    public final ImageView lockIcon;
    public final SimpleDraweeView profilePhotoList;

    private final FriendsHolderCallback friendsHolderCallback;

    private int position = 0;

    public void bindPosition(int mPos) {
        this.position = mPos;
    }

    public FriendsViewHolder(View view, FriendsHolderCallback friendsHolderCallback) {

        super(view);

        this.userNameList = (TextView) view.findViewById(R.id.userNameList);
        this.telephoneNumberList = (TextView) view.findViewById(R.id.telephoneNumberList);
        this.newSongs = (TextView) view.findViewById(R.id.newSongs);
        this.profilePhotoList = (SimpleDraweeView) view.findViewById(R.id.profilePhotoList);
        this.lockIcon = (ImageView) view.findViewById(R.id.lockIcon);

        this.friendsHolderCallback = friendsHolderCallback;

        view.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

        final Cursor friendCursor = friendsHolderCallback.getFriendForPosition(position);
        friendsHolderCallback.handOverClickDetails(
                friendCursor.getLong(0),  //friendId
                friendCursor.getShort(5), //status
                friendCursor.getShort(4), //networkType
                friendCursor.getString(2) //userName
        );

    }

    protected interface FriendsHolderCallback {

        @Nonnull
        Cursor getFriendForPosition(int position);

        void handOverClickDetails(long friendId, short status, short networkType, String userName);
    }
}

