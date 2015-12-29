package reach.project.onBoarding;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.common.base.Optional;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;

import reach.backend.entities.userApi.model.OldUserContainerNew;
import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.UseContext;
import reach.project.utils.auxiliaryClasses.UseContextAndFragment;

import static com.google.common.base.Preconditions.checkNotNull;

public class AccountCreation extends Fragment {

    private static String imageFilePath = null;

    private static WeakReference<AccountCreation> reference = null;

    public static Fragment newInstance(Optional<OldUserContainerNew> container) {

        final AccountCreation fragment = new AccountCreation();

        if (container.isPresent()) {

            final Bundle bundle = new Bundle(2);
            final OldUserContainerNew userContainer = container.get();
            bundle.putStringArray("oldData", new String[]{
                    userContainer.getName() == null ? "" : userContainer.getName(),
                    userContainer.getImageId() == null ? "" : userContainer.getImageId()});
            fragment.setArguments(bundle);
        }

        reference = new WeakReference<>(fragment);
        return fragment;
    }

    private final int IMAGE_PICKER_SELECT = 999;
    private SplashInterface mListener = null;
    private ImageView profilePhotoSelector = null;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_account_creation, container, false);
        final EditText userName = (EditText) rootView.findViewById(R.id.userName);

        profilePhotoSelector = (ImageView) rootView.findViewById(R.id.displayPic);
        profilePhotoSelector.setOnClickListener(imagePicker);
        userName.requestFocus();

        final String oldImageId;
        final FragmentActivity activity = getActivity();
        final Bundle arguments;
        final String[] oldData;
        if ((arguments = getArguments()) != null && (oldData = arguments.getStringArray("oldData")) != null && oldData.length == 2) {

            /**
             * oldData[0] = name;
             * oldData[1] = imageId;
             */
            if (!TextUtils.isEmpty(oldData[0])) {

                userName.setText(oldData[0]);
                userName.setSelection(oldData[0].length());
            }

            oldImageId = oldData[1];

            if (!TextUtils.isEmpty(oldData[1])) {

                Uri uriToDisplay = null;
                if (!TextUtils.isEmpty(oldData[1]) && !oldData[1].equals("hello_world"))
                    uriToDisplay = Uri.parse(StaticData.cloudStorageImageBaseUrl + oldData[1]);
                profilePhotoSelector.setImageURI(uriToDisplay);
            }
        } else
            oldImageId = "";

        rootView.findViewById(R.id.verify).setOnClickListener(view -> {

            final String name;
            if (TextUtils.isEmpty(name = userName.getText().toString().trim())) {
                Toast.makeText(activity, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }

            //OK
            ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(userName.getWindowToken(), 0);
            view.setOnClickListener(null);
            view.setEnabled(false);
            Log.i("Ayush", "Cleared everything : AccountCreation underway");
            profilePhotoSelector.setOnClickListener(null);

            final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
            final String phoneNumber = SharedPrefUtils.getPhoneNumber(sharedPreferences);
            mListener.onOpenScan(name, imageFilePath, oldImageId, phoneNumber);

            //TODO track
            /*final Map<PostParams, String> simpleParams = MiscUtils.getMap(2);
            simpleParams.put(PostParams.USER_NUMBER, phoneNumber);
            simpleParams.put(PostParams.USER_NAME, name);
            try {
                UsageTracker.trackLogEvent(simpleParams, UsageTracker.NAME_ENTERED);
            } catch (JSONException ignored) {}*/
        });
        return rootView;
    }

    private final View.OnClickListener imagePicker = v -> {

        final Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), IMAGE_PICKER_SELECT);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Sorry! Your device does not support this feature", Toast.LENGTH_SHORT).show();
        }
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
            mListener = (SplashInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement SplashInterface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private static final class ProcessImage extends AsyncTask<InputStream, Void, File> {

        private static final int BUF_SIZE = 0x1000; // 4K

        private long copy(InputStream from, RandomAccessFile to)
                throws IOException {

            checkNotNull(from);
            checkNotNull(to);

            byte[] buf = new byte[BUF_SIZE];
            long total = 0;
            while (true) {
                int r = from.read(buf);
                if (r == -1) {
                    break;
                }
                to.write(buf, 0, r);
                total += r;
            }
            return total;
        }

        @Override
        protected File doInBackground(InputStream... params) {

            if (params[0] == null)
                return null;

            File tempFile = null;
            RandomAccessFile accessFile = null;

            try {

                tempFile = File.createTempFile("profile_photo", ".tmp");
                accessFile = new RandomAccessFile(tempFile, "rws");
                accessFile.setLength(0);
                copy(params[0], accessFile);
            } catch (IOException e) {

                e.printStackTrace();
                if (tempFile != null && !tempFile.delete() && accessFile != null)
                    try {
                        accessFile.setLength(0);
                    } catch (IOException ignored) {
                    }
                return null;

            } finally {
                MiscUtils.closeQuietly(accessFile, params[0]);
            }

            try {
                return MiscUtils.compressImage(tempFile); //return compressed image
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
            dialog = MiscUtils.useContextFromFragment(reference, (UseContext<ProgressDialog, Context>) ProgressDialog::new).orNull();
            if (dialog != null) {
                dialog.setCancelable(false);
                dialog.show();
            }
        }

        @Override
        protected void onPostExecute(File file) {

            super.onPostExecute(file);
            MiscUtils.useContextAndFragment(reference, new UseContextAndFragment<Activity, AccountCreation>() {
                @Override
                public void work(Activity activity, AccountCreation fragment) {

                    if (file == null) {

                        //TODO
                        ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                                .setCategory("Failed to set profile photo")
                                        //.setAction("User Id - " + serverId)
//                                .setLabel("Phone Number - " + phoneNumber)
                                .setValue(1)
                                .build());
                        Toast.makeText(activity, "Failed to set Profile Photo, try again", Toast.LENGTH_LONG).show();

                    } else if (fragment.profilePhotoSelector != null) {

                        //save profile photo path
                        imageFilePath = file.getPath();
                        fragment.profilePhotoSelector.setImageURI(Uri.parse("file://" + imageFilePath));
                    }
                }
            });

            if (dialog != null)
                dialog.dismiss();
        }
    }
}