package reach.project.coreViews;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.CloudStorageUtils;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.UploadProgress;
import reach.project.viewHelpers.CircleTransform;

public class EditProfileFragment extends Fragment {

    private final int IMAGE_PICKER_SELECT = 999;

    private EditText firstName = null;
    private TextView uploadText = null;
    private View editProfileContainer = null;
    private ProgressBar loadingBar = null;
    private ImageView profile = null;
    private SharedPreferences sharedPreferences = null;

    private static Uri imageUri = null;
    private static long userId = 0;
    private static String imageId = null;
    private static WeakReference<EditProfileFragment> reference = null;

    public static EditProfileFragment newInstance() {

        EditProfileFragment fragment;
        if (reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new EditProfileFragment());
        return fragment;
    }

    private View.OnClickListener doUpdate = v -> {

        if (firstName.length() == 0)
            Toast.makeText(v.getContext(), "Please enter your name", Toast.LENGTH_SHORT).show();
        else {

            ((InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(firstName.getWindowToken(), 0);
            editProfileContainer.setVisibility(View.INVISIBLE);
            loadingBar.setVisibility(View.VISIBLE);
            uploadText.setText("");
            firstName.setEnabled(false);

            final String name = firstName.getText().toString();

            new UpdateProfile().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, name);
            //save to cache
            SharedPrefUtils.storeUserName(sharedPreferences, name);
            SharedPrefUtils.storeImageId(sharedPreferences, imageId);
        }
    };

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        final Activity activity = getActivity();
        final ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle("Edit Profile");

        sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_APPEND);
        profile = (ImageView) rootView.findViewById(R.id.profilePhoto);
        uploadText = (TextView) rootView.findViewById(R.id.uploadText);
        firstName = (EditText) rootView.findViewById(R.id.firstName);
        editProfileContainer = rootView.findViewById(R.id.editProfilecontainer);

        loadingBar = (ProgressBar) rootView.findViewById(R.id.loadingBar);
        loadingBar.setIndeterminate(false);
        UpdateProfile.uploadProgress.dialogWeakReference = new WeakReference<>(loadingBar);

        final String imageId = SharedPrefUtils.getImageId(sharedPreferences);
        userId = SharedPrefUtils.getServerId(sharedPreferences);
        firstName.setText(SharedPrefUtils.getUserName(sharedPreferences));

        if (!TextUtils.isEmpty(imageId) && !imageId.equals("hello_world")) {
            profile.setBackgroundResource(0);
            Picasso.with(container.getContext()).load(StaticData.cloudStorageImageBaseUrl + imageId).transform(new CircleTransform()).into(profile);
        }

        profile.setOnClickListener(imagePicker);
        rootView.findViewById(R.id.editLibrary).setOnClickListener(doUpdate);

        return rootView;
    }

    private final View.OnClickListener imagePicker = view -> {

        final Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), IMAGE_PICKER_SELECT);
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {

        final Activity activity;
        if ((activity = getActivity()) == null || activity.isFinishing())
            return;

        if (requestCode != IMAGE_PICKER_SELECT || resultCode != Activity.RESULT_OK || (imageUri = data.getData()) == null) {

            Toast.makeText(activity, "Failed to set Profile Photo, try again", Toast.LENGTH_SHORT).show();
            return;
        }

        //set image
        Picasso.with(activity)
                .load(imageUri)
                .resize(350, 350)
                .centerCrop()
                .transform(new CircleTransform())
                .into(profile);
    }

    private static final class UpdateProfile extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(final String... name) {

            uploadImage();

            if (imageUri == null || TextUtils.isEmpty(imageId))
                return false;

            //TODO we are ignoring fail case !
            MiscUtils.autoRetry(() -> StaticData.userEndpoint.updateUserDetails(userId, ImmutableList.of(name[0], imageId)).execute(), Optional.<Predicate<Void>>absent()).orNull();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {

            MiscUtils.useFragment(reference, fragment -> {

                if (fragment.firstName == null || fragment.uploadText == null || fragment.editProfileContainer == null || fragment.loadingBar == null)
                    return null;

                if (success)
                    Toast.makeText(fragment.getActivity(), "Changes saved successfully!!", Toast.LENGTH_SHORT).show();
                else {
                    Toast.makeText(fragment.getActivity(), "Failed, try again", Toast.LENGTH_SHORT).show();
                    fragment.profile.setImageBitmap(null);
                }

                fragment.firstName.setEnabled(true);
                fragment.firstName.requestFocus();
                fragment.uploadText.setText("Edit\nPhoto");
                fragment.editProfileContainer.setVisibility(View.VISIBLE);
                fragment.loadingBar.setVisibility(View.INVISIBLE);

                return null;
            });
        }

        private static void uploadImage() {

            //get the image stream
            final InputStream imageStream = MiscUtils.useContextFromFragment(reference, context -> {
                try {
                    return context.getContentResolver().openInputStream(imageUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                return null;
            }).orNull();

            if (imageStream == null) {

                uploadProgress.error();
                return;
            }

            //copy the file
            File tempFile;
            FileOutputStream outputStream = null;
            try {
                tempFile = File.createTempFile("profilePhoto", null);
                outputStream = new FileOutputStream(tempFile);
                ByteStreams.copy(imageStream, outputStream);
            } catch (IOException e) {

                e.printStackTrace();
                uploadProgress.error();
                return;
            } finally {
                MiscUtils.closeQuietly(outputStream, imageStream);
            }

            //TODO make this
            try {
                tempFile = MiscUtils.compressImage(tempFile, 800);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //get the key
            final InputStream keyStream = MiscUtils.useContextFromFragment(reference, context -> {
                try {
                    return context.getAssets().open("key.p12");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }).orNull();

            if (keyStream == null) {
                uploadProgress.error();
                return;
            }

            CloudStorageUtils.uploadFile(tempFile, keyStream, uploadProgress);
        }

        private static final UploadProgress uploadProgress = new UploadProgress() {

            @Override
            public void success(String fileName) {

                //save fileName
                imageId = fileName;

                if (dialogWeakReference == null)
                    return;
                final ProgressBar progressBar = dialogWeakReference.get();
                if (progressBar == null)
                    return;

                MiscUtils.runOnUiThreadFragment(reference, (Activity context) -> {

                    Toast.makeText(context, "Uploaded successfully", Toast.LENGTH_SHORT).show();
                    return null;
                });
            }

            @Override
            public void error() {

                if (dialogWeakReference == null)
                    return;
                final ProgressBar progressBar = dialogWeakReference.get();
                if (progressBar == null)
                    return;

                imageId = null;
                imageUri = null;
            }

            @Override
            public void progressChanged(MediaHttpUploader uploader) throws IOException {

                switch (uploader.getUploadState()) {

                    case INITIATION_STARTED:
//                    System.out.println("Initiation Started");
                        break;
                    case INITIATION_COMPLETE:
//                    System.out.println("Initiation Completed");
                        break;
                    case MEDIA_IN_PROGRESS:

                        if (dialogWeakReference == null)
                            return;
                        final ProgressBar progressBar = dialogWeakReference.get();
                        if (progressBar == null)
                            return;
                        progressBar.setProgress((int) (uploader.getProgress() * 100));
                        break;
                    case MEDIA_COMPLETE:
                        break;
                }
            }
        };
    }
}
