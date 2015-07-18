package reach.project.adapter;

import android.animation.ValueAnimator;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.io.IOException;

import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.database.notifications.BecameFriends;
import reach.project.database.notifications.Like;
import reach.project.database.notifications.Push;
import reach.project.database.notifications.PushAccepted;
import reach.project.database.notifications.Types;
import reach.project.database.sql.ReachNotificationsHelper;
import reach.project.utils.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.PushContainer;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.StringCompress;
import reach.project.viewHelpers.CircleTransform;

/**
 * Created by ashish on 10/07/15.
 */
public class ReachNotificationAdapter extends ResourceCursorAdapter {

    private boolean open;
    private Application reachApplication;

    public ReachNotificationAdapter(Context context, int layout, Cursor c, int flags, Application reachApp) {
        super(context, layout, c, flags);
        this.reachApplication = reachApp;
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
    public void bindView(View view, final Context context, Cursor cursor) {
        if (cursor == null) return;

        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        final int a = MiscUtils.dpToPx(80);
        final int b = MiscUtils.dpToPx(125);
        final Types type = Types.valueOf(cursor.getString(1));

        switch (type) {

            case DEFAULT:
                throw new IllegalArgumentException();
            case LIKE:

                Like like = ReachNotificationsHelper.getLike(cursor).get();
                viewHolder.userName.setText(like.getHostName());
                viewHolder.userInitials.setText(MiscUtils.generateInitials(like.getHostName()));
                viewHolder.linearLayout.getLayoutParams().height = a;
                viewHolder.actionBlock.setVisibility(View.GONE);
                viewHolder.librarayBtn.setVisibility(View.VISIBLE);
                viewHolder.notifType.setText(" likes " + like.getSongName());
                Picasso.with(context).load(StaticData.cloudStorageImageBaseUrl + like.getImageId())
                        .transform(new CircleTransform())
                        .into(viewHolder.profilePhoto);
                break;
            case PUSH:

                Push push = ReachNotificationsHelper.getPush(cursor).get();
                open = true;
                viewHolder.userName.setText(push.getHostName());
                viewHolder.userInitials.setText(MiscUtils.generateInitials(push.getHostName()));
                Picasso.with(context).load(StaticData.cloudStorageImageBaseUrl + push.getImageId()).transform(new CircleTransform()).into(viewHolder.profilePhoto);


                final String unCompressed;
                try {
                    unCompressed = StringCompress.decompress(Base64.decode(push.getPushContainer(), Base64.DEFAULT));
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                final PushContainer pushContainer = new Gson().fromJson(unCompressed, PushContainer.class);

                String cMsg = pushContainer.getCustomMessage();
                String msg;
                if (cMsg != null && cMsg.length() > 0)
                    msg = cMsg + ". Start listening to ";
                else
                    msg = "wants you to listen to ";

                msg = msg + pushContainer.getFirstSongName();

                if (pushContainer.getSongCount() == 2)
                    msg = msg + " and 1 other song";
                else if (pushContainer.getSongCount() > 2)
                    msg = msg + " and " + (pushContainer.getSongCount() - 1) + " other songs";

                viewHolder.notifType.setText(msg);

                viewHolder.linearLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!open) {
                            expand(viewHolder.linearLayout, a, b);
                            open = true;
                        } else {
                            expand(viewHolder.linearLayout, b, a);
                            open = false;
                        }
                    }
                });
                viewHolder.accept.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        final Intent pushAddSong = new Intent(context, ReachActivity.class);
                        MiscUtils.autoRetryAsync(new DoWork<Void>() {
                            @Override
                            protected Void doWork() throws IOException {
                                return StaticData.notificationApi.pushAccepted(pushContainer.getFirstSongName(),
                                        pushContainer.hashCode(),
                                        pushContainer.getReceiverId(),
                                        pushContainer.getSenderId(),
                                        (int) pushContainer.getSongCount()).execute();
                            }
                        }, Optional.<Predicate<Void>>absent());

