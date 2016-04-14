package reach.project.coreViews.myProfile;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.github.florent37.materialviewpager.MaterialViewPager;
import com.github.florent37.materialviewpager.header.HeaderDesign;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.core.StaticData;
import reach.project.coreViews.myProfile.apps.ApplicationFragment;
import reach.project.coreViews.myProfile.music.MyLibraryFragment;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

/**
 * A placeholder fragment containing a simple view.
 */
public class MyProfileFragment extends Fragment {

    private static final ResizeOptions PROFILE_PHOTO_RESIZE = new ResizeOptions(150, 150);
    private static final ResizeOptions COVER_PHOTO_RESIZE = new ResizeOptions(500, 300);
    private Toolbar toolbar;

    private static final MaterialViewPager.Listener MATERIAL_LISTENER = page -> {

        switch (page) {
            case 0:
                return HeaderDesign.fromColorResAndUrl(
                        R.color.reach_color,
                        "");
            case 1:
                return HeaderDesign.fromColorResAndUrl(
                        R.color.reach_color,
                        "");
            default:
                throw new IllegalStateException("Size of 2 expected");
        }
    };

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_my_profile, container, false);
        Log.d("Ashish", "MyProfileFragment - onCreateView");

        toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        final Activity activity = getActivity();

        final SharedPreferences preferences = getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE);

        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setTitle("Manage Songs Privacy");
        toolbar.inflateMenu(R.menu.myprofile_fragment_menu);
        final MenuItem menuItem = toolbar.getMenu().findItem(R.id.done_button);
        MenuItemCompat.setActionView(menuItem, R.layout.toolbar_done_button);
        final View doneButtonContainer = MenuItemCompat.getActionView(menuItem).findViewById(R.id.doneButtonLayout);
        doneButtonContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(getActivity(), ReachActivity.class);
                intent.setAction(ReachActivity.OPEN_MY_PROFILE_APPS_FIRST);
                getActivity().startActivity(intent);
                getActivity().finish();
            }
        });
        toolbar.setNavigationIcon(null);
        //toolbar.setOnMenuItemClickListener(menuItemClickListener);
        if(savedInstanceState == null){
            getFragmentManager().beginTransaction().replace(R.id.music_privacy_frag_container,MyLibraryFragment.getInstance("Songs")).commit();
        }

        final RelativeLayout headerRoot = (RelativeLayout) rootView.findViewById(R.id.headerRoot);
        final String userName = SharedPrefUtils.getUserName(preferences);

        ((TextView) headerRoot.findViewById(R.id.userName)).setText(userName);
        ((TextView) headerRoot.findViewById(R.id.userHandle)).setText("@" + userName.toLowerCase().split(" ")[0]);

        final SimpleDraweeView profilePic = (SimpleDraweeView) headerRoot.findViewById(R.id.profilePic);
        profilePic.setImageURI(AlbumArtUri.getUserImageUri(
                SharedPrefUtils.getServerId(preferences),
                "imageId",
                "rw", //webP
                true, //circular
                PROFILE_PHOTO_RESIZE.width,
                PROFILE_PHOTO_RESIZE.height));

        final SimpleDraweeView coverPic = (SimpleDraweeView) headerRoot.findViewById(R.id.coverPic);
        coverPic.setImageURI(AlbumArtUri.getUserImageUri(
                SharedPrefUtils.getServerId(preferences),
                "coverPicId",
                "rw", //webP
                false, //simple crop
                COVER_PHOTO_RESIZE.width,
                COVER_PHOTO_RESIZE.height));

        new CountUpdater((TextView) headerRoot.findViewById(R.id.musicCount),
                (TextView) headerRoot.findViewById(R.id.appCount)).execute(activity);

        return rootView;
    }

    private final static class CountUpdater extends AsyncTask<Context, Void, Integer> {

        private static final String TAG = CountUpdater.class.getSimpleName();
        private final WeakReference<TextView> musicCountReference, appCountReference;

        private CountUpdater(TextView musicCount, TextView appCount) {
            this.musicCountReference = new WeakReference<>(musicCount);
            this.appCountReference = new WeakReference<>(appCount);
        }

        @Override
        protected Integer doInBackground(Context... params) {

            final ContentResolver contentResolver = params[0].getContentResolver();
            //final PackageManager packageManager = params[0].getPackageManager();

            //TODO done meta 1
            final Cursor cursor = contentResolver.query(SongProvider.CONTENT_URI,
                    new String[]{SongHelper.COLUMN_ID}, SongHelper.COLUMN_META_HASH + " != ?", new String[]{StaticData.NULL_STRING}, null);
            final int songCount;
            if (cursor != null) {
                songCount = cursor.getCount();
                cursor.close();
            } else
                songCount = 0;

            return songCount;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            Log.d(TAG, "App Count = " + integer);

            MiscUtils.useReference(musicCountReference, textView -> {
                textView.setText(integer + "");
            });
        }
    }

}