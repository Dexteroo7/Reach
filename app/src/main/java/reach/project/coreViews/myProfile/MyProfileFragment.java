package reach.project.coreViews.myProfile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.github.florent37.materialviewpager.MaterialViewPager;
import com.github.florent37.materialviewpager.header.HeaderDesign;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.myProfile.apps.ApplicationFragment;
import reach.project.coreViews.myProfile.music.MyLibraryFragment;
import reach.project.music.MySongsProvider;
import reach.project.player.PlayerActivity;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

/**
 * A placeholder fragment containing a simple view.
 */
public class MyProfileFragment extends Fragment {

    private static WeakReference<MyProfileFragment> reference = null;

    public static MyProfileFragment newInstance() {

        MyProfileFragment fragment;
        if (reference == null || (fragment = reference.get()) == null || MiscUtils.isFragmentDead(fragment))
            reference = new WeakReference<>(fragment = new MyProfileFragment());
        else
            Log.i("Ayush", "Reusing YourProfileAppFragment object :)");
        return fragment;
    }

    private final Toolbar.OnMenuItemClickListener menuItemClickListener = item -> {

        boolean check;
        switch (item.getItemId()) {
            case R.id.show_visible:
                check = !item.isChecked();
                //TODO show visible files
                item.setChecked(check);
                return true;
            case R.id.show_invisible:
                check = !item.isChecked();
                //TODO show invisible files
                item.setChecked(check);
                return true;
            case R.id.player_button:
                PlayerActivity.openActivity(getContext());
                return true;
        }
        return false;
    };

    private static final ViewPager.PageTransformer PAGE_TRANSFORMER = (view, position) -> {

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
    };

    private final MaterialViewPager.Listener materialListener = page -> {

        switch (page) {
            case 0:
                return HeaderDesign.fromColorResAndUrl(
                        R.color.reach_color,
                        "");
            case 1:
                return HeaderDesign.fromColorResAndUrl(
                        R.color.reach_color,
                        "");
            default:throw new IllegalStateException("Size of 2 expected");
        }
    };

    public void onDestroyView() {
        super.onDestroyView();
        Log.d("Ashish", "MyProfileFragment - onDestroyView");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Ashish", "MyProfileFragment - onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Ashish", "MyProfileFragment - onDestroy");
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.activity_your_profile, container, false);
        Log.d("Ashish", "MyProfileFragment - onCreateView");

        final MaterialViewPager materialViewPager = (MaterialViewPager) rootView.findViewById(R.id.materialViewPager);
        final Toolbar toolbar = materialViewPager.getToolbar();
        final Activity activity = getActivity();

        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setTitle("My Profile");
        toolbar.inflateMenu(R.menu.myprofile_menu);

        final MenuItem menuItem = toolbar.getMenu().findItem(R.id.edit_button);
        MenuItemCompat.setActionView(menuItem, R.layout.edit_profile_button);
        final View editProfileContainer = MenuItemCompat.getActionView(menuItem).findViewById(R.id.editProfileLayout);
        editProfileContainer.setOnClickListener(v -> startActivity(new Intent(activity, EditProfileActivity.class)));
        toolbar.setNavigationIcon(null);
        toolbar.setOnMenuItemClickListener(menuItemClickListener);

        final RelativeLayout headerRoot = (RelativeLayout) materialViewPager.findViewById(R.id.headerRoot);
        final SharedPreferences preferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        final String userName = SharedPrefUtils.getUserName(preferences);

        final Cursor cursor = activity.getContentResolver().query(MySongsProvider.CONTENT_URI,
                null, null, null, null);
        final int songCount;
        if (cursor != null) {
            songCount = cursor.getCount();
            cursor.close();
        } else
            songCount = 0;

        ((TextView) headerRoot.findViewById(R.id.userName)).setText(userName);
        ((TextView) headerRoot.findViewById(R.id.musicCount)).setText(songCount + "");
        ((TextView) headerRoot.findViewById(R.id.userHandle)).setText("@" + userName.toLowerCase().split(" ")[0]);

//        new Thread(() -> ((TextView) headerRoot.findViewById(R.id.appCount)).setText(MiscUtils
//                .getInstalledApps(activity.getPackageManager()).size() + "")).start();

        final SimpleDraweeView profilePic = (SimpleDraweeView) headerRoot.findViewById(R.id.profilePic);
        profilePic.setController(MiscUtils.getControllerResize(profilePic.getController(),
                Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + SharedPrefUtils.getImageId(preferences)), 100, 100));

        final SimpleDraweeView coverPic = (SimpleDraweeView) headerRoot.findViewById(R.id.coverPic);
        coverPic.setController(MiscUtils.getControllerResize(coverPic.getController(),
                Uri.parse(MiscUtils.getRandomPic()), 500, 500));

        final PagerAdapter pagerAdapter = new FragmentPagerAdapter(getChildFragmentManager()) {

            @Override
            public Fragment getItem(int position) {
                switch (position) {

                    case 0:
                        return ApplicationFragment.getInstance("Apps");
                    case 1:
                        return MyLibraryFragment.getInstance("Songs");
                    default:
                        throw new IllegalStateException("Count and size clash");
                }
            }

            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {

                    case 0:
                        return "Apps";
                    case 1:
                        return "Songs";
                    default:
                        throw new IllegalStateException("Count and size clash");
                }
            }
        };

        final ViewPager viewPager = materialViewPager.getViewPager();
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(viewPager.getAdapter().getCount());
        viewPager.setPageMargin(-1 * (MiscUtils.dpToPx(40)));
        viewPager.setPageTransformer(true, PAGE_TRANSFORMER);

        materialViewPager.setMaterialViewPagerListener(materialListener);
        materialViewPager.getPagerTitleStrip().setViewPager(viewPager);
        materialViewPager.getPagerTitleStrip().setOnTouchListener((v, event) -> true);

        return rootView;
    }
}