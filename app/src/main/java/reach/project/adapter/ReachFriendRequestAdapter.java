package reach.project.adapter;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import reach.backend.entities.userApi.model.ReachFriend;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;
import reach.project.viewHelpers.CircleTransform;

/**
 * Created by ashish on 14/07/15.
 */
public class ReachFriendRequestAdapter extends ArrayAdapter<ReachFriend> {

    private Context ctx;
    private int rId;
    private boolean open;

    public ReachFriendRequestAdapter(Context context, int resourceId, List<ReachFriend> reachFriendList) {
        super(context, resourceId, reachFriendList);
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
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(rId, parent, false);
        }
        final ImageView profilePhoto = (ImageView) convertView.findViewById(R.id.profilePhotoList);
        final TextView userName = (TextView) convertView.findViewById(R.id.userNameList);
        final TextView notifType = (TextView) convertView.findViewById(R.id.notifType);
        final LinearLayout librarayBtn = (LinearLayout) convertView.findViewById(R.id.libraryButton);
        final LinearLayout actionBlock = (LinearLayout) convertView.findViewById(R.id.actionBlock);
        final LinearLayout linearLayout = (LinearLayout) convertView.findViewById(R.id.linearLayout);
        final RelativeLayout accept = (RelativeLayout) convertView.findViewById(R.id.acceptBlock);
        final RelativeLayout reject = (RelativeLayout) convertView.findViewById(R.id.rejectBlock);

        final ReachFriend reachFriend = getItem(position);

        final int a = MiscUtils.dpToPx(80);
        final int b = MiscUtils.dpToPx(125);

        Picasso.with(ctx).load(StaticData.cloudStorageImageBaseUrl + reachFriend.getImageId()).transform(new CircleTransform()).into(profilePhoto);
        userName.setText(reachFriend.getUserName());
        open = true;
        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!open) {
                    expand(linearLayout,a,b);
                    open = true;
                }
                else {
                    expand(linearLayout,b,a);
                    open = false;
                }
            }
        });
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linearLayout.setClickable(false);
                expand(linearLayout,b,a);
                actionBlock.setVisibility(View.GONE);
                librarayBtn.setVisibility(View.VISIBLE);
                notifType.setText("added to your friends");
            }
        });
        reject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //delete entry
            }
        });
        /*else {
            linearLayout.getLayoutParams().height = a;
            actionBlock.setVisibility(View.GONE);
            librarayBtn.setVisibility(View.VISIBLE);
            notifType.setText("added to your friends");
        }*/
        return convertView;
    }
}
