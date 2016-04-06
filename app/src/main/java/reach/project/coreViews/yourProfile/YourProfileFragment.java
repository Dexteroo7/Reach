package reach.project.coreViews.yourProfile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.ancillaryViews.SettingsActivity;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.coreViews.yourProfile.music.YourProfileMusicFragment;
import reach.project.notificationCentre.NotificationActivity;
import reach.project.utils.MiscUtils;
import reach.project.utils.ancillaryClasses.SuperInterface;

/**
 * Created by gauravsobti on 06/04/16.
 */
public class YourProfileFragment extends Fragment {

    public static final String TAG = YourProfileFragment.class.getSimpleName();

    private SuperInterface mListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_your_profile, container, false);
        reference = new WeakReference<>(this);

        sharedPreferences = getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE);
        final Toolbar toolbar = (Toolbar) v.findViewById(R.id.toolbar);
        toolbar.setTitle("Profile");
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: " + "Inside navigation click listener");
                mListener.removeYourProfileFragment(YourProfileFragment.this);
            }
        });

        toolbar.inflateMenu(R.menu.yourprofile_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                /*case R.id.player_button:
                    PlayerActivity.openActivity(this);
                    return true;*/
                case R.id.notif_button:
                    NotificationActivity.openActivity(getActivity(), NotificationActivity.OPEN_NOTIFICATIONS);
                    return true;
                case R.id.settings_button:
                    startActivity(new Intent(getActivity(), SettingsActivity.class));
                    return true;
                default:
                    return false;
            }
        });

        long userId = 0;
        if (getArguments() != null) {
            userId = getArguments().getLong("userId", 0L);
            if (userId == 0) {
                throw new IllegalArgumentException("user id can not be null");
            }
        }


        if (savedInstanceState == null) {
            YourProfileMusicFragment frag = YourProfileMusicFragment.newInstance(userId);
            /*CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ;
            params.setBehavior( null );*/
            getFragmentManager().beginTransaction().replace(R.id.your_profile_music_frag_container, frag).commit();


        }
        final Cursor cursor = getActivity().getContentResolver().query(
                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
                new String[]{ReachFriendsHelper.COLUMN_PHONE_NUMBER,
                        ReachFriendsHelper.COLUMN_USER_NAME,
                        ReachFriendsHelper.COLUMN_NUMBER_OF_SONGS,
                        ReachFriendsHelper.COLUMN_IMAGE_ID,
                        ReachFriendsHelper.COLUMN_NETWORK_TYPE,
                        ReachFriendsHelper.COLUMN_STATUS,
                        ReachFriendsHelper.COLUMN_NUMBER_OF_APPS,
                        ReachFriendsHelper.COLUMN_COVER_PIC_ID},
                ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{userId + ""}, null);
        int numberOfSongs = 0;

        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
            final String uName = cursor.getString(1);
            numberOfSongs = cursor.getInt(2);
            final String imageId = cursor.getString(3);
            final TextView userName = (TextView) v.findViewById(R.id.userName);
            final TextView musicCount = (TextView) v.findViewById(R.id.musicCount);
            final TextView userHandle = (TextView) v.findViewById(R.id.userHandle);
            final SimpleDraweeView profilePic = (SimpleDraweeView) v.findViewById(R.id.profilePic);
            final SimpleDraweeView coverPic = (SimpleDraweeView) v.findViewById(R.id.coverPic);

            userName.setText(uName);
            musicCount.setText(numberOfSongs + "");
            //appCount.setText(numberOfApps + "");
            userHandle.setText("@" + uName.toLowerCase().split(" ")[0]);
            profilePic.setController(MiscUtils.getControllerResize(profilePic.getController(),
                    Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + imageId), 100, 100));
            coverPic.setController(MiscUtils.getControllerResize(coverPic.getController(),
                    Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + cursor.getString(7)), 500, 300));
//            coverPic.setController(MiscUtils.getControllerResize(coverPic.getController(),
//                    Uri.parse(MiscUtils.getRandomPic()), 500, 500));

            cursor.close();
        }
        return v;
    }

    private static final String OPEN_MY_PROFILE_APPS = "OPEN_MY_PROFILE_APPS";
    private static final String OPEN_MY_PROFILE_SONGS = "OPEN_MY_PROFILE_SONGS";

    private SharedPreferences sharedPreferences;

    public static YourProfileFragment openProfile(long userId, Context context) {

        YourProfileFragment fragment = new YourProfileFragment();
        Bundle bundle = new Bundle();
        bundle.putLong("userId", userId);
        fragment.setArguments(bundle);

        return fragment;
    }


    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        try {
            mListener = (SuperInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement SplashInterface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private static WeakReference<YourProfileFragment> reference = null;


}
