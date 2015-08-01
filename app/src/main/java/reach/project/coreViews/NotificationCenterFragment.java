package reach.project.coreViews;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.viewHelpers.CustomViewPager;
import reach.project.viewHelpers.SlidingTabLayout;
import reach.project.viewHelpers.ViewPagerReusable;


public class NotificationCenterFragment extends Fragment {


    private static WeakReference<NotificationCenterFragment> reference;

    public static NotificationCenterFragment newInstance() {

        NotificationCenterFragment fragment;
        if(reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new NotificationCenterFragment());
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //swipeContainerNotifications
        final View rootView = inflater.inflate(R.layout.fragment_notification_center, container, false);
        final CustomViewPager viewPager = (CustomViewPager) rootView.findViewById(R.id.viewPager);

        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeContainerContacts);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.reach_color), getResources().getColor(R.color.reach_blue));
        swipeRefreshLayout.setBackgroundResource(R.color.white);
        swipeRefreshLayout.setOnRefreshListener(LocalUtils.refreshListener);

        viewPager.setAdapter(new ViewPagerReusable(
                getChildFragmentManager(),
                new String[]{"Requests", "notifications"},
                new Fragment[]{
                        FriendRequestFragment.newInstance(),
                        NotificationFragment.newInstance()}));
        viewPager.setPagingEnabled(false);

        final SlidingTabLayout slidingTabLayout = (SlidingTabLayout) rootView.findViewById(R.id.sliding_tabs);
        slidingTabLayout.setDistributeEvenly(true);
        slidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return Color.parseColor("#FFCC0000");
            }
        });
        slidingTabLayout.setViewPager(viewPager);
        return rootView;
    }

    public static final SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {

            final ContactsListFragment fragment;
            if (reference == null || (fragment = reference.get()) == null)
                return;

            if (!pinging.get()) {
                pinging.set(true);
                new SendPing().executeOnExecutor(StaticData.threadPool, fragment.swipeRefreshLayout);
            }
        }
    };

    public static final AbsListView.OnScrollListener scrollListener = new AbsListView.OnScrollListener() {

        @Override
        public void onScrollStateChanged(AbsListView absListView, int i) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {

            boolean enable = false;
            if (view.getChildCount() > 0) {

                final boolean firstItemVisible = view.getFirstVisiblePosition() == 0;
                final boolean topOfFirstItemVisible = view.getChildAt(0).getTop() == 0;
                enable = firstItemVisible && topOfFirstItemVisible;
            }

            swipeRefreshLayout.setEnabled(enable);
        }
    };

}