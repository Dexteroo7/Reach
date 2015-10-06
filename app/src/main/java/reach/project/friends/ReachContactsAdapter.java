package reach.project.friends;

/**
 * Created by dexter on 1/8/14.
 */

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;

public class ReachContactsAdapter extends ResourceCursorAdapter {

//    private final int grey;
//    private final int color;
//    private final int layoutParameter = MiscUtils.dpToPx(20);
//    private final CircleTransform transform = new CircleTransform();
//    private final int[] colors;

    public ReachContactsAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
//        this.grey = ContextCompat.getColor(context, R.color.darkgrey);
//        this.color = ContextCompat.getColor(context, R.color.reach_color);
//        this.colors = context.getResources().getIntArray(R.array.androidcolors);
    }

    private final class ViewHolder {

        private final TextView userNameList, telephoneNumberList, onlineText, newSongs;
        private final ImageView profilePhotoList, onlineIcon, lockIcon, profileGradient;

        private ViewHolder(TextView userNameList,
                           TextView telephoneNumberList,
                           TextView onlineText,
                           TextView newSongs,
                           ImageView onlineIcon,
                           ImageView profilePhotoList,
                           ImageView lockIcon,
                           ImageView profileGradient) {

            this.userNameList = userNameList;
            this.telephoneNumberList = telephoneNumberList;
            this.onlineText = onlineText;
            this.onlineIcon = onlineIcon;
            this.newSongs = newSongs;
            this.profilePhotoList = profilePhotoList;
            this.lockIcon = lockIcon;
            this.profileGradient = profileGradient;
        }
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

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {

        final ViewHolder viewHolder = (ViewHolder) view.getTag();

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
                !newSongs.equals("hello_world") && Integer.parseInt(newSongs)>0) {

            viewHolder.newSongs.setVisibility(View.VISIBLE);
            viewHolder.newSongs.setText("+"+newSongs);
        }
        else {
            viewHolder.newSongs.setVisibility(View.INVISIBLE);
            viewHolder.newSongs.setText("");
        }

        //first invalidate
        viewHolder.profilePhotoList.setImageBitmap(null);
        viewHolder.profileGradient.setImageResource(R.drawable.gradient_light);
        viewHolder.profileGradient.setBackgroundResource(0);
        if (!TextUtils.isEmpty(imageId) &&
                !imageId.equals("hello_world")) {
            Picasso.with(context).load(StaticData.cloudStorageImageBaseUrl +
                    imageId).centerCrop().fit().centerCrop().into(viewHolder.profilePhotoList);
        } else {
            viewHolder.profilePhotoList.setBackgroundColor(Color.parseColor("#eeeeee"));
            if (status == ReachFriendsHelper.ONLINE_REQUEST_GRANTED)
                Picasso.with(context).load(R.drawable.default_profile01)
                        .centerCrop().fit().centerCrop().into(viewHolder.profilePhotoList);
            else {
                Picasso.with(context).load(R.drawable.default_profile02)
                        .centerCrop().fit().centerCrop().into(viewHolder.profilePhotoList);
                if (status == ReachFriendsHelper.REQUEST_NOT_SENT || status == ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED) {
                    viewHolder.profileGradient.setImageBitmap(null);
                    viewHolder.profileGradient.setBackgroundColor(Color.parseColor("#60000000"));
                }
            }
        }



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

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        final View view = super.newView(context, cursor, parent);
        final ViewHolder viewHolder = new ViewHolder(
                (TextView) view.findViewById(R.id.userNameList),
                (TextView) view.findViewById(R.id.telephoneNumberList),
                (TextView) view.findViewById(R.id.onlineText),
                (TextView) view.findViewById(R.id.newSongs),
                (ImageView) view.findViewById(R.id.onlineIcon),
                (ImageView) view.findViewById(R.id.profilePhotoList),
                (ImageView) view.findViewById(R.id.lockIcon),
                (ImageView) view.findViewById(R.id.profileGradient));
        view.setTag(viewHolder);
        return view;
    }
}