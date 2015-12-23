package reach.project.onBoarding;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.common.base.Optional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import reach.backend.entities.userApi.model.InsertContainer;
import reach.backend.entities.userApi.model.ReachUser;
import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.utils.CloudStorageUtils;
import reach.project.utils.MetaDataScanner;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.UploadProgress;
import reach.project.utils.auxiliaryClasses.UseContext2;
import reach.project.utils.auxiliaryClasses.UseContextAndFragment;

public class ScanFragment extends Fragment {

    private static String toUpload = null;
    private static String imageId = "hello_world";
    private static long serverId = 0;
    private static WeakReference<ScanFragment> reference = null;

    public static ScanFragment newInstance(String name, String toUpload, String imageId) {

        final Bundle args;
        ScanFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new ScanFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing ScanFragment object :)");
            args = fragment.getArguments();
        }
        args.putString("name", name);
        args.putString("imageId", imageId);
        args.putString("toUpload", toUpload);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_scan, container, false);

        imageId = getArguments().getString("imageId");
        toUpload = getArguments().getString("toUpload");

        new SaveUserData(rootView.findViewById(R.id.sendButton),
                (TextView) rootView.findViewById(R.id.scanCount),
                (ProgressBar) rootView.findViewById(R.id.scanProgress),
                MiscUtils.getDeviceId(getActivity()).trim().replace(" ", "-"))
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getArguments().getString("name"),
                        SharedPrefUtils.getPhoneNumber(getActivity()
                                .getSharedPreferences("Reach", Context.MODE_PRIVATE)));

        return rootView;
    }

    private static class SaveUserData extends AsyncTask<String, Void, ReachUser> {

        final View next;
        final TextView scanCount;
        final ProgressBar progress;
        final String deviceId;

        private SaveUserData( View next,
                              TextView scanCount, ProgressBar progress,
                              String deviceId) {
            this.next = next;
            this.scanCount = scanCount;
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
                    CloudStorageUtils.uploadImage(new File(toUpload), keyStream, uploadProgress);
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

                //TODO firebase
                /*MiscUtils.useContextAndFragment(reference, new UseContextAndFragment<Activity, AccountCreation>() {
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
                });*/
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

                    if (toUpload != null && TextUtils.isEmpty(user.getImageId()))
                        Toast.makeText(activity, "Profile photo could not be uploaded", Toast.LENGTH_SHORT).show();

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
                //TODO onAccountCreated
                //fragment.mListener.onAccountCreated();
                return null;
            });

            @Override
            public boolean handleMessage(Message message) {

                if (message == null)
                    return false;

                if (message.what == MetaDataScanner.FINISHED) {
                    scanCount.setText(songs+"");
                    progress.setProgress(100);
                    next.setOnClickListener(proceed);
                } else if (message.what == MetaDataScanner.SCANNING_SONGS) {
                    scanCount.setText(message.arg1+"");
                    progress.setProgress(message.arg1*10);
                    songs = message.arg1 + 1;
                }

                return true;
            }
        }));
    }
}
