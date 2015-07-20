package reach.project.adapter;

import android.animation.ValueAnimator;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;

import reach.backend.entities.messaging.model.MyString;
import reach.backend.entities.userApi.model.ReceivedRequest;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.FriendRequestFragment;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.utils.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.viewHelpers.CircleTransform;

/**
 * Created by ashish on 14/07/15.
 */
public class ReachFriendRequestAdapter extends ArrayAdapter<ReceivedRequest> {

    private Context ctx;
    private int rId;

    public ReachFriendRequestAdapter(Context context, int resourceId, List<ReceivedRequest> receivedRequestList) {
        super(context, resourceId, receivedRequestList);
        this.ctx = context;
        this.rId = resourceId;
    }

    private void expand(final ViewGroup viewGroup, int a,int b)
    {
        ValueAnimator va = ValueAnimator.ofInt(a, b);
        va.setDuration(300);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                viewGroup.getLayoutParams().height = (Integer) animation.getAnimatedValue();
                viewGroup.requestLayout();
            }
        });
        //va.setInterpolator(new DecelerateInterpolator());
        va.start();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(rId, parent, false);
        final ImageView profilePhoto = (ImageView) convertView.findViewById(R.id.profilePhotoList);
        final TextView userName = (TextView) convertView.findViewById(R.id.userNameList);
        final TextView notifType = (TextView) convertView.findViewById(R.id.notifType);
        final TextView userInitials = (TextView) convertView.findViewById(R.id.userInitials);
        final LinearLayout libraryBtn = (LinearLayout) convertView.findViewById(R.id.libraryButton);
        final LinearLayout actionBlock = (LinearLayout) convertView.findViewById(R.id.actionBlock);
        final LinearLayout linearLayout = (LinearLayout) convertView.findViewById(R.id.linearLayout);
        final RelativeLayout accept = (RelativeLayout) convertView.findViewById(R.id.acceptBlock);
        final RelativeLayout reject = (RelativeLayout) convertView.findViewById(R.id.rejectBlock);

        final ReceivedRequest receivedRequest = getItem(position);

        final int a = MiscUtils.dpToPx(70);
        final int b = MiscUtils.dpToPx(110);

        Picasso.with(ctx).load(StaticData.cloudStorageImageBaseUrl + receivedRequest.getImageId()).transform(new CircleTransform()).into(profilePhoto);
        userName.setText(receivedRequest.getUserName());
        userInitials.setText(MiscUtils.generateInitials(receivedRequest.getUserName()));

        if (FriendRequestFragment.accepted.get(position)) {
            linearLayout.getLayoutParams().height = a;
            actionBlock.setVisibility(View.GONE);
            libraryBtn.setVisibility(View.VISIBLE);
            notifType.setText("added to your friends");
        }
        else {
            notifType.setText("has sent you an access request");
            libraryBtn.setVisibility(View.GONE);
            actionBlock.setVisibility(View.VISIBLE);

            if (!FriendRequestFragment.opened.get(position))
                linearLayout.getLayoutParams().height = a;
            else
                linearLayout.getLayoutParams().height = b;

            linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!FriendRequestFragment.opened.get(position))
                        expand(linearLayout,a,b);
                    else
                        expand(linearLayout,b,a);
                    FriendRequestFragment.opened.set(position,!FriendRequestFragment.opened.get(position));
                }
            });
            final long myId = SharedPrefUtils.getServerId(ctx.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS));
            final HandleReply handleReply = new HandleReply(ctx);

            accept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleReply.execute(myId + "", receivedRequest.getId() + "", "PERMISSION_GRANTED");
                    FriendRequestFragment.accepted.set(position,true);
                    expand(linearLayout, b, a);
                    linearLayout.setClickable(false);
                    actionBlock.setVisibility(View.GONE);
                    libraryBtn.setVisibility(View.VISIBLE);
                    notifType.setText("added to your friends");
                }
            });
            reject.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleReply.execute(myId + "", receivedRequest.getId() + "", "PERMISSION_REJECTED");
                    //delete entry
                    remove(receivedRequest);
                    FriendRequestFragment.accepted.remove(position);
                    FriendRequestFragment.opened.remove(position);
                    notifyDataSetChanged();
                }
            });
        }
        return convertView;
    }

    private final class HandleReply extends AsyncTask<String, Void, Boolean> {

        private final Context context;

        private HandleReply(Context context) {
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(String... params) {

            final long clientId = Long.parseLong(params[0]);
            final long hostId = Long.parseLong(params[1]);
            final String message = params[2];
            final MyString myString = MiscUtils.autoRetry(new DoWork<MyString>() {
                @Override
                protected MyString doWork() throws IOException {

                    StaticData.notificationApi.addBecameFriends(clientId, hostId).execute();

                    return StaticData.messagingEndpoint.messagingEndpoint().handleReply(clientId, hostId, message).execute();
                }
            }, Optional.<Predicate<MyString>>of(new Predicate<MyString>() {
                @Override
                public boolean apply(MyString input) {
                    return (input == null || TextUtils.isEmpty(input.getString()) || input.getString().equals("false"));
                }
            })).orNull();
            if(myString == null || TextUtils.isEmpty(myString.getString()) || myString.getString().equals("false"))
                return false;
            else if(message.equals("PERMISSION_GRANTED") && ctx.getContentResolver() != null) {

                final ContentValues values = new ContentValues();
                values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.ONLINE_REQUEST_GRANTED);
                ctx.getContentResolver().update(
                        Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + hostId),
                        values,
                        ReachFriendsHelper.COLUMN_ID + " = ?",
                        new String[]{hostId + ""});
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {

            super.onPostExecute(aBoolean);
            if(isCancelled() || context == null)
                return;
            if(!aBoolean)
                Toast.makeText(context, "Network Error on reply", Toast.LENGTH_SHORT).show();
        }
    }
}
