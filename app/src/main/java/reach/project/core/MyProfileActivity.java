package reach.project.core;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.CursorLoader;
import android.support.v4.util.Pair;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.ancillaryViews.TermsActivity;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.coreViews.myProfile.EditProfileActivity;
import reach.project.music.ReachDatabase;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

/**
 * Created by gauravsobti on 05/03/16.
 */
public class MyProfileActivity extends AppCompatActivity {

    private static final ResizeOptions PROFILE_PHOTO_RESIZE = new ResizeOptions(200, 200);
    private static final ResizeOptions COVER_PHOTO_RESIZE = new ResizeOptions(500, 300);
    private SharedPreferences preferences;
    public static boolean profileEdited;
    private ActionBar actionBar;
    private SimpleDraweeView profilePic;
    private static final String TAG = MyProfileActivity.class.getSimpleName();
    private SimpleDraweeView coverPic;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);


        preferences = getSharedPreferences("Reach", Context.MODE_PRIVATE);

        /*toolbar.inflateMenu(R.menu.myprofile_menu);
        final Menu menu = toolbar.getMenu();
        final MenuItem useMobileData =  menu.findItem(R.id.useMobileData);
        if(SharedPrefUtils.getMobileData(preferences)) {
            useMobileData.setChecked(true);
        }
        else{
            useMobileData.setChecked(false);
        }

        final MenuItem menuItem = toolbar.getMenu().findItem(R.id.edit_button);*/

        //TODO: get username

        //toolbar.setOnMenuItemClickListener(menuItemClickListener);
        profilePic = (SimpleDraweeView) findViewById(R.id.profilePic);
        coverPic = (SimpleDraweeView) findViewById(R.id.coverPic);

        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        setUpProfile();

        final TextView appCount = (TextView) findViewById(R.id.appCount);
        final TextView songsCount = (TextView) findViewById(R.id.songsCount);
        final TextView userName = (TextView) findViewById(R.id.userName);
        final TextView friendsCount = (TextView) findViewById(R.id.friendsCount);
        new CountUpdater(songsCount, appCount, friendsCount).execute(this);
        userName.setText(SharedPrefUtils.getUserName(preferences));
        //friendsCount.setText(StaticData.friendsCount+"");


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void setUpProfile() {
        Log.d(TAG, "SetUp Profile Called");
        actionBar.setTitle("My Profile");

        profilePic.setImageURI(AlbumArtUri.getUserImageUri(
                SharedPrefUtils.getServerId(preferences),
                "imageId",
                "rw", //webP
                true, //circular
                PROFILE_PHOTO_RESIZE.width,
                PROFILE_PHOTO_RESIZE.height));

        coverPic.setImageURI(AlbumArtUri.getUserImageUri(
                SharedPrefUtils.getServerId(preferences),
                "coverPicId",
                "rw", //webP
                false, //simple crop
                COVER_PHOTO_RESIZE.width,
                COVER_PHOTO_RESIZE.height));

    }

    @Override
    public void onStart() {
        super.onStart();


    }

    @Override
    public void onStop() {
        super.onStop();


    }

    private final static class CountUpdater extends AsyncTask<Context, Void, int[]> {

        private final WeakReference<TextView> musicCountReference, appCountReference, friendsCountReference;

        private CountUpdater(TextView musicCount, TextView appCount, TextView friendsCount) {
            this.musicCountReference = new WeakReference<>(musicCount);
            this.appCountReference = new WeakReference<>(appCount);
            this.friendsCountReference = new WeakReference<>(friendsCount);
        }

        @Override
        protected int[] doInBackground(Context... params) {

            final ContentResolver contentResolver = params[0].getContentResolver();
            final PackageManager packageManager = params[0].getPackageManager();

            final Cursor cursor = contentResolver.query(SongProvider.CONTENT_URI,
                    new String[]{SongHelper.COLUMN_ID}, null, null, null);
            final int songCount, friendsCount;
            if (cursor != null) {
                songCount = cursor.getCount();
                cursor.close();
            } else
                songCount = 0;


            final Cursor friendsDataCursor = contentResolver.query(ReachFriendsProvider.CONTENT_URI,
                    null,
                    ReachFriendsHelper.COLUMN_STATUS + " != ?",
                    new String[]{ReachFriendsHelper.Status.REQUEST_NOT_SENT.getString()},
                    null
            );

            if (friendsDataCursor != null) {
                friendsCount = friendsDataCursor.getCount();
                friendsDataCursor.close();
            } else {
                friendsCount = 0;
            }

            final int appCount = MiscUtils.getInstalledApps(packageManager).size();

            int[] array = {songCount, appCount, friendsCount};

            /*return new Pair<>(songCount + "", appCount + "");*/
            return array;
        }

        @Override
        protected void onPostExecute(int[] counts) {
            super.onPostExecute(counts);

            MiscUtils.useReference(musicCountReference, textView -> {
                textView.setText(counts[0] + "");
            });

            MiscUtils.useReference(appCountReference, textView -> {
                textView.setText(counts[1]+ "");
            });
            MiscUtils.useReference(friendsCountReference, textView -> {
                textView.setText(counts[2]+" friends");
            });
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "On Resume Called");
        if (profileEdited) {
            setUpProfile();
            profileEdited = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.myprofile_menu, menu);
        final MenuItem useMobileData = menu.findItem(R.id.useMobileData);
        if (SharedPrefUtils.getMobileData(preferences)) {
            useMobileData.setChecked(true);
        } else {
            useMobileData.setChecked(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.useMobileData:
                final SharedPreferences preferences = getSharedPreferences("Reach", Context.MODE_PRIVATE);
                if (item.isChecked()) {
                    item.setChecked(false);
                    SharedPrefUtils.setDataOff(preferences);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getContentResolver().delete(
                                    SongProvider.CONTENT_URI,
                                    SongHelper.COLUMN_OPERATION_KIND + " = ? and " +
                                            SongHelper.COLUMN_STATUS + " != ?",
                                    new String[]{ReachDatabase.OperationKind.UPLOAD_OP.getString(), ReachDatabase.Status.PAUSED_BY_USER.getString()});
                        }
                    }).start();
                } else {
                    item.setChecked(true);
                    SharedPrefUtils.setDataOn(preferences);
                }
                return true;


            case R.id.rateOurApp: {

                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=reach.project")));
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Play store app not installed", Toast.LENGTH_SHORT).show();
                } finally {
                    return true;
                }

            }

            case R.id.edit_button: {

                startActivity(new Intent(this, EditProfileActivity.class));
                //Toast.makeText(this, "Edit Profile",Toast.LENGTH_LONG).show();
                return true;
            }

            case R.id.conditions: {
                startActivity(new Intent(this, TermsActivity.class));
                return true;
            }


        }

        return super.onOptionsItemSelected(item);
    }
}
