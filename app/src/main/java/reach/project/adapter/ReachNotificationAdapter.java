package reach.project.adapter;

import android.animation.ValueAnimator;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import reach.backend.entities.userApi.model.ReachFriend;
import reach.project.R;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.utils.MiscUtils;

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

        String notif = "";

        viewHolder.userName.setText(notif.split("`")[0]);
        if (notif.split("`")[1].equals("1")) {
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
        }
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
