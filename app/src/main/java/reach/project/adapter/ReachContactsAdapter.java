package reach.project.adapter;

/**
 * Created by dexter on 1/8/14.
 */

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.utils.MiscUtils;
import reach.project.viewHelpers.CircleTransform;

public class ReachContactsAdapter extends ResourceCursorAdapter {

    private final int grey;
    private final int color;
    private final int layoutParameter = MiscUtils.dpToPx(20);
    private final CircleTransform transform = new CircleTransform();

    public ReachContactsAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
        this.grey = context.getResources().getColor(R.color.darkgrey);
        this.color = context.getResources().getColor(R.color.reach_color);
    }

    private final class ViewHolder {

        private final TextView userNameList, telephoneNumberList, netType, userInitials, featured, listStatus;
        private final ImageView profilePhotoList, networkStatus, listToggle, note;
        private final FrameLayout profilePhoto;

        private ViewHolder(TextView userNameList,
                           TextView telephoneNumberList,
                           TextView netType,
                           TextView userInitials,
                           TextView featured,
                           TextView listStatus,
                           ImageView listToggle,
                           ImageView profilePhotoList,
                           ImageView networkStatus,
                           ImageView note,
                           FrameLayout profilePhoto) {

            this.userNameList = userNameList;
            this.telephoneNumberList = telephoneNumberList;
            this.netType = netType;
            this.userInitials = userInitials;
            this.featured = featured;
            this.listStatus = listStatus;
            this.listToggle = listToggle;
            this.profilePhotoList = profilePhotoList;
            this.networkStatus = networkStatus;
            this.note = note;
            this.profilePhoto = profilePhoto;
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
    };

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {

        final ViewHolder viewHolder = (ViewHolder) view.getTag();

        final long serverId = cursor.getLong(0);
        final String phoneNumber = cursor.getString(1);
        final String userName = cursor.getString(2);
        final String imageId = cursor.getString(3);
        final short networkType = cursor.getShort(4);
        final short status = cursor.getShort(5);
        final int numberOfSongs = cursor.getShort(6);

        viewHolder.userInitials.setText(MiscUtils.generateInitials(userName));
        viewHolder.userNameList.setText(MiscUtils.capitalizeFirst(userName));
        viewHolder.telephoneNumberList.setText(numberOfSongs + " songs");

        //first invalidate
        viewHolder.profilePhotoList.setImageBitmap(null);
        viewHolder.profilePhotoList.setVisibility(View.INVISIBLE);
        if (!TextUtils.isEmpty(imageId) &&
                !imageId.equals("hello_world")) {

            Picasso.with(context).load(StaticData.cloudStorageImageBaseUrl +
                    imageId).transform(transform).noPlaceholder().into(viewHolder.profilePhotoList);
            viewHolder.profilePhotoList.setVisibility(View.VISIBLE);
        }

        if (networkType == 1) {
            Picasso.with(context).load(R.drawable.wifi).into(viewHolder.networkStatus);
            viewHolder.netType.setText("");
        } else if (networkType > 1 && networkType < 5) {
            Picasso.with(context).load(R.drawable.phone).into(viewHolder.networkStatus);
            viewHolder.netType.setTextColor(grey);
            viewHolder.netType.setText(networkType + "G");
        } else {
            viewHolder.networkStatus.setImageBitmap(null);
            viewHolder.netType.setText("");
        }

        //first invalidate
        viewHolder.featured.setVisibility(View.GONE);
        final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) viewHolder.featured.getLayoutParams();
        if (phoneNumber.equals("0000000001") || phoneNumber.equals("0000000002") || phoneNumber.equals("8860872102")) {

            viewHolder.featured.setVisibility(View.VISIBLE);
            if (status < 2) {
                params.setMargins(0, 0, layoutParameter, 0);
                viewHolder.featured.setLayoutParams(params);
            } else {
                params.setMargins(0, 0, 0, 0);
                viewHolder.featured.setLayoutParams(params);
            }
        }

        //first invalidate
        viewHolder.userNameList.setTextColor(grey);
        viewHolder.listStatus.setTextColor(grey);
        viewHolder.telephoneNumberList.setTextColor(grey);
        viewHolder.note.setImageResource(R.drawable.ic_music_count_grey);
        viewHolder.profilePhoto.setBackgroundResource(R.drawable.circular_background_dark);

        switch (status) {

            case ReachFriendsHelper.OFFLINE_REQUEST_GRANTED:

                viewHolder.listToggle.setImageResource(R.drawable.icon_user_offline);
                viewHolder.listStatus.setText("Offline");
                break;
            case ReachFriendsHelper.ONLINE_REQUEST_GRANTED:
                if (networkType == (short) 5) {
                    viewHolder.listToggle.setImageResource(R.drawable.icon_user_offline);
                    viewHolder.listStatus.setText("Offline");
                }
                else {
                    viewHolder.listToggle.setImageResource(R.drawable.icon_user_online);
                    viewHolder.listStatus.setText("Online");
                    viewHolder.note.setImageResource(R.drawable.ic_music_count);
                    viewHolder.userNameList.setTextColor(color);
                    viewHolder.telephoneNumberList.setTextColor(color);
                    viewHolder.profilePhoto.setBackgroundResource(R.drawable.circular_background_pink);
                }
                break;
            case ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED:

                viewHolder.listToggle.setImageResource(R.drawable.ic_pending_lock);
                viewHolder.listStatus.setText("Pending");
                break;
            case ReachFriendsHelper.REQUEST_NOT_SENT:

                viewHolder.listToggle.setImageResource(R.drawable.icon_locked);
                viewHolder.listStatus.setText("");
                break;
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        final View view = super.newView(context, cursor, parent);
        final ViewHolder viewHolder = new ViewHolder(
                (TextView) view.findViewById(R.id.userNameList),
                (TextView) view.findViewById(R.id.telephoneNumberList),
                (TextView) view.findViewById(R.id.netType),
                (TextView) view.findViewById(R.id.userInitials),
                (TextView) view.findViewById(R.id.featured),
                (TextView) view.findViewById(R.id.listStatus),
                (ImageView) view.findViewById(R.id.listToggle),
                (ImageView) view.findViewById(R.id.profilePhotoList),
                (ImageView) view.findViewById(R.id.status),
                (ImageView) view.findViewById(R.id.note),
                (FrameLayout) view.findViewById(R.id.profilePhoto));
        view.setTag(viewHolder);
        return view;
    }
}