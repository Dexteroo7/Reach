package reach.project.adapter;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;

import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.database.notifications.BecameFriends;
import reach.project.database.notifications.Like;
import reach.project.database.notifications.NotificationBaseLocal;
import reach.project.database.notifications.Push;
import reach.project.database.notifications.PushAccepted;
import reach.project.database.notifications.Types;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.DoWork;
import reach.project.viewHelpers.CircleTransform;

/**
 * Created by ashish on 10/07/15.
 */
public class ReachNotificationAdapter extends ArrayAdapter<NotificationBaseLocal> {

    private final CircleTransform transform = new CircleTransform();
    private final int resID;
    private final Activity activity;
    private final long serverID;

    public ReachNotificationAdapter(Activity mActivity, int resourceID, List<NotificationBaseLocal> notificationBaseLocalList, long serverId) {
        super(mActivity, resourceID, notificationBaseLocalList);
        this.resID = resourceID;
        this.activity = mActivity;
        this.serverID = serverId;
    }

    private final class ViewHolder {

        private final ImageView profilePhoto;
        private final TextView userName, notifType, userInitials;
        private final LinearLayout librarayBtn, actionBlock, linearLayout;
        private final RelativeLayout accept, reject;

        private ViewHolder(ImageView profilePhoto,
                           TextView userName,
                           TextView notifType,
                           TextView userInitials,
                           LinearLayout librarayBtn,
                           LinearLayout actionBlock,
                           LinearLayout linearLayout,
                           RelativeLayout accept,
                           RelativeLayout reject
        ) {

            this.profilePhoto = profilePhoto;
            this.userName = userName;
            this.notifType = notifType;
            this.userInitials = userInitials;
            this.librarayBtn = librarayBtn;
            this.actionBlock = actionBlock;
            this.linearLayout = linearLayout;
            this.accept = accept;
            this.reject = reject;
        }
    }

    private void expand(final ViewGroup viewGroup, int a, int b) {
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
    public View getView(int position, View convertView, ViewGroup parent) {

        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(activity).inflate(resID, parent, false);
            viewHolder = new ViewHolder(
                    (ImageView) convertView.findViewById(R.id.profilePhotoList),
                    (TextView) convertView.findViewById(R.id.userNameList),
                    (TextView) convertView.findViewById(R.id.notifType),
                    (TextView) convertView.findViewById(R.id.userInitials),
                    (LinearLayout) convertView.findViewById(R.id.libraryButton),
                    (LinearLayout) convertView.findViewById(R.id.actionBlock),
                    (LinearLayout) convertView.findViewById(R.id.linearLayout),
                    (RelativeLayout) convertView.findViewById(R.id.acceptBlock),
                    (RelativeLayout) convertView.findViewById(R.id.rejectBlock));
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        final int a = MiscUtils.dpToPx(70);
        final int b = MiscUtils.dpToPx(110);
        final NotificationBaseLocal notificationBaseLocal = getItem(position);
        final Types type = notificationBaseLocal.getTypes();

        switch (type) {

            case DEFAULT:
                throw new IllegalArgumentException();
            case LIKE:

                final Like like = (Like) notificationBaseLocal;
                viewHolder.userName.setText(like.getHostName());
                viewHolder.userInitials.setText(MiscUtils.generateInitials(like.getHostName()));
                viewHolder.linearLayout.getLayoutParams().height = a;
                viewHolder.linearLayout.setClickable(false);
                viewHolder.actionBlock.setVisibility(View.GONE);
                viewHolder.librarayBtn.setVisibility(View.VISIBLE);
                viewHolder.notifType.setText(" likes " + like.getSongName());
                Picasso.with(activity).load(StaticData.cloudStorageImageBaseUrl + like.getImageId())
                        .transform(transform)
                        .into(viewHolder.profilePhoto);
                break;

            case PUSH:

                final Push push = (Push) notificationBaseLocal;
                viewHolder.userName.setText(push.getHostName());
                viewHolder.userInitials.setText(MiscUtils.generateInitials(push.getHostName()));
                Picasso.with(activity).load(StaticData.cloudStorageImageBaseUrl + push.getImageId()).transform(transform).into(viewHolder.profilePhoto);

                viewHolder.librarayBtn.setVisibility(View.GONE);
                viewHolder.actionBlock.setVisibility(View.VISIBLE);

                final String customMessage = push.getCustomMessage();
                final StringBuilder buffer = new StringBuilder();
                if (!TextUtils.isEmpty(customMessage))
                    buffer.append(customMessage).append(". Start listening to ");
                else
                    buffer.append("wants you to listen to ");

                buffer.append(push.getFirstSongName());

                if (push.getSize() == 2)
                    buffer.append(" and 1 other song");
                else if (push.getSize() > 2)
                    buffer.append(" and ").append(push.getSize() - 1).append(" other songs");

                viewHolder.notifType.setText(buffer.toString());

                if (push.getExpanded() == 0)
                    viewHolder.linearLayout.getLayoutParams().height = a;
                else
                    viewHolder.linearLayout.getLayoutParams().height = b;

                viewHolder.linearLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (push.getExpanded() == 0) {
                            expand(viewHolder.linearLayout, a, b);
                            push.setExpanded((short) 1);
                        } else {
                            expand(viewHolder.linearLayout, b, a);
                            push.setExpanded((short) 0);
                        }
                    }
                });

