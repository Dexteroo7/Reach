package reach.project.adapter;

import android.animation.ValueAnimator;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.google.gson.Gson;

import reach.project.R;
import reach.project.database.notifications.BecameFriends;
import reach.project.database.notifications.Like;
import reach.project.database.notifications.Push;
import reach.project.database.notifications.PushAccepted;
import reach.project.database.notifications.Types;
import reach.project.database.sql.ReachNotificationsHelper;
import reach.project.utils.MiscUtils;
import reach.project.utils.PushContainer;

/**
 * Created by ashish on 10/07/15.
 */
public class ReachNotificationAdapter extends ResourceCursorAdapter {

    private boolean open;

    public ReachNotificationAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
    }

    private final class ViewHolder{

        private final TextView userName, notifType;
        private final LinearLayout librarayBtn, actionBlock, linearLayout;
        private final RelativeLayout accept, reject;

        private ViewHolder(TextView userName,
                           TextView notifType,
                           LinearLayout librarayBtn,
                           LinearLayout actionBlock,
                           LinearLayout linearLayout,
                           RelativeLayout accept,
                           RelativeLayout reject
                           ) {

            this.userName = userName;
            this.notifType = notifType;
            this.librarayBtn = librarayBtn;
            this.actionBlock = actionBlock;
            this.linearLayout = linearLayout;
            this.accept = accept;
            this.reject = reject;
        }
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
    public void bindView(View view, Context context, Cursor cursor) {
        if(cursor == null) return;

        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        final int a = MiscUtils.dpToPx(80);
        final int b = MiscUtils.dpToPx(125);


        Types type = Types.DEFAULT;
        switch (type) {
            case DEFAULT:
                break;
            case LIKE:
                Like like = ReachNotificationsHelper.getLike(cursor).get();
                viewHolder.userName.setText(like.getHostName());
                viewHolder.linearLayout.getLayoutParams().height = a;
                viewHolder.actionBlock.setVisibility(View.GONE);
                viewHolder.librarayBtn.setVisibility(View.VISIBLE);
                viewHolder.notifType.setText( " likes " + like.getSongName());
                break;
            case PUSH:
                Push push = ReachNotificationsHelper.getPush(cursor).get();
                open = true;
                viewHolder.userName.setText(push.getHostName());

                final PushContainer pushContainer = new Gson().fromJson(push.getPushContainer(), PushContainer.class);

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
                        viewHolder.linearLayout.setClickable(false);
                        expand(viewHolder.linearLayout, b, a);
                        viewHolder.actionBlock.setVisibility(View.GONE);
                        viewHolder.librarayBtn.setVisibility(View.VISIBLE);
                        String txt = " song";
                        if (pushContainer.getSongCount()>1)
                            txt = txt + "s";
                        viewHolder.notifType.setText(" pushed " + pushContainer.getSongCount() + txt + " to you");
                    }
                });
                viewHolder.reject.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //delete entry
                    }
                });
                break;
            case BECAME_FRIENDS:
                BecameFriends becameFriends = ReachNotificationsHelper.getBecameFriends(cursor).get();
                viewHolder.userName.setText(becameFriends.getHostName());
                viewHolder.linearLayout.getLayoutParams().height = a;
                viewHolder.actionBlock.setVisibility(View.GONE);
                viewHolder.librarayBtn.setVisibility(View.VISIBLE);
                viewHolder.notifType.setText("added to your friends");
                break;
            case PUSH_ACCEPTED:
                PushAccepted pushAccepted = ReachNotificationsHelper.getPushAccepted(cursor).get();
                viewHolder.userName.setText(pushAccepted.getHostName());
                viewHolder.linearLayout.setClickable(false);
                expand(viewHolder.linearLayout, b, a);
                viewHolder.actionBlock.setVisibility(View.GONE);
                viewHolder.librarayBtn.setVisibility(View.VISIBLE);
                String txt = " song";
                if (pushAccepted.getSize()>1)
                    txt = txt + "s";
                viewHolder.notifType.setText(" pushed " + pushAccepted.getSize() + txt + " to you");
                break;
        }

        /*viewHolder.userName.setText();
        if () {
            open = true;
            viewHolder.linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!open) {
                        expand(viewHolder.linearLayout,a,b);
                        open = true;
                    }
                    else {
                        expand(viewHolder.linearLayout,b,a);
                        open = false;
                    }
                }
            });
            viewHolder.accept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewHolder.linearLayout.setClickable(false);
                    expand(viewHolder.linearLayout,b,a);
                    viewHolder.actionBlock.setVisibility(View.GONE);
                    viewHolder.librarayBtn.setVisibility(View.VISIBLE);
                    viewHolder.notifType.setText("added to your friends");
                }
            });
            viewHolder.reject.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //delete entry
                }
            });
        }
        else {
            viewHolder.linearLayout.getLayoutParams().height = a;
            viewHolder.actionBlock.setVisibility(View.GONE);
            viewHolder.librarayBtn.setVisibility(View.VISIBLE);
            viewHolder.notifType.setText("added to your friends");
        }*/
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        final View view = super.newView(context, cursor, parent);
        final ViewHolder viewHolder = new ViewHolder(
                (TextView) view.findViewById(R.id.userNameList),
                (TextView) view.findViewById(R.id.notifType),
                (LinearLayout) view.findViewById(R.id.libraryButton),
                (LinearLayout) view.findViewById(R.id.actionBlock),
                (LinearLayout) view.findViewById(R.id.linearLayout),
                (RelativeLayout) view.findViewById(R.id.acceptBlock),
                (RelativeLayout) view.findViewById(R.id.rejectBlock));
        view.setTag(viewHolder);
        return view;
    }
}
