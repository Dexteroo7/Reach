package reach.project.coreViews;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.utils.auxiliaryClasses.SuperInterface;
import reach.project.viewHelpers.ViewPagerReusable;

public class MyReachFragment extends Fragment {

    private TextView notificationCount;

    private SuperInterface mListener;

    private static WeakReference<MyReachFragment> reference = null;

    public static MyReachFragment newInstance() {

        MyReachFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            Log.i("Ayush", "Creating new instance of my reach fragment");
            reference = new WeakReference<>(fragment = new MyReachFragment());
        } else
            Log.i("Ayush", "Reusing contacts list fragment object :)");

        return fragment;
    }

    public static void checkNewNotifications() {

        final MyReachFragment fragment;
        if (reference == null || (fragment = reference.get()) == null || fragment.notificationCount == null)
            return;

        final int friendRequestCount = FriendRequestFragment.receivedRequests.size();
        final int notificationsCount = NotificationFragment.notifications.size();

        if (friendRequestCount == 0 && notificationsCount == 0)
            fragment.notificationCount.setVisibility(View.GONE);
        else {
            fragment.notificationCount.setVisibility(View.VISIBLE);
            fragment.notificationCount.setText(String.valueOf(friendRequestCount + notificationsCount));
        }
    }

    private final View.OnClickListener openNotification = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mListener.onOpenNotificationDrawer();
        }
    };

    private final View.OnClickListener pushLibraryListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            mListener.onOpenPushLibrary();
        }
    };

    private final View.OnClickListener inviteFriendListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mListener.onOpenInvitePage();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        final Activity activity = getActivity();
        if (activity == null || activity.isFinishing())
            return;
        setHasOptionsMenu(true);
        mListener.setUpDrawer();
        mListener.toggleDrawer(false);
        mListener.toggleSliding(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_myreach, container, false);

        ((AppCompatActivity) getActivity()).setSupportActionBar((Toolbar) rootView.findViewById(R.id.myReachToolbar));
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
            actionBar.setTitle("Reach");
            mListener.setUpNavigationViews();
        }
        mListener.setUpDrawer();
        rootView.findViewById(R.id.share_music_fab).setOnClickListener(pushLibraryListener);
        rootView.findViewById(R.id.invite_friend_fab).setOnClickListener(inviteFriendListener);

        final ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.viewPager);
        viewPager.setAdapter(new ViewPagerReusable(
                getChildFragmentManager(),
                new String[]{"FRIENDS", "INVITE"},
                new Fragment[]{
                        ContactsListFragment.newInstance(), // Friends
                        AllContactsFragment.newInstance()})); // Invite
        final TabLayout slidingTabLayout = (TabLayout) rootView.findViewById(R.id.sliding_tabs);
        slidingTabLayout.post(() -> slidingTabLayout.setupWithViewPager(viewPager));
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        if (menu == null || inflater == null)
            return;
        menu.clear();

        inflater.inflate(R.menu.myreach_menu, menu);

        final MenuItem notificationButton = menu.findItem(R.id.notif_button);
        if (notificationButton == null)
            return;
        notificationButton.setActionView(R.layout.reach_queue_counter);
        final View notificationContainer = notificationButton.getActionView().findViewById(R.id.counterContainer);
        notificationContainer.setOnClickListener(openNotification);
        notificationCount = (TextView) notificationContainer.findViewById(R.id.reach_q_count);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (SuperInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
