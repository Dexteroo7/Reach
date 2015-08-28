package reach.project.onBoarding;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.io.ByteStreams;
import com.localytics.android.Localytics;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import reach.backend.entities.userApi.model.MyString;
import reach.backend.entities.userApi.model.OldUserContainerNew;
import reach.backend.entities.userApi.model.ReachUser;
import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.core.StaticData;
import reach.project.utils.CloudStorageUtils;
import reach.project.utils.MiscUtils;
import reach.project.utils.MusicScanner;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.SuperInterface;
import reach.project.utils.auxiliaryClasses.UploadProgress;
import reach.project.viewHelpers.CircleTransform;

public class AccountCreation extends Fragment {

    private static Uri imageUri;
    private static String imageId = "hello_world";
    private static WeakReference<AccountCreation> reference;

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
    private SuperInterface mListener;
    private TextView uploadText;
    private ImageView profilePhotoSelector;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_account_creation, container, false);
        final EditText userName = (EditText) rootView.findViewById(R.id.firstName);
        final TextView progress = (TextView) rootView.findViewById(R.id.syncStatus);

        //give reference to uploadProgress
        final ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        progressBar.setIndeterminate(false);
        SaveUserData.uploadProgress.dialogWeakReference = new WeakReference<>(progressBar);

        uploadText = (TextView) rootView.findViewById(R.id.uploadText);
        profilePhotoSelector = (ImageView) rootView.findViewById(R.id.displayPic);
        profilePhotoSelector.setOnClickListener(imagePicker);
        userName.requestFocus();

        final FragmentActivity activity = getActivity();
        final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
        final Bundle arguments;
        final String[] oldData;
        if ((arguments = getArguments()) != null &&
                (oldData = arguments.getStringArray("oldData")) != null &&
                oldData.length == 2) {
            /**
             * oldData[0] = name;
             * oldData[1] = imageId;
             */
            if (!TextUtils.isEmpty(oldData[0]))
                userName.setText(oldData[0]);
            if (!TextUtils.isEmpty(oldData[1])) {

                imageId = oldData[1];
                Picasso.with(activity)
                        .load(StaticData.cloudStorageImageBaseUrl + imageId)
                        .resize(350, 350)
                        .centerCrop()
                        .transform(new CircleTransform())
                        .into(profilePhotoSelector);
            }
        }

        rootView.findViewById(R.id.importMusic).setOnClickListener(view -> {

            final String name;
            if (TextUtils.isEmpty(userName.getText()) ||
                    TextUtils.isEmpty(name = userName.getText().toString().trim())) {
                Toast.makeText(activity, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }

            final InputMethodManager inputMethodManager =
                    (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            final String phoneNumber = SharedPrefUtils.getUserNumber(sharedPreferences);
            inputMethodManager.hideSoftInputFromWindow(userName.getWindowToken(), 0);

            if (TextUtils.isEmpty(phoneNumber)) {
                Log.i("Downloader", "Account creation could not find number");
                mListener.startNumberVerification();
                return;
            }

            view.setOnClickListener(null);
            view.setEnabled(false);
            if (isRemoving() || isDetached() || activity.isFinishing())
                return;
            //reset the whole databases
            sharedPreferences.edit().clear().apply();
            Log.i("Ayush", "Cleared everything : AccountCreation underway");
            profilePhotoSelector.setOnClickListener(null);
            uploadText.setVisibility(View.GONE);
            ((TextView) rootView.findViewById(R.id.tourText)).setText(name);
            rootView.findViewById(R.id.bottomPart1).setVisibility(View.INVISIBLE);
            rootView.findViewById(R.id.bottomPart2).setVisibility(View.VISIBLE);
            progress.setText("Starting Profile Creation");

            new SaveUserData(
                    rootView.findViewById(R.id.bottomPart2),
                    rootView.findViewById(R.id.bottomPart3),
                    rootView.findViewById(R.id.nextBtn),
                    (TextView) rootView.findViewById(R.id.telephoneNumber),
                    progress,
                    SharedPrefUtils.getDeviceId(activity).trim().replace(" ", "-")).executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR,
                    name,
                    phoneNumber);

        });
        return rootView;
    }

    private final View.OnClickListener imagePicker = v -> {

        final Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        // intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Photo"),
                IMAGE_PICKER_SELECT);
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
                .into(profilePhotoSelector);
    }

    private static class SaveUserData extends AsyncTask<String, String, ReachUser> {

        final View bottomPart2, bottomPart3, next;
        final TextView phoneNumber, progress;
        final String deviceId;

        private SaveUserData(View bottomPart2,
                             View bottomPart3,
                             View next,
                             TextView phoneNumber,
                             TextView progress,
                             String deviceId) {

            this.bottomPart2 = bottomPart2;
            this.bottomPart3 = bottomPart3;
            this.next = next;
            this.phoneNumber = phoneNumber;
            this.progress = progress;
            this.deviceId = deviceId;
        }

        @Override
        protected ReachUser doInBackground(String... strings) {

            //TODO no fall back !
            uploadImage();

            final String gcmId;
            final GoogleCloudMessaging messagingInstance = MiscUtils.useContextFromFragment(reference, GoogleCloudMessaging::getInstance).orNull();

            if (messagingInstance == null)
                gcmId = null;
            else
                gcmId = MiscUtils.autoRetry(() -> messagingInstance.register("528178870551"), Optional.<Predicate<String>>of(TextUtils::isEmpty)).orNull();

            if (TextUtils.isEmpty(gcmId))
                return null;

            final ReachUser user = new ReachUser();
            user.setDeviceId(deviceId);
            user.setMegaBytesReceived(0L);
            user.setMegaBytesSent(0L);
            user.setStatusSong("hello_world");
            user.setGcmId(gcmId);
            user.setUserName(strings[0]);
            user.setPhoneNumber(strings[1]);
            user.setImageId(imageId);
            //insert User-object and get the userID
            final long id;
            final String toParse;
            final MyString dataAfterWork = MiscUtils.autoRetry(() -> StaticData.userEndpoint.insert(user).execute(), Optional.<Predicate<MyString>>absent()).orNull();
            if (dataAfterWork == null || TextUtils.isEmpty(toParse = dataAfterWork.getString()))
                id = 0;
            else
                id = Long.parseLong(toParse);
            Log.i("Ayush", "Id received = " + id);
            if (id == 0) //failed
                return null;
            //finally set the userID, probably unnecessary
            user.setId(id);
            return user;
        }

        @Override
        protected void onPostExecute(final ReachUser user) {

            super.onPostExecute(user);

            if (user == null) {

                MiscUtils.useContextFromFragment(reference, activity -> {

                    Toast.makeText(activity, "Network failed !", Toast.LENGTH_LONG).show();
                    activity.finish();
                    return null;
                });

                return;
            }

            ReachActivity.serverId = user.getId();
            if (!StaticData.debugMode) {

                AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {

                    final String locID = Localytics.getCustomerId();
                    if (TextUtils.isEmpty(locID)) {

                        Localytics.setCustomerId(user.getPhoneNumber());
                        Localytics.setCustomerFullName(user.getUserName());
                    }
                });
            }

            MiscUtils.useContextFromFragment(reference, activity -> {

                SharedPrefUtils.storeReachUser(activity.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS), user);
                final Intent intent = new Intent(activity, MusicScanner.class);
                intent.putExtra("messenger", messenger);
                intent.putExtra("first", true);
                activity.startService(intent);
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

                MiscUtils.runOnUiThreadFragment(reference, (AccountCreation fragment) -> {

                    //fail
                    Toast.makeText(fragment.getActivity(), "Profile photo could not be uploaded", Toast.LENGTH_SHORT).show();
                    fragment.profilePhotoSelector.setImageBitmap(null);
                    return null;
                });
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


        //cant be made static :(
        private final Messenger messenger = new Messenger(new Handler(new Handler.Callback() {

            long songs = 0, playLists = 0;

            private final View.OnClickListener proceed = v -> MiscUtils.useFragment(reference, fragment -> {
                fragment.mListener.onAccountCreated();
                return null;
            });

            @Override
            public boolean handleMessage(Message message) {

                if (message == null)
                    return false;

                if (message.what == MusicScanner.FINISHED) {

                    bottomPart2.setVisibility(View.INVISIBLE);
                    bottomPart3.setVisibility(View.VISIBLE);
                    phoneNumber.setText(songs + " songs");
                    next.setOnClickListener(proceed);
                } else if (message.what == MusicScanner.SONGS) {
                    progress.setText("Found " + message.arg1 + " songs");
                    songs = message.arg1 + 1;
                } else if (message.what == MusicScanner.PLAY_LISTS) {

                    progress.setText("Found " + message.arg1 + " playLists");
                    playLists = message.arg1 + 1;
                } else if (message.what == MusicScanner.ALBUM_ARTIST)
                    progress.setText("Creating account");

                return true;
            }
        }));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (SuperInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
