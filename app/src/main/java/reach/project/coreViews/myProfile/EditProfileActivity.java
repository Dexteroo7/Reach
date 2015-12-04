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
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.utils.CloudStorageUtils;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.UploadProgress;

public class EditProfileActivity extends AppCompatActivity {

    private final int IMAGE_PICKER_SELECT = 999;

    private EditText firstName = null;
    private TextView uploadText = null;
    private SimpleDraweeView profile = null;
    private SharedPreferences sharedPreferences = null;

    private static InputStream keyStream;
    private static File toUpload = null;
    private static String imageId = null;
    private static long userId = 0;
    private static WeakReference<EditProfileActivity> reference;

    private void doUpdate() {

        if (firstName.length() == 0)
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
        else {

            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(firstName.getWindowToken(), 0);
            uploadText.setText("");
            firstName.setEnabled(false);

            final String name = firstName.getText().toString();

            new UpdateProfile().executeOnExecutor(StaticData.temporaryFix, name);
            //save to cache
            SharedPrefUtils.storeUserName(sharedPreferences, name);
            SharedPrefUtils.storeImageId(sharedPreferences, imageId);
        }
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        reference = new WeakReference<>(this);

        final Toolbar mToolbar = (Toolbar) findViewById(R.id.editProfileToolbar);

        mToolbar.setTitle("Edit Profile");
        mToolbar.setNavigationOnClickListener(v -> NavUtils.navigateUpFromSameTask(this));

        sharedPreferences = getSharedPreferences("Reach", Context.MODE_APPEND);
        profile = (SimpleDraweeView) findViewById(R.id.profilePic);
        uploadText = (TextView) findViewById(R.id.editText);
        firstName = (EditText) findViewById(R.id.name);

        final String imageId = SharedPrefUtils.getImageId(sharedPreferences);
        final String uName = SharedPrefUtils.getUserName(sharedPreferences);
        userId = SharedPrefUtils.getServerId(sharedPreferences);

        firstName.setText(uName);
        firstName.setSelection(uName.length());

        if (!TextUtils.isEmpty(imageId) && !imageId.equals("hello_world"))
            profile.setImageBitmap(null);
        profile.setImageURI(Uri.parse(StaticData.cloudStorageImageBaseUrl + imageId));

        profile.setOnClickListener(imagePicker);
    }

    private final View.OnClickListener imagePicker = view -> {

        final Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), IMAGE_PICKER_SELECT);
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {

        if (isFinishing()) {

            Log.i("Ayush", "ACTIVITY NOT FOUND !");
            return;
        }

        final Uri imageUri;
        if (requestCode != IMAGE_PICKER_SELECT || resultCode != Activity.RESULT_OK || (imageUri = data.getData()) == null) {

            Toast.makeText(this, "Failed to set Profile Photo, try again", Toast.LENGTH_SHORT).show();
            return;
        }

        InputStream imageStream;
        try {
            imageStream = getContentResolver().openInputStream(imageUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            imageStream = null;
        }

        new ProcessImage().executeOnExecutor(StaticData.temporaryFix, imageStream);
    }

    private static final class ProcessImage extends AsyncTask<InputStream, Void, File> {

        @Override
        protected File doInBackground(InputStream... params) {

            if (params[0] == null)
                return null;

            File tempFile = null;
            FileOutputStream outputStream = null;
            try {
                tempFile = File.createTempFile("profile_photo", ".tmp");
                new RandomAccessFile(tempFile, "rws").setLength(0);
                outputStream = new FileOutputStream(tempFile);
                ByteStreams.copy(params[0], outputStream);
                outputStream.flush();
            } catch (IOException e) {

                e.printStackTrace();
                if (tempFile != null)
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                return null;
            } finally {
                MiscUtils.closeQuietly(outputStream, params[0]);
            }

            try {
                return MiscUtils.compressImage(tempFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

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

                    toUpload = null;
                    Toast.makeText(activity, "Failed to set Profile Photo, try again", Toast.LENGTH_LONG).show();
                } else if (activity.profile != null) {
                    toUpload = file;
                    activity.profile.setImageURI(Uri.parse("file://" + toUpload.getAbsolutePath()));
                }
            });

            if (dialog != null)
                dialog.dismiss();
        }
    }

    private static final class UpdateProfile extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(final String... name) {

            //get the key
            if (toUpload != null) {

                MiscUtils.useActivity(reference, activity -> {
                    try {
                        keyStream =  activity.getAssets().open("key.p12");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                if (keyStream == null) {
                    uploadProgress.error();
                    return false;
                }

                //try upload
                CloudStorageUtils.uploadImage(toUpload, keyStream, uploadProgress);

                //uploadProgress will set the file name
                if (TextUtils.isEmpty(imageId))
                    return false;
            }

            Log.i("Ayush", "Pushing " + userId + " " + name[0] + " " + imageId);
            MiscUtils.autoRetry(() -> StaticData.userEndpoint.updateUserDetails(userId, ImmutableList.of(name[0], imageId)).execute(), Optional.absent());
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {

            MiscUtils.useActivity(reference, activity -> {

                if (activity.firstName == null || activity.uploadText == null)
                    return;

                if (imageId == null) {
                    if (activity.profile != null)
                        activity.profile.setImageBitmap(null);
                    return;
                }

                if (success != null && success) {

                    Toast.makeText(activity, "Changes saved successfully!!", Toast.LENGTH_SHORT).show();
                    SharedPrefUtils.storeImageId(activity.sharedPreferences, imageId);
                    //TODO
                    //if (fragment.mListener != null && activity.firstName != null)
                    //    fragment.mListener.updateDetails(toUpload, activity.firstName.getText().toString());
                } else {

                    Toast.makeText(activity, "Failed, try again", Toast.LENGTH_SHORT).show();
                    activity.profile.setImageBitmap(null);
                }

                activity.firstName.setEnabled(true);
                activity.firstName.requestFocus();
                activity.uploadText.setText("Edit\nPhoto");

            });
        }

        private static final UploadProgress uploadProgress = new UploadProgress() {

            @Override
            public void success(String fileName) {
                //save fileName
                imageId = fileName;
            }

            @Override
            public void error() {

//                if (dialogWeakReference == null)
//                    return;
//                final ProgressBar progressBar = dialogWeakReference.get();
//                if (progressBar == null)
//                    return;
                imageId = null;
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
    }
}
