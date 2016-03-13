package reach.project.coreViews.myProfile;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.google.common.base.Optional;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;

import reach.backend.entities.userApi.model.ReachUser;
import reach.project.R;
import reach.project.core.MyProfileActivity;
import reach.project.core.StaticData;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.CloudStorageUtils;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

public class EditProfileActivity extends AppCompatActivity {

    private static final int IMAGE_PICKER_SELECT = 999;
    private static final int COVER_PICKER_SELECT = 888;

    private static long myId = 0;
    private static WeakReference<EditProfileActivity> reference;

    private static final ResizeOptions PROFILE_PHOTO_RESIZE = new ResizeOptions(150, 150);
    private static final ResizeOptions COVER_PHOTO_RESIZE = new ResizeOptions(500, 300);

    private final ExecutorService profileEditor = MiscUtils.getRejectionExecutor();

    @Nullable
    private static Uri toUploadProfilePhoto = null;
    @Nullable
    private static Uri toUploadCoverPhoto = null;

    @Nullable
    private EditText firstName = null, email = null;
    @Nullable
    private SimpleDraweeView profileDrawee = null;
    @Nullable
    private SimpleDraweeView coverDrawee = null;
    @Nullable
    private ProgressDialog progressDialog = null;

    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        reference = new WeakReference<>(this);
        progressDialog = new ProgressDialog(this);

        final Toolbar mToolbar = (Toolbar) findViewById(R.id.editProfileToolbar);
        mToolbar.setTitle("Edit Profile");
        mToolbar.inflateMenu(R.menu.edit_profile_menu);
        mToolbar.setOnMenuItemClickListener(navListener);
        mToolbar.setNavigationOnClickListener(v -> onBackPressed());

        final SharedPreferences sharedPreferences = getSharedPreferences("Reach", Context.MODE_PRIVATE);
        final String userName = SharedPrefUtils.getUserName(sharedPreferences);
        final String emailId = SharedPrefUtils.getEmailId(sharedPreferences);
        myId = SharedPrefUtils.getServerId(sharedPreferences);

        firstName = (EditText) findViewById(R.id.name);
        firstName.setText(userName);
        firstName.setSelection(userName.length());
        email = (EditText) findViewById(R.id.email);
        email.setText(emailId);
        email.setSelection(emailId.length());

        profileDrawee = (SimpleDraweeView) findViewById(R.id.profilePic);
        profileDrawee.setTag(IMAGE_PICKER_SELECT);
        profileDrawee.setOnClickListener(imagePicker);
        profileDrawee.setImageURI(AlbumArtUri.getUserImageUri(
                myId,
                "imageId",
                "rw", //webP
                true, //circular
                PROFILE_PHOTO_RESIZE.width,
                PROFILE_PHOTO_RESIZE.height));

