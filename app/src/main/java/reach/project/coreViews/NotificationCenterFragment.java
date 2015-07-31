package reach.project.coreViews;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import reach.project.R;
import reach.project.viewHelpers.CustomViewPager;
import reach.project.viewHelpers.ViewPagerReusable;


public class NotificationCenterFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_notification_center, container, false);
        final CustomViewPager viewPager = (CustomViewPager) rootView.findViewById(R.id.viewPager);
        viewPager.setAdapter(new ViewPagerReusable(
                getChildFragmentManager(),
                new String[]{"Requests", "Notifications"},
                new Fragment[]{
                        FriendRequestFragment.newInstance(),
                        NotificationFragment.newInstance()}));
        viewPager.setPagingEnabled(false);

        final TabLayout slidingTabLayout = (TabLayout) rootView.findViewById(R.id.sliding_tabs);
        /*slidingTabLayout.setDistributeEvenly(true);
        slidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return Color.parseColor("#FFCC0000");
            }
        });*/
        slidingTabLayout.setupWithViewPager(viewPager);
        return rootView;
    }
}