                viewHolder.accept.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        final Intent pushAddSong = new Intent(activity, ReachActivity.class);
                        pushAddSong.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        pushAddSong.setAction("process_multiple");
                        pushAddSong.putExtra("data", push.getPushContainer());
                        //start the Activity
                        activity.startActivity(pushAddSong);

                        ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                                .setCategory("Accept - Pushed song")
                                .setAction("User - " + SharedPrefUtils.getServerId(activity.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)))
                                .setAction("User Name - " + SharedPrefUtils.getUserName(activity.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)))
                                .setLabel("Sender - " + push.getHostName() + ", Songs - " + push.getSize())
                                .setValue(push.getSize())
                                .build());

                        expand(viewHolder.linearLayout, b, a);
                        viewHolder.linearLayout.setClickable(false);
                        viewHolder.actionBlock.setVisibility(View.GONE);
                        viewHolder.librarayBtn.setVisibility(View.VISIBLE);
                        String txt = " song";
                        if (push.getSize() > 1)
                            txt = txt + "s";
                        viewHolder.notifType.setText(" pushed " + push.getSize() + txt + " to you");

                        final PushAccepted accepted = new PushAccepted();
                        accepted.setHostName(push.getHostName());
                        accepted.setRead(push.getRead());
                        accepted.setHostId(push.getHostId());
                        accepted.setImageId(push.getImageId());
                        accepted.setSystemTime(push.getSystemTime());
                        accepted.setExpanded((short) 0);
                        accepted.setTypes(Types.PUSH_ACCEPTED);
                        accepted.setSize(push.getSize());
                        accepted.setFirstSongName(push.getHostName());

                        MiscUtils.autoRetryAsync(new DoWork<Void>() {
                            @Override
                            public Void doWork() throws IOException {
                                return StaticData.notificationApi.pushAccepted(
                                        accepted.getFirstSongName(),
                                        push.getNotificationId(),
                                        serverID,
                                        push.getHostId(),
                                        push.getSize()).execute();
                            }
                        }, Optional.<Predicate<Void>>absent());

                    }
                });
                viewHolder.reject.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        remove(push);
                        MiscUtils.autoRetryAsync(new DoWork<Void>() {
                            @Override
                            public Void doWork() throws IOException {
                                return StaticData.notificationApi.removeNotification(push.getNotificationId(), serverID).execute();
                            }
                        }, Optional.<Predicate<Void>>absent());
                    }
                });
                break;

            case BECAME_FRIENDS:

                final BecameFriends becameFriends = (BecameFriends) notificationBaseLocal;
                viewHolder.userName.setText(becameFriends.getHostName());
                viewHolder.userInitials.setText(MiscUtils.generateInitials(becameFriends.getHostName()));
                viewHolder.linearLayout.getLayoutParams().height = a;
                viewHolder.linearLayout.setClickable(false);
                viewHolder.actionBlock.setVisibility(View.GONE);
                viewHolder.librarayBtn.setVisibility(View.VISIBLE);
                viewHolder.notifType.setText("added to your friends");
                Picasso.with(activity).load(StaticData.cloudStorageImageBaseUrl + becameFriends.getImageId()).transform(transform).into(viewHolder.profilePhoto);
                break;
            case PUSH_ACCEPTED:

                final PushAccepted pushAccepted = (PushAccepted) notificationBaseLocal;
                viewHolder.userName.setText(pushAccepted.getHostName());
                viewHolder.userInitials.setText(MiscUtils.generateInitials(pushAccepted.getHostName()));
                viewHolder.linearLayout.getLayoutParams().height = a;
                viewHolder.linearLayout.setClickable(false);
                viewHolder.actionBlock.setVisibility(View.GONE);
                viewHolder.librarayBtn.setVisibility(View.VISIBLE);

                final StringBuilder builder = new StringBuilder();
                builder.append("pushed ").append(pushAccepted.getSize()).append(" song");
                if (pushAccepted.getSize() > 1)
                    builder.append("s ");
                builder.append(" to you");
                viewHolder.notifType.setText(builder.toString());
                Picasso.with(activity).load(StaticData.cloudStorageImageBaseUrl + pushAccepted.getImageId()).transform(transform).into(viewHolder.profilePhoto);
                break;
        }
        return convertView;
    }
}