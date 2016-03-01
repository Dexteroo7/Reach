package reach.project.onBoarding;

import android.app.Activity;
import android.content.ContentResolver;
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
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPOutputStream;

import okio.BufferedSink;
import reach.project.R;
import reach.project.apps.App;
import reach.project.apps.AppCursorHelper;
import reach.project.apps.AppList;
import reach.project.core.ReachActivity;
import reach.project.core.ReachApplication;
import reach.project.music.MusicList;
import reach.project.music.Song;
import reach.project.music.SongCursorHelper;
import reach.project.utils.FireOnce;
import reach.project.utils.KeyValuePair;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.ancillaryClasses.UseActivityWithResult;

public class ScanFragment extends Fragment {

    private static final String USER_NAME = "USER_NAME";
    private static final String PROFILE_PHOTO_URI = "PROFILE_PHOTO_URI";
    private static final String COVER_PHOTO_URI = "COVER_PHOTO_URI";
    private static final String PHONE_NUMBER = "PHONE_NUMBER";

    private static final ResizeOptions PROFILE_PHOTO_RESIZE = new ResizeOptions(150, 150);
    private static final ResizeOptions COVER_PHOTO_RESIZE = new ResizeOptions(500, 300);

    @Nullable
    private static WeakReference<ScanFragment> reference = null;
    private static long serverId = 0;
    private static Uri profilePicUri = null;

    private SharedPreferences sharedPreferences;

    public static ScanFragment newInstance(String name, Uri profilePicUri, Uri coverPicUri, String phoneNumber) {

        final Bundle args;
        final ScanFragment fragment;

        reference = new WeakReference<>(fragment = new ScanFragment());
        fragment.setArguments(args = new Bundle());

        args.putString(USER_NAME, name);
        args.putString(PHONE_NUMBER, phoneNumber);
        args.putParcelable(PROFILE_PHOTO_URI, profilePicUri);
        args.putParcelable(COVER_PHOTO_URI, coverPicUri);
        return fragment;
    }

