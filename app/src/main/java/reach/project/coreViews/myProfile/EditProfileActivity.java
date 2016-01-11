package reach.project.coreViews.myProfile;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.utils.CloudStorageUtils;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.ancillaryClasses.UploadProgress;

public class EditProfileActivity extends AppCompatActivity {

    private static final int IMAGE_PICKER_SELECT = 999;
    private static final int COVER_PICKER_SELECT = 888;

    @Nullable
    private static File toUploadProfilePhoto = null;
    @Nullable
    private static File toUploadCoverPhoto = null;

    @Nullable
    private static String profilePhotoId = null;
    @Nullable
    private static String coverPhotoId = null;

    private static long userId = 0;
    private static WeakReference<EditProfileActivity> reference;

    @Nullable
    private EditText firstName = null;
    @Nullable
    private SimpleDraweeView profile = null;
    @Nullable
    private SimpleDraweeView cover = null;

    private final Random random = new Random();

    private final ExecutorService profileEditor = Executors.newSingleThreadExecutor();

    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        reference = new WeakReference<>(this);

        final Toolbar mToolbar = (Toolbar) findViewById(R.id.editProfileToolbar);
        mToolbar.setTitle("Edit Profile");
        mToolbar.setNavigationOnClickListener(v -> doUpdate());

        final SharedPreferences sharedPreferences = getSharedPreferences("Reach", Context.MODE_APPEND);
        profilePhotoId = SharedPrefUtils.getImageId(sharedPreferences);
        coverPhotoId = SharedPrefUtils.getCoverImageId(sharedPreferences);
        final String uName = SharedPrefUtils.getUserName(sharedPreferences);
        userId = SharedPrefUtils.getServerId(sharedPreferences);

        firstName = (EditText) findViewById(R.id.name);
        firstName.setText(uName);
        firstName.setSelection(uName.length());

        profile = (SimpleDraweeView) findViewById(R.id.profilePic);
        profile.setTag(IMAGE_PICKER_SELECT);
        profile.setOnClickListener(imagePicker);

