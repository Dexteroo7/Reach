package reach.project.coreViews;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import reach.project.utils.auxiliaryClasses.SuperInterface;
import reach.project.utils.auxiliaryClasses.UploadProgress;
import reach.project.utils.auxiliaryClasses.UseContext;
import reach.project.utils.auxiliaryClasses.UseContextAndFragment;

public class EditProfileFragment extends Fragment {

    private final int IMAGE_PICKER_SELECT = 999;
//    private final CircleTransform transform = new CircleTransform();

    private SuperInterface mListener;
    private EditText firstName = null;
    private TextView uploadText = null;
    private View editProfileContainer = null;
    private ProgressBar loadingBar = null;
    private ImageView profile = null;
    private SharedPreferences sharedPreferences = null;

    private static File toUpload = null;
    private static String imageId = null;
    private static long userId = 0;
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
            editProfileContainer.setVisibility(View.GONE);
            loadingBar.setVisibility(View.VISIBLE);
            uploadText.setText("");
            firstName.setEnabled(false);

            final String name = firstName.getText().toString();

            new UpdateProfile().executeOnExecutor(StaticData.temporaryFix, name);
            //save to cache
            SharedPrefUtils.storeUserName(sharedPreferences, name);
            SharedPrefUtils.storeImageId(sharedPreferences, imageId);
        }
    };

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        final Toolbar mToolbar = (Toolbar) rootView.findViewById(R.id.editProfileToolbar);
        final Activity activity = getActivity();

        mToolbar.setTitle("Edit Profile");
        mToolbar.setNavigationOnClickListener(v -> activity.onBackPressed());

        sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_APPEND);
        profile = (ImageView) rootView.findViewById(R.id.profilePhoto);
        uploadText = (TextView) rootView.findViewById(R.id.uploadText);
        firstName = (EditText) rootView.findViewById(R.id.firstName);
        editProfileContainer = rootView.findViewById(R.id.editProfilecontainer);
        loadingBar = (ProgressBar) rootView.findViewById(R.id.loadingBar);

        final String imageId = SharedPrefUtils.getImageId(sharedPreferences);
        userId = SharedPrefUtils.getServerId(sharedPreferences);
        String uName = SharedPrefUtils.getUserName(sharedPreferences);
        firstName.setText(uName);
        firstName.setSelection(uName.length());

        if (!TextUtils.isEmpty(imageId) && !imageId.equals("hello_world")) {
            profile.setBackgroundResource(0);
//            Picasso.with(container.getContext()).load(StaticData.cloudStorageImageBaseUrl + imageId).fit().centerCrop().transform(transform).into(profile);
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
        if ((activity = getActivity()) == null || activity.isFinishing()) {

            Log.i("Ayush", "ACTIVITY NOT FOUND !");
            return;
        }

        final Uri imageUri;
        if (requestCode != IMAGE_PICKER_SELECT || resultCode != Activity.RESULT_OK || (imageUri = data.getData()) == null) {

            Toast.makeText(activity, "Failed to set Profile Photo, try again", Toast.LENGTH_SHORT).show();
            return;
        }

        InputStream imageStream;
        try {
            imageStream = activity.getContentResolver().openInputStream(imageUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            imageStream = null;
        }

        new ProcessImage().executeOnExecutor(StaticData.temporaryFix, imageStream);
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        try {
            mListener = (SuperInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {

        super.onDetach();
        mListener = null;
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
            dialog = MiscUtils.useContextFromFragment(reference, (UseContext<ProgressDialog, Context>) ProgressDialog::new).orNull();
            if (dialog != null) {
                dialog.setCancelable(false);
                dialog.show();
            }
        }

        @Override
        protected void onPostExecute(File file) {

            super.onPostExecute(file);

            MiscUtils.useContextAndFragment(reference, new UseContextAndFragment<Activity, EditProfileFragment>() {
                @Override
                public void work(Activity activity, EditProfileFragment fragment) {

                    if (file == null) {

                        ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                                .setCategory("Profile photo failed")
                                .setAction("User Id - " + userId)
                                .setValue(1)
                                .build());

                        toUpload = null;
                        Toast.makeText(activity, "Failed to set Profile Photo, try again", Toast.LENGTH_LONG).show();
                    } else if (fragment.profile != null) {

                        toUpload = file;
//                        Picasso.with(activity).load(toUpload).fit().centerCrop().transform(fragment.transform).into(fragment.profile);
                    }
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
                    return false;
                }

                //try upload
                CloudStorageUtils.uploadFile(toUpload, keyStream, uploadProgress);

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

            MiscUtils.useFragment(reference, fragment -> {

                if (fragment.firstName == null || fragment.uploadText == null || fragment.editProfileContainer == null || fragment.loadingBar == null)
                    return null;

                if (imageId == null) {
                    if (fragment.profile != null)
                        fragment.profile.setImageBitmap(null);
                    return null;
                }

                if (success != null && success) {

                    Toast.makeText(fragment.getActivity(), "Changes saved successfully!!", Toast.LENGTH_SHORT).show();
                    SharedPrefUtils.storeImageId(fragment.sharedPreferences, imageId);
                    if (fragment.mListener != null && fragment.firstName != null)
                        fragment.mListener.updateDetails(toUpload, fragment.firstName.getText().toString());
                } else {

                    Toast.makeText(fragment.getActivity(), "Failed, try again", Toast.LENGTH_SHORT).show();
                    fragment.profile.setImageBitmap(null);
                }

                fragment.firstName.setEnabled(true);
                fragment.firstName.requestFocus();
                fragment.uploadText.setText("Edit\nPhoto");
                fragment.editProfileContainer.setVisibility(View.VISIBLE);
                fragment.loadingBar.setVisibility(View.GONE);

                return null;
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
