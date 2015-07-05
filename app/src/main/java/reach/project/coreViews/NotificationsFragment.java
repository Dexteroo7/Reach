package reach.project.coreViews;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import reach.project.R;
import reach.project.utils.MiscUtils;
import reach.project.utils.SuperInterface;


public class NotificationsFragment extends Fragment {

    public static NotificationsFragment newInstance() {
        return new NotificationsFragment();
    }
    public NotificationsFragment() {
    }

    private SuperInterface mListener;
    private boolean open;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (SuperInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnInviteDialogListener");
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_notifications, container, false);
        final ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
        if(actionBar != null)
            actionBar.setTitle("Notifications");
        final ListView notificationsList = (ListView) rootView.findViewById(R.id.notificationsList);
        notificationsList.setAdapter(new NotificationsAdapter(getActivity(),R.layout.notification_item,
                new String[]{"Ashish Kumar`1","Ayush Verma`2"}));
        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private class NotificationsAdapter extends ArrayAdapter<String> {
        int rId;
        public NotificationsAdapter(Context context, int resourceId, String[] notifications) {
            super(context, resourceId, notifications);
            this.rId = resourceId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(rId, parent, false);
            }
            final TextView userName = (TextView) convertView.findViewById(R.id.userNameList);
            final TextView notifType = (TextView) convertView.findViewById(R.id.notifType);
            final LinearLayout librarayBtn = (LinearLayout) convertView.findViewById(R.id.libraryButton);
            final LinearLayout actionBlock = (LinearLayout) convertView.findViewById(R.id.actionBlock);
            final LinearLayout linearLayout = (LinearLayout) convertView.findViewById(R.id.linearLayout);
            final RelativeLayout accept = (RelativeLayout) convertView.findViewById(R.id.acceptBlock);
            final RelativeLayout reject = (RelativeLayout) convertView.findViewById(R.id.rejectBlock);

            final String notif = getItem(position);

            final int a = MiscUtils.dpToPx(80);
            final int b = MiscUtils.dpToPx(125);

            userName.setText(notif.split("`")[0]);
            if (notif.split("`")[1].equals("1")) {
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
            }
            else {
                linearLayout.getLayoutParams().height = a;
                actionBlock.setVisibility(View.GONE);
                librarayBtn.setVisibility(View.VISIBLE);
                notifType.setText("added to your friends");
            }
            return convertView;
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
}
