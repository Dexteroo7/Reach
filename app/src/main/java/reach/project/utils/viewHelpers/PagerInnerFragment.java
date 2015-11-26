package reach.project.utils.viewHelpers;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import reach.project.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class PagerInnerFragment extends Fragment {

    public static final String PARCEL_INNER_PAGER = "PARCEL_INNER_PAGER";

    public static PagerInnerFragment getNewInstance(@NonNull PagerFragment.Pages page) {

        //sanity check
        final String checkHeader = page.header;
        if (TextUtils.isEmpty(checkHeader))
            throw new IllegalArgumentException("Must provide a page header");

        //sanity check
        final String[] checkTitles = page.titles;
        if (checkTitles == null)
            throw new IllegalArgumentException("Page titles are missing");

        final String[] fragmentNames = page.classes;
        if (fragmentNames == null || fragmentNames.length != checkTitles.length)
            throw new IllegalArgumentException("Page title and fragment count mismatch");

        //return
        final Bundle bundle = new Bundle(1);
        bundle.putParcelable(PARCEL_INNER_PAGER, page);

        final PagerInnerFragment pagerInnerFragment = new PagerInnerFragment();
        pagerInnerFragment.setArguments(bundle);
        return pagerInnerFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_pager_inner, container, false);
        final ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.viewPagerInner);
        final TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.tabLayoutInner);
        final TextView pageHeaderText = (TextView) rootView.findViewById(R.id.pageHeaderText);

        final Bundle arguments = getArguments();
        final Parcelable parcelables = arguments.getParcelable(PARCEL_INNER_PAGER);
        if (parcelables == null || !(parcelables instanceof PagerFragment.Pages))
            return null;

        final PagerFragment.Pages page = (PagerFragment.Pages) parcelables;
        final Fragment[] fragments = new Fragment[page.classes.length];
        final String[] titles = new String[page.titles.length];

        for (int index = 0; index < page.classes.length; index++) {

            try {
                final Class fragmentClass = Class.forName(page.classes[index]);
                final Method invoker = fragmentClass.getMethod("getInstance", String.class);
                fragments[index] = (Fragment) invoker.invoke(null, "Bitch");
            } catch (ClassNotFoundException | NoSuchMethodException |
                    IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }

            titles[index] = page.titles[index];
        }

        viewPager.setOffscreenPageLimit(1); //1 is good enough to support lazy loading
        viewPager.setAdapter(new FragmentPagerAdapter(getChildFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return fragments[position];
            }

            @Override
            public int getCount() {
                return fragments.length;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return titles[position];
            }
        });


        tabLayout.setupWithViewPager(viewPager);
        pageHeaderText.setText(page.header);
        return rootView;
    }
}