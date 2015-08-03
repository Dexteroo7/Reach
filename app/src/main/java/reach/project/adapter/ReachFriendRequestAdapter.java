package reach.project.adapter;

import android.animation.ValueAnimator;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import reach.backend.entities.messaging.model.MyString;
import reach.backend.entities.userApi.model.ReceivedRequest;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.utils.auxiliaryClasses.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.viewHelpers.CircleTransform;

/**
 * Created by ashish on 14/07/15.
 */
public class ReachFriendRequestAdapter extends ArrayAdapter<ReceivedRequest> {

    public final SparseBooleanArray accepted = new SparseBooleanArray();
    private final SparseBooleanArray opened = new SparseBooleanArray();

    final int a = MiscUtils.dpToPx(70);
    final int b = MiscUtils.dpToPx(110);

    private final Context ctx;
    private final int rId;
    private final long serverId;

    public ReachFriendRequestAdapter(Context context, int resourceId, List<ReceivedRequest> receivedRequestList, long serverId) {
        super(context, resourceId, receivedRequestList);
        this.ctx = context;
        this.rId = resourceId;
        this.serverId = serverId;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(rId, parent, false);

        final ImageView profilePhoto = (ImageView) convertView.findViewById(R.id.profilePhotoList);
        final TextView userName = (TextView) convertView.findViewById(R.id.userNameList);
        final TextView notificationType = (TextView) convertView.findViewById(R.id.notifType);
        final TextView userInitials = (TextView) convertView.findViewById(R.id.userInitials);

        final View libraryBtn = convertView.findViewById(R.id.libraryButton);
        final View actionBlock = convertView.findViewById(R.id.actionBlock);
        final View linearLayout = convertView.findViewById(R.id.linearLayout);
        final View accept = convertView.findViewById(R.id.acceptBlock);
        final View reject = convertView.findViewById(R.id.rejectBlock);

        final ReceivedRequest receivedRequest = getItem(position);

        Picasso.with(ctx).load(StaticData.cloudStorageImageBaseUrl + receivedRequest.getImageId()).transform(new CircleTransform()).into(profilePhoto);
        userName.setText(receivedRequest.getUserName());
        userInitials.setText(MiscUtils.generateInitials(receivedRequest.getUserName()));

        if (accepted.get(position, false)) {

            linearLayout.getLayoutParams().height = a;
            actionBlock.setVisibility(View.GONE);
            libraryBtn.setVisibility(View.VISIBLE);
            notificationType.setText("added to your friends");
        }
        else {

            notificationType.setText("has sent you an access request");
            libraryBtn.setVisibility(View.GONE);
            actionBlock.setVisibility(View.VISIBLE);

            if (!opened.get(position, false))
                linearLayout.getLayoutParams().height = a;
            else
                linearLayout.getLayoutParams().height = b;

            linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (!opened.get(position, false))
                        expand(linearLayout,a,b);
                    else
                        expand(linearLayout,b,a);
                    opened.put(position, !opened.get(position, false));
                }
            });

            final HandleReply handleReply = new HandleReply(ctx);
            accept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleReply.execute(serverId + "", receivedRequest.getId() + "", "PERMISSION_GRANTED");
                    accepted.put(position, true);
                    expand(linearLayout, b, a);
                    linearLayout.setClickable(false);
                    actionBlock.setVisibility(View.GONE);
                    libraryBtn.setVisibility(View.VISIBLE);
                    notificationType.setText("added to your friends");
                }
            });
            reject.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    handleReply.execute(serverId + "", receivedRequest.getId() + "", "PERMISSION_REJECTED");
                    //delete entry
                    remove(receivedRequest);
                    accepted.delete(position);
                    opened.delete(position);
                    notifyDataSetChanged();
                }
            });
        }
        return convertView;
    }

    private void expand(final View view, int a,int b) {

        final ValueAnimator va = ValueAnimator.ofInt(a, b);
        va.setDuration(300);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                view.getLayoutParams().height = (Integer) animation.getAnimatedValue();
                view.requestLayout();
            }
        });
        //va.setInterpolator(new DecelerateInterpolator());
        va.start();
    }

    private static final class HandleReply extends AsyncTask<String, Void, Boolean> {

        private final WeakReference<Context> contextWeakReference;

        private HandleReply(Context context) {
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Boolean doInBackground(String... params) {

            final long clientId = Long.parseLong(params[0]);
            final long hostId = Long.parseLong(params[1]);
            final String message = params[2];
            final MyString myString = MiscUtils.autoRetry(new DoWork<MyString>() {
                @Override
                public MyString doWork() throws IOException {

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

            final Context context = contextWeakReference.get();
            if(context == null || context.getContentResolver() == null)
                return true;

            else if(message.equals("PERMISSION_GRANTED") && context.getContentResolver() != null) {

                final ContentValues values = new ContentValues();
                values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.ONLINE_REQUEST_GRANTED);
                context.getContentResolver().update(
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
            if(aBoolean)
                return;

            final Context context = contextWeakReference.get();
            if(context == null)
                return;
            Toast.makeText(context, "Network Error on reply", Toast.LENGTH_SHORT).show();
        }
    }
}
