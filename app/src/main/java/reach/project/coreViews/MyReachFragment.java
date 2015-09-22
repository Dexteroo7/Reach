package reach.project.coreViews;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.friends.ContactsListFragment;
import reach.project.notificationCentre.FriendRequestFragment;
import reach.project.notificationCentre.NotificationFragment;
import reach.project.utils.auxiliaryClasses.SuperInterface;
import reach.project.utils.viewHelpers.ViewPagerReusable;

public class MyReachFragment extends Fragment {

    private TextView notificationCount;

    private SuperInterface mListener;

    private SearchView searchView;

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
        mListener.toggleDrawer(false);
        mListener.toggleSliding(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_myreach, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            FrameLayout myReachFrame = (FrameLayout) rootView.findViewById(R.id.myReachFrame);
            CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) myReachFrame.getLayoutParams();
            layoutParams.setMargins(0,0,0,0);
            myReachFrame.setLayoutParams(layoutParams);
        }
        Toolbar mToolbar = (Toolbar) rootView.findViewById(R.id.myReachToolbar);
        mToolbar.setTitle("Reach");
        mToolbar.setNavigationOnClickListener(v -> mListener.onOpenNavigationDrawer());
        mToolbar.inflateMenu(R.menu.myreach_menu);
        Menu menu = mToolbar.getMenu();
        searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search_button));

        final MenuItem notificationButton = menu.findItem(R.id.notif_button);
        if (notificationButton != null) {
            MenuItemCompat.setActionView(notificationButton, R.layout.reach_queue_counter);
            final View notificationContainer = MenuItemCompat.getActionView(notificationButton).findViewById(R.id.counterContainer);
            notificationContainer.setOnClickListener(openNotification);
            notificationCount = (TextView) notificationContainer.findViewById(R.id.reach_q_count);
        }

        mListener.setUpNavigationViews();
        rootView.findViewById(R.id.share_music_fab).setOnClickListener(pushLibraryListener);
        rootView.findViewById(R.id.invite_friend_fab).setOnClickListener(inviteFriendListener);

        final ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.viewPager);
        ContactsListFragment contactsListFragment = ContactsListFragment.newInstance();
        AllContactsFragment allContactsFragment = AllContactsFragment.newInstance();

        viewPager.setAdapter(new ViewPagerReusable(
                getChildFragmentManager(),
                new String[]{"FRIENDS", "INVITE"},
                new Fragment[]{
                        contactsListFragment, // Friends
                        allContactsFragment})); // Invite
        contactsListFragment.setSearchView(searchView);
        final TabLayout slidingTabLayout = (TabLayout) rootView.findViewById(R.id.sliding_tabs);
        slidingTabLayout.post(() -> slidingTabLayout.setupWithViewPager(viewPager));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0)
                    contactsListFragment.setSearchView(searchView);
                else if (position == 1)
                    allContactsFragment.setSearchView(searchView);

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        return rootView;
    }

    @Override
    public void onDestroyView() {
        if (searchView != null) {
            searchView.setOnQueryTextListener(null);
            searchView.setOnCloseListener(null);
            searchView.setQuery(null, false);
            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }

        searchView = null;
        super.onDestroyView();
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
