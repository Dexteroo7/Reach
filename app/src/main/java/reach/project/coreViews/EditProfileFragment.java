package reach.project.coreViews;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.CloudStorageUtils;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.DoWork;
import reach.project.viewHelpers.CircleTransform;

public class EditProfileFragment extends Fragment {

    private EditText firstName;
    private TextView uploadText;
    private View editProfileContainer;
    private ProgressBar loadingBar;
    private ImageView profile;
    private SharedPreferences sharedPreferences;

    private final int IMAGE_PICKER_SELECT = 999;
    private long userId;
    private String imageId;

    private static WeakReference<EditProfileFragment> reference = null;

    public static EditProfileFragment newInstance() {

        EditProfileFragment fragment;
        if (reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new EditProfileFragment());
        return fragment;
    }

    @Override
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

        rootView.findViewById(R.id.editLibrary).setOnClickListener(v -> {

            if (firstName.length() == 0)
                Toast.makeText(activity, "Please enter your name", Toast.LENGTH_SHORT).show();
            else {
                ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(firstName.getWindowToken(), 0);
                editProfileContainer.setVisibility(View.INVISIBLE);
                loadingBar.setVisibility(View.VISIBLE);
                uploadText.setText("");
                firstName.setEnabled(false);
                new UpdateProfile().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, firstName.getText().toString());
            }
        });

        final String imageId = SharedPrefUtils.getImageId(sharedPreferences);
        userId = SharedPrefUtils.getServerId(sharedPreferences);
        firstName.setText(SharedPrefUtils.getUserName(sharedPreferences));

        if (!TextUtils.isEmpty(imageId) && !imageId.equals("hello_world")) {
            profile.setBackgroundResource(0);
            Picasso.with(container.getContext()).load(StaticData.cloudStorageImageBaseUrl + imageId).transform(new CircleTransform()).into(profile);
        }

        profile.setOnClickListener(imagePicker);
        return rootView;
    }

    private final View.OnClickListener imagePicker = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            // intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Photo"),
                    IMAGE_PICKER_SELECT);
        }
    };

    private class UpdateProfile extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(final String... name) {

            //save to cache
            SharedPrefUtils.storeUserName(sharedPreferences, name[0]);
            SharedPrefUtils.storeImageId(sharedPreferences, imageId);

            MiscUtils.autoRetry(new DoWork<Void>() {
                @Override
                public Void doWork() throws IOException {
                    return StaticData.userEndpoint.updateUserDetails(userId, ImmutableList.of(name[0], imageId)).execute();
                }
            }, Optional.<Predicate<Void>>absent()).orNull();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            final Activity activity = getActivity();
            if (firstName == null || uploadText == null || editProfileContainer == null || loadingBar == null ||
                    isRemoving() || isCancelled() || activity == null || activity.isFinishing())
                return;
            Toast.makeText(activity,
                    "Changes saved successfully!!", Toast.LENGTH_SHORT).show();
            firstName.setEnabled(true);
            firstName.requestFocus();
            uploadText.setText("Edit\nPhoto");
            editProfileContainer.setVisibility(View.VISIBLE);
            loadingBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {

        final FragmentActivity activity = getActivity();
        if (activity == null ||
                activity.isFinishing()) return;
        final Uri mImageUri;
        if (requestCode != IMAGE_PICKER_SELECT ||
                resultCode != Activity.RESULT_OK ||
                (mImageUri = data.getData()) == null) {

            Toast.makeText(activity, "Failed to set Profile Photo, try again", Toast.LENGTH_SHORT).show();
            return;
        }

        final InputStream stream;
        try {
            stream = activity.getAssets().open("key.p12");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        final File tempFile;
        try {
            tempFile = File.createTempFile("profilePhoto", ".jpg");
            Files.copy(() -> {
                return activity.getContentResolver().openInputStream(mImageUri);
            }, tempFile);
        } catch (IOException e) {
            e.printStackTrace();
            MiscUtils.closeAndIgnore(stream);
            return;
        }

        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {

            Optional<String> newImage = CloudStorageUtils.uploadFile(tempFile, stream);
            if (newImage.isPresent())
                imageId = newImage.get();
            else
                imageId = "hello_world";
        });

        uploadText.setVisibility(View.INVISIBLE);
        Picasso.with(activity)
                .load(mImageUri)
                .resize(350, 350)
                .centerCrop()
                .transform(new CircleTransform())
                .into(profile);
    }
}