        if (!TextUtils.isEmpty(profilePhotoId) && !profilePhotoId.equals("hello_world"))
            profile.setController(MiscUtils.getControllerwithResize(profile.getController(),
                    Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + profilePhotoId), 100, 100));

        cover = (SimpleDraweeView) findViewById(R.id.coverPic);
        cover.setTag(COVER_PICKER_SELECT);
        cover.setOnClickListener(imagePicker);
        Uri coverUri;
        if (!TextUtils.isEmpty(coverPhotoId) && !coverPhotoId.equals("hello_world"))
            coverUri = Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + coverPhotoId);
        else
            coverUri = Uri.parse(MiscUtils.getRandomPic(random));

        cover.setController(MiscUtils.getControllerwithResize(profile.getController(),
                coverUri, 500, 500));
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

        final InputStream imageStream;
        try {
            imageStream = getContentResolver().openInputStream(imageUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Photo not found, try again", Toast.LENGTH_SHORT).show();
            return;
        }

        if (requestCode == IMAGE_PICKER_SELECT)
            new ProcessProfilePhoto().executeOnExecutor(profileEditor, imageStream);
        else if (requestCode == COVER_PICKER_SELECT)
            new ProcessCoverPhoto().executeOnExecutor(profileEditor, imageStream);
    }

    private static final class ProcessProfilePhoto extends AsyncTask<InputStream, Void, File> {

        @Override
        protected File doInBackground(InputStream... params) {

            if (params[0] == null)
                return null;

            File tempFile = null;
            FileOutputStream outputStream = null;
            RandomAccessFile randomAccessFile = null;

            try {
                tempFile = File.createTempFile("profile_photo", ".tmp");
                (randomAccessFile = new RandomAccessFile(tempFile, "rws")).setLength(0);
                ByteStreams.copy(params[0], outputStream = new FileOutputStream(tempFile));
                outputStream.flush();
            } catch (IOException e) {

                e.printStackTrace();
                if (tempFile != null)
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                return null;
            } finally {
                MiscUtils.closeQuietly(outputStream, params[0], randomAccessFile);
            }

            try {
                return MiscUtils.compressImage(tempFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Nullable
        private ProgressDialog dialog = null;

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
            MiscUtils.useActivity(reference, activity -> dialog = new ProgressDialog(activity));
            if (dialog != null) {
                dialog.setCancelable(false);
                dialog.show();
            }
        }

        @Override
        protected void onPostExecute(File file) {

            super.onPostExecute(file);

            MiscUtils.useActivity(reference, activity -> {

                if (file == null) {

                    ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                            .setCategory("Profile photo failed")
                            .setAction("User Id - " + userId)
                            .setValue(1)
                            .build());

                    toUploadProfilePhoto = null;
                    Toast.makeText(activity, "Failed to set Profile Photo, try again", Toast.LENGTH_LONG).show();
                } else if (activity.profile != null) {

                    toUploadProfilePhoto = file;
                    activity.profile.setController(MiscUtils.getControllerwithResize(activity.profile.getController(),
                            Uri.parse("file://" + file.getAbsolutePath()), 100, 100));
                }
            });

            if (dialog != null)
                dialog.dismiss();
        }
    }

    private static final class ProcessCoverPhoto extends AsyncTask<InputStream, Void, File> {

        @Override
        protected File doInBackground(InputStream... params) {

            if (params[0] == null)
                return null;

            File tempFile = null;
            FileOutputStream outputStream = null;
            RandomAccessFile randomAccessFile = null;

            try {
                tempFile = File.createTempFile("cover_photo", ".tmp");
                (randomAccessFile = new RandomAccessFile(tempFile, "rws")).setLength(0);
                ByteStreams.copy(params[0], outputStream = new FileOutputStream(tempFile));
                outputStream.flush();
            } catch (IOException e) {

                e.printStackTrace();
                if (tempFile != null)
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                return null;
            } finally {
                MiscUtils.closeQuietly(outputStream, params[0], randomAccessFile);
            }

            try {
                return MiscUtils.compressImage(tempFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Nullable
        private ProgressDialog dialog = null;

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
            MiscUtils.useActivity(reference, activity -> dialog = new ProgressDialog(activity));
            if (dialog != null) {
                dialog.setCancelable(false);
                dialog.show();
            }
        }

        @Override
        protected void onPostExecute(File file) {

            super.onPostExecute(file);

            MiscUtils.useActivity(reference, activity -> {

                if (file == null) {

                    ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                            .setCategory("Cover photo failed")
                            .setAction("User Id - " + userId)
                            .setValue(1)
                            .build());

                    toUploadCoverPhoto = null;
                    Toast.makeText(activity, "Failed to set Cover Photo, try again", Toast.LENGTH_LONG).show();
                } else if (activity.cover != null) {

                    toUploadCoverPhoto = file;
                    activity.cover.setController(MiscUtils.getControllerwithResize(activity.cover.getController(),
                            Uri.parse("file://" + file.getAbsolutePath()), 500, 500));
                }
            });

            if (dialog != null)
                dialog.dismiss();
        }
    }

    private void doUpdate() {

        if (firstName.length() == 0) //let it throw, should never happen
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
        else if (!MiscUtils.isOnline(this)) {
            Toast.makeText(this, "No internet found", Toast.LENGTH_SHORT).show();
            NavUtils.navigateUpFromSameTask(this); //move back
        }
        else {

            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(firstName.getWindowToken(), 0);
            firstName.setEnabled(false);
            new UpdateProfile().executeOnExecutor(profileEditor, firstName.getText().toString());
        }
    }

    private static final class UpdateProfile extends AsyncTask<String, Void, Boolean> {

        @Nullable
        private ProgressDialog dialog = null;

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
            MiscUtils.useActivity(reference, activity -> dialog = new ProgressDialog(activity));
            if (dialog != null) {
                dialog.setCancelable(false);
                dialog.show();
            }
        }

        /**
         * When sending list of info to update
         * 0 : userName
         * 1 : imageId
         * 2 : coverPicId
         * 3 : statusSong
         * 4 : emailId
         * 5 : birthday
         */
        @Override
        protected Boolean doInBackground(final String... name) {

            //upload Profile photo
            InputStream keyStream = MiscUtils.useContextFromContext(reference, activity -> {
                try {
                    return activity.getAssets().open("key.p12");
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }).orNull();

            if (keyStream != null && toUploadProfilePhoto != null && toUploadProfilePhoto.length() > 0)
                CloudStorageUtils.uploadImage(toUploadProfilePhoto, keyStream, PROFILE_PIC_PROGRESS);
            else
                PROFILE_PIC_PROGRESS.error();

            //upload Cover photo
            keyStream = MiscUtils.useContextFromContext(reference, activity -> {
                try {
                    return activity.getAssets().open("key.p12");
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }).orNull();

            if (keyStream != null && toUploadCoverPhoto != null && toUploadCoverPhoto.length() > 0)
                CloudStorageUtils.uploadImage(toUploadCoverPhoto, keyStream, COVER_PIC_PROGRESS);
            else
                COVER_PIC_PROGRESS.error();

            //upload user data
            final List<String> toPush = Arrays.asList(
                    name[0],
                    profilePhotoId == null ? "" : profilePhotoId,
                    coverPhotoId == null ? "" : coverPhotoId);
            Log.i("Ayush", "Pushing " + userId + " " + MiscUtils.seqToString(toPush));
            try {
                return StaticData.USER_API.updateUserDetails(userId, toPush).execute() == null;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {

            MiscUtils.useActivity(reference, activity -> {

                if (activity.firstName == null)
                    return;

                if (TextUtils.isEmpty(profilePhotoId) && activity.profile != null)
                    activity.profile.setImageBitmap(null);

                if (TextUtils.isEmpty(coverPhotoId) && activity.cover != null)
                    activity.cover.setImageBitmap(null);

                if (success != null && success) {

                    Toast.makeText(activity, "Changes saved successfully!!", Toast.LENGTH_SHORT).show();
                    final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_APPEND);
                    SharedPrefUtils.storeImageId(sharedPreferences, profilePhotoId);
                    SharedPrefUtils.storeCoverImageId(sharedPreferences, coverPhotoId);
                    SharedPrefUtils.storeUserName(sharedPreferences, activity.firstName.getText().toString());

                } else {

                    Toast.makeText(activity, "Failed, try again", Toast.LENGTH_SHORT).show();
                    if (activity.profile != null)
                        activity.profile.setImageBitmap(null);
                    if (activity.cover != null)
                        activity.cover.setImageBitmap(null);
                }

                activity.firstName.setEnabled(true);
                activity.firstName.requestFocus();
            });

            if (dialog != null)
                dialog.dismiss();
        }

        private static final UploadProgress PROFILE_PIC_PROGRESS = new UploadProgress() {

            @Override
            public void success(String fileName) {
                //save fileName
                profilePhotoId = fileName;
            }

            @Override
            public void error() {

//                if (dialogWeakReference == null)
//                    return;
//                final ProgressBar progressBar = dialogWeakReference.get();
//                if (progressBar == null)
//                    return;
                profilePhotoId = null;
            }

            @Override
            public void progressChanged(MediaHttpUploader uploader) throws IOException {

//                switch (uploader.getUploadState()) {
//
//                    case INITIATION_STARTED:
////                    System.out.println("Initiation Started");
//                        break;
//                    case INITIATION_COMPLETE:
////                    System.out.println("Initiation Completed");
//                        break;
//                    case MEDIA_IN_PROGRESS:
//
//                        if (dialogWeakReference == null)
//                            return;
//                        final ProgressBar progressBar = dialogWeakReference.get();
//                        if (progressBar == null)
//                            return;
//                        progressBar.setProgress((int) (uploader.getProgress() * 100));
//                        break;
//                    case MEDIA_COMPLETE:
//                        break;
//                }
            }
        };

        private static final UploadProgress COVER_PIC_PROGRESS = new UploadProgress() {
            @Override
            public void success(String fileName) {
                //save fileName
                coverPhotoId = fileName;
            }

            @Override
            public void error() {
                coverPhotoId = null;
            }

            @Override
            public void progressChanged(MediaHttpUploader uploader) throws IOException {
                //ignored
            }
        };
    }
}
