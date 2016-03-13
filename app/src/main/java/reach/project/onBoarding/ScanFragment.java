package reach.project.onBoarding;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
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
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import okhttp3.CacheControl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import reach.project.R;
import reach.project.apps.App;
import reach.project.apps.AppCursorHelper;
import reach.project.core.ReachActivity;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.music.Song;
import reach.project.music.SongCursorHelper;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.utils.ContentType;
import reach.project.utils.FireOnce;
import reach.project.utils.KeyValuePair;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.ancillaryClasses.UseActivityWithResult;
import reach.project.utils.viewHelpers.HandOverMessage;

public class ScanFragment extends Fragment {

    private static final String USER_NAME = "USER_NAME";
    private static final String OLD_USER_ID = "OLD_USER_ID";
    private static final String COVER_PHOTO_ID = "COVER_PHOTO_ID";
    private static final String PROFILE_PHOTO_ID = "PROFILE_PHOTO_ID";
    private static final String COVER_PHOTO_URI = "COVER_PHOTO_URI";
    private static final String PROFILE_PHOTO_URI = "PROFILE_PHOTO_URI";
    private static final String OLD_USER_STATES = "OLD_USER_STATES";

    private static final ResizeOptions PROFILE_PHOTO_RESIZE = new ResizeOptions(150, 150);
    private static final ResizeOptions COVER_PHOTO_RESIZE = new ResizeOptions(500, 300);

    @Nullable
    private static WeakReference<ScanFragment> reference = null;

    public static ScanFragment newInstance(String name,
                                           long oldUserId,
                                           String oldProfilePicId,
                                           String oldCoverPicId,
                                           Uri newProfilePicUri,
                                           Uri newCoverPicUri,
                                           Serializable contentState) {

        final Bundle args;
        final ScanFragment fragment;

        reference = new WeakReference<>(fragment = new ScanFragment());
        fragment.setArguments(args = new Bundle());

        args.putString(USER_NAME, name);
        args.putLong(OLD_USER_ID, oldUserId);
        args.putString(PROFILE_PHOTO_ID, oldProfilePicId);
        args.putString(COVER_PHOTO_ID, oldCoverPicId);
        args.putParcelable(PROFILE_PHOTO_URI, newProfilePicUri);
        args.putParcelable(COVER_PHOTO_URI, newCoverPicUri);
        args.putSerializable(OLD_USER_STATES, contentState);
        return fragment;
    }

