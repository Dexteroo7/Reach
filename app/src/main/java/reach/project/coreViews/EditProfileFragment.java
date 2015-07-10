package reach.project.coreViews;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.CloudStorageUtils;
import reach.project.utils.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.StringCompress;
import reach.project.viewHelpers.CircleTransform;

public class EditProfileFragment extends Fragment {

    private ImageView profile;
    private EditText firstName,lastName;
    private TextView uploadText;
    private RelativeLayout relativeLayout;
    private ProgressBar loadingBar;
    private View rootView;
    private ActionBar actionBar;

    private SharedPreferences sharedPreferences;
    private Bitmap bitmap;

    private final int IMAGE_PICKER_SELECT = 999;
    private long userId;

    private static WeakReference<EditProfileFragment> reference = null;
    public static EditProfileFragment newInstance() {

        EditProfileFragment fragment;
        if(reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new EditProfileFragment());
        return fragment;
    }
    public EditProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onDestroyView() {

        profile = null;
        firstName = null;
        lastName = null;
        uploadText = null;
        relativeLayout = null;
        loadingBar = null;
        rootView = null;
        actionBar = null;
        sharedPreferences = null;
        if(bitmap != null)
            bitmap.recycle();
        bitmap = null;
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle("Edit Profile");
        profile = (ImageView) rootView.findViewById(R.id.profilePhoto);
        uploadText = (TextView) rootView.findViewById(R.id.uploadText);
        firstName = (EditText) rootView.findViewById(R.id.firstName);
        lastName = (EditText) rootView.findViewById(R.id.lastName);
        relativeLayout = (RelativeLayout) rootView.findViewById(R.id.editProfilecontainer);
        loadingBar = (ProgressBar) rootView.findViewById(R.id.loadingBar);
        rootView.findViewById(R.id.editLibrary).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    if (firstName.length() == 0 || lastName.length() == 0 )
                        Toast.makeText(getActivity(), "Please enter your name", Toast.LENGTH_SHORT).show();
                    else {
                        ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                                .hideSoftInputFromWindow(lastName.getWindowToken(), 0);
                        relativeLayout.setVisibility(View.INVISIBLE);
                        loadingBar.setVisibility(View.VISIBLE);
                        uploadText.setText("");
                        firstName.setEnabled(false);
                        lastName.setEnabled(false);
                        new UpdateProfile().executeOnExecutor(StaticData.threadPool, firstName.getText()+" "+lastName.getText());
                    }
            }
        });
        sharedPreferences = getActivity().getSharedPreferences("Reach", Context.MODE_APPEND);
        final String[] name = SharedPrefUtils.getUserName(sharedPreferences).split(" ");
        final String imageId = SharedPrefUtils.getImageId(sharedPreferences);
        userId = SharedPrefUtils.getServerId(sharedPreferences);
        firstName.setText(name[0]);
        String lName = "";
        for (int i = 1; i < name.length; i++) {
            lName = lName + name[i];
            if (i != (name.length - 1))
                lName = lName + " ";
        }
        lastName.setText(lName);
        if (!TextUtils.isEmpty(imageId) && !imageId.equals("hello_world")) {
            profile.setBackgroundResource(0);
            Picasso.with(container.getContext()).load(StaticData.cloudStorageImageBaseUrl + imageId).transform(new CircleTransform()).into(profile);
        }

        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                // intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Photo"),
                        IMAGE_PICKER_SELECT);
            }
        });
        return rootView;
    }

    private class UpdateProfile extends AsyncTask<String, Void, Void> {

        private String newImageId = "hello_world";

        @Override
        protected Void doInBackground(final String... name) {

            if(bitmap != null) {
                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                if (!bitmap.isRecycled()) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream);
                    bitmap.recycle();
                }
                if (byteArrayOutputStream.toByteArray()!=null && byteArrayOutputStream.toByteArray().length>0)
                    newImageId = CloudStorageUtils.uploadFile(byteArrayOutputStream.toByteArray(), false);
            }
            else
                newImageId = SharedPrefUtils.getImageId(sharedPreferences);

            //save to cache
            SharedPrefUtils.storeUserName(sharedPreferences.edit(), name[0]);
            SharedPrefUtils.storeImageId(sharedPreferences.edit(), newImageId);

            MiscUtils.autoRetry(new DoWork<Void>() {
                @Override
                protected Void doWork() throws IOException {
                    return StaticData.userEndpoint.updateUserDetails(userId, ImmutableList.of(name[0], newImageId)).execute();
                }
            }, Optional.<Predicate<Void>>absent()).orNull();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            if(firstName == null || lastName == null || uploadText == null || relativeLayout == null || loadingBar == null ||
               isRemoving() || isCancelled() || getActivity() == null || getActivity().isFinishing())
                return;
            Toast.makeText(getActivity(),
                    "Changes saved successfully!!", Toast.LENGTH_SHORT).show();
            firstName.setEnabled(true);
            firstName.requestFocus();
            lastName.setEnabled(true);
            uploadText.setText("Edit\nPhoto");
            relativeLayout.setVisibility(View.VISIBLE);
            loadingBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        //Log.i("Ayush", "Editing profile pic " + requestCode + " " + resultCode + " " + data.toString());
        if (requestCode == IMAGE_PICKER_SELECT  && resultCode == Activity.RESULT_OK && data!=null) {
            final Uri mImageUri = data.getData();
            if(mImageUri != null)
                new ImageBitmapExtractor().executeOnExecutor(StaticData.threadPool, data.getData());
            uploadText.setText("");
        }
    }

    private class ImageBitmapExtractor extends AsyncTask<Uri, Void, Bitmap> {

        Uri temp;
        @Override
        protected Bitmap doInBackground(Uri... uris) {

            if(uris == null || uris.length == 0) return null;
            temp = uris[0];
            try {
                return Picasso.with(getActivity())
                        .load(uris[0])
                        .resize(350, 350)
                        .centerCrop()
                        .get();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap1) {
            super.onPostExecute(bitmap1);

            if(isCancelled() || getActivity() == null || getActivity().isFinishing() || profile == null)
                return;

            if(bitmap1 == null)
                Toast.makeText(getActivity(), "Failed to set Profile Photo, try again", Toast.LENGTH_SHORT).show();
            else {
                bitmap = bitmap1;
                Picasso.with(getActivity())
                        .load(temp)
                        .resize(350, 350)
                        .centerCrop()
                        .transform(new CircleTransform())
                        .into(profile);
            }
        }
    }
}
