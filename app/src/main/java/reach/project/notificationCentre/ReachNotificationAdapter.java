package reach.project.notificationCentre;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.common.base.Optional;

import java.util.List;

import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.notificationCentre.notifications.BecameFriends;
import reach.project.notificationCentre.notifications.Like;
import reach.project.notificationCentre.notifications.NotificationBaseLocal;
import reach.project.notificationCentre.notifications.Push;
import reach.project.notificationCentre.notifications.PushAccepted;
import reach.project.notificationCentre.notifications.Types;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

/**
 * Created by ashish on 10/07/15.
 */
public class ReachNotificationAdapter extends ArrayAdapter<NotificationBaseLocal> {

    public static final SparseBooleanArray accepted = new SparseBooleanArray();
    private static final SparseBooleanArray opened = new SparseBooleanArray();
    private static final int a = MiscUtils.dpToPx(70);
    private static final int b = MiscUtils.dpToPx(110);

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

        private final SimpleDraweeView profilePhoto;
        private final TextView userName, notifType, userInitials;
        private final LinearLayout librarayBtn, actionBlock, linearLayout;
        private final RelativeLayout accept, reject;

        private ViewHolder(SimpleDraweeView profilePhoto,
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

    private static void expand(final View view, int a, int b) {

        final ValueAnimator animator = ValueAnimator.ofInt(a, b).setDuration(300);
        animator.addUpdateListener(animation -> {

            view.getLayoutParams().height = (Integer) animation.getAnimatedValue();
            view.requestLayout();
        });
        animator.start();
    }

    private static final View.OnClickListener expander = view -> {

        final int itemId = (int) view.getTag();
        final boolean toggle = !opened.get(itemId, false);

        if (toggle)
            expand(view, a, b);
        else
            expand(view, b, a);
        opened.put(itemId, toggle);
    };

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        //TODO
        //MyReachFragment.checkNewNotifications();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        final ViewHolder viewHolder;
        if (convertView == null) {

            convertView = LayoutInflater.from(activity).inflate(resID, parent, false);
            viewHolder = new ViewHolder(
                    (SimpleDraweeView) convertView.findViewById(R.id.profilePhotoList),
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


        final NotificationBaseLocal notificationBaseLocal = getItem(position);
        final Types type = notificationBaseLocal.getTypes();
        final StringBuilder builder = new StringBuilder();

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
                viewHolder.profilePhoto.setController(MiscUtils.getControllerwithResize(viewHolder.profilePhoto.getController(),
                        Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + like.getImageId()), 100, 100));
                break;

            case PUSH:

                final Push push = (Push) notificationBaseLocal;

                viewHolder.userName.setText(push.getHostName());
                viewHolder.userInitials.setText(MiscUtils.generateInitials(push.getHostName()));
                viewHolder.profilePhoto.setController(MiscUtils.getControllerwithResize(viewHolder.profilePhoto.getController(),
                        Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + push.getImageId()), 100, 100));

                if (accepted.get(notificationBaseLocal.getNotificationId(), false)) {

                    viewHolder.linearLayout.getLayoutParams().height = a;
                    viewHolder.linearLayout.setClickable(false);
                    viewHolder.actionBlock.setVisibility(View.GONE);
                    viewHolder.librarayBtn.setVisibility(View.VISIBLE);

                    builder.append("shared ").append(push.getSize()).append(" song");
                    if (push.getSize() > 1)
                        builder.append("s ");
                    builder.append(" with you");
                    viewHolder.notifType.setText(builder.toString());

                } else {

                    viewHolder.librarayBtn.setVisibility(View.GONE);
                    viewHolder.actionBlock.setVisibility(View.VISIBLE);

                    final String customMessage = push.getCustomMessage();
                    if (!TextUtils.isEmpty(customMessage))
                        builder.append(customMessage).append(". Start listening to ");
                    else
                        builder.append("wants you to listen to ");

                    builder.append(push.getFirstSongName());

                    if (push.getSize() == 2)
                        builder.append(" and 1 other song");
                    else if (push.getSize() > 2)
                        builder.append(" and ").append(push.getSize() - 1).append(" other songs");

                    viewHolder.notifType.setText(builder.toString());

                    if (!opened.get(notificationBaseLocal.getNotificationId(), false))
                        viewHolder.linearLayout.getLayoutParams().height = a;
                    else
                        viewHolder.linearLayout.getLayoutParams().height = b;
                    viewHolder.linearLayout.setTag(notificationBaseLocal.getNotificationId());
                    viewHolder.linearLayout.setOnClickListener(expander);

                    viewHolder.accept.setOnClickListener(view -> {

                        final Intent pushAddSong = new Intent(activity, ReachActivity.class);
                        pushAddSong.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        pushAddSong.setAction(ReachActivity.ADD_PUSH_SONG);
                        pushAddSong.putExtra("data", push.getPushContainer());
                        //start the Activity
                        activity.startActivity(pushAddSong);

                        ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                                .setCategory("Accept - Pushed song")
                                .setAction("User Name - " + SharedPrefUtils.getUserName(activity.getSharedPreferences("Reach", Context.MODE_PRIVATE)))
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
                        viewHolder.notifType.setText(" shared " + push.getSize() + txt + " with you");

                        MiscUtils.autoRetryAsync(() -> StaticData.NOTIFICATION_API.pushAccepted(
                                push.getFirstSongName(),
                                push.getNotificationId(),
                                serverID,
                                push.getHostId(),
                                push.getSize()).execute(), Optional.absent());
                        accepted.put(notificationBaseLocal.getNotificationId(), true);

                    });

                    viewHolder.reject.setOnClickListener(v -> {

                        MiscUtils.autoRetryAsync(() -> StaticData.NOTIFICATION_API.removeNotification(push.getNotificationId(), serverID).execute(), Optional.absent());
                        remove(push);
                        accepted.delete(notificationBaseLocal.getNotificationId());
                    });
                }
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
                viewHolder.profilePhoto.setController(MiscUtils.getControllerwithResize(viewHolder.profilePhoto.getController(),
                        Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + becameFriends.getImageId()), 100, 100));
                break;

            case PUSH_ACCEPTED:

                final PushAccepted pushAccepted = (PushAccepted) notificationBaseLocal;
                viewHolder.userName.setText(pushAccepted.getHostName());
                viewHolder.userInitials.setText(MiscUtils.generateInitials(pushAccepted.getHostName()));
                viewHolder.linearLayout.getLayoutParams().height = a;
                viewHolder.linearLayout.setClickable(false);
                viewHolder.actionBlock.setVisibility(View.GONE);
                viewHolder.librarayBtn.setVisibility(View.VISIBLE);

                builder.append("shared ").append(pushAccepted.getSize()).append(" song");
                if (pushAccepted.getSize() > 1)
                    builder.append("s ");
                builder.append(" with you");
                viewHolder.notifType.setText(builder.toString());
                viewHolder.profilePhoto.setController(MiscUtils.getControllerwithResize(viewHolder.profilePhoto.getController(),
                        Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + pushAccepted.getImageId()), 100, 100));
                break;
        }
        return convertView;
    }
}