    private final ExecutorService accountUploader = MiscUtils.getRejectionExecutor();

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final Activity activity = getActivity();
        final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        final String phoneNumber = SharedPrefUtils.getPhoneNumber(sharedPreferences);
        final Bundle arguments = getArguments();
        final String userName = arguments.getString(USER_NAME);
        final String coverPicId = arguments.getParcelable(COVER_PHOTO_ID);
        final Uri profilePicUri = arguments.getParcelable(PROFILE_PHOTO_URI);
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_scan, container, false);
        rootView.findViewById(R.id.countContainer).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.countContainer).setVisibility(View.INVISIBLE);
        ((TextView) rootView.findViewById(R.id.userName)).setText(userName);
        final SimpleDraweeView coverPic = (SimpleDraweeView) rootView.findViewById(R.id.coverPic);
        final SimpleDraweeView profilePic = (SimpleDraweeView) rootView.findViewById(R.id.profilePic);

        if (coverPicId != null) {

            final DraweeController draweeController = MiscUtils.getControllerResize(coverPic.getController(), Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + coverPicId), COVER_PHOTO_RESIZE);
            coverPic.setController(draweeController);
        }

        if (profilePicUri != null) {

            final DraweeController draweeController = MiscUtils.getControllerResize(profilePic.getController(), profilePicUri, PROFILE_PHOTO_RESIZE);
            profilePic.setController(draweeController);
        }

        final PackageManager packageManager = activity.getPackageManager();
        final String packageName = activity.getPackageName();
        final int versionCode;
        try {
            versionCode = packageManager.getPackageInfo(packageName, 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e); //let the app crash
        }

        //TODO read this info in bulk to reduce disk I/O on UI thread.....
        final String code = sharedPreferences.getString("code", "");
        final String utm_source = sharedPreferences.getString("utm_source", "");
        final String utm_medium = sharedPreferences.getString("utm_medium", "");
        final String utm_campaign = sharedPreferences.getString("utm_campaign", "");
        final String utm_content = sharedPreferences.getString("utm_content", "");
        final String utm_term = sharedPreferences.getString("utm_term", "");

        final List<KeyValuePair> utm = new ArrayList<>();
        if (!TextUtils.isEmpty(utm_source))
            utm.add(new KeyValuePair.Builder().key("utm_source").value(utm_source).build());
        if (!TextUtils.isEmpty(utm_medium))
            utm.add(new KeyValuePair.Builder().key("utm_medium").value(utm_medium).build());
        if (!TextUtils.isEmpty(utm_campaign))
            utm.add(new KeyValuePair.Builder().key("utm_campaign").value(utm_campaign).build());
        if (!TextUtils.isEmpty(utm_content))
            utm.add(new KeyValuePair.Builder().key("utm_content").value(utm_content).build());
        if (!TextUtils.isEmpty(utm_term))
            utm.add(new KeyValuePair.Builder().key("utm_term").value(utm_term).build());
        if (!TextUtils.isEmpty(code))
            utm.add(new KeyValuePair.Builder().key("code").value(code).build());

        final OnboardingData onboardingData = new OnboardingData.Builder()
                .version(versionCode)
                .deviceId(MiscUtils.getDeviceId(activity).trim().replace(" ", "-"))
                .userName(userName)
                .coverPicUri(coverPicId)
                .phoneNumber(phoneNumber)
                .emailId(SharedPrefUtils.getEmailId(sharedPreferences))
                .promoCode("hello_world")
                .utmPairs(utm).build();

        new SaveUserData()
                .executeOnExecutor(accountUploader,

                        rootView.findViewById(R.id.sendButton), //0
                        rootView.findViewById(R.id.scanCount), //1
                        rootView.findViewById(R.id.totalSongs), //2
                        rootView.findViewById(R.id.totalApps), //3
                        rootView.findViewById(R.id.scanProgress), //4
                        rootView.findViewById(R.id.scan1), //5
                        rootView.findViewById(R.id.scan2), //6
                        onboardingData,//7
                        arguments.getSerializable(OLD_USER_STATES), //8
                        arguments.getLong(OLD_USER_ID, 0), //9
                        profilePicUri); //10

        return rootView;
    }

    private static class SaveUserData extends AsyncTask<Object, Void, Long> {

        private TextView finishOnBoarding;
        private TextView scanCount;
        private TextView musicCount;
        private TextView appCount;
        private ProgressBar scanProgress;
        private LinearLayout switchLayout1, switchLayout2;

        private int totalMusic = 0;
        private int totalApps = 0;
        private int totalExpected = 0;

        //call back for counting songs
        final HandOverMessage<Integer> songProcessCounter = message -> {
            totalMusic = message;
            publishProgress();
        };

        //call back for counting apps
        final HandOverMessage<Integer> appProcessCounter = message -> {
            totalApps = message;
            publishProgress();
        };

        @SuppressWarnings("StaticPseudoFunctionalStyleMethod")
        @Override
        protected Long doInBackground(Object... objects) {

            this.finishOnBoarding = (TextView) objects[0];
            this.scanCount = (TextView) objects[1];
            this.musicCount = (TextView) objects[2];
            this.appCount = (TextView) objects[3];
            this.scanProgress = (ProgressBar) objects[4];
            this.switchLayout1 = (LinearLayout) objects[5];
            this.switchLayout2 = (LinearLayout) objects[6];

            final OnboardingData onboardingData = (OnboardingData) objects[7];
            final EnumMap<ContentType, Map<String, EnumSet<ContentType.State>>> stateTable = (EnumMap<ContentType, Map<String, EnumSet<ContentType.State>>>) objects[8];
            final long olderUserId = (long) objects[9];
            final Uri profilePhotoUri = (Uri) objects[10];

            //get song cursor
            final Cursor musicCursor = MiscUtils.useContextFromFragment(reference, activity -> {
                return activity.getContentResolver().query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        SongCursorHelper.ANDROID_SONG_HELPER.getProjection(), null, null, null);
            }).orNull();

            //get installedApps
            final List<ApplicationInfo> installedApps = MiscUtils.useContextFromFragment(reference, activity -> {
                return MiscUtils.getInstalledApps(activity.getPackageManager());
            }).or(Collections.emptyList());

            //get total file count
            Log.i("Ayush", "Scanning  " + (musicCursor != null ? musicCursor.getCount() : 0) + " songs");
            Log.i("Ayush", "Scanning  " + installedApps.size() + " apps");
            totalExpected = (musicCursor != null ? musicCursor.getCount() : 0) + installedApps.size();
            publishProgress();

            //genres get filled here
            final Set<String> genres = MiscUtils.getSet(100);

            //get the song builders
            final List<Song> deviceSongs = MiscUtils.useContextFromFragment(reference, activity -> {
                return SongCursorHelper.getSongs(
                        musicCursor,
                        stateTable == null ? Collections.emptyMap() : stateTable.get(ContentType.MUSIC),
                        olderUserId,
                        activity.getContentResolver(),
                        genres,
                        songProcessCounter);
            }).or(Collections.emptyList());

            //get the app builders
            final List<App> deviceApps = MiscUtils.useContextFromFragment(reference, activity -> {
                return AppCursorHelper.getApps(
                        installedApps,
                        activity.getPackageManager(),
                        appProcessCounter,
                        stateTable == null ? Collections.emptyMap() : stateTable.get(ContentType.APP));
            }).or(Collections.emptyList());

            //we will post this
            final AccountCreationData accountCreationData = new AccountCreationData.Builder()
                    .onboardingData(onboardingData)
                    .apps(deviceApps)
                    .songs(deviceSongs)
                    .genres(ImmutableList.copyOf(genres)).build();

            final byte[] toPost = MiscUtils.compressProto(accountCreationData);
            Log.i("Ayush",
                    "Found, Songs:" + accountCreationData.songs.size() +
                            " Apps:" + accountCreationData.apps.size() +
                            " kBs:" + toPost.length / 1024);

            final Pair<String, Bitmap> imageHashBitmapPair = getImageHashBitmapPair(profilePhotoUri);

            final RequestBody requestBody = new RequestBody() {
                @Override
                public MediaType contentType() {
                    return MediaType.parse("application/octet-stream");
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {

                    //write count of accountCreationData bytes
                    sink.writeInt(toPost.length);
                    //write accountCreationData
                    sink.write(toPost);
                    if (imageHashBitmapPair != null) {
                        //write image hash
                        sink.writeUtf8(imageHashBitmapPair.first);
                        //write image
                        imageHashBitmapPair.second.compress(Bitmap.CompressFormat.WEBP, 80, sink.outputStream());
                    }
                }
            };

            final Request request = new Request.Builder()
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .url("https://1-dot-client-module-dot-able-door-616.appspot.com/client-module/onBoarding/createAccount")
                    .post(requestBody)
                    .build();

            final long serverId;
            try {
                final Response response = ReachApplication.OK_HTTP_CLIENT.newCall(request).execute();
                if (response.isSuccessful()) {

                    final String serverIdString = response.body().string();
                    Log.i("Ayush", "Got response string " + serverIdString);
                    serverId = Long.parseLong(serverIdString);
                } else {
                    Log.i("Ayush", response.code() + " " + response.message());
                    return 0L; //fail
                }
            } catch (IOException e) {
                e.printStackTrace();
                return 0L; //fail
            }

            final ContentValues[] contentValues = new ContentValues[deviceSongs.size()];
            for (int index = 0; index < deviceSongs.size(); index++)
                contentValues[index] = SongHelper.contentValuesCreator(deviceSongs.get(index), serverId);

            final WeakReference<Context> contextWeakReference =
                    MiscUtils.useContextFromFragment(reference, (UseActivityWithResult<Activity, WeakReference<Context>>) activity -> {

                        //save the songs
                        final int count = activity.getContentResolver().bulkInsert(
                                SongProvider.CONTENT_URI,
                                contentValues);
                        Log.i("Ayush", "Inserted " + count + " songs");

                        //store the user details
                        final SharedPreferences preferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
                        SharedPrefUtils.storeReachUser(
                                preferences,
                                accountCreationData.onboardingData,
                                "",
                                imageHashBitmapPair != null ? imageHashBitmapPair.first : "",
                                serverId);

                        return new WeakReference<>(activity);

                    }).orNull();

            //sync up contacts
            FireOnce.contactSync(
                    contextWeakReference,
                    serverId,
                    onboardingData.phoneNumber);

            //sync up gcmId
            FireOnce.checkGCM(
                    contextWeakReference,
                    serverId);

            Log.i("Ayush", "Id received = " + serverId);
            return serverId;
        }

        @Override
        protected void onPostExecute(final Long userId) {

            super.onPostExecute(userId);
            final long serverId = userId == null ? 0 : userId;
            Log.i("Ayush", "Final Id " + serverId);

            //set serverId here
            MiscUtils.useContextAndFragment(reference, (activity, fragment) -> {

                final Tracker tracker = ((ReachApplication) activity.getApplication()).getTracker();
                tracker.setScreenName(AccountCreation.class.getPackage().getName());
                if (serverId != 0) {

                    tracker.set("&uid", serverId + "");
                    tracker.send(new HitBuilders.ScreenViewBuilder().setCustomDimension(1, serverId + "").build());

                    finishOnBoarding.setText("CLICK TO PROCEED");
                    finishOnBoarding.setTextColor(ContextCompat.getColor(activity, R.color.reach_color));
                    finishOnBoarding.setOnClickListener(PROCEED);
                    finishOnBoarding.setVisibility(View.VISIBLE);
                } else {

                    tracker.send(new HitBuilders.EventBuilder()
                            .setCategory("SEVERE ERROR, account creation failed")
                            .setAction("User Id - " + serverId)
                            .setValue(1)
                            .build());

                    Toast.makeText(activity, "Network error", Toast.LENGTH_SHORT).show();
                    activity.finish();
                }
            });
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

            final int currentTotal = totalApps + totalMusic;

            scanCount.setText(currentTotal + "");
            musicCount.setText(totalMusic + "");
            appCount.setText(totalApps + "");

            if (totalExpected > currentTotal)
                scanProgress.setProgress((currentTotal * 100) / totalExpected);
            else
                scanProgress.setProgress(100); //error case
        }

//        //TODO make static :(
//        private final Messenger messenger = new Messenger(new Handler(new Handler.Callback() {
//
//            @Override
//            public boolean handleMessage(Message message) {
//
//                if (message == null)
//                    return false;
//
//                if (message.what == MetaDataScanner.FINISHED) {
//
//
//                } else if (message.what == MetaDataScanner.SCANNING_MUSIC) {
//
//                    totalMusic = message.arg1;
//                    Log.d("Ayush", totalMusic + "," + totalApps);
//
//                    totalFiles = totalMusic + totalApps;
//                    scanCount.setText(totalFiles + "");
//
//                    if (totalExpected > totalFiles)
//                        scanProgress.setProgress((totalFiles * 100) / totalExpected);
//                    else
//                        scanProgress.setProgress(100); //error case
//
//                } else if (message.what == MetaDataScanner.SCANNING_APPS) {
//
//                    totalApps = message.arg1;
//                    Log.d("Ayush", totalMusic + "," + totalApps);
//
//                    totalFiles = totalMusic + totalApps;
//                    scanCount.setText(totalFiles + "");
//
//                    if (totalExpected > totalFiles)
//                        scanProgress.setProgress((totalFiles * 100) / totalExpected);
//                    else
//                        scanProgress.setProgress(100); //error case
//
//                } else if (message.what == MetaDataScanner.UPLOADING) {
//
//                    Log.d("Ayush", totalMusic + "," + totalApps);
//                    totalFiles = totalMusic + totalApps;
//                    scanCount.setText(totalFiles + "");
//                    musicCount.setText(totalMusic + "");
//                    appCount.setText(totalApps + "");
//
//                    switchLayout1.setVisibility(View.INVISIBLE);
//                    switchLayout2.setVisibility(View.VISIBLE);
//                    scanProgress.setProgress(100);
//
//                } else if (message.what == MetaDataScanner.TOTAL_EXPECTED) {
//
//                    totalExpected = message.arg1;
//                }
//
//                return true;
//            }
//        }));
    }

    private static final View.OnClickListener PROCEED = v -> MiscUtils.useFragment(reference, fragment -> {

        final Activity activity = fragment.getActivity();
        final Intent intent = new Intent(activity, ReachActivity.class);
        intent.setAction(ReachActivity.OPEN_MY_PROFILE_APPS_FIRST);
        activity.startActivity(intent);
        activity.finish();
    });

    private static Pair<String, Bitmap> getImageHashBitmapPair(@Nullable Uri profilePhotoUri) {

        //if its a network uri then obviously not new
        final boolean isProfileUriNew = profilePhotoUri != null && !profilePhotoUri.toString().startsWith("http");
        if (isProfileUriNew) {

            final InputStream optionsStream = MiscUtils.useContextFromFragment(reference, activity -> {
                try {
                    return activity.getContentResolver().openInputStream(profilePhotoUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return null;
                }
            }).orNull();

            final InputStream decodeStream = MiscUtils.useContextFromFragment(reference, activity -> {
                try {
                    return activity.getContentResolver().openInputStream(profilePhotoUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return null;
                }
            }).orNull();

            if (optionsStream == null || decodeStream == null)
                return null;

            //first calculate the original size
            final BitmapFactory.Options options = MiscUtils.getRequiredOptions(optionsStream);
            if (options.outHeight == 0 || options.outWidth == 0)
                return null; //failed
            MiscUtils.closeQuietly(optionsStream);

            //resize and calculate the hash as well
            final HashingInputStream hashingInputStream = new HashingInputStream(
                    Hashing.md5(), decodeStream);
            final Bitmap resizedBitmap = BitmapFactory.decodeStream(hashingInputStream, null, options);
            MiscUtils.closeQuietly(decodeStream, hashingInputStream);
            final String imageHash = hashingInputStream.hash().toString();

            return new Pair<>(imageHash, resizedBitmap);

        } else
            return null;
    }
}