package reach.project.viewHelpers;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by dexter on 6/7/14.
 */
public class ViewPagerReusable extends FragmentPagerAdapter {

    private final String[] items;
    private final Fragment[] fragments;

    public ViewPagerReusable(final FragmentManager fm, final String[] tags,
                             final Fragment[] fragments) {

        super(fm);
        this.items = tags;
        this.fragments = fragments;
    }

    @Override
    public Fragment getItem(int position) {
        return fragments[position];
    }

    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return items[position];
    }
}