    private final ExecutorService accountUploader = MiscUtils.getRejectionExecutor();

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final Activity activity = getActivity();
        final View rootView = inflater.inflate(R.layout.fragment_scan, container, false);
        final String phoneNumber = getArguments().getString(PHONE_NUMBER);
        final String userName = getArguments().getString(USER_NAME);
        final Uri coverPicUri = getArguments().getParcelable(COVER_PHOTO_URI);
        profilePicUri = getArguments().getParcelable(PROFILE_PHOTO_URI);
        sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);

        rootView.findViewById(R.id.countContainer).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.countContainer).setVisibility(View.INVISIBLE);
        ((TextView) rootView.findViewById(R.id.userName)).setText(userName);
        final SimpleDraweeView coverPic = (SimpleDraweeView) rootView.findViewById(R.id.coverPic);
        final SimpleDraweeView profilePic = (SimpleDraweeView) rootView.findViewById(R.id.profilePic);

        if (coverPicUri != null) {

            final DraweeController draweeController = MiscUtils.getControllerResize(coverPic.getController(), coverPicUri, COVER_PHOTO_RESIZE);
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
            e.printStackTrace();
            //TODO fail
            return rootView;
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
                .version(86) //TODO
                .deviceId(MiscUtils.getDeviceId(activity).trim().replace(" ", "-"))
                .userName(userName)
                .phoneNumber(phoneNumber)
                .profilePicUri(profilePicUri != null ? profilePicUri.toString() : null)
                .coverPicUri(coverPicUri != null ? coverPicUri.toString() : null)
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
                        onboardingData); //7

        return rootView;
    }

    private static class SaveUserData extends AsyncTask<Object, Void, Long> {

        private TextView finishOnBoarding;
        private TextView scanCount;
        private TextView musicCount;
        private TextView appCount;
        private ProgressBar scanProgress;
        private LinearLayout switchLayout1, switchLayout2;

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
            //generate and post new data

            final RequestBody requestBody = new RequestBody() {

                @Override
                public MediaType contentType() {
                    return MediaType.parse("application/gzip");
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {

                    final ExecutorService executorService = Executors.newFixedThreadPool(2);
                    final Future<List<App.Builder>> appFuture = executorService.submit(GET_APP_BUILDER);
                    final Future<Pair<String, Bitmap>> imageFuture = executorService.submit(GET_IMAGE_AND_HASH);

                    final DataOutputStream outputStream = new DataOutputStream(new GZIPOutputStream(sink.outputStream()));
                    Log.i("Ayush", "Writing onboardingData" + onboardingData.toString());
                    final byte[] onBoardingBytes = onboardingData.toByteArray();
                    outputStream.writeInt(onBoardingBytes.length);
                    outputStream.write(onBoardingBytes);

                    try {

                        //write songs
                        final MusicList musicList = new MusicList.Builder()
                                .clientId(0L)
                                .genres(Collections.emptyList())
                                .song(Lists.transform(getSongs(), SongCursorHelper.SONG_BUILDER)).build();
                        final byte[] musicBytes = musicList.toByteArray();
                        outputStream.writeInt(musicBytes.length);
                        outputStream.write(musicBytes);
                        Log.i("Ayush", "Music written " + musicList.song.size());

                        //write apps
                        final AppList appList = new AppList.Builder()
                                .clientId(0L)
                                .app(Lists.transform(appFuture.get(), AppCursorHelper.BUILDER_APP_FUNCTION)).build();
                        final byte[] appBytes = appList.toByteArray();
                        outputStream.writeInt(appBytes.length);
                        outputStream.write(appBytes);
                        Log.i("Ayush", "Apps written " + appList.app.size());

                        //write photo
                        final Pair<String, Bitmap> imageAndHash = imageFuture.get();
                        if (imageAndHash != null && !TextUtils.isEmpty(imageAndHash.first) && imageAndHash.second != null) {

                            outputStream.writeUTF(imageAndHash.first);
                            imageAndHash.second.compress(Bitmap.CompressFormat.WEBP, 80, outputStream);
                            Log.i("Ayush", "Image written");
                        }

                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    } finally {
                        executorService.shutdownNow();
                    }
                }
            };

            final Request request = new Request.Builder()
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .header("Content-Encoding", "gzip")
                    .url("https://1-dot-client-module-dot-able-door-616.appspot.com/client-module/onBoarding/createAccount") //TODO
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

            //TODO insert songs, apps and imageId into DB

            MiscUtils.useContextAndFragment(reference, (activity, fragment) -> {

                SharedPrefUtils.storeReachUser(fragment.sharedPreferences, onboardingData, "", serverId);
                final Tracker tracker = ((ReachApplication) activity.getApplication()).getTracker();
                tracker.setScreenName(AccountCreation.class.getPackage().getName());
                if (serverId != 0) {

                    tracker.set("&uid", serverId + "");
                    tracker.send(new HitBuilders.ScreenViewBuilder().setCustomDimension(1, serverId + "").build());
                }
                //sync up contacts
                final WeakReference<Context> contextWeakReference = new WeakReference<>(activity.getApplicationContext());
                FireOnce.contactSync(
                        contextWeakReference,
                        serverId,
                        onboardingData.phoneNumber);

                //sync up gcmId
                FireOnce.checkGCM(
                        contextWeakReference,
                        serverId);
            });

            Log.i("Ayush", "Id received = " + serverId);
            return serverId;
        }

        @Override
        protected void onPostExecute(final Long userId) {

            Log.i("Ayush", "Final Id " + userId);
            super.onPostExecute(userId);

            //set serverId here
            MiscUtils.useContextAndFragment(reference, (activity, fragment) -> {

                if (userId == null || userId == 0) {

                    ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
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
        }

//        private int totalFiles = 0;
//        private int totalMusic = 0;
//        private int totalApps = 0;
//        private int totalExpected = 0;
//
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
//                    finishOnBoarding.setText("CLICK TO PROCEED");
//                    MiscUtils.useContextFromFragment(reference, activity -> {
//                        finishOnBoarding.setTextColor(ContextCompat.getColor(activity, R.color.reach_color));
//
//                    });
//                    finishOnBoarding.setOnClickListener(PROCEED);
//                    finishOnBoarding.setVisibility(View.VISIBLE);
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

    private static List<Song.Builder> getSongs() {

        final String[] projection = SongCursorHelper.ANDROID_SONG_HELPER.getProjection();
        final Function<Cursor, Song.Builder> parser = (Function<Cursor, Song.Builder>) SongCursorHelper.ANDROID_SONG_HELPER.getParser();

        final Cursor musicCursor = MiscUtils.useContextFromFragment(reference, context -> {
            return context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection, null, null, null);
        }).orNull();

        if (musicCursor == null) {

            Log.i("Ayush", "Meta-data sync failed");
            return Collections.emptyList();
        } else {

            final List<Song.Builder> toReturn = new ArrayList<>(musicCursor.getCount());
            while (musicCursor.moveToNext()) {

                Song.Builder builder = parser.apply(musicCursor);
                builder = SongCursorHelper.DEFAULT_VISIBILITY.apply(builder);
                if (builder != null)
                    toReturn.add(builder);
            }
            return toReturn;
        }
    }

    private static final Callable<List<App.Builder>> GET_APP_BUILDER = () -> MiscUtils.useContextFromFragment(reference, (UseActivityWithResult<Activity, List<App.Builder>>) activity -> {

        final PackageManager packageManager = activity.getPackageManager();
        final List<ApplicationInfo> applicationInfos = MiscUtils.getInstalledApps(packageManager);
        Log.i("Ayush", "Reading apps " + applicationInfos.size());
        final Iterable<App.Builder> builders = FluentIterable.from(applicationInfos)
                .transform(AppCursorHelper.getParser(packageManager, Collections.emptySet()))
                .filter(Predicates.notNull());
        final List<App.Builder> toReturn = ImmutableList.copyOf(builders);
        Log.i("Ayush", "Total apps " + toReturn.size());
        return toReturn;
    }).or(Collections.emptyList());

    private static final Callable<Pair<String, Bitmap>> GET_IMAGE_AND_HASH = () -> {

        final InputStream profilePicOptionsStream = MiscUtils.useContextFromFragment(reference, activity -> {

            final ContentResolver contentResolver = activity.getContentResolver();
            try {
                return contentResolver.openInputStream(profilePicUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }).orNull();

        final InputStream profilePicDecodeStream = MiscUtils.useContextFromFragment(reference, activity -> {

            final ContentResolver contentResolver = activity.getContentResolver();
            try {
                return contentResolver.openInputStream(profilePicUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }).orNull();

        if (profilePicDecodeStream == null || profilePicOptionsStream == null)
            return null;

        //first calculate the original size
        final BitmapFactory.Options options = MiscUtils.getRequiredOptions(profilePicOptionsStream);
        if (options.outHeight == 0 || options.outWidth == 0)
            return null; //failed
        MiscUtils.closeQuietly(profilePicOptionsStream);

        //resize and calculate the hash as well
        final HashingInputStream hashingInputStream = new HashingInputStream(
                Hashing.md5(), profilePicDecodeStream);
        final Bitmap resizedBitmap = BitmapFactory.decodeStream(hashingInputStream, null, options);
        MiscUtils.closeQuietly(profilePicDecodeStream, hashingInputStream);
        final String imageHash = hashingInputStream.hash().toString();
        return new Pair<>(imageHash, resizedBitmap); //failed
    };
}