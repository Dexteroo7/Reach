package reach.project.coreViews.friends.friendsAdapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.imagepipeline.common.ResizeOptions;

import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import reach.project.R;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.invite.InviteActivity;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreListHolder;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Can not use ReachCursor adapter as item type is Object not cursor
 * Created by dexter on 18/11/15.
 */
@SuppressLint("SetTextI18n")
public class FriendsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements HandOverMessage<Cursor> {

    private final HandOverMessage<ClickData> handOverMessage;
    private final ResizeOptions resizeOptions = new ResizeOptions(150, 150);
    private Random random = new Random();
    private final long inviteId = random.nextInt(Integer.MAX_VALUE);
    private final long lockedId = random.nextInt(Integer.MAX_VALUE);

    public FriendsAdapter(HandOverMessage<ClickData> handOverMessage) {

        this.handOverMessage = handOverMessage;
        setHasStableIds(true);
    }

    public static final String[] REQUIRED_PROJECTION = new String[]{

            ReachFriendsHelper.COLUMN_ID, //0
            ReachFriendsHelper.COLUMN_PHONE_NUMBER, //1
            ReachFriendsHelper.COLUMN_USER_NAME, //2
            ReachFriendsHelper.COLUMN_IMAGE_ID, //3
            ReachFriendsHelper.COLUMN_COVER_PIC_ID, //4
            ReachFriendsHelper.COLUMN_NETWORK_TYPE, //5
            ReachFriendsHelper.COLUMN_STATUS, //6
            ReachFriendsHelper.COLUMN_NUMBER_OF_SONGS, //7
            ReachFriendsHelper.COLUMN_NUMBER_OF_APPS, //8
            ReachFriendsHelper.COLUMN_NEW_SONGS, //9
            ReachFriendsHelper.COLUMN_NEW_APPS, //10
            ReachFriendsHelper.COLUMN_HASH //11
    };

    public static final byte VIEW_TYPE_FRIEND = 0;
    public static final byte VIEW_TYPE_LOCKED = 1;
    public static final byte VIEW_TYPE_INVITE = 2;

    ///////////Vertical Cursor (parent)
    @Nullable
    private Cursor verticalCursor = null;
    private int verticalCursorCount = 0;

    public void setVerticalCursor(@Nullable Cursor cursor) {

        if (this.verticalCursor != null)
            this.verticalCursor.close();
        this.verticalCursor = cursor;

        Log.i("Ayush", "Setting vertical cursor " + (cursor != null ? cursor.getCount() : 0));
        notifyDataSetChanged();
    }
    ///////////Vertical Cursor (parent)

    ///////////Horizontal Cursor
    private final LockedFriendsAdapter lockedFriendsAdapter = new LockedFriendsAdapter(this, R.layout.myreach_item);

    public void setHorizontalCursor(@Nullable Cursor cursor) {

        Log.i("Ayush", "Setting horizontal cursor " + (cursor != null ? cursor.getCount() : 0));
        lockedFriendsAdapter.setCursor(cursor);
    }
    ///////////Horizontal Cursor

    private final HandOverMessage<Integer> clickDataHandOver = position -> {

        final Object object = getItem(position);
        if (!(object instanceof Cursor))
            throw new IllegalStateException("Resource cursor has been corrupted");
        FriendsAdapter.this.handOverMessage((Cursor) object);
    };

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Log.i("Ayush", "Creating view holder " + getClass().getName());

        final Context context = parent.getContext();

