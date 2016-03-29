package reach.project.core;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.ancillaryViews.TermsActivity;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.coreViews.invite.InviteActivity;
import reach.project.coreViews.myProfile.EditProfileActivity;
import reach.project.music.ReachDatabase;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.CustomDraweeView;

/**
 * Created by gauravsobti on 05/03/16.
 */
public class MyProfileActivity extends AppCompatActivity implements View.OnClickListener {

    private static final ResizeOptions PROFILE_PHOTO_RESIZE = new ResizeOptions(200, 200);
    private static final ResizeOptions COVER_PHOTO_RESIZE = new ResizeOptions(500, 300);
    private SharedPreferences preferences;
    public static boolean profileEdited;
    public static boolean countChanged = true;
    private ActionBar actionBar;
    private CustomDraweeView profilePic;
    private static final String TAG = MyProfileActivity.class.getSimpleName();
    private SimpleDraweeView coverPic;
    public static final String APP_COUNT_KEY = "app_count";
    public static final String MUSIC_COUNT_KEY = "music_count";
    public static final String FRIEND_COUNT_KEY = "friend_count";

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private TextView appCount;
    private TextView songsCount;
    private TextView friendsCount;
    private AlertDialog appPrivacyAlertDialog;
    private LayoutInflater inflater;
    private AlertDialog songPrivacyAlertDialog;

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
        profilePic = (CustomDraweeView) findViewById(R.id.profilePic);
        coverPic = (SimpleDraweeView) findViewById(R.id.coverPic);

        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        setUpProfile();

        appCount = (TextView) findViewById(R.id.appCount);
        appCount.setOnClickListener(this);
        songsCount = (TextView) findViewById(R.id.songsCount);
        final TextView userName = (TextView) findViewById(R.id.userName);
        friendsCount = (TextView) findViewById(R.id.friendsCount);
        final Button inviteFriendsBtn = (Button) findViewById(R.id.invite_more_friends);
        inviteFriendsBtn.setOnClickListener(this);
        final Button appsManagePrivacyBtn = (Button) findViewById(R.id.apps_manage_privacy);
        appsManagePrivacyBtn.setOnClickListener(this);
        final Button songsManagePrivacyBtn = (Button) findViewById(R.id.songs_manage_privacy);
        songsManagePrivacyBtn.setOnClickListener(this);
        friendsCount.setOnClickListener(this);
        userName.setText(SharedPrefUtils.getUserName(preferences));
        findViewById(R.id.fb_share_container).setOnClickListener(this);
        //friendsCount.setText(StaticData.friendsCount+"");
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
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.invite_more_friends: {
                startActivity(new Intent(MyProfileActivity.this, InviteActivity.class));
                break;
            }
            case R.id.friendsCount: {
                ReachActivity.openActivityOnParticularTab(this, 2);
                break;
            }
            case R.id.apps_manage_privacy: {
                if(appPrivacyAlertDialog!=null){
                    appPrivacyAlertDialog.show();
                }
                else {
                    if(inflater == null){
                        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    }
                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    alertDialogBuilder.setTitle("APPS PRIVACY:");
                    final View vv = inflater.inflate(R.layout.apps_privacy_frag_layout, null);
                    alertDialogBuilder.setView(vv);

                    appPrivacyAlertDialog = alertDialogBuilder.show();
                }

                break;
            }
            case R.id.songs_manage_privacy: {
                Log.d(TAG, "Show songs privacy");
                if(songPrivacyAlertDialog!=null){
                    Log.d(TAG, "songs privacy alertDialog !=null");
                    songPrivacyAlertDialog.show();
                }
                else {
                    if(inflater == null){
                        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    }
                    Log.d(TAG, "songs privacy alertDialog ==null");
                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    alertDialogBuilder.setTitle("SONGS PRIVACY:");
                    final View vvv = inflater.inflate(R.layout.songs_privacy_frag_layout, null);
                    alertDialogBuilder.setView(vvv);
                    songPrivacyAlertDialog = alertDialogBuilder.show();
                }

                break;
            }
            case R.id.fb_share_container: {
                try{
                    ApplicationInfo info = getPackageManager().
                            getApplicationInfo("com.facebook.katana", 0 );

                } catch( PackageManager.NameNotFoundException e ){
                    Toast.makeText(this, "Please install the facebook application first!", Toast.LENGTH_SHORT).show();
                    SharedPrefUtils.storeFacebookShareButtonVisibleOrNot(preferences,false);
                    return;
                }
                Log.d(TAG, "FB share clicked");
                if(inflater == null){
                    inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                }
                final View vv = inflater.inflate(R.layout.fb_share_layout,null);
                final TextView username = (TextView)vv.findViewById(R.id.username);
                username.setTypeface(Typeface.createFromAsset(getAssets(),
                        "permanentmarker.ttf")
                        );
                username.setText("- " + SharedPrefUtils.getUserName(preferences));
                vv.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                vv.layout(0, 0, vv.getMeasuredWidth(),vv.getMeasuredHeight());

                final Bitmap vBitmap = Bitmap.createBitmap(vv.getMeasuredWidth(),
                        vv.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

                Canvas canvas = new Canvas(vBitmap);
                vv.draw(canvas);

                SharePhoto photo = new SharePhoto.Builder()
                        .setBitmap(vBitmap)
                        .build();
                SharePhotoContent content = new SharePhotoContent.Builder()
                        .addPhoto(photo)
                        .build();

                Toast.makeText(getApplicationContext(), "Sharing On Facebook", Toast.LENGTH_SHORT).show();
                ShareDialog.show(this,content);

                break;
            }

            default:
                break;

        }

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

            if (counts == null || counts.length == 0) {
                return;
            }

            MiscUtils.useReference(musicCountReference, textView -> {
                textView.setText(counts[0] + " songs");
            });

            MiscUtils.useReference(appCountReference, textView -> {
                textView.setText(counts[1] + " applications");
            });
            MiscUtils.useReference(friendsCountReference, textView -> {
                textView.setText(counts[2] + " friends");
            });
            StaticData.librarySongsCount = counts[0];
            StaticData.appsCount = counts[1];
            StaticData.friendsCount = counts[2];

            countChanged = false;

        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "On Resume Called");
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (profileEdited) {
            setUpProfile();
            profileEdited = false;
        }

        if (countChanged) {
            new CountUpdater(songsCount, appCount, friendsCount).execute(this);

        } else {
            appCount.setText(StaticData.appsCount + " applications");
            songsCount.setText(StaticData.librarySongsCount + " songs");
            friendsCount.setText(StaticData.friendsCount + " friends");
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
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putString(APP_COUNT_KEY, appCount.getText().toString());
        outState.putString(FRIEND_COUNT_KEY, friendsCount.getText().toString());
        outState.putString(MUSIC_COUNT_KEY, songsCount.getText().toString());

        super.onSaveInstanceState(outState, outPersistentState);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        friendsCount.setText(savedInstanceState.getString(FRIEND_COUNT_KEY, ""));
        appCount.setText(savedInstanceState.getString(APP_COUNT_KEY, ""));
        songsCount.setText(savedInstanceState.getString(MUSIC_COUNT_KEY, ""));

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
