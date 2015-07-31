package reach.project.adapter;

/**
 * Created by dexter on 1/8/14.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.ref.WeakReference;

import reach.backend.entities.messaging.model.MyString;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.database.ReachFriend;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.utils.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.viewHelpers.CircleTransform;

public class ReachContactsAdapter extends ResourceCursorAdapter {

    private final Context context;

    public ReachContactsAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
        this.context = context;
    }

    private final class ViewHolder{

        private final TextView userNameList,telephoneNumberList,netType,userInitials,featured,listStatus;
        private final ImageView profilePhotoList, networkStatus,online_offline,listToggle,note;
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
                           ImageView online_offline,
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
            this.online_offline = online_offline;
            this.note = note;
            this.profilePhoto = profilePhoto;
        }
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {

        if(cursor == null) return;

        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        final ReachFriend reachFriendsDatabase = ReachFriendsHelper.cursorToProcess(cursor);
        //First show the initials
        viewHolder.userInitials.setText(MiscUtils.generateInitials(reachFriendsDatabase.getUserName()));
        //First get rid of profile photo
        viewHolder.profilePhotoList.setImageBitmap(null);
        int grey = context.getResources().getColor(R.color.darkgrey);

        if (!TextUtils.isEmpty(reachFriendsDatabase.getImageId()) &&
            !reachFriendsDatabase.getImageId().equals("hello_world")) {

            Picasso.with(context).load(StaticData.cloudStorageImageBaseUrl +
                    reachFriendsDatabase.getImageId()).transform(new CircleTransform()).noPlaceholder().into(new WeakReference<>(viewHolder.profilePhotoList).get());
            viewHolder.profilePhotoList.setVisibility(View.VISIBLE);
        } else
            viewHolder.profilePhotoList.setVisibility(View.GONE);


        viewHolder.userNameList.setText(reachFriendsDatabase.getUserName());
        viewHolder.telephoneNumberList.setText(reachFriendsDatabase.getNumberOfSongs()+" songs");
        final String phoneNumber = reachFriendsDatabase.getPhoneNumber();
        if (phoneNumber.equals("0000000001") || phoneNumber.equals("0000000002") || phoneNumber.equals("8860872102"))
            viewHolder.featured.setVisibility(View.VISIBLE);
        else
            viewHolder.featured.setVisibility(View.GONE);

        final long networkType = reachFriendsDatabase.getNetworkType();
        if(networkType == 1) {
            Picasso.with(context).load(R.drawable.wifi).into(viewHolder.networkStatus);
            viewHolder.netType.setText("");
        } else if(networkType > 1 && networkType < 5) {
            Picasso.with(context).load(R.drawable.phone).into(viewHolder.networkStatus);
            viewHolder.netType.setTextColor(grey);
            viewHolder.netType.setText(networkType + "G");
        } else if (networkType == 5){
            Picasso.with(context).load(R.drawable.phone).into(viewHolder.networkStatus);
            viewHolder.netType.setTextColor(Color.RED);
            viewHolder.netType.setText(" Uploads disabled");
        } else {
            viewHolder.networkStatus.setImageBitmap(null);
            viewHolder.netType.setText("");
        }

        viewHolder.online_offline.setVisibility(View.GONE);
        if(reachFriendsDatabase.getStatus() < 2) {

            viewHolder.listToggle.setVisibility(View.GONE);
            viewHolder.listToggle.setClickable(false);
            final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)viewHolder.featured.getLayoutParams();
            params.setMargins(0, 0, MiscUtils.dpToPx(20), 0);
            viewHolder.featured.setLayoutParams(params);
            if(reachFriendsDatabase.getStatus() == 1)
                viewHolder.online_offline.setVisibility(View.VISIBLE);
        } else {

            viewHolder.listToggle.setVisibility(View.VISIBLE);
            viewHolder.listToggle.setClickable(true);
            final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)viewHolder.featured.getLayoutParams();
            params.setMargins(0, 0, 0, 0);
            viewHolder.featured.setLayoutParams(params);
            if (reachFriendsDatabase.getStatus() == 2)
                viewHolder.listToggle.setImageResource(R.drawable.wait_selector);
            else
                viewHolder.listToggle.setImageResource(R.drawable.lock_selector);
            viewHolder.listToggle.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    new SendRequest().executeOnExecutor(
                            StaticData.threadPool,
                            reachFriendsDatabase.getServerId(),
                            (long) reachFriendsDatabase.getStatus());

                    Toast.makeText(context, "Access Request sent to " + reachFriendsDatabase.getUserName(), Toast.LENGTH_SHORT).show();
                    final ContentValues values = new ContentValues();
                    values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED);
                    context.getContentResolver().update(
                            Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + reachFriendsDatabase.getServerId()),
                            values,
                            ReachFriendsHelper.COLUMN_ID + " = ?",
                            new String[]{reachFriendsDatabase.getServerId() + ""});
                }
            });
        }
        int status = reachFriendsDatabase.getStatus();
        viewHolder.online_offline.setVisibility(View.GONE);
        viewHolder.listToggle.setVisibility(View.VISIBLE);
        switch (status) {
            case ReachFriendsHelper.OFFLINE_REQUEST_GRANTED:
                viewHolder.listToggle.setImageResource(R.drawable.icon_user_offline);
                viewHolder.listStatus.setText("Offline");
                break;
            case ReachFriendsHelper.ONLINE_REQUEST_GRANTED:
                viewHolder.listToggle.setImageResource(R.drawable.icon_user_online);
                viewHolder.note.setImageResource(R.drawable.note_pink);
                viewHolder.listStatus.setText("Online");
                viewHolder.userNameList.setTextColor(context.getResources().getColor(R.color.reach_color));
                viewHolder.listStatus.setTextColor(context.getResources().getColor(R.color.reach_color));
                viewHolder.telephoneNumberList.setTextColor(context.getResources().getColor(R.color.reach_color));
                viewHolder.profilePhoto.setBackgroundResource(R.drawable.circular_background_pink);
                break;
            case ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED:
                viewHolder.listToggle.setImageResource(R.drawable.ic_pending_lock);
                viewHolder.listStatus.setText("Waiting");
                break;
            case ReachFriendsHelper.REQUEST_NOT_SENT:
                viewHolder.listToggle.setImageResource(R.drawable.icon_locked);
                viewHolder.listStatus.setText("");
                break;
            default:
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
                (ImageView) view.findViewById(R.id.circle),
                (ImageView) view.findViewById(R.id.note),
                (FrameLayout) view.findViewById(R.id.profilePhoto));
        view.setTag(viewHolder);
        return view;
    }

    private final class SendRequest extends AsyncTask<Long, Void, Long> {

        @Override
        protected Long doInBackground(final Long... params) {

            final MyString dataAfterWork = MiscUtils.autoRetry(new DoWork<MyString>() {
                @Override
                protected MyString doWork() throws IOException {
                    return StaticData
                            .messagingEndpoint
                            .messagingEndpoint()
                            .requestAccess(SharedPrefUtils.getServerId(context.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)), params[0]).execute();
                }
            }, Optional.<Predicate<MyString>>of(new Predicate<MyString>() {
                @Override
                public boolean apply(MyString input) {
                    return (input == null || TextUtils.isEmpty(input.getString()) || input.getString().equals("false"));
                }
            })).orNull();

            final String toParse;
            if(dataAfterWork == null || TextUtils.isEmpty(toParse = dataAfterWork.getString()) || toParse.equals("false"))
                return params[0];
            return null;
        }

        @Override
        protected void onPostExecute(Long response) {
            super.onPostExecute(response);

            if(context.getContentResolver() == null)
                return;
            if(response != null && response > 0) {

                Toast.makeText(context, "Request Failed", Toast.LENGTH_SHORT).show();
                final ContentValues values = new ContentValues();
                values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.REQUEST_NOT_SENT);
                context.getContentResolver().update(
                        Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + response),
                        values,
                        ReachFriendsHelper.COLUMN_ID + " = ?",
                        new String[]{response + ""});
            }

        }

    }
}