                        if (!StaticData.debugMode) {
                            ((ReachApplication) reachApplication).getTracker().send(new HitBuilders.EventBuilder()
                                    .setCategory("Accept - Pushed song")
                                    .setAction("User - " + SharedPrefUtils.getServerId(context.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)))
                                    .setAction("User Name - " + SharedPrefUtils.getUserName(context.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)))
                                    .setLabel("Sender - " + pushContainer.getUserName() + ", Songs - " + pushContainer.getSongCount())
                                    .setValue(pushContainer.getSongCount())
                                    .build());
                        }
                        pushAddSong.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        pushAddSong.setAction("process_multiple");
                        pushAddSong.putExtra("data", unCompressed);
                        //start the Activity
                        context.startActivity(pushAddSong);

                        expand(viewHolder.linearLayout, b, a);
                        viewHolder.actionBlock.setVisibility(View.GONE);
                        viewHolder.librarayBtn.setVisibility(View.VISIBLE);
                        String txt = " song";
                        if (pushContainer.getSongCount() > 1)
                            txt = txt + "s";
                        viewHolder.notifType.setText(" pushed " + pushContainer.getSongCount() + txt + " to you");
                        viewHolder.linearLayout.setOnClickListener(null);
                        //TODO delete push object

                    }
                });
                viewHolder.reject.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //TODO delete push object
                    }
                });
                break;
            case BECAME_FRIENDS:
                BecameFriends becameFriends = ReachNotificationsHelper.getBecameFriends(cursor).get();
                viewHolder.userName.setText(becameFriends.getHostName());
                viewHolder.userInitials.setText(MiscUtils.generateInitials(becameFriends.getHostName()));
                viewHolder.linearLayout.getLayoutParams().height = a;
                viewHolder.actionBlock.setVisibility(View.GONE);
                viewHolder.librarayBtn.setVisibility(View.VISIBLE);
                viewHolder.notifType.setText("added to your friends");
                Picasso.with(context).load(StaticData.cloudStorageImageBaseUrl + becameFriends.getImageId()).transform(new CircleTransform()).into(viewHolder.profilePhoto);
                break;
            case PUSH_ACCEPTED:
                PushAccepted pushAccepted = ReachNotificationsHelper.getPushAccepted(cursor).get();
                viewHolder.userName.setText(pushAccepted.getHostName());
                viewHolder.userInitials.setText(MiscUtils.generateInitials(pushAccepted.getHostName()));
                viewHolder.linearLayout.getLayoutParams().height = a;
                viewHolder.actionBlock.setVisibility(View.GONE);
                viewHolder.librarayBtn.setVisibility(View.VISIBLE);
                String txt = " song";
                if (pushAccepted.getSize() > 1)
                    txt = txt + "s";
                viewHolder.notifType.setText("pushed " + pushAccepted.getSize() + txt + " to you");
                Picasso.with(context).load(StaticData.cloudStorageImageBaseUrl + pushAccepted.getImageId()).transform(new CircleTransform()).into(viewHolder.profilePhoto);
                break;
        }

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        final View view = super.newView(context, cursor, parent);
        final ViewHolder viewHolder = new ViewHolder(
                (ImageView) view.findViewById(R.id.profilePhotoList),
                (TextView) view.findViewById(R.id.userNameList),
                (TextView) view.findViewById(R.id.notifType),
                (TextView) view.findViewById(R.id.userInitials),
                (LinearLayout) view.findViewById(R.id.libraryButton),
                (LinearLayout) view.findViewById(R.id.actionBlock),
                (LinearLayout) view.findViewById(R.id.linearLayout),
                (RelativeLayout) view.findViewById(R.id.acceptBlock),
                (RelativeLayout) view.findViewById(R.id.rejectBlock));
        view.setTag(viewHolder);
        return view;
    }
}
