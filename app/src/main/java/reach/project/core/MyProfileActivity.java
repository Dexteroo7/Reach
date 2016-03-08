package reach.project.core;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
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

import reach.project.R;
import reach.project.ancillaryViews.TermsActivity;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.coreViews.myProfile.EditProfileActivity;
import reach.project.player.PlayerActivity;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

/**
 * Created by gauravsobti on 05/03/16.
 */
public class MyProfileActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    public static boolean profileEdited;
    private ActionBar actionBar;
    private SimpleDraweeView profilePic;
    private static final String TAG = MyProfileActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);
        final Toolbar toolbar =(Toolbar) findViewById(R.id.toolbar);


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

        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        setUpProfile();

        final TextView appCount = (TextView)findViewById(R.id.appCount);
        final TextView songsCount = (TextView)findViewById(R.id.songsCount);
        final TextView friendsCount = (TextView) findViewById(R.id.friendsCount);

        appCount.setText(StaticData.appsCount+"");
        songsCount.setText((StaticData.downloadedSongsCount+StaticData.librarySongsCount)+"");
        friendsCount.setText(StaticData.friendsCount+"");


    }

    private void setUpProfile(){
        Log.d(TAG,"SetUp Profile Called");
        actionBar.setTitle(SharedPrefUtils.getUserName(preferences));
        profilePic.setImageURI(AlbumArtUri.getUserImageUri(
                SharedPrefUtils.getServerId(preferences),
                "imageId",
                "rw", //webP
                false, //circular
                StaticData.deviceWidth,
                MiscUtils.dpToPx(256)));
        
    }
    

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"On Resume Called");
        if(profileEdited){
            setUpProfile();
            profileEdited = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.myprofile_menu, menu);
        final MenuItem useMobileData =  menu.findItem(R.id.useMobileData);
        if(SharedPrefUtils.getMobileData(preferences)) {
            useMobileData.setChecked(true);
        }
        else{
            useMobileData.setChecked(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.useMobileData:
                final SharedPreferences preferences =getSharedPreferences("Reach", Context.MODE_PRIVATE);
                if(item.isChecked()){
                    item.setChecked(false);
                    SharedPrefUtils.setDataOff(preferences);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getContentResolver().delete(
                                    ReachDatabaseProvider.CONTENT_URI,
                                    ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ? and " +
                                            ReachDatabaseHelper.COLUMN_STATUS + " != ?",
                                    new String[]{"1", ReachDatabase.PAUSED_BY_USER + ""});
                        }
                    }).start();
                }
                else{
                    item.setChecked(true);
                    SharedPrefUtils.setDataOn(preferences);
                }
                return true;



            case R.id.rateOurApp:{

                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=reach.project")));
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Play store app not installed", Toast.LENGTH_SHORT).show();
                }
                finally {
                    return true;
                }

            }

            case R.id.edit_button: {

                startActivity(new Intent(this, EditProfileActivity.class));
                //Toast.makeText(this, "Edit Profile",Toast.LENGTH_LONG).show();
                return true;
            }

            case R.id.conditions:{
                startActivity(new Intent(this, TermsActivity.class));
                return true;
            }



        }

        return super.onOptionsItemSelected(item);
    }
}
