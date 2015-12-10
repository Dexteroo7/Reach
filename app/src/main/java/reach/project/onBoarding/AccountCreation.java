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
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
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

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.Map;

import reach.backend.entities.userApi.model.InsertContainer;
import reach.backend.entities.userApi.model.OldUserContainerNew;
import reach.backend.entities.userApi.model.ReachUser;
import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.usageTracking.PostParams;
import reach.project.usageTracking.UsageTracker;
import reach.project.utils.CloudStorageUtils;
import reach.project.utils.MetaDataScanner;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.SuperInterface;
import reach.project.utils.auxiliaryClasses.UploadProgress;
import reach.project.utils.auxiliaryClasses.UseContext;
import reach.project.utils.auxiliaryClasses.UseContext2;
import reach.project.utils.auxiliaryClasses.UseContextAndFragment;
import reach.project.utils.viewHelpers.CircleTransform;

public class AccountCreation extends Fragment {

    private static File toUpload = null;
    private static String imageId = "hello_world";
    private static String phoneNumber = "";
    private static long serverId = 0;
    private final CircleTransform transform = new CircleTransform();
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
    private SuperInterface mListener = null;
    private ImageView profilePhotoSelector = null;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_account_creation, container, false);
        final EditText userName = (EditText) rootView.findViewById(R.id.firstName);
        final TextView progress = (TextView) rootView.findViewById(R.id.syncStatus);
        final TextView uploadText = (TextView) rootView.findViewById(R.id.uploadText);
        final ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        //give reference to uploadProgress
        progressBar.setIndeterminate(false);

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
                Picasso.with(activity)
                        .load(StaticData.cloudStorageImageBaseUrl + imageId)
                        .transform(transform)
                        .fit()
                        .centerCrop()
                        .into(profilePhotoSelector);
            }
        }

        rootView.findViewById(R.id.importMusic).setOnClickListener(view -> {

            final String name;
            if (TextUtils.isEmpty(name = userName.getText().toString().trim())) {
                Toast.makeText(activity, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }

            phoneNumber = SharedPrefUtils.getUserNumber(sharedPreferences);

            if (TextUtils.isEmpty(phoneNumber)) {
                Log.i("Downloader", "Account creation could not find number");
                mListener.startNumberVerification();
                return;
            }

            //OK
            ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(userName.getWindowToken(), 0);
            view.setOnClickListener(null);
            view.setEnabled(false);
            sharedPreferences.edit().clear().apply();
            Log.i("Ayush", "Cleared everything : AccountCreation underway");
            profilePhotoSelector.setOnClickListener(null);
            uploadText.setVisibility(View.GONE);
            ((TextView) rootView.findViewById(R.id.tourText)).setText(name);
            rootView.findViewById(R.id.bottomPart1).setVisibility(View.INVISIBLE);
            rootView.findViewById(R.id.bottomPart2).setVisibility(View.VISIBLE);
            progress.setText("Starting Profile Creation");

            final Map<PostParams, String> simpleParams = MiscUtils.getMap(2);
            simpleParams.put(PostParams.USER_NUMBER, phoneNumber);
            simpleParams.put(PostParams.USER_NAME, name);
            try {
                UsageTracker.trackLogEvent(simpleParams, UsageTracker.NAME_ENTERED);
            } catch (JSONException ignored) {}

            new SaveUserData(
                    rootView.findViewById(R.id.bottomPart2),
                    rootView.findViewById(R.id.bottomPart3),
                    rootView.findViewById(R.id.nextBtn),
                    (TextView) rootView.findViewById(R.id.telephoneNumber),
                    progress,
                    MiscUtils.getDeviceId(activity).trim().replace(" ", "-"))
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, name, phoneNumber);
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

    private static class SaveUserData extends AsyncTask<String, Void, ReachUser> {

        final View bottomPart2, bottomPart3, next;
        final TextView phoneNumber, progress;
        final String deviceId;

        private SaveUserData(View bottomPart2, View bottomPart3, View next,
                             TextView phoneNumber, TextView progress,
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

            final ReachUser user = new ReachUser();

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

                if (keyStream != null) {
                    //try upload
                    CloudStorageUtils.uploadImage(toUpload, keyStream, uploadProgress);
                }
            }

            final String gcmId;
            final GoogleCloudMessaging messagingInstance = MiscUtils.useContextFromFragment
                    (reference, GoogleCloudMessaging::getInstance).orNull();

            if (messagingInstance == null)
                gcmId = null;
            else
                gcmId = MiscUtils.autoRetry(() -> messagingInstance.register("528178870551"), Optional.of(TextUtils::isEmpty)).orNull();

            if (TextUtils.isEmpty(gcmId)) {

                MiscUtils.useContextFromFragment(reference, new UseContext2<Activity>() {
                    @Override
                    public void work(Activity activity) {

                        ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                                .setCategory("GCM could not be initialized")
                                .setAction("User Id - " + serverId)
                                .setLabel("Phone Number - " + phoneNumber)
                                .setValue(1)
                                .build());
                    }
                });
            }

            user.setDeviceId(deviceId);
            user.setGcmId(gcmId);
            user.setUserName(strings[0]);
            user.setPhoneNumber(strings[1]);
            user.setImageId(imageId);

            //insert User-object and get the userID
            final InsertContainer dataAfterWork = MiscUtils.autoRetry(() -> StaticData.userEndpoint.insertNew(user).execute(), Optional.absent()).orNull();

            if (dataAfterWork == null || dataAfterWork.getUserId() == null) {

                user.setId(0L);
                user.setChatToken("");
            } else {

                user.setId(dataAfterWork.getUserId());
                serverId = dataAfterWork.getUserId();

                MiscUtils.useContextAndFragment(reference, new UseContextAndFragment<Activity, AccountCreation>() {
                    @Override
                    public void work(Activity activity, AccountCreation fragment) {

                        if (!TextUtils.isEmpty(dataAfterWork.getFireBaseToken())) {

                            user.setChatToken(dataAfterWork.getFireBaseToken());
                            final Optional<Firebase> firebaseOptional = fragment.mListener.getFireBase();
                            if (firebaseOptional.isPresent())
                                firebaseOptional.get().authWithCustomToken(dataAfterWork.getFireBaseToken(), authHandler);
                            else {

                                ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                                        .setCategory("Chat auth failed in account creation")
                                        .setAction("User Id - " + user.getId())
                                        .setValue(1)
                                        .build());
                            }
                        } else {

                            ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                                    .setCategory("Chat auth failed in account creation severe")
                                    .setAction("User Id - " + user.getId())
                                    .setValue(1)
                                    .build());
                        }
                    }
                });
            }
            Log.i("Ayush", "Id received = " + user.getId());
            return user;
        }

        @Override
        protected void onPostExecute(final ReachUser user) {

            super.onPostExecute(user);

            //set serverId here
            MiscUtils.useContextAndFragment(reference, new UseContextAndFragment<Activity, AccountCreation>() {

                @Override
                public void work(Activity activity, AccountCreation fragment) {

                    if (toUpload != null && TextUtils.isEmpty(user.getImageId())) {

                        Toast.makeText(activity, "Profile photo could not be uploaded", Toast.LENGTH_SHORT).show();
                        if (fragment.profilePhotoSelector != null)
                            fragment.profilePhotoSelector.setImageBitmap(null);
                    }

                    if (user.getId() == 0) {

                        ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                                .setCategory("SEVERE ERROR, account creation failed")
                                .setAction("User Id - " + serverId)
                                .setLabel("Phone Number - " + phoneNumber)
                                .setValue(1)
                                .build());

                        activity.finish();
                        return;
                    }


                    final MixpanelAPI mixpanel = MixpanelAPI.getInstance(activity, StaticData.mixPanelId);
                    final MixpanelAPI.People people = mixpanel.getPeople();
                    final Tracker tracker = ((ReachApplication) activity.getApplication()).getTracker();
                    tracker.setScreenName(AccountCreation.class.getPackage().getName());

                    if (user.getId() != 0) {

                        tracker.set("&uid", user.getId() + "");
                        tracker.send(new HitBuilders.ScreenViewBuilder().setCustomDimension(1, user.getId() + "").build());
                        mixpanel.identify(user.getId() + "");
                        JSONObject props = new JSONObject();
                        try {
                            props.put("UserID", user.getId() + "");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        mixpanel.registerSuperPropertiesOnce(props);
                        people.identify(user.getId() + "");
                        people.set("UserID", user.getId() + "");
                    }
                    people.set("$phone", user.getPhoneNumber() + "");
                    people.set("$name", user.getUserName() + "");

                    SharedPrefUtils.storeReachUser(activity.getSharedPreferences("Reach", Context.MODE_PRIVATE), user);
                    final Intent intent = new Intent(activity, MetaDataScanner.class);
                    intent.putExtra("messenger", messenger);
                    intent.putExtra("first", true);
                    activity.startService(intent);
                }
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

        //TODO cant be made static :(
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

                if (message.what == MetaDataScanner.FINISHED) {

                    bottomPart2.setVisibility(View.INVISIBLE);
                    bottomPart3.setVisibility(View.VISIBLE);
                    phoneNumber.setText(songs + " songs");
                    next.setOnClickListener(proceed);
                } else if (message.what == MetaDataScanner.SCANNING_SONGS) {
                    progress.setText("Found " + message.arg1 + " songs");
                    songs = message.arg1 + 1;
                } else if (message.what == MetaDataScanner.UPLOADING)
                    progress.setText("Creating account");

                return true;
            }
        }));
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
                                .setAction("User Id - " + serverId)
                                .setLabel("Phone Number - " + phoneNumber)
                                .setValue(1)
                                .build());

                        toUpload = null;
                        Toast.makeText(context, "Failed to set Profile Photo, try again", Toast.LENGTH_LONG).show();
                    } else if (fragment.profilePhotoSelector != null) {

                        toUpload = file;
                        Picasso.with(context)
                                .load(toUpload)
                                .fit()
                                .centerCrop()
                                .transform(fragment.transform)
                                .centerCrop().into(fragment.profilePhotoSelector);
                    }
                }
            });

            if (dialog != null)
                dialog.dismiss();
        }
    }

    private static final Firebase.AuthResultHandler authHandler = new Firebase.AuthResultHandler() {

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
    };
}