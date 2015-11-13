package reach.project.yourprofile;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.florent37.materialviewpager.MaterialViewPager;
import com.github.florent37.materialviewpager.header.HeaderDesign;

import reach.project.R;
import reach.project.utils.MiscUtils;

public class YourProfileFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_yourprofile, container, false);

        /*Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("");*/

        MaterialViewPager mViewPager = (MaterialViewPager) rootView.findViewById(R.id.materialViewPager);

        mViewPager.getViewPager().setAdapter(new FragmentStatePagerAdapter(getChildFragmentManager()) {

            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    default:
                        return new YourProfilePageFragment();
                }
            }

            @Override
            public int getCount() {
                return 3;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case 0:
                        return "Apps";
                    case 1:
                        return "Music";
                    case 2:
                        return "Photos";
                }
                return "";
            }
        });

        mViewPager.setMaterialViewPagerListener(page -> {
            switch (page) {
                case 0:
                    return HeaderDesign.fromColorResAndUrl(
                            R.color.green,
                            "https://fs01.androidpit.info/a/63/0e/android-l-wallpapers-630ea6-h900.jpg");
                case 1:
                    return HeaderDesign.fromColorResAndUrl(
                            R.color.blue,
                            "http://cdn1.tnwcdn.com/wp-content/blogs.dir/1/files/2014/06/wallpaper_51.jpg");
                case 2:
                    return HeaderDesign.fromColorResAndUrl(
                            R.color.cyan,
                            "http://www.droid-life.com/wp-content/uploads/2014/10/lollipop-wallpapers10.jpg");
            }
            return null;
        });

        ViewPager viewPager = mViewPager.getViewPager();
        viewPager.setOffscreenPageLimit(mViewPager.getViewPager().getAdapter().getCount());
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
        mViewPager.getPagerTitleStrip().setViewPager(mViewPager.getViewPager());

        return rootView;
    }
}
