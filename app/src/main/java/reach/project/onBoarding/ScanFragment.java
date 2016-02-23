package reach.project.onBoarding;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.appspot.able_door_616.blahApi.BlahApi;
import com.appspot.able_door_616.blahApi.model.JsonMap;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Optional;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;

import reach.backend.entities.userApi.model.InsertContainer;
import reach.backend.entities.userApi.model.ReachUser;
import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.utils.CloudEndPointsUtils;
import reach.project.utils.CloudStorageUtils;
import reach.project.utils.FireOnce;
import reach.project.utils.MetaDataScanner;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

public class ScanFragment extends Fragment {

    private static final String USER_NAME = "USER_NAME";
    private static final String OLD_IMAGE_ID = "OLD_IMAGE_ID";
    private static final String OLD_COVER_ID = "OLD_COVER_ID";
    private static final String IMAGE_FILE_URI = "IMAGE_FILE_URI";
    private static final String PHONE_NUMBER = "PHONE_NUMBER";

    private static final ResizeOptions PROFILE_PHOTO_RESIZE = new ResizeOptions(150, 150);
    private static final ResizeOptions COVER_PHOTO_RESIZE = new ResizeOptions(500, 300);

    @Nullable
    private static Uri profilePicUri = null;
    @Nullable
    private static String oldImageId = null, oldCoverId = null;
    @Nullable
    private static WeakReference<ScanFragment> reference = null;
    private static long serverId = 0;

    private SharedPreferences sharedPreferences;

    public static ScanFragment newInstance(String name, Uri profilePicUri,
                                           String oldImageId, String oldCoverPicId,
                                           String phoneNumber) {

        final Bundle args;
        final ScanFragment fragment;

        reference = new WeakReference<>(fragment = new ScanFragment());
        fragment.setArguments(args = new Bundle());

        args.putString(USER_NAME, name);
        args.putString(OLD_IMAGE_ID, oldImageId);
        args.putString(OLD_COVER_ID, oldCoverPicId);
        args.putString(PHONE_NUMBER, phoneNumber);

        if (profilePicUri != null)
            args.putParcelable(IMAGE_FILE_URI, profilePicUri);
        return fragment;
    }

