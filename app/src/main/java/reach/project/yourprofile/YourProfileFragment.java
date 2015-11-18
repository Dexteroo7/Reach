package reach.project.yourProfile;

import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.florent37.materialviewpager.MaterialViewPager;
import com.github.florent37.materialviewpager.header.HeaderDesign;
import com.squareup.picasso.Picasso;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.friends.ReachFriendsHelper;
import reach.project.friends.ReachFriendsProvider;
import reach.project.utils.MiscUtils;
import reach.project.yourProfile.music.YourProfileMusicFragment;

public class YourProfileFragment extends Fragment {

    public static YourProfileFragment newInstance(long userId) {

        final Bundle args = new Bundle();
        args.putLong("userId", userId);

        final YourProfileFragment fragment = new YourProfileFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_yourprofile, container, false);
        MaterialViewPager mViewPager = (MaterialViewPager) rootView.findViewById(R.id.materialViewPager);

        Toolbar toolbar = mViewPager.getToolbar();
        toolbar.setTitle("Profile");
        toolbar.setTitleTextColor(Color.WHITE);

        long userId = getArguments().getLong("userId", 0L);

        final Cursor cursor = getActivity().getContentResolver().query(
                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
                new String[]{ReachFriendsHelper.COLUMN_PHONE_NUMBER,
                        ReachFriendsHelper.COLUMN_USER_NAME,
                        ReachFriendsHelper.COLUMN_NUMBER_OF_SONGS,
                        ReachFriendsHelper.COLUMN_IMAGE_ID,
                        ReachFriendsHelper.COLUMN_NETWORK_TYPE,
                        ReachFriendsHelper.COLUMN_STATUS},
                ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{userId + ""}, null);
        int numberOfSongs = 0;

        if (cursor != null) {
            cursor.moveToFirst();
            final String uName = cursor.getString(1);
            numberOfSongs = cursor.getInt(2);
            final String imageId = cursor.getString(3);

            RelativeLayout headerRoot = (RelativeLayout) mViewPager.findViewById(R.id.headerRoot);
            TextView userName = (TextView) headerRoot.findViewById(R.id.userName);
            TextView musicCount = (TextView) headerRoot.findViewById(R.id.musicCount);
            TextView userHandle = (TextView) headerRoot.findViewById(R.id.userHandle);
            ImageView profilePic = (ImageView) headerRoot.findViewById(R.id.profilePic);

            userName.setText(uName);
            musicCount.setText(numberOfSongs+"");
            userHandle.setText("@" + uName.toLowerCase().split(" ")[0]);
            Picasso.with(getActivity())
                    .load(StaticData.cloudStorageImageBaseUrl + imageId)
                    .fit()
                    .centerCrop()
                    .into(profilePic);

            cursor.close();
        }

        final int finalNumberOfSongs = numberOfSongs;
        mViewPager.getViewPager().setAdapter(new FragmentStatePagerAdapter(getChildFragmentManager()) {

            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    default:
                        return YourProfileMusicFragment.newInstance(userId);
                }
            }

            @Override
            public int getCount() {
                return 1;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case 0:
                        return "Songs (" + finalNumberOfSongs + ")";
                }
                return "";
            }
        });

        mViewPager.setMaterialViewPagerListener(page -> {
            switch (page) {
                case 0:
                    return HeaderDesign.fromColorResAndUrl(
                            R.color.reach_grey,
                            "");
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