package reach.project.friends;

import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.common.base.Optional;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;
import reach.project.utils.ReachCursorAdapter;

/**
 * TODO useless now
 * Created by dexter on 16/11/15.
 */
public class CustomAdapter extends ReachCursorAdapter {

    private ItemClickListener mListener = null;

    public void setOnItemClickListener(ItemClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public int getItemId(@Nonnull Cursor cursor) {
        return cursor.getInt(0);
    }

    public interface ItemClickListener {
        void onItemClick(int pos);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.myreach_item, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final Optional<Cursor> cursorOptional = getItem(position);
        if (!cursorOptional.isPresent())
            return;

        final Cursor cursor = cursorOptional.get();
        final ViewHolder viewHolder = (ViewHolder) holder;
        ((ViewHolder) holder).bindTest(position);

        if (!cursor.moveToPosition(position))
            return;

//        final long serverId = cursor.getLong(0);
//        final String phoneNumber = cursor.getString(1);
        final String userName = cursor.getString(2);
        final String imageId = cursor.getString(3);
//        final short networkType = cursor.getShort(4);
        final short status = cursor.getShort(5);
        final int numberOfSongs = cursor.getShort(6);
        final String newSongs = cursor.getString(7);

        viewHolder.userNameList.setText(MiscUtils.capitalizeFirst(userName));
        viewHolder.telephoneNumberList.setText(numberOfSongs + " songs ");
        if ((status == ReachFriendsHelper.ONLINE_REQUEST_GRANTED || status == ReachFriendsHelper.OFFLINE_REQUEST_GRANTED) &&
                !newSongs.equals("hello_world") && Integer.parseInt(newSongs) > 0) {

            viewHolder.newSongs.setVisibility(View.VISIBLE);
            viewHolder.newSongs.setText("+" + newSongs);
        } else {
            viewHolder.newSongs.setVisibility(View.INVISIBLE);
            viewHolder.newSongs.setText("");
        }

        //first invalidate
        viewHolder.profilePhotoList.setImageBitmap(null);
        viewHolder.profileGradient.setImageResource(R.drawable.gradient_light);
        viewHolder.profileGradient.setBackgroundResource(0);

        final Uri uriToDisplay;

        if (!TextUtils.isEmpty(imageId) && !imageId.equals("hello_world"))
            uriToDisplay = Uri.parse(StaticData.cloudStorageImageBaseUrl + imageId);
        else {

            viewHolder.profilePhotoList.setBackgroundColor(Color.parseColor("#eeeeee"));
            if (status == ReachFriendsHelper.ONLINE_REQUEST_GRANTED)
                uriToDisplay = Uri.parse("res:///" + R.drawable.default_profile01);
            else {

                uriToDisplay = Uri.parse("res:///" + R.drawable.default_profile02);
                if (status == ReachFriendsHelper.REQUEST_NOT_SENT || status == ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED) {

                    viewHolder.profileGradient.setImageBitmap(null);
                    viewHolder.profileGradient.setBackgroundColor(Color.parseColor("#60000000"));
                }
            }
        }

        viewHolder.profilePhotoList.setImageURI(uriToDisplay);

        /*if (networkType == 1) {
            Picasso.with(context).load(R.drawable.wifi).into(viewHolder.networkStatus);
            viewHolder.netType.setText("");
        } else if (networkType > 1 && networkType < 6) {
            Picasso.with(context).load(R.drawable.phone).into(viewHolder.networkStatus);
            viewHolder.netType.setTextColor(grey);
            if (networkType<5)
                viewHolder.netType.setText(networkType + "G");
        } else {
            viewHolder.networkStatus.setImageBitmap(null);
            viewHolder.netType.setText("");
        }*/

        /*//first invalidate
        viewHolder.featured.setVisibility(View.GONE);
        final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) viewHolder.featured.getLayoutParams();
        if (phoneNumber.equals("8860872102")) {

            viewHolder.featured.setVisibility(View.VISIBLE);
            if (status < 2) {
                params.setMargins(0, 0, layoutParameter, 0);
                viewHolder.featured.setLayoutParams(params);
            } else {
                params.setMargins(0, 0, 0, 0);
                viewHolder.featured.setLayoutParams(params);
            }
        }*/

        //first invalidate
        //viewHolder.userNameList.setTextColor(grey);
        //viewHolder.listStatus.setTextColor(grey);
        //viewHolder.telephoneNumberList.setTextColor(grey);
        //viewHolder.note.setImageResource(R.drawable.ic_music_count_grey);
        //viewHolder.profilePhoto.setBackgroundResource(R.drawable.circular_background_dark);
        //viewHolder.onlineStatus.setVisibility(View.INVISIBLE);
        viewHolder.onlineText.setText("");
        viewHolder.onlineIcon.setImageBitmap(null);
        viewHolder.lockIcon.setVisibility(View.GONE);

        ((LinearLayout) viewHolder.onlineIcon.getParent()).setVisibility(View.VISIBLE);

        switch (status) {

            case ReachFriendsHelper.OFFLINE_REQUEST_GRANTED:
                viewHolder.onlineText.setText("Offline");
                viewHolder.onlineIcon.setImageResource(R.drawable.circular_offline);
                //viewHolder.listToggle.setImageResource(R.drawable.icon_user_offline);
                //viewHolder.listStatus.setText("Offline");
                break;
            case ReachFriendsHelper.ONLINE_REQUEST_GRANTED:
                viewHolder.onlineText.setText("Online");
                viewHolder.onlineIcon.setImageResource(R.drawable.circular_online);
                //viewHolder.listToggle.setImageResource(R.drawable.icon_user_online);
                //viewHolder.listStatus.setText("Online");
                //viewHolder.note.setImageResource(R.drawable.ic_music_count);
                //viewHolder.userNameList.setTextColor(color);
                //viewHolder.telephoneNumberList.setTextColor(color);
                //viewHolder.profilePhoto.setBackgroundResource(R.drawable.circular_background_pink);
                break;
            case ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED:
                viewHolder.onlineText.setText("Pending");
                viewHolder.onlineIcon.setImageResource(R.drawable.ic_pending_pink);
                //viewHolder.listToggle.setImageResource(R.drawable.ic_pending_lock);
                //viewHolder.listStatus.setText("Pending");
                break;
            case ReachFriendsHelper.REQUEST_NOT_SENT:
                viewHolder.lockIcon.setVisibility(View.VISIBLE);
                ((LinearLayout) viewHolder.onlineIcon.getParent()).setVisibility(View.GONE);
                viewHolder.profileGradient.setImageBitmap(null);
                viewHolder.profileGradient.setBackgroundColor(Color.parseColor("#60000000"));
                //viewHolder.listToggle.setImageResource(R.drawable.icon_locked);
                //viewHolder.listStatus.setText("");
                break;
        }

    }

    private final class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView userNameList, telephoneNumberList, onlineText, newSongs;
        private final ImageView onlineIcon, lockIcon, profileGradient;
        private final SimpleDraweeView profilePhotoList;

        private int position;

        private void bindTest(int mPos) {
            this.position = mPos;
        }

        public ViewHolder(View view) {

            super(view);
            this.userNameList = (TextView) view.findViewById(R.id.userNameList);
            this.telephoneNumberList = (TextView) view.findViewById(R.id.telephoneNumberList);
            this.onlineText = (TextView) view.findViewById(R.id.onlineText);
            this.onlineIcon = (ImageView) view.findViewById(R.id.onlineIcon);
            this.newSongs = (TextView) view.findViewById(R.id.newSongs);
            this.profilePhotoList = (SimpleDraweeView) view.findViewById(R.id.profilePhotoList);
            this.lockIcon = (ImageView) view.findViewById(R.id.lockIcon);
            this.profileGradient = (ImageView) view.findViewById(R.id.profileGradient);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mListener != null)
                mListener.onItemClick(position);
        }
    }
}