        coverDrawee = (SimpleDraweeView) findViewById(R.id.coverPic);
        coverDrawee.setTag(COVER_PICKER_SELECT);
        coverDrawee.setOnClickListener(imagePicker);
        //TODO set failure image, loader and placeHolder
        coverDrawee.setImageURI(AlbumArtUri.getUserImageUri(
                myId,
                "coverPicId",
                "rw", //webP
                false, //simple crop
                COVER_PHOTO_RESIZE.width,
                COVER_PHOTO_RESIZE.height));
    }

    @Override
    public void onBackPressed() {
        MiscUtils.navigateUp(this);
    }

    private final View.OnClickListener imagePicker = view -> {

        final Object tag = view.getTag();
        if (!(tag instanceof Integer))
            return;
        final Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), (int) tag);
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {

        if (isFinishing()) {

            Log.i("Ayush", "ACTIVITY NOT FOUND !");
            return;
        }

        final Uri imageUri;
        if (resultCode != Activity.RESULT_OK || (imageUri = data.getData()) == null) {
            Toast.makeText(this, "Failed to set Profile Photo, try again", Toast.LENGTH_SHORT).show();
            return;
        }

        if (requestCode == IMAGE_PICKER_SELECT && profileDrawee != null) {

            final DraweeController draweeController = MiscUtils.getControllerResize(
                    profileDrawee.getController(), imageUri, PROFILE_PHOTO_RESIZE);
            profileDrawee.setController(draweeController);
            toUploadProfilePhoto = imageUri;

        } else if (requestCode == COVER_PICKER_SELECT && coverDrawee != null) {

            final DraweeController draweeController = MiscUtils.getControllerResize(
                    coverDrawee.getController(), imageUri, COVER_PHOTO_RESIZE);
            coverDrawee.setController(draweeController);
            toUploadCoverPhoto = imageUri;
        } else {
            finish(); //should not happen TODO track
        }
    }

    private final Toolbar.OnMenuItemClickListener navListener = item -> {

        MiscUtils.useActivity(reference, activity -> {
            if (firstName!=null && firstName.length() == 0) //let it throw, should never happen
                Toast.makeText(activity, "Please enter your name", Toast.LENGTH_SHORT).show();
            else if (!MiscUtils.isOnline(activity)) {

                Toast.makeText(activity, "No internet found", Toast.LENGTH_SHORT).show();
                onBackPressed(); //move back
            } else {

                final SharedPreferences sharedPreferences = EditProfileActivity.this.getSharedPreferences("Reach", Context.MODE_PRIVATE);
                final String oldUserName = SharedPrefUtils.getUserName(sharedPreferences);
                final String newUserName = firstName.getText().toString();
                final String oldEmailId = SharedPrefUtils.getEmailId(sharedPreferences);
                final String newEmailId = email.getText().toString();

                if (oldUserName.equals(newUserName) && oldEmailId.equals(newEmailId) && toUploadProfilePhoto == null && toUploadCoverPhoto == null)
                    onBackPressed(); //nothing to change, exit
                else {

                    ((InputMethodManager) EditProfileActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(firstName.getWindowToken(), 0);

                    final ContentResolver contentResolver = EditProfileActivity.this.getContentResolver();

                    try {
                        final InputStream profilePicOptionsStream = toUploadProfilePhoto != null ? contentResolver.openInputStream(toUploadProfilePhoto) : null;
                        final InputStream profilePicDecodeStream = toUploadProfilePhoto != null ? contentResolver.openInputStream(toUploadProfilePhoto) : null;
                        final InputStream coderPicOptionsStream = toUploadCoverPhoto != null ? contentResolver.openInputStream(toUploadCoverPhoto) : null;
                        final InputStream coderPicDecodeStream = toUploadCoverPhoto != null ? contentResolver.openInputStream(toUploadCoverPhoto) : null;
                        new UpdateProfile(new WeakReference<>(progressDialog))
                                .executeOnExecutor(profileEditor,
                                        newUserName,
                                        profilePicOptionsStream,
                                        profilePicDecodeStream,
                                        coderPicOptionsStream,
                                        coderPicDecodeStream,
                                        null,
                                        newEmailId);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(activity, "Could not update profile data", Toast.LENGTH_SHORT).show();
                    } finally {
                        toUploadProfilePhoto = null;
                        toUploadCoverPhoto = null;
                    }
                }
            }
        });
        return true;
    };

    private static final class UpdateProfile extends AsyncTask<Object, String, Boolean> {

        private final WeakReference<ProgressDialog> reference;

        private UpdateProfile(WeakReference<ProgressDialog> reference) {
            this.reference = reference;
        }

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
            MiscUtils.useReference(reference, progressDialog -> {
                progressDialog.setCancelable(false);
                progressDialog.show();
            });
        }

        @Override
        protected void onProgressUpdate(String... toToast) {
            super.onProgressUpdate(toToast);
            MiscUtils.useActivity(EditProfileActivity.reference, context -> Toast.makeText(context, toToast[0], Toast.LENGTH_SHORT).show());
        }

        /**
         * When sending list of info to update
         * <p>
         * 0 : userName
         * <p>
         * 1 : imageId options stream
         * 2 : imageId decode stream
         * 3 : coverPicId options stream
         * 4 : coverPicId decode stream
         * <p>
         * 5 : statusSong
         * 6 : emailId
         * 7 : birthday
         */
        @Override
        protected Boolean doInBackground(final Object... name) {

            final ReachUser reachUser = new ReachUser();
            reachUser.setId(myId);

            reachUser.setUserName((String) name[0]);
            final InputStream profilePicOptionsStream = (InputStream) name[1];
            final InputStream profilePicDecodeStream = (InputStream) name[2];
            final InputStream coderPicOptionsStream = (InputStream) name[3];
            final InputStream coderPicDecodeStream = (InputStream) name[4];
            reachUser.setEmailId((String) name[6]);

            //upload profile pic
            if (profilePicOptionsStream != null && profilePicDecodeStream != null) {

                final String profilePicId = CloudStorageUtils.uploadImage(profilePicOptionsStream, profilePicDecodeStream, myId);
                if (TextUtils.isEmpty(profilePicId))
                    return false;
                reachUser.setImageId(profilePicId);
            }

            //upload cover pic
            if (coderPicOptionsStream != null && coderPicDecodeStream != null) {

                final String coverPicId = CloudStorageUtils.uploadImage(coderPicOptionsStream, coderPicDecodeStream, myId);
                if (TextUtils.isEmpty(coverPicId))
                    return false;
                reachUser.setCoverPicId(coverPicId);
            }

            //upload user data
            Log.i("Ayush", "Pushing " + myId + " " + reachUser.toString());
            final boolean success = MiscUtils.autoRetry(() -> {
                StaticData.USER_API.updateUserDetailsNew(reachUser).execute();
                return true;
            }, Optional.absent()).or(false);

            if (success) {

                //locally save the values
                MiscUtils.useActivity(EditProfileActivity.reference, context -> {

                    final SharedPreferences sharedPreferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
                    if (!TextUtils.isEmpty(reachUser.getImageId()))
                        SharedPrefUtils.storeImageId(sharedPreferences, reachUser.getImageId());
                    if (!TextUtils.isEmpty(reachUser.getCoverPicId()))
                        SharedPrefUtils.storeCoverImageId(sharedPreferences, reachUser.getCoverPicId());
                    if (!TextUtils.isEmpty(reachUser.getUserName()))
                        SharedPrefUtils.storeUserName(sharedPreferences, reachUser.getUserName());
                    if (!TextUtils.isEmpty(reachUser.getEmailId()))
                        SharedPrefUtils.storeEmailId(sharedPreferences, reachUser.getEmailId());
                });
            }

            return success;
        }

        @Override
        protected void onPostExecute(Boolean success) {

            super.onPostExecute(success);

            MiscUtils.useActivity(EditProfileActivity.reference, activity -> {

                if (success != null && success) {
                    Toast.makeText(activity, "Changes saved successfully!!", Toast.LENGTH_SHORT).show();
                    MyProfileActivity.profileEdited = true;
                }
                else {

                    Toast.makeText(activity, "Failed, try again", Toast.LENGTH_SHORT).show();

                    final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
                    final String userName = SharedPrefUtils.getUserName(sharedPreferences);
                    final String emailId = SharedPrefUtils.getEmailId(sharedPreferences);

                    if (activity.profileDrawee != null)
                        activity.profileDrawee.setImageURI(AlbumArtUri.getUserImageUri(
                                myId,
                                "imageId",
                                "rw", //webP
                                true, //circular
                                PROFILE_PHOTO_RESIZE.width,
                                PROFILE_PHOTO_RESIZE.height));
                    if (activity.coverDrawee != null)
                        activity.coverDrawee.setImageURI(AlbumArtUri.getUserImageUri(
                                myId,
                                "coverPicId",
                                "rw", //webP
                                false, //simple crop
                                COVER_PHOTO_RESIZE.width,
                                COVER_PHOTO_RESIZE.height));
                    if (activity.firstName != null)
                        activity.firstName.setText(userName);
                    if (activity.email != null)
                        activity.email.setText(emailId);
                }

                if (activity.firstName != null)
                    activity.firstName.requestFocus();
                if (activity.email != null)
                    activity.email.requestFocus();
            });

            MiscUtils.useReference(reference, Dialog::dismiss);
        }
    }
}
