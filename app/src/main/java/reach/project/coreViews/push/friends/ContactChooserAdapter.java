package reach.project.coreViews.push.friends;

import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

import java.util.Set;

import javax.annotation.Nonnull;

import reach.project.core.StaticData;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.utils.MiscUtils;
import reach.project.utils.ReachCursorAdapter;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 21/12/15.
 */
class ContactChooserAdapter extends ReachCursorAdapter<FriendsViewHolder> {

    public ContactChooserAdapter(HandOverMessage<Cursor> handOverMessage, int resourceId) {
        super(handOverMessage, resourceId);
    }

    final Set<Long> selectedUsers = MiscUtils.getSet(10);
    final String[] requiredProjection = new String[]{

            ReachFriendsHelper.COLUMN_ID, //0
            ReachFriendsHelper.COLUMN_USER_NAME, //1
            ReachFriendsHelper.COLUMN_IMAGE_ID, //2
            ReachFriendsHelper.COLUMN_NUMBER_OF_SONGS, //3
            ReachFriendsHelper.COLUMN_NUMBER_OF_APPS //4
    };

    @Override
    public FriendsViewHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        return new FriendsViewHolder(itemView, handOverMessage);
    }

    @Override
    public void onBindViewHolder(FriendsViewHolder holder, Cursor item) {

        final long userId = item.getLong(0);
        final String userName = item.getString(1);
        final String imageId = item.getString(2);

        holder.userNameList.setText(userName);
        holder.telephoneNumberList.setText(item.getInt(3)+"");
        holder.appCount.setText(item.getInt(4)+"");

        Uri uriToDisplay = null;
        if (!TextUtils.isEmpty(imageId) && !imageId.equals("hello_world"))
            uriToDisplay = Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + imageId);
        holder.profilePhotoList.setImageURI(uriToDisplay);

        //use this
        if (selectedUsers.contains(userId)) {
            holder.mask.setVisibility(View.VISIBLE);
            holder.checkBox.setChecked(true);
        }
        else {
            holder.mask.setVisibility(View.GONE);
            holder.checkBox.setChecked(false);
        }
    }

    @Override
    public long getItemId(@Nonnull Cursor cursor) {

        return cursor.getLong(0); //_id
    }
}