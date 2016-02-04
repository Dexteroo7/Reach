package reach.project.onBoarding;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
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

import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.gcm.GoogleCloudMessaging;
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
import reach.project.utils.CloudStorageUtils;
import reach.project.utils.MetaDataScanner;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.ancillaryClasses.UseContext2;
import reach.project.utils.ancillaryClasses.UseContextAndFragment;

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

    public static ScanFragment newInstance(String name, Uri profilePicUri,
                                           String oldImageId, String oldCoverPicId,
                                           String phoneNumber) {

        final Bundle args;
        ScanFragment fragment;
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

        oldImageId = getArguments().getString(OLD_IMAGE_ID, null);
        oldCoverId = getArguments().getString(OLD_COVER_ID, null);
        profilePicUri = getArguments().getParcelable(IMAGE_FILE_URI);

        final String phoneNumber = getArguments().getString(PHONE_NUMBER);
        final String userName = getArguments().getString(USER_NAME);

        rootView.findViewById(R.id.countContainer).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.countContainer).setVisibility(View.INVISIBLE);

        ((TextView) rootView.findViewById(R.id.userName)).setText(userName);
        SimpleDraweeView coverPic = (SimpleDraweeView) rootView.findViewById(R.id.coverPic);
        SimpleDraweeView profilePic = (SimpleDraweeView) rootView.findViewById(R.id.profilePic);
        coverPic.setController(MiscUtils.getControllerResize(coverPic.getController(),
                Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + oldCoverId), COVER_PHOTO_RESIZE));


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
        } else if (!TextUtils.isEmpty(oldImageId)) {

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
                        userName, phoneNumber);

        return rootView;
    }

    private static class SaveUserData extends AsyncTask<Object, Void, ReachUser> {

        TextView next;
        TextView scanCount;
        TextView musicCount;
        TextView appCount;
        ProgressBar progress;
        String deviceId;
        LinearLayout scan1, scan2;

        @Override
        protected ReachUser doInBackground(Object... objects) {

            final InputStream profilePicOptionsStream, profilePicDecodeStream;
            final String userName, phoneNumber;

            next = (TextView) objects[0];
            scanCount = (TextView) objects[1];
            musicCount = (TextView) objects[2];
            appCount = (TextView) objects[3];
            progress = (ProgressBar) objects[4];
            deviceId = (String) objects[5];
            scan1 = (LinearLayout) objects[6];
            scan2 = (LinearLayout) objects[7];
            profilePicOptionsStream = (InputStream) objects[8];
            profilePicDecodeStream = (InputStream) objects[9];
            userName = (String) objects[10];
            phoneNumber = (String) objects[11];

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
                                .setValue(1)
                                .build());
                    }
                });
            }

            final ReachUser user = new ReachUser();
            user.setDeviceId(deviceId);
            user.setGcmId(gcmId);
            user.setUserName(userName);
            user.setPhoneNumber(phoneNumber);
            user.setImageId(oldImageId);
            user.setCoverPicId(oldCoverId);

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
            MiscUtils.useContextAndFragment(reference, new UseContextAndFragment<Activity, ScanFragment>() {

                @Override
                public void work(Activity activity, ScanFragment fragment) {

                    if (user.getId() == 0) {

                        ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                                .setCategory("SEVERE ERROR, account creation failed")
                                .setAction("User Id - " + serverId)
                                .setValue(1)
                                .build());

                        activity.finish();
                        return;
                    }


                    final Tracker tracker = ((ReachApplication) activity.getApplication()).getTracker();
                    tracker.setScreenName(AccountCreation.class.getPackage().getName());

                    if (user.getId() != 0) {
                        
                        tracker.set("&uid", user.getId() + "");
                        tracker.send(new HitBuilders.ScreenViewBuilder().setCustomDimension(1, user.getId() + "").build());
                    }

                    SharedPrefUtils.storeReachUser(activity.getSharedPreferences("Reach", Context.MODE_PRIVATE), user);
                    final Intent intent = new Intent(activity, MetaDataScanner.class);
                    intent.putExtra("messenger", messenger);
                    intent.putExtra("first", true);
                    activity.startService(intent);
                }
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

                    next.setText("CLICK TO PROCEED");
                    MiscUtils.useContextFromFragment(reference, new UseContext2<Context>() {
                        @Override
                        public void work(Context context) {
                            next.setTextColor(ContextCompat.getColor(context, R.color.reach_color));
                        }
                    });
                    next.setOnClickListener(PROCEED);
                    next.setVisibility(View.VISIBLE);
                } else if (message.what == MetaDataScanner.SCANNING_MUSIC) {

                    totalMusic = message.arg1;
                    Log.d("Ayush", totalMusic + "," + totalApps);

                    totalFiles = totalMusic + totalApps;
                    scanCount.setText(totalFiles + "");

                    if (totalExpected > totalFiles)
                        progress.setProgress((totalFiles * 100) / totalExpected);
                    else
                        progress.setProgress(100); //error case

                } else if (message.what == MetaDataScanner.SCANNING_APPS) {

                    totalApps = message.arg1;
                    Log.d("Ayush", totalMusic + "," + totalApps);

                    totalFiles = totalMusic + totalApps;
                    scanCount.setText(totalFiles + "");

                    if (totalExpected > totalFiles)
                        progress.setProgress((totalFiles * 100) / totalExpected);
                    else
                        progress.setProgress(100); //error case

                } else if (message.what == MetaDataScanner.UPLOADING) {

                    Log.d("Ayush", totalMusic + "," + totalApps);
                    totalFiles = totalMusic + totalApps;
                    scanCount.setText(totalFiles + "");
                    musicCount.setText(totalMusic + "");
                    appCount.setText(totalApps + "");

                    scan1.setVisibility(View.INVISIBLE);
                    scan2.setVisibility(View.VISIBLE);
                    progress.setProgress(100);

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
        intent.setAction(ReachActivity.OPEN_MY_PROFILE_APPS);
        activity.startActivity(intent);
        activity.finish();
    });
}
