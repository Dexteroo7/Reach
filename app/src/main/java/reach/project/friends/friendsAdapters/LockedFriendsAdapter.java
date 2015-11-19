package reach.project.friends.friendsAdapters;

import android.database.Cursor;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.common.base.Optional;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.ReachCursorAdapter;

/**
 * Created by dexter on 18/11/15.
 */
public final class LockedFriendsAdapter extends ReachCursorAdapter {

    @Override
    public int getItemId(@Nonnull Cursor cursor) {
        return cursor.getInt(0); //TODO shift to dirtyHash
    }

    @Override
    public LockedFriendHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new LockedFriendHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.myreach_item, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        LockedFriendHolder friendHolder = (LockedFriendHolder) holder;
        friendHolder.bindTest(position);
        Optional<Cursor> cursorOptional = getItem(position);
        if (cursorOptional.isPresent()) {
            Cursor cursor = cursorOptional.get();
            friendHolder.userNameList.setText(cursor.getString(2));
            friendHolder.telephoneNumberList.setText(cursor.getInt(6) + "");
            friendHolder.profilePhotoList.setImageURI(Uri.parse(StaticData.cloudStorageImageBaseUrl + cursor.getString(3)));
            friendHolder.lockIcon.setVisibility(View.VISIBLE);
        }
    }

    public class LockedFriendHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        public final TextView userNameList, telephoneNumberList, newSongs;
        public final ImageView lockIcon;
        public final SimpleDraweeView profilePhotoList;
        private int pos;

        private LockedFriendHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            this.userNameList = (TextView) itemView.findViewById(R.id.userNameList);
            this.telephoneNumberList = (TextView) itemView.findViewById(R.id.telephoneNumberList);
            this.newSongs = (TextView) itemView.findViewById(R.id.newSongs);
            this.profilePhotoList = (SimpleDraweeView) itemView.findViewById(R.id.profilePhotoList);
            this.lockIcon = (ImageView) itemView.findViewById(R.id.lockIcon);
        }

        private void bindTest(int mPos) {
            pos = mPos;
        }

        @Override
        public void onClick(View v) {
            Snackbar.make(v, " clicked", Snackbar.LENGTH_SHORT).show();
        }
    }

}
