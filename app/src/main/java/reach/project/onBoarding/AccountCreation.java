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
import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

public class AccountCreation extends Fragment {

    private static String toUpload = null;
    private static String phoneNumber = "";
    private static String imageId = "hello_world";
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
        /*final TextView progress = (TextView) rootView.findViewById(R.id.syncStatus);
        final TextView uploadText = (TextView) rootView.findViewById(R.id.uploadText);
        final ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        //give reference to uploadProgress
        progressBar.setIndeterminate(false);*/

        profilePhotoSelector = (ImageView) rootView.findViewById(R.id.displayPic);
        profilePhotoSelector.setOnClickListener(imagePicker);
        userName.requestFocus();

        final FragmentActivity activity = getActivity();
        final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
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
            if (!TextUtils.isEmpty(oldData[1])) {

                imageId = oldData[1];
                //TODO replace picasso
                /*Picasso.with(activity)
                        .load(StaticData.cloudStorageImageBaseUrl + imageId)
                        .fit()
                        .centerCrop()
                        .into(profilePhotoSelector);*/
            }
        }

        rootView.findViewById(R.id.verify).setOnClickListener(view -> {

            final String name;
            if (TextUtils.isEmpty(name = userName.getText().toString().trim())) {
                Toast.makeText(activity, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }

            phoneNumber = SharedPrefUtils.getPhoneNumber(sharedPreferences);

            if (TextUtils.isEmpty(phoneNumber)) {
                Log.i("Downloader", "Account creation could not find number");
                //TODO startNumberVerification
                //mListener.startNumberVerification();
                return;
            }

            //OK
            ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(userName.getWindowToken(), 0);
            view.setOnClickListener(null);
            view.setEnabled(false);
            sharedPreferences.edit().clear().apply();
            Log.i("Ayush", "Cleared everything : AccountCreation underway");
            profilePhotoSelector.setOnClickListener(null);
            mListener.onOpenScan(name, imageId, toUpload);

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
        }
        catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getContext(),"Sorry! Your device does not support this feature",Toast.LENGTH_SHORT).show();
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

        @Override
        protected File doInBackground(InputStream... params) {

            if (params[0] == null)
                return null;

            File tempFile = null;
            FileOutputStream outputStream = null;
            try {
                tempFile = File.createTempFile("profile_photo", ".tmp");
                final RandomAccessFile accessFile = new RandomAccessFile(tempFile, "rws");
                accessFile.setLength(0);
                accessFile.close();
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
            MiscUtils.useContextAndFragment(reference, new UseContextAndFragment<Activity, AccountCreation>() {
                @Override
                public void work(Activity activity, AccountCreation fragment) {

                    final Context context = fragment.getActivity();
                    if (file == null) { //

                        ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                                .setCategory("Chat auth failed in account creation")
                                //.setAction("User Id - " + serverId)
                                .setLabel("Phone Number - " + phoneNumber)
                                .setValue(1)
                                .build());

                        toUpload = null;
                        Toast.makeText(context, "Failed to set Profile Photo, try again", Toast.LENGTH_LONG).show();
                    } else if (fragment.profilePhotoSelector != null) {

                        toUpload = file.getAbsolutePath();
                        //TODO replace picasso
                        /*Picasso.with(context)
                                .load(toUpload)
                                .fit()
                                .centerCrop()
                                .centerCrop().into(fragment.profilePhotoSelector);*/
                    }
                }
            });

            if (dialog != null)
                dialog.dismiss();
        }
    }

    //TODO firebase
    /*private static final Firebase.AuthResultHandler authHandler = new Firebase.AuthResultHandler() {

        @Override
        public void onAuthenticationError(FirebaseError error) {

            MiscUtils.useContextFromFragment(reference, new UseContext2<Activity>() {
                @Override
                public void work(Activity activity) {

                    ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                            .setCategory("SEVERE ERROR " + error.getDetails())
                            .setAction("User Id - " + serverId)
                            .setAction("Label - " + phoneNumber)
                            .setValue(1)
                            .build());
                }
            });
            Log.e("Ayush", "Login Failed! " + error.getMessage());
        }

        @Override
        public void onAuthenticated(AuthData authData) {

            final String chatUUID = authData.getUid();
            MiscUtils.useContextFromFragment(reference, context -> {

                Log.i("Ayush", "Login Succeeded! storing " + chatUUID);
                SharedPrefUtils.storeChatUUID(context.getSharedPreferences("Reach", Context.MODE_PRIVATE), chatUUID);
            });

            Log.i("Ayush", "Chat authenticated " + chatUUID);
            final Map<String, Object> userData = MiscUtils.getMap(6);
            userData.put("uid", authData.getAuth().get("uid"));
            userData.put("phoneNumber", authData.getAuth().get("phoneNumber"));
            userData.put("userName", authData.getAuth().get("userName"));
            userData.put("imageId", authData.getAuth().get("imageId"));
            userData.put("lastActivated", 0);
            userData.put("newMessage", true);

            final Optional<Firebase> firebaseOptional = MiscUtils.useFragment(reference, fragment -> {
                return fragment.mListener.getFireBase().orNull();
            });

            if (firebaseOptional.isPresent())
                firebaseOptional.get().child("user").child(chatUUID).setValue(userData);
        }
    };*/
}