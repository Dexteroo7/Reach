package reach.project.utils.viewHelpers;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.astuetz.PagerSlidingTabStrip;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import reach.project.R;
import reach.project.utils.MiscUtils;

/**
 * A placeholder fragment containing a simple view.
 */
public class PagerFragment extends Fragment {

    public static final String PARCEL_PAGER = "PARCEL_PAGER";

    public static class Pages implements Parcelable {

        public Pages(Class<Fragment>[] classes, String[] titles, String headerText) {

            setClasses(classes);
            this.titles = titles;
            this.header = headerText;
        }

        public static final Parcelable.Creator<Pages> CREATOR = new Parcelable.Creator<Pages>() {
            @Override
            public Pages createFromParcel(Parcel in) {
                return new Pages(in);
            }

            @Override
            public Pages[] newArray(int size) {
                return new Pages[size];
            }
        };

        public void setClasses(@NonNull Class<Fragment>[] realClasses) {

            classes = new String[realClasses.length];
            for (int index = 0; index < realClasses.length; index++)
                classes[index] = realClasses[index].getName();
        }

        public String[] classes;
        public String[] titles;
        public String header;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeStringArray(classes);
            dest.writeStringArray(titles);
            dest.writeString(header);
        }

        private Pages(Parcel in) {

            this.classes = in.createStringArray();
            this.titles = in.createStringArray();
            this.header = in.readString();
        }
    }

    public static PagerFragment getNewInstance(@NonNull Pages... pages) {

        //sanity check
        for (Pages page : pages) {

            if (page.classes == null || page.titles == null)
                throw new IllegalArgumentException("Page title or fragment not found");

            if (page.classes.length != page.titles.length)
                throw new IllegalArgumentException("Page title and fragment count mismatch");
        }

        final Bundle bundle = new Bundle(1);
        bundle.putParcelableArray(PARCEL_PAGER, pages);

        final PagerFragment pagerFragment = new PagerFragment();
        pagerFragment.setArguments(bundle);
        return pagerFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_pager, container, false);
        final ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.viewPager);
        final PagerSlidingTabStrip tabLayout = (PagerSlidingTabStrip) rootView.findViewById(R.id.tabLayoutPager);
        final Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.pagerToolbar);

        toolbar.setTitle("Manager");
        toolbar.inflateMenu(R.menu.pager_menu);

        final Bundle arguments = getArguments();
        final Parcelable[] parcelables = arguments.getParcelableArray(PARCEL_PAGER);
        if (parcelables == null || parcelables.length == 0 || !(parcelables instanceof Pages[]))
            return null;

        final Pages[] pages = (Pages[]) parcelables;
        final String[] titles = new String[pages.length];
        final Fragment[] fragments = new Fragment[pages.length];

        for (int index = 0; index < pages.length; index++) {

            titles[index] = pages[index].header;
            if (pages[index].classes.length > 1)
                fragments[index] = PagerInnerFragment.getNewInstance(pages[index]);
            else if (pages[index].classes.length == 1) {

                try {
                    final Class fragmentClass = Class.forName(pages[index].classes[0]);
                    final Method invoker = fragmentClass.getMethod("getInstance", String.class);
                    fragments[index] = (Fragment) invoker.invoke(null, pages[index].header);
                } catch (ClassNotFoundException | NoSuchMethodException |
                        IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }

        viewPager.setAdapter(new FragmentPagerAdapter(getChildFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return fragments[position];
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return titles[position];
            }

            @Override
            public int getCount() {
                return fragments.length;
            }
        });

        viewPager.setOffscreenPageLimit(1); //there is 1 page on either side
        viewPager.setPageMargin(-1 * (MiscUtils.dpToPx(50)));
        viewPager.setPageTransformer(true, (view, position) -> {

            if (position <= 1) {

                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(0.85f, 1 - Math.abs(position));
                float vertMargin = view.getHeight() * (1 - scaleFactor) / 2;
                float horzMargin = view.getWidth() * (1 - scaleFactor) / 2;
                if (position < 0)
                    view.setTranslationX(horzMargin - vertMargin / 2);
                else
                    view.setTranslationX(-horzMargin + vertMargin / 2);

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
            }
        });
        tabLayout.setViewPager(viewPager);
        return rootView;
    }
}