        switch (viewType) {

            case VIEW_TYPE_FRIEND: {

                return new FriendsViewHolder(LayoutInflater.from(context)
                        .inflate(R.layout.myreach_item, parent, false), clickDataHandOver);
            }

            case VIEW_TYPE_LOCKED: {
                return new MoreListHolder(parent);
            }

            case VIEW_TYPE_INVITE:
                return new SingleItemViewHolder(LayoutInflater.from(context)
                        .inflate(R.layout.myreach_invite, parent, false), position -> context.startActivity(new Intent(context, InviteActivity.class)));

            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final Object friend = getItem(position);
        if (friend instanceof Cursor) {

            final Cursor cursorExactType = (Cursor) friend;
            final FriendsViewHolder viewHolder = (FriendsViewHolder) holder;
            viewHolder.bindPosition(position);

            final long serverId = cursorExactType.getLong(0);
//        final String phoneNumber = cursor.getString(1);
            final String userName = cursorExactType.getString(2);
//            final String imageId = cursorExactType.getString(3);
            final String coverPicId = cursorExactType.getString(4);
//        final short networkType = cursor.getShort(4);
            final short status = cursorExactType.getShort(6);

            final int numberOfSongs = cursorExactType.getInt(7);
            final int numberOfApps = cursorExactType.getInt(8);
            final int newSongs = cursorExactType.getInt(9);
            final int newApps = cursorExactType.getInt(10);
            //final int newFiles = newSongs + newApps;

            //Capitalize only if required
            final char firstLetter = userName.charAt(0);
            if (Character.isLetter(firstLetter) && Character.isLowerCase(firstLetter) && userName.length() > 1)
                viewHolder.userNameList.setText(Character.toUpperCase(firstLetter) + userName.substring(1));
            else
                viewHolder.userNameList.setText(userName);

            viewHolder.telephoneNumberList.setText(numberOfSongs + "");
            viewHolder.appCount.setText(numberOfApps + "");
            if (status <= ReachFriendsHelper.OFFLINE_REQUEST_GRANTED && newSongs > 0) {

                //display new songs
                viewHolder.newSongs.setVisibility(View.VISIBLE);
                viewHolder.newSongs.setText("+" + newSongs);
            } else {

                viewHolder.newSongs.setVisibility(View.INVISIBLE);
                viewHolder.newSongs.setText("");
            }

            //first invalidate
            //viewHolder.profilePhotoList.setController(null);

            viewHolder.profilePhotoList.setImageURI(AlbumArtUri.getUserImageUri(
                    serverId,
                    "imageId",
                    "rw",
                    true,
                    200,
                    200));

            /*else {
                if (status == ReachFriendsHelper.ONLINE_REQUEST_GRANTED)
                    uriToDisplay = Uri.parse("res:///" + R.drawable.default_profile01);
                else
                    uriToDisplay = Uri.parse("res:///" + R.drawable.default_profile02);
            }*/

            viewHolder.coverPic.setController(MiscUtils.getControllerResize(viewHolder.coverPic.getController(), Uri.parse(MiscUtils.getRandomPic()), resizeOptions));
            viewHolder.lockIcon.setVisibility(View.GONE);

            switch (status) {

                case ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED:
                    viewHolder.lockIcon.setImageResource(R.drawable.icon_pending_invite);
                    viewHolder.lockIcon.setVisibility(View.VISIBLE);
                    //viewHolder.listStatus.setText("Pending");
                    break;
                case ReachFriendsHelper.REQUEST_NOT_SENT:
                    viewHolder.lockIcon.setVisibility(View.VISIBLE);
                    //viewHolder.listToggle.setImageResource(R.drawable.icon_locked);
                    //viewHolder.listStatus.setText("");
                    break;
            }

            //use
        } else if (friend instanceof Boolean) {

            final MoreListHolder horizontalViewHolder = (MoreListHolder) holder;
            horizontalViewHolder.headerText.setText("Newly added");
            if (horizontalViewHolder.listOfItems.getLayoutManager() == null)
                horizontalViewHolder.listOfItems.setLayoutManager(new CustomLinearLayoutManager(horizontalViewHolder.listOfItems.getContext(), LinearLayoutManager.HORIZONTAL, false));
            horizontalViewHolder.listOfItems.setAdapter(lockedFriendsAdapter);
        } else {
            //Invite does not need any modifications
        }
    }

    /**
     * Will either return Cursor object OR flag for horizontal list
     *
     * @param position position to load
     * @return object (boolean : locked adapter, int : invite, cursor : normal)
     */
    @Nonnull
    private Object getItem(int position) {

        //Invite card
        if (position == 9 || verticalCursor == null)
            return 1;

            //Locked friends adapter
        else if (position == 10)
            return false;

        else if (position < 9) {

            if (position == verticalCursorCount)
                return 1; //Invite card, second last item
            else if (position == verticalCursorCount + 1)
                return false; //Locked friends adapter, last item

            //Vertical cursor item
            if (verticalCursor.moveToPosition(position))
                return verticalCursor;
            else
                throw new IllegalStateException("Cursor move should have been successful " + position + " " + verticalCursor.getCount());
        } else {

            //9th and 10th position will be occupied before hand
            final int relativePosition = position - 2;

            if (verticalCursor.moveToPosition(relativePosition))
                return verticalCursor;
            else
                throw new IllegalStateException("Cursor move should have been successful " + position + " " + verticalCursor.getCount());
        }
    }


    @Override
    public int getItemViewType(int position) {

        final Object item = getItem(position);
        if (item instanceof Cursor)
            return VIEW_TYPE_FRIEND;
        else if (item instanceof Boolean)
            return VIEW_TYPE_LOCKED;
        else
            return VIEW_TYPE_INVITE;
    }

    @Override
    public long getItemId(int position) {

        final Object item = getItem(position);
        if (item instanceof Cursor)
            return ((Cursor) item).getLong(0); //_id
        else if (item instanceof Boolean)
            return lockedId;
        else
            return inviteId;
    }

    @Override
    public int getItemCount() {

        if (verticalCursor != null)
            verticalCursorCount = verticalCursor.getCount();
        return verticalCursorCount + 2; //adjust for horizontal list
    }

    @Override
    public void handOverMessage(@NonNull Cursor cursor) {

        final ClickData clickData = new ClickData();
        clickData.friendId = cursor.getLong(0);
        clickData.networkType = cursor.getShort(5);
        clickData.status = cursor.getShort(6);
        clickData.userName = cursor.getString(2);

        Log.i("Ayush", "Detected status" + clickData.status);
        handOverMessage.handOverMessage(clickData);
    }

    public class ClickData {

        public long friendId;
        public short status, networkType;
        public String userName;
    }
}