    private final ExecutorService accountUploader = MiscUtils.getRejectionExecutor();

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_scan, container, false);
        final String phoneNumber = getArguments().getString(PHONE_NUMBER);
        final String userName = getArguments().getString(USER_NAME);
        sharedPreferences = getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE);

        oldImageId = getArguments().getString(OLD_IMAGE_ID, null);
        oldCoverId = getArguments().getString(OLD_COVER_ID, null);
        profilePicUri = getArguments().getParcelable(IMAGE_FILE_URI);

        rootView.findViewById(R.id.countContainer).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.countContainer).setVisibility(View.INVISIBLE);

        ((TextView) rootView.findViewById(R.id.userName)).setText(userName);
        final SimpleDraweeView coverPic = (SimpleDraweeView) rootView.findViewById(R.id.coverPic);
        final SimpleDraweeView profilePic = (SimpleDraweeView) rootView.findViewById(R.id.profilePic);

        if (!TextUtils.isEmpty(oldCoverId) && !oldCoverId.equals("hello_world")) {

            final DraweeController draweeController = MiscUtils.getControllerResize(coverPic.getController(),
                    Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + oldCoverId), COVER_PHOTO_RESIZE);
            coverPic.setController(draweeController);
        }

        InputStream profilePicOptionsStream = null, profilePicDecodeStream = null;
        if (profilePicUri != null) {

            final DraweeController draweeController = MiscUtils.getControllerResize(
                    profilePic.getController(), profilePicUri, PROFILE_PHOTO_RESIZE);
            profilePic.setController(draweeController);

            final ContentResolver contentResolver = getActivity().getContentResolver();
            try {
                profilePicOptionsStream = contentResolver.openInputStream(profilePicUri);
                profilePicDecodeStream = contentResolver.openInputStream(profilePicUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "Could not update profile data", Toast.LENGTH_SHORT).show();
            }
        } else if (!TextUtils.isEmpty(oldImageId) && !oldImageId.equals("hello_world")) {

            final String url = StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + oldImageId;
            final DraweeController draweeController = MiscUtils.getControllerResize(
                    profilePic.getController(),
                    Uri.parse(url), PROFILE_PHOTO_RESIZE);
            profilePic.setController(draweeController);
        }

        new SaveUserData()
                .executeOnExecutor(accountUploader,

                        rootView.findViewById(R.id.sendButton),
                        rootView.findViewById(R.id.scanCount),
                        rootView.findViewById(R.id.totalSongs),
                        rootView.findViewById(R.id.totalApps),
                        rootView.findViewById(R.id.scanProgress),
                        MiscUtils.getDeviceId(getActivity()).trim().replace(" ", "-"),
                        rootView.findViewById(R.id.scan1),
                        rootView.findViewById(R.id.scan2),
                        profilePicDecodeStream,
                        profilePicOptionsStream,
                        userName, phoneNumber, SharedPrefUtils.getEmailId(sharedPreferences));

        return rootView;
    }

    private static class SaveUserData extends AsyncTask<Object, Void, ReachUser> {

        private TextView finishOnBoarding;
        private TextView scanCount;
        private TextView musicCount;
        private TextView appCount;
        private ProgressBar scanProgress;
        private LinearLayout switchLayout1, switchLayout2;

        @Override
        protected ReachUser doInBackground(Object... objects) {

            final InputStream profilePicOptionsStream, profilePicDecodeStream;
            final String userName, phoneNumber;

            finishOnBoarding = (TextView) objects[0];
            scanCount = (TextView) objects[1];
            musicCount = (TextView) objects[2];
            appCount = (TextView) objects[3];
            scanProgress = (ProgressBar) objects[4];
            final String deviceId = (String) objects[5];
            switchLayout1 = (LinearLayout) objects[6];
            switchLayout2 = (LinearLayout) objects[7];
            profilePicOptionsStream = (InputStream) objects[8];
            profilePicDecodeStream = (InputStream) objects[9];
            userName = (String) objects[10];
            phoneNumber = (String) objects[11];
            final String emailId = (String) objects[12];

            //SKIP GCM here
//            final String gcmId;
//            final GoogleCloudMessaging messagingInstance = MiscUtils.useContextFromFragment
//                    (reference, GoogleCloudMessaging::getInstance).orNull();
//
//            if (messagingInstance == null)
//                gcmId = null;
//            else
//                gcmId = MiscUtils.autoRetry(() -> messagingInstance.register("528178870551"), Optional.of(TextUtils::isEmpty)).orNull();
//
//            if (TextUtils.isEmpty(gcmId)) {
//
//                MiscUtils.useContextFromFragment(reference, activity -> {
//                    ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
//                            .setCategory("GCM could not be initialized")
//                            .setAction("User Id - " + serverId)
//                            .setValue(1)
//                            .build());
//                });
//            }

            final ReachUser user = new ReachUser();
            user.setDeviceId(deviceId);
            user.setUserName(userName);
            user.setPhoneNumber(phoneNumber);
            user.setImageId(oldImageId);
            user.setCoverPicId(oldCoverId);
            user.setEmailId(emailId);

            //insert User-object and get the userID
            final InsertContainer dataAfterWork = MiscUtils.autoRetry(() -> StaticData.USER_API.insertNew(user).execute(), Optional.absent()).orNull();

            if (dataAfterWork == null || dataAfterWork.getUserId() == null) {

                user.setId(0L);
                user.setChatToken("");
            } else {

                user.setId(dataAfterWork.getUserId());
                serverId = dataAfterWork.getUserId();

                if (profilePicUri != null) {

                    final String newImageId = CloudStorageUtils.uploadImage(profilePicOptionsStream, profilePicDecodeStream, serverId);
                    if (!TextUtils.isEmpty(newImageId)) {

                        user.setImageId(newImageId);
                        try {
                            StaticData.USER_API.updateUserDetailsNew(user).execute();
                        } catch (IOException e) {
                            e.printStackTrace();
                            user.setImageId(oldImageId); //reset
                        }
                    }
                }

                //TODO firebase
            }

            Log.i("Ayush", "Id received = " + user.getId());
            return user;
        }

        @Override
        protected void onPostExecute(final ReachUser user) {

            super.onPostExecute(user);

            //set serverId here
            MiscUtils.useContextAndFragment(reference, (activity, fragment) -> {

                if (user.getId() == 0) {

                    ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                            .setCategory("SEVERE ERROR, account creation failed")
                            .setAction("User Id - " + serverId)
                            .setValue(1)
                            .build());

                    Toast.makeText(activity, "Network error", Toast.LENGTH_SHORT).show();
                    activity.finish();
                    return;
                }


                final Tracker tracker = ((ReachApplication) activity.getApplication()).getTracker();
                tracker.setScreenName(AccountCreation.class.getPackage().getName());

                final long userId = user.getId();
                if (userId != 0) {

                    MiscUtils.autoRetryAsync(() -> {
                        final HttpTransport transport = new NetHttpTransport();
                        final JsonFactory factory = new JacksonFactory();
                        final GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(activity, StaticData.SCOPE);
                        credential.setSelectedAccountName(SharedPrefUtils.getEmailId(fragment.sharedPreferences));
                        Log.i("Ayush", "Using credential " + credential.getSelectedAccountName());

                        final BlahApi businessApi = CloudEndPointsUtils.updateBuilder(new BlahApi.Builder(transport, factory, credential)
                                .setRootUrl("https://1-dot-business-module-dot-able-door-616.appspot.com/_ah/api/")).build();

                        try {
                            final String code = fragment.sharedPreferences.getString("code", "");
                            final String utm_source = fragment.sharedPreferences.getString("utm_source", "");
                            final String utm_medium = fragment.sharedPreferences.getString("utm_medium", "");
                            final String utm_campaign = fragment.sharedPreferences.getString("utm_campaign", "");
                            final String utm_content = fragment.sharedPreferences.getString("utm_content", "");
                            final String utm_term = fragment.sharedPreferences.getString("utm_term", "");

                            if (!TextUtils.isEmpty(code) && !TextUtils.isEmpty(utm_source)) {
                                final JsonMap jsonMap = new JsonMap();
                                jsonMap.set("utm_source", utm_source);
                                if (!TextUtils.isEmpty(utm_medium))
                                    jsonMap.set("utm_medium", utm_medium);
                                if (!TextUtils.isEmpty(utm_campaign))
                                    jsonMap.set("utm_campaign", utm_campaign);
                                if (!TextUtils.isEmpty(utm_content))
                                    jsonMap.set("utm_content", utm_content);
                                if (!TextUtils.isEmpty(utm_term))
                                    jsonMap.set("utm_term", utm_term);
                                businessApi.addNewUTMAndCode(userId, code, jsonMap).execute();
                            }
                            else if (!TextUtils.isEmpty(code) && TextUtils.isEmpty(utm_source)) {
                                businessApi.addPromoCodeIfNotPresent(userId, code).execute();
                            }
                            else if (TextUtils.isEmpty(code) && !TextUtils.isEmpty(utm_source)) {
                                final JsonMap jsonMap = new JsonMap();
                                jsonMap.set("utm_source", utm_source);
                                if (!TextUtils.isEmpty(utm_medium))
                                    jsonMap.set("utm_medium", utm_medium);
                                if (!TextUtils.isEmpty(utm_campaign))
                                    jsonMap.set("utm_campaign", utm_campaign);
                                if (!TextUtils.isEmpty(utm_content))
                                    jsonMap.set("utm_content", utm_content);
                                if (!TextUtils.isEmpty(utm_term))
                                    jsonMap.set("utm_term", utm_term);
                                businessApi.addNewUTM(userId, jsonMap).execute();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }, Optional.absent());

                    tracker.set("&uid", userId + "");
                    tracker.send(new HitBuilders.ScreenViewBuilder().setCustomDimension(1, userId + "").build());
                }

                SharedPrefUtils.storeReachUser(fragment.sharedPreferences, user);
                final Intent intent = new Intent(activity, MetaDataScanner.class);
                intent.putExtra("messenger", messenger);
                intent.putExtra("first", true);
                activity.startService(intent);

                //sync up contacts
                final WeakReference<Context> reference = new WeakReference<>(activity.getApplicationContext());
                FireOnce.contactSync(
                        reference,
                        userId,
                        user.getPhoneNumber());

                //sync up gcmId
                FireOnce.checkGCM(
                        reference,
                        userId);
            });
        }

        private int totalFiles = 0;
        private int totalMusic = 0;
        private int totalApps = 0;
        private int totalExpected = 0;

        //TODO make static :(
        private final Messenger messenger = new Messenger(new Handler(new Handler.Callback() {

            @Override
            public boolean handleMessage(Message message) {

                if (message == null)
                    return false;

                if (message.what == MetaDataScanner.FINISHED) {

                    finishOnBoarding.setText("CLICK TO PROCEED");
                    MiscUtils.useContextFromFragment(reference, activity -> {
                        finishOnBoarding.setTextColor(ContextCompat.getColor(activity, R.color.reach_color));

                    });
                    finishOnBoarding.setOnClickListener(PROCEED);
                    finishOnBoarding.setVisibility(View.VISIBLE);
                } else if (message.what == MetaDataScanner.SCANNING_MUSIC) {

                    totalMusic = message.arg1;
                    Log.d("Ayush", totalMusic + "," + totalApps);

                    totalFiles = totalMusic + totalApps;
                    scanCount.setText(totalFiles + "");

                    if (totalExpected > totalFiles)
                        scanProgress.setProgress((totalFiles * 100) / totalExpected);
                    else
                        scanProgress.setProgress(100); //error case

                } else if (message.what == MetaDataScanner.SCANNING_APPS) {

                    totalApps = message.arg1;
                    Log.d("Ayush", totalMusic + "," + totalApps);

                    totalFiles = totalMusic + totalApps;
                    scanCount.setText(totalFiles + "");

                    if (totalExpected > totalFiles)
                        scanProgress.setProgress((totalFiles * 100) / totalExpected);
                    else
                        scanProgress.setProgress(100); //error case

                } else if (message.what == MetaDataScanner.UPLOADING) {

                    Log.d("Ayush", totalMusic + "," + totalApps);
                    totalFiles = totalMusic + totalApps;
                    scanCount.setText(totalFiles + "");
                    musicCount.setText(totalMusic + "");
                    appCount.setText(totalApps + "");

                    switchLayout1.setVisibility(View.INVISIBLE);
                    switchLayout2.setVisibility(View.VISIBLE);
                    scanProgress.setProgress(100);

                } else if (message.what == MetaDataScanner.TOTAL_EXPECTED) {

                    totalExpected = message.arg1;
                }

                return true;
            }
        }));
    }

    private static final View.OnClickListener PROCEED = v -> MiscUtils.useFragment(reference, fragment -> {

        final Activity activity = fragment.getActivity();
        final Intent intent = new Intent(activity, ReachActivity.class);
        intent.setAction(ReachActivity.OPEN_MY_PROFILE_APPS_FIRST);
        activity.startActivity(intent);
        activity.finish();
    });
}
