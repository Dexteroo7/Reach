package reach.project.coreViews.friends.friendsAdapters;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.ListHolder;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Can not use ReachCursor adapter as item type is Object not cursor
 * Created by dexter on 18/11/15.
 */
@SuppressLint("SetTextI18n")
public class FriendsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener, HandOverMessage<Cursor> {

    private final HandOverMessage<ClickData> handOverMessage;

    public FriendsAdapter(HandOverMessage<ClickData> handOverMessage) {
        this.handOverMessage = handOverMessage;
    }

    public static String[] requiredProjection = new String[]{

            ReachFriendsHelper.COLUMN_ID, //0
            ReachFriendsHelper.COLUMN_PHONE_NUMBER, //1
            ReachFriendsHelper.COLUMN_USER_NAME, //2
            ReachFriendsHelper.COLUMN_IMAGE_ID, //3
            ReachFriendsHelper.COLUMN_NETWORK_TYPE, //4
            ReachFriendsHelper.COLUMN_STATUS, //5
            ReachFriendsHelper.COLUMN_NUMBER_OF_SONGS, //6
            ReachFriendsHelper.COLUMN_NEW_SONGS, //7
    };

    public static final byte VIEW_TYPE_FRIEND = 0;
    public static final byte VIEW_TYPE_LOCKED = 1;
    public static final byte VIEW_TYPE_FRIEND_LARGE = 2;
    public static final byte VIEW_TYPE_INVITE = 3;

    ///////////Vertical Cursor (parent)
    @Nullable
    private Cursor verticalCursor = null;
    private int oldParentCount = 0;

    public void setVerticalCursor(@Nullable Cursor cursor) {

        if (this.verticalCursor != null)
            this.verticalCursor.close();
        this.verticalCursor = cursor;

        if (cursor != null)
            notifyDataSetChanged();
        else
            notifyItemRangeRemoved(0, oldParentCount);
    }
    ///////////Vertical Cursor (parent)

    ///////////Horizontal Cursor
    private final LockedFriendsAdapter lockedFriendsAdapter = new LockedFriendsAdapter(this, R.layout.myreach_item);

    public void setHorizontalCursor(@Nullable Cursor cursor) {
        lockedFriendsAdapter.setCursor(cursor);
    }
    ///////////Horizontal Cursor

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case VIEW_TYPE_FRIEND: {
                return new FriendsViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.myreach_item, parent, false), position -> {

                    if (verticalCursor == null || !verticalCursor.moveToPosition(position))
                        throw new IllegalStateException("Resource cursor has been computed");

                    final ClickData clickData = new ClickData();
                    clickData.friendId = verticalCursor.getLong(0);
                    clickData.networkType = verticalCursor.getShort(4);
                    clickData.status = verticalCursor.getShort(5);
                    clickData.userName = verticalCursor.getString(2);
                    handOverMessage.handOverMessage(clickData);
                });
            }

            case VIEW_TYPE_LOCKED: {
                return new ListHolder(parent);
            }

            case VIEW_TYPE_FRIEND_LARGE: {
                return new FriendsViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.myreach_large_item, parent, false), position -> {

                            if (verticalCursor == null || !verticalCursor.moveToPosition(position))
                                throw new IllegalStateException("Resource cursor has been computed");

                            final ClickData clickData = new ClickData();
                            clickData.friendId = verticalCursor.getLong(0);
                            clickData.networkType = verticalCursor.getShort(4);
                            clickData.status = verticalCursor.getShort(5);
                            clickData.userName = verticalCursor.getString(2);
                            handOverMessage.handOverMessage(clickData);
                        });
            }

            case VIEW_TYPE_INVITE:
                return new SingleItemViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.myreach_invite, parent, false), new HandOverMessage<Integer>() {
                    @Override
                    public void handOverMessage(@Nonnull Integer position) {

                    }
                });

            default:return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final Object friend = getItem(position);
        if (friend instanceof Cursor) {

            final Cursor cursorExactType = (Cursor) friend;
            final FriendsViewHolder viewHolder = (FriendsViewHolder) holder;
            viewHolder.bindPosition(position);

//        final long serverId = cursor.getLong(0);
//        final String phoneNumber = cursor.getString(1);
            final String userName = cursorExactType.getString(2);
            final String imageId = cursorExactType.getString(3);
//        final short networkType = cursor.getShort(4);
            final short status = cursorExactType.getShort(5);
            final int numberOfSongs = cursorExactType.getShort(6);
            final String newSongs = cursorExactType.getString(7);

            viewHolder.userNameList.setText(MiscUtils.capitalizeFirst(userName));
            viewHolder.telephoneNumberList.setText(numberOfSongs + "");
            if ((status == ReachFriendsHelper.ONLINE_REQUEST_GRANTED || status == ReachFriendsHelper.OFFLINE_REQUEST_GRANTED) &&
                    !newSongs.equals("hello_world") && Integer.parseInt(newSongs) > 0) {

                viewHolder.newSongs.setVisibility(View.VISIBLE);
                viewHolder.newSongs.setText("+" + newSongs);
            } else {
                viewHolder.newSongs.setVisibility(View.INVISIBLE);
                viewHolder.newSongs.setText("");
            }

            //first invalidate
            viewHolder.profilePhotoList.setImageURI(null);

            Uri uriToDisplay = null;

            if (!TextUtils.isEmpty(imageId) && !imageId.equals("hello_world"))
                uriToDisplay = Uri.parse(StaticData.cloudStorageImageBaseUrl + imageId);
            /*else {
                if (status == ReachFriendsHelper.ONLINE_REQUEST_GRANTED)
                    uriToDisplay = Uri.parse("res:///" + R.drawable.default_profile01);
                else
                    uriToDisplay = Uri.parse("res:///" + R.drawable.default_profile02);
            }*/

            viewHolder.coverPic.setImageURI(Uri.parse("res:///" + MiscUtils.getRandomPic()));
            viewHolder.profilePhotoList.setImageURI(uriToDisplay);
            viewHolder.lockIcon.setVisibility(View.GONE);

            switch (status) {

                case ReachFriendsHelper.OFFLINE_REQUEST_GRANTED:
                    //viewHolder.listToggle.setImageResource(R.drawable.icon_user_offline);
                    //viewHolder.listStatus.setText("Offline");
                    break;
                case ReachFriendsHelper.ONLINE_REQUEST_GRANTED:
                    //viewHolder.listToggle.setImageResource(R.drawable.icon_user_online);
                    //viewHolder.listStatus.setText("Online");
                    //viewHolder.note.setImageResource(R.drawable.ic_music_count);
                    //viewHolder.userNameList.setTextColor(color);
                    //viewHolder.telephoneNumberList.setTextColor(color);
                    //viewHolder.profilePhoto.setBackgroundResource(R.drawable.circular_background_pink);
                    break;
                case ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED:
                    viewHolder.lockIcon.setVisibility(View.VISIBLE);
                    //viewHolder.listToggle.setImageResource(R.drawable.ic_pending_lock);
                    //viewHolder.listStatus.setText("Pending");
                    break;
                case ReachFriendsHelper.REQUEST_NOT_SENT:
                    viewHolder.lockIcon.setVisibility(View.VISIBLE);
                    //viewHolder.listToggle.setImageResource(R.drawable.icon_locked);
                    //viewHolder.listStatus.setText("");
                    break;
            }

            //use
        }
        else if (friend instanceof Boolean){

            final ListHolder horizontalViewHolder = (ListHolder) holder;
            horizontalViewHolder.headerText.setText("Newly added");
            horizontalViewHolder.listOfItems.setLayoutManager(
                    new CustomLinearLayoutManager(holder.itemView.getContext(),
                            LinearLayoutManager.HORIZONTAL, false));
            horizontalViewHolder.listOfItems.setAdapter(lockedFriendsAdapter);
        }
        else {
            //
        }
    }

    /**
     * Will either return Cursor object OR flag for horizontal list
     *
     * @param position position to load
     * @return object
     */
    @Nonnull
    private Object getItem(int position) {

        if (position == 9 || verticalCursor == null)
            return 1;
        else if (position == 10)
            return false;

        else if (position < 9) {

            if (position == oldParentCount)
                return 1;
            else if (position == oldParentCount + 1)
                return false;

            if (verticalCursor.moveToPosition(position))
                return verticalCursor;
            else
                return 1;
        } else {

            final int relativePosition = position - 2;

            if (verticalCursor.moveToPosition(relativePosition))
                return verticalCursor;
            else
                return 1;
        }
    }


    @Override
    public int getItemViewType(int position) {

        final Object item = getItem(position);
        if (item instanceof Cursor) {
            if (position == 0)
                return VIEW_TYPE_FRIEND_LARGE;
            return VIEW_TYPE_FRIEND;
        }
        else if (item instanceof Boolean)
            return VIEW_TYPE_LOCKED;
        else
            return VIEW_TYPE_INVITE;
    }

    @Override
    public long getItemId(int position) {

        final Object item = getItem(position);
        if (item instanceof Cursor)
            return ((Cursor) item).getInt(0); //TODO shift to dirtyHash
        else if (item instanceof Boolean)
            return 0;
        else
            return 1;
    }

    @Override
    public int getItemCount() {

        if (verticalCursor != null)
            oldParentCount = verticalCursor.getCount();
        return oldParentCount + 2; //adjust for horizontal list
    }

    @Override
    public void handOverMessage(@NonNull Cursor cursor) {

        final ClickData clickData = new ClickData();
        clickData.friendId = cursor.getLong(0);
        clickData.networkType = cursor.getShort(4);
        clickData.status = cursor.getShort(5);
        clickData.userName = cursor.getString(2);
        handOverMessage.handOverMessage(clickData);
    }

    @Override
    public void onClick(View v) {
        //TODO MORE BUTTON HANDLE
    }

    public class ClickData {

        public long friendId;
        public short status, networkType;
        public String userName;
    }
}
