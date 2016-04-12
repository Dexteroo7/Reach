package reach.project.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.AbstractDraweeController;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.android.gms.analytics.HitBuilders;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.hash.HashFunction;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.squareup.wire.Message;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnull;

import reach.backend.entities.messaging.model.MyBoolean;
import reach.project.apps.App;
import reach.project.core.ReachActivity;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.music.ReachDatabase;
import reach.project.music.Song;
import reach.project.music.SongCursorHelper;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.player.PlayerActivity;
import reach.project.reachProcess.auxiliaryClasses.Connection;
import reach.project.reachProcess.reachService.ProcessManager;
import reach.project.usageTracking.AppMetadata;
import reach.project.usageTracking.PostParams;
import reach.project.usageTracking.SongMetadata;
import reach.project.usageTracking.UsageTracker;
import reach.project.utils.ancillaryClasses.DoWork;
import reach.project.utils.ancillaryClasses.UseActivity;
import reach.project.utils.ancillaryClasses.UseActivityWithResult;
import reach.project.utils.ancillaryClasses.UseContext;
import reach.project.utils.ancillaryClasses.UseContextWithResult;
import reach.project.utils.ancillaryClasses.UseFragment;
import reach.project.utils.ancillaryClasses.UseFragmentAndActivity;
import reach.project.utils.ancillaryClasses.UseFragmentWithResult;
import reach.project.utils.ancillaryClasses.UseReference;
import reach.project.utils.ancillaryClasses.UseReferenceWithResult;
import reach.project.utils.viewHelpers.RetryHook;

/**
 * Created by dexter on 1/10/14.
 */

public enum MiscUtils {
    ;

    private static final String TAG = MiscUtils.class.getSimpleName();

    @NonNull
    public static String combinationFormatter(final long millis) {

        final long seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
                - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        final long hours = TimeUnit.MILLISECONDS.toHours(millis);

        final String toSend = (hours == 0 ? "00" : hours < 10 ? "0" + hours : hours) + ":" +
                (minutes == 0 ? "00" : minutes < 10 ? "0" + minutes : minutes) + ":" +
                (seconds == 0 ? "00" : seconds < 10 ? "0" + seconds : seconds);

        return toSend.startsWith("00:") ? toSend.substring(3) : toSend;
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public static String dateFormatter(final long milliSeconds) {

        return new SimpleDateFormat("dd-MM-yyyy HH-mm-ss", Locale.getDefault()).format(
                new Date(milliSeconds));
    }

    public static void closeQuietly(Collection... collections) {
        if (collections == null || collections.length == 0)
            return;
        for (Collection collection : collections)
            if (collection != null)
                collection.clear();
    }

    public static void closeQuietly(Closeable... closeables) {
        for (Closeable closeable : closeables)
            if (closeable != null)
                try {
                    closeable.close();
                } catch (IOException ignored) {
                }
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null)
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
    }

    public static List<App> getApplications(PackageManager packageManager, SharedPreferences preferences) {

        final List<ApplicationInfo> applicationInfoList = getInstalledApps(packageManager);
        if (applicationInfoList == null || applicationInfoList.isEmpty())
            return Collections.emptyList();

        final Map<String, Boolean> packageVisibility = SharedPrefUtils.getPackageVisibilities(preferences);
        final List<App> applicationsFound = new ArrayList<>();

        for (ApplicationInfo applicationInfo : applicationInfoList) {

            if (applicationInfo.packageName.equals("reach.project"))
                continue;

            final App.Builder appBuilder = new App.Builder();

            appBuilder.launchIntentFound(packageManager.getLaunchIntentForPackage(applicationInfo.packageName) != null);
            appBuilder.applicationName(applicationInfo.loadLabel(packageManager) + "");
            appBuilder.description(applicationInfo.loadDescription(packageManager) + "");
            appBuilder.packageName(applicationInfo.packageName);
            appBuilder.processName(applicationInfo.processName);

            try {
                appBuilder.installDate(
                        packageManager.getPackageInfo(applicationInfo.packageName, 0).firstInstallTime);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            final Boolean visibility = packageVisibility.get(applicationInfo.packageName);
            if (visibility == null)
                appBuilder.visible(true);
            else
                appBuilder.visible(visibility);
            applicationsFound.add(appBuilder.build());
        }

        return applicationsFound;
    }

    /**
     * Scan the phoneBook for numbers and return a collection
     *
     * @param resolver to query
     * @return collection of phoneNumbers (won't return null :) )
     */
    public static Set<String> scanPhoneBook(ContentResolver resolver) {

        final StringBuilder builder = new StringBuilder();
        final HashSet<String> container = new HashSet<>();
        final Cursor phoneNumbers = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER}, //fetch only numbers
                null, null, null); //fetch all rows
        if (phoneNumbers == null)
            return container;

        String phoneNumber;

        while (phoneNumbers.moveToNext()) {

            //reset
            builder.setLength(0);

            //get the number
            phoneNumber = phoneNumbers.getString(0);
            if (TextUtils.isEmpty(phoneNumber))
                continue;

            //if character is a number append !
            for (int i = 0; i < phoneNumber.length(); i++) {
                final char test = phoneNumber.charAt(i);
                if (Character.isDigit(test))
                    builder.append(test);
            }

            //ignore if at-least 10 digits not found !
            if (builder.length() < 10)
                continue;

            //take the last 10 digits
            container.add(builder.substring(builder.length() - 10));
        }

        phoneNumbers.close();
        return container;
    }

    //TODO test
    public static CharSequence generateInitials(String name) {

        if(name == null){
            return "A";
        }
        name = name.trim();
        if (TextUtils.isEmpty(name))
            return "A";

        final String[] splitter = name.split(" ");
        switch (splitter.length) {

            case 0:
                return "A";
            case 1:
                return (splitter[0].charAt(0) + "").toUpperCase();
            default:
                try {
                    if (splitter[0].length() == 0 || splitter[1].length() == 0)
                        return "A";
                    return (splitter[0].charAt(0) + "" + splitter[1].charAt(0)).toUpperCase();
                }
                catch (StringIndexOutOfBoundsException e){
                    //TODO: Activate when activating Crittercism
                    //Crittercism.leaveBreadcrumb("Invite Page StringIndexOutOfBoundException, String name = " + name);
                    return "A";
                }
        }
    }

    public static AbstractDraweeController getControllerResize(DraweeController oldController,
                                                               Uri uri, int width, int height) {

        return Fresco.newDraweeControllerBuilder()
                .setOldController(oldController)
                .setImageRequest(ImageRequestBuilder.newBuilderWithSource(uri)
                        .setResizeOptions(new ResizeOptions(width, height))
                        .build())
                .build();
    }

    public static AbstractDraweeController getControllerResize(DraweeController oldController,
                                                               Uri uri, ResizeOptions resizeOptions) {

        return Fresco.newDraweeControllerBuilder()
                .setOldController(oldController)
                .setImageRequest(ImageRequestBuilder.newBuilderWithSource(uri)
                        .setResizeOptions(resizeOptions)
                        .build())
                .build();
    }

    //TODO use this....
    public static void setUriToView(@Nonnull SimpleDraweeView simpleDraweeView,
                                    @Nonnull Uri uri,
                                    @Nullable ResizeOptions resizeOptions) {

        final DraweeController draweeController = Fresco.newDraweeControllerBuilder()
                .setOldController(simpleDraweeView.getController())
                .setImageRequest(ImageRequestBuilder.newBuilderWithSource(uri)
                        .setResizeOptions(resizeOptions)
                        .build())
                .build();

        simpleDraweeView.setController(draweeController);
    }

    public static <T, E> Map<T, E> getMap(int capacity) {

        if (capacity < 1000) //use lighter collection
            if (Build.VERSION.SDK_INT >= 19)
                return new android.util.ArrayMap<>(capacity);
            else
                return new ArrayMap<>(capacity); //else use the support one
        else
            return new HashMap<>(capacity);
    }

    public static <T> Set<T> getSet(int capacity) {

        if (capacity < 1000 && Build.VERSION.SDK_INT >= 23) //use lighter collection
            return new ArraySet<>(capacity);
        else
            return new HashSet<>(capacity);
    }

    public static boolean containsIgnoreCase(CharSequence str, CharSequence searchStr) {

        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(searchStr))
            return false;
        final int len = searchStr.length();
        final int max = str.length() - len;
        for (int i = 0; i <= max; i++)
            if (regionMatches(str, true, i, searchStr, 0, len))
                return true;
        return false;
    }

    private static boolean regionMatches(CharSequence cs, boolean ignoreCase, int thisStart,
                                         CharSequence substring, int start, int length) {
        if (cs instanceof String && substring instanceof String) {
            return ((String) cs).regionMatches(ignoreCase, thisStart, (String) substring, start, length);
        } else {
            return cs.toString().regionMatches(ignoreCase, thisStart, substring.toString(), start, length);
        }
    }

    public static void setEmptyTextForListView(ListView listView, String emptyText) {

        if (listView.getContext() == null)
            return;
        final TextView emptyTextView = new TextView(listView.getContext());
        emptyTextView.setText(emptyText);

        final ViewParent parent = listView.getParent();
        if (parent == null ||
                parent.getClass() == null ||
                TextUtils.isEmpty(parent.getClass().getName()))
            return;
        final String parentType = parent.getClass().getName();

        if (parentType.equals("android.widget.FrameLayout")) {
            emptyTextView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            listView.setEmptyView(emptyTextView);
            final FrameLayout frameLayout = (FrameLayout) parent;
            frameLayout.removeViewAt(1);
            frameLayout.addView(emptyTextView);
        } else if (parentType.equals("android.widget.RelativeLayout")) {

            final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            emptyTextView.setLayoutParams(layoutParams);
            listView.setEmptyView(emptyTextView);
            final RelativeLayout relativeLayout = (RelativeLayout) parent;
            relativeLayout.removeViewAt(1);
            relativeLayout.addView(emptyTextView);
        }
    }

    public static boolean playSong(Song song, Context context) {

        if (song.size == 0 || song.duration == 0 || TextUtils.isEmpty(song.path)) {
            Toast.makeText(context, "Bad song", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (song.getProcessed() == 0) {
            Toast.makeText(context, "Streaming will start in a few seconds", Toast.LENGTH_SHORT).show();
            return false;
        }

        //store the last played song
        SharedPrefUtils.storeLastPlayed(context, song);

        //submit the music request
        ProcessManager.submitMusicRequest(context,
                Optional.of(song),
                ProcessManager.ACTION_NEW_SONG);

        //open player activity
        PlayerActivity.openActivity(context);
        ////////////////////////////////////////
        return true;
    }

//    public static void setEmptyTextforGridView(GridView gridView, String emptyText) {
//
//        if (gridView.getContext() == null)
//            return;
//        final TextView emptyTextView = new TextView(gridView.getContext());
//        emptyTextView.setText(emptyText);
//
//        final ViewParent parent = gridView.getParent();
//        if (parent == null ||
//                parent.getClass() == null ||
//                TextUtils.isEmpty(parent.getClass().getValue()))
//            return;
//        final String parentType = parent.getClass().getValue();
//
//        if (parentType.equals("android.widget.FrameLayout")) {
//            emptyTextView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
//            gridView.setEmptyView(emptyTextView);
//            final FrameLayout frameLayout = (FrameLayout) parent;
//            frameLayout.removeViewAt(1);
//            frameLayout.addView(emptyTextView);
//        } else if (parentType.equals("android.widget.RelativeLayout")) {
//
//            final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
//            emptyTextView.setLayoutParams(layoutParams);
//            gridView.setEmptyView(emptyTextView);
//            final RelativeLayout relativeLayout = (RelativeLayout) parent;
//            relativeLayout.removeViewAt(1);
//            relativeLayout.addView(emptyTextView);
//        }
//    }
//
//    public static ListView addLoadingToMusicListView(ListView listView) {
//
//        if (listView.getContext() == null)
//            return listView;
//        final ProgressBar loading = new ProgressBar(listView.getContext());
//        loading.setIndeterminate(true);
//        loading.setPadding(0, 0, 0, dpToPx(60));
//
//        final ViewParent parent = listView.getParent();
//
//        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
//        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
//        loading.setLayoutParams(layoutParams);
//        listView.setEmptyView(loading);
//        final RelativeLayout relativeLayout = (RelativeLayout) parent;
//        relativeLayout.addView(loading);
//        return listView;
//    }

    // For generating "%searchfilter%" for finding rows in database like that name
    public static String getFilterLikeString(String filter){

        return StaticData.PERCENTAGE_STRING + filter + StaticData.PERCENTAGE_STRING;

    }

    public static ListView addLoadingToListView(ListView listView) {

        if (listView.getContext() == null)
            return listView;
        final ProgressBar loading = new ProgressBar(listView.getContext());
        loading.setIndeterminate(true);

        final ViewParent parent = listView.getParent();
        if (parent == null ||
                parent.getClass() == null ||
                TextUtils.isEmpty(parent.getClass().getName()))
            return listView;
        final String parentType = parent.getClass().getName();

        if (parentType.equals("android.widget.FrameLayout")) {

            loading.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            listView.setEmptyView(loading);
            final FrameLayout frameLayout = (FrameLayout) parent;
            frameLayout.addView(loading);
        } else if (parentType.equals("android.widget.RelativeLayout")) {

            final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            loading.setLayoutParams(layoutParams);
            listView.setEmptyView(loading);
            final RelativeLayout relativeLayout = (RelativeLayout) parent;
            relativeLayout.addView(loading);
        }
        return listView;
    }


    /*public static ListView removeLoadingFromListView(ListView listView) {

        if (listView.getContext() == null)
            return listView;

        final ViewParent parent = listView.getParent();
        if (parent == null ||
                parent.getClass() == null ||
                TextUtils.isEmpty(parent.getClass().getName()))
            return listView;
        final String parentType = parent.getClass().getName();

        if (parentType.equals("android.widget.FrameLayout")) {
            Log.d(TAG, "removeLoadingFromListView: parent = FrameLayout");
            final FrameLayout frameLayout = (FrameLayout) parent;
            frameLayout.removeView(listView.getEmptyView());
        } else if (parentType.equals("android.widget.RelativeLayout")) {
            Log.d(TAG, "removeLoadingFromListView: parent = RelativeLayout");
            final RelativeLayout relativeLayout = (RelativeLayout) parent;
            relativeLayout.removeView(listView.getEmptyView());
        }
        return listView;
    }*/

//    public static GridView addLoadingToGridView(GridView gridView) {
//
//        final ProgressBar loading = new ProgressBar(gridView.getContext());
//        loading.setIndeterminate(true);
//        final ViewParent parent = gridView.getParent();
//        final String parentType = parent.getClass().getValue();
//        if (parentType.equals("android.widget.FrameLayout")) {
//            loading.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
//            gridView.setEmptyView(loading);
//            final FrameLayout frameLayout = (FrameLayout) parent;
//            frameLayout.addView(loading);
//        } else if (parentType.equals("android.widget.RelativeLayout")) {
//
//            final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
//            loading.setLayoutParams(layoutParams);
//            gridView.setEmptyView(loading);
//            final RelativeLayout relativeLayout = (RelativeLayout) parent;
//            relativeLayout.addView(loading);
//        }
//        return gridView;
//    }

    public static MyBoolean sendGCM(final String message, final long hostId, final long clientId) {

        return MiscUtils.autoRetry(() -> {
            Log.i("Downloader", "Sending message " + message);
            return StaticData.MESSAGING_API.sendMessage(message, hostId, clientId).execute();
        }, Optional.of(input -> input == null)).orNull();
    }

    public static <Result> Optional<Result> useContextFromContext(final WeakReference<? extends Context> reference,
                                                                  final UseContextWithResult<Result> task) {

        final Context context;
        if (reference == null || (context = reference.get()) == null)
            return Optional.absent();

        if (context instanceof Activity && ((Activity) context).isFinishing())
            return Optional.absent();

        return Optional.fromNullable(task.work(context));
    }

    public static void useContextFromContext(final WeakReference<? extends Context> reference,
                                             final UseContext task) {

        final Context context;
        if (reference == null || (context = reference.get()) == null)
            return;

        if (context instanceof Activity && ((Activity) context).isFinishing())
            return;

        task.work(context);
    }

    public static <Result> Optional<Result> useContextFromFragment(final WeakReference<? extends Fragment> reference,
                                                                   final UseActivityWithResult<Activity, Result> task) {

        final Fragment fragment;
        if (reference == null || (fragment = reference.get()) == null)
            return Optional.absent();

        //checks on the fragment
        if (isFragmentDead(fragment))
            return Optional.absent();

        final Activity activity = fragment.getActivity();
        if (activity == null || activity.isFinishing())
            return Optional.absent();

        return Optional.fromNullable(task.work(activity));
    }

    public static void useContextFromFragment(final WeakReference<? extends Fragment> reference,
                                              final UseActivity<Activity> task) {

        final Fragment fragment;
        if (reference == null || (fragment = reference.get()) == null)
            return;

        //checks on the fragment
        if (isFragmentDead(fragment))
            return;

        final Activity activity = fragment.getActivity();
        if (activity != null && !activity.isFinishing())
            task.work(activity);
    }

    public static <Param extends Fragment> void useContextAndFragment(final WeakReference<Param> reference,
                                                                      final UseFragmentAndActivity<Param> task) {

        final Param fragment;
        if (reference == null || (fragment = reference.get()) == null)
            return;

        //checks on the fragment
        if (isFragmentDead(fragment))
            return;

        final Activity activity = fragment.getActivity();
        if (activity != null && !activity.isFinishing())
            task.work(activity, fragment);
    }

    public static <Param extends Fragment, Result> Optional<Result> useFragment(final WeakReference<Param> reference,
                                                                                final UseFragmentWithResult<Param, Result> task) {

        final Param fragment;
        if (reference == null || (fragment = reference.get()) == null)
            return Optional.absent();

        //checks on the fragment
        if (isFragmentDead(fragment))
            return Optional.absent();

        final Activity activity = fragment.getActivity();
        if (activity == null || activity.isFinishing())
            return Optional.absent();

        return Optional.fromNullable(task.work(fragment));
    }

    public static <Param extends Fragment> void useFragment(final WeakReference<Param> reference,
                                                            final UseFragment<Param> task) {

        final Param fragment;
        if (reference == null || (fragment = reference.get()) == null)
            return;

        //checks on the fragment
        if (isFragmentDead(fragment))
            return;

        final Activity activity = fragment.getActivity();
        if (activity != null && !activity.isFinishing())
            task.work(fragment);
    }

    public static <Param extends Activity> void useActivity(final WeakReference<Param> reference,
                                                            final UseActivity<Param> task) {

        final Param activity;
        if (reference == null || (activity = reference.get()) == null || activity.isFinishing())
            return;

        task.work(activity);
    }

    public static <Param extends Activity, Result> Optional<Result> useActivityWithResult(final WeakReference<Param> reference,
                                                                                          final UseActivityWithResult<Param, Result> task) {

        final Param activity;
        if (reference == null || (activity = reference.get()) == null || activity.isFinishing())
            return Optional.absent();
        return Optional.fromNullable(task.work(activity));
    }

    public static <Param> void useReference(final WeakReference<Param> reference,
                                            final UseReference<Param> task) {

        final Param param;
        if (reference == null || (param = reference.get()) == null)
            return;
        task.useReference(param);
    }

    public static <Param, Result> Optional<Result> useReference(final WeakReference<Param> reference,
                                                                final UseReferenceWithResult<Param, Result> task) {

        final Param param;
        if (reference == null || (param = reference.get()) == null)
            return Optional.absent();
        return Optional.of(task.useReference(param));
    }

    //    public static <T extends Activity> void runOnUiThread(final WeakReference<T> reference,
//                                                          final UseContextWithResult<Void, T> task) {
//
//        final T activity;
//        if (reference == null || (activity = reference.get()) == null || activity.isFinishing())
//            return;
//
//        activity.runOnUiThread(() -> task.work(activity));
//    }
//

    public static <T extends Fragment> void runOnUiThreadFragment(final WeakReference<T> reference,
                                                                  final UseContext task) {

        final T fragment;
        if (reference == null || (fragment = reference.get()) == null)
            return;

        //checks on the fragment
        if (isFragmentDead(fragment))
            return;

        final Activity activity = fragment.getActivity();
        if (activity == null || activity.isFinishing())
            return;

        activity.runOnUiThread(() -> task.work(activity));
    }

    public static <Param extends Fragment> void runOnUiThreadFragment(final WeakReference<Param> reference,
                                                                      final UseFragmentAndActivity<Param> task) {

        final Param fragment;
        if (reference == null || (fragment = reference.get()) == null)
            return;

        //checks on the fragment
        if (isFragmentDead(fragment))
            return;

        final Activity activity = fragment.getActivity();
        if (activity == null || activity.isFinishing())
            return;

        activity.runOnUiThread(() -> task.work(activity, fragment));
    }

    public static <Param extends Fragment> void runOnUiThreadFragment(final WeakReference<Param> reference,
                                                                      final UseFragment<Param> task) {

        final Param fragment;
        if (reference == null || (fragment = reference.get()) == null)
            return;

        //checks on the fragment
        if (isFragmentDead(fragment))
            return;

        final Activity activity = fragment.getActivity();
        if (activity == null || activity.isFinishing())
            return;

        activity.runOnUiThread(() -> task.work(fragment));
    }

    public static <Param extends Activity> void runOnUiThreadActivity(final WeakReference<Param> reference,
                                                                      final UseActivity<Param> task) {

        final Param activity;
        if (reference == null || (activity = reference.get()) == null || activity.isFinishing())
            return;

        activity.runOnUiThread(() -> task.work(activity));
    }
//
//    public static <T extends Fragment> void runOnUiThreadFragment(final WeakReference<T> reference,
//                                                                  final UseFragmentWithResult<Void, T> task) {
//
//        final T fragment;
//        if (reference == null || (fragment = reference.get()) == null)
//            return;
//    //checks on the fragment
//    if (isFragmentDead(fragment))
//            return Optional.absent();
//
//        final Activity activity = fragment.getActivity();
//        if (activity == null || activity.isFinishing())
//            return;
//
//        activity.runOnUiThread(() -> task.work(fragment));
//    }

    public static <Param extends Fragment> boolean isOnline(@Nullable Param stuff) {

        return !(stuff == null || isFragmentDead(stuff)) && isOnline(stuff.getActivity());
    }

    public static <Param extends Context> boolean isOnline(@Nullable Param stuff) {

        if (stuff == null)
            return false;

        //return false if this context is being destroyed
        if (stuff instanceof Activity && ((Activity) stuff).isFinishing())
            return false;

        final NetworkInfo networkInfo =
                ((ConnectivityManager) stuff.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());

    }

    /**
     * Performs a work, retries upon failure with exponential back-off.
     * Kindly don't use on UI thread.
     *
     * @param <T>       the return type of work
     * @param task      the work that needs to be performed
     * @param predicate the extra condition for failure
     * @return the result/output of performing the work
     */
    public static <T> Optional<T> autoRetry(@NonNull final DoWork<T> task,
                                            @NonNull final Optional<Predicate<T>> predicate) {
        return autoRetry(task, predicate, Optional.absent());
    }

    /**
     * Performs a work, retries upon failure with exponential back-off.
     * Kindly don't use on UI thread.
     *
     * @param <T>       the return type of work
     * @param task      the work that needs to be performed
     * @param predicate the extra condition for failure
     * @return the result/output of performing the work
     */
    public static <T> Optional<T> autoRetry(@NonNull final DoWork<T> task,
                                            @NonNull final Optional<Predicate<T>> predicate,
                                            @NonNull final Optional<RetryHook> retryHookOptional) {

        for (int retry = 0; retry <= StaticData.NETWORK_RETRY; ++retry) {

            if (retryHookOptional.isPresent() && retry > 0)
                retryHookOptional.get().retryCount(retry);

            try {

                Thread.sleep(retry * StaticData.NETWORK_CALL_WAIT);
                final T resultAfterWork = task.doWork();
                /**
                 * If the result was not
                 * desirable we RETRY.
                 */
                if (predicate.isPresent() && predicate.get().apply(resultAfterWork))
                    continue;
                /**
                 * Else we return
                 */
                return Optional.fromNullable(resultAfterWork);
            } catch (InterruptedException | UnknownHostException | NullPointerException | SocketTimeoutException e) {

                try {
                    Log.i("Ayush", e.getLocalizedMessage());
                } catch (NullPointerException ignored) {
                }
                e.printStackTrace();
                return Optional.absent(); //do not retry

            } catch (GoogleJsonResponseException e) {

                try {
                    Log.i("Ayush", e.getLocalizedMessage());
                } catch (NullPointerException ignored) {
                }
                if (e.getLocalizedMessage().contains("404"))
                    return Optional.absent(); //do not retry on 404

            } catch (Exception e) {

                try {
                    Log.i("Ayush", e.getLocalizedMessage());
                } catch (NullPointerException ignored) {
                }
                e.printStackTrace();
            }
        }
        return Optional.absent();
    }

    public static String getMusicStorageKey(long serverId) {
        return serverId + "MUSIC";
    }

    public static String getAppStorageKey(long serverId) {
        return serverId + "APP";
    }

    /**
     * Performs a work, retries upon failure with exponential back-off.
     * This is to be used if returned value is of no importance other than checking for failure.
     * Automatically delegates to separate thread.
     *
     * @param task      the work that needs to be performed
     * @param predicate the extra condition for failure
     * @param <T>       the return type of work
     */
    public static <T> void autoRetryAsync(@NonNull final DoWork<T> task,
                                          @NonNull final Optional<Predicate<T>> predicate) {

        new Thread(() -> {

            for (int retry = 0; retry <= StaticData.NETWORK_RETRY; ++retry) {

                try {

                    Thread.sleep(retry * StaticData.NETWORK_CALL_WAIT);
                    final T resultAfterWork = task.doWork();
                    /**
                     * If the result was not
                     * desirable we RETRY.
                     */
                    if (predicate.isPresent() && predicate.get().apply(resultAfterWork))
                        continue;
                    /**
                     * Else we return
                     */
                    return;
                } catch (InterruptedException | UnknownHostException | NullPointerException | SocketTimeoutException e) {

                    try {
                        Log.i("Ayush", e.getLocalizedMessage());
                    } catch (NullPointerException ignored) {
                    }
                    e.printStackTrace();
                    return; //do not retry

                } catch (GoogleJsonResponseException e) {

                    try {
                        Log.i("Ayush", e.getLocalizedMessage());
                    } catch (NullPointerException ignored) {
                    }
                    if (e.getLocalizedMessage().contains("404"))
                        return; //do not retry on 404

                } catch (Exception e) {

                    try {
                        Log.i("Ayush", e.getLocalizedMessage());
                    } catch (NullPointerException ignored) {
                    }
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static String getOsName() {

        final Field[] fields = Build.VERSION_CODES.class.getFields();
        for (Field field : fields) {

            final String fieldName = field.getName();
            int fieldValue = -1;

            try {
                fieldValue = field.getInt(new Object());
            } catch (IllegalArgumentException |
                    IllegalAccessException |
                    NullPointerException ignored) {
            }

            if (fieldValue == Build.VERSION.SDK_INT)
                return fieldName;
        }

        return "hello_world";
    }

    public synchronized static StartDownloadOperation startDownloadOperation(Context context,
                                                                             ReachDatabase reachDatabase,
                                                                             long receiverId,
                                                                             long senderId,
                                                                             long databaseId) {
        return new StartDownloadOperation(context, reachDatabase, receiverId, senderId, databaseId);
    }

    public static List<ApplicationInfo> fastGetApps(PackageManager packageManager) {

        final Process process;
        try {
            process = Runtime.getRuntime().exec("pm list packages");
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        final ExecutorService executorService = Executors.newFixedThreadPool(20);
        final ExecutorCompletionService<ApplicationInfo> completionService = new ExecutorCompletionService<>(executorService);

        int counter = 0;
        while (true) {

            final String line;
            try {
                line = bufferedReader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            if (TextUtils.isEmpty(line))
                continue;
            final String packageName = line.substring(line.indexOf(':') + 1);
            if (TextUtils.isEmpty(packageName))
                continue;

            //load on thread
            completionService.submit(() -> {

                final ApplicationInfo toReturn = packageManager.getApplicationInfo(packageName, 0);
                return SYSTEM_APP_REMOVER.apply(toReturn) ? toReturn : null;
            });
            counter++;
        }

        process.destroy();
        MiscUtils.closeQuietly(bufferedReader);

        final List<ApplicationInfo> finalList = new ArrayList<>(counter);
        for (int index = 0; index < counter; index++) {
            try {
                finalList.add(completionService.take().get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executorService.shutdownNow();

        return finalList;
    }

    public static final Predicate<ApplicationInfo> SYSTEM_APP_REMOVER =
            input -> input != null &&
                    (input.flags & ApplicationInfo.FLAG_SYSTEM) == 0 &&
                    (input.flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0 &&
                    (input.flags & ApplicationInfo.FLAG_TEST_ONLY) == 0;

    public static List<ApplicationInfo> getInstalledApps(PackageManager packageManager) {

        try {

            final List<ApplicationInfo> applications = packageManager.getInstalledApplications(0);
            final Iterator<ApplicationInfo> iterator = applications.iterator();
            while (iterator.hasNext()) {

                final ApplicationInfo applicationInfo = iterator.next();
                if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        || (applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0
                        || (applicationInfo.flags & ApplicationInfo.FLAG_TEST_ONLY) != 0)
                    iterator.remove();
            }
            return applications;

        } catch (Exception e) {
            e.printStackTrace();
            return fastGetApps(packageManager);
        }
    }

    public static void openAppInPlayStore(Activity activity, String packageName, Long senderId, String page) {

        final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach",
                Context.MODE_PRIVATE);
        ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                .setCategory("Transaction - Open App")
                .setAction("User Name - " + SharedPrefUtils.getUserName(sharedPreferences))
                .setLabel(page)
                .setValue(1)
                .build());

        //usage tracking
        final Map<PostParams, String> simpleParams = getMap(6);
        simpleParams.put(PostParams.USER_ID, SharedPrefUtils.getServerId(sharedPreferences) + "");
        simpleParams.put(PostParams.DEVICE_ID, getDeviceId(activity));
        simpleParams.put(PostParams.OS, getOsName());
        simpleParams.put(PostParams.OS_VERSION, Build.VERSION.SDK_INT + "");
        try {
            simpleParams.put(PostParams.APP_VERSION,
                    activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode + "");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        simpleParams.put(PostParams.SCREEN_NAME, "unknown");

        final Map<AppMetadata, String> complexParams = getMap(2);
        complexParams.put(AppMetadata.PACKAGE_NAME, packageName + "");
        complexParams.put(AppMetadata.SENDER_ID, senderId + "");

        try {
            UsageTracker.trackApp(simpleParams, complexParams, UsageTracker.CLICK_APP);
        } catch (JSONException ignored) {
        }
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="
                + packageName)));
    }

    public static void openApp(Context context, String packageName) {
        try {
            context.startActivity(context.getPackageManager().getLaunchIntentForPackage(packageName));
        } catch (Exception ignored) {
        }
    }

    private static class StartDownloadOperation implements Runnable {

        private final ReachDatabase reachDatabase;
        private final long receiverId, senderId, databaseId;
        private final WeakReference<Context> contextReference;

        private StartDownloadOperation(Context context,
                                       ReachDatabase reachDatabase,
                                       long receiverId,
                                       long senderId,
                                       long databaseId) {

            this.contextReference = new WeakReference<>(context);
            this.reachDatabase = reachDatabase;
            this.receiverId = receiverId;
            this.senderId = senderId;
            this.databaseId = databaseId;
        }

        private String generateRequest(ReachDatabase reachDatabase) {

            final Connection connection = new Connection.Builder()
                    .setSongId(reachDatabase.getSongId())
                    .setMetaHash(reachDatabase.getMetaHash())
                    .setSenderId(reachDatabase.getSenderId())
                    .setReceiverId(reachDatabase.getReceiverId())
                    .setUniqueIdReceiver(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE))
                    .setUniqueIdSender(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE))
                    .setLogicalClock(reachDatabase.getLogicalClock())
                    .setOffset(reachDatabase.getProcessed())
                    .setLength(reachDatabase.getLength())
                    .setUrl("")
                    .setSenderIp("")
                    .setMessageType("REQ").build();

            return "CONNECT" + new Gson().toJson(connection);
        }

        private String fakeResponse(ReachDatabase reachDatabase) {

            final Connection connection = new Connection.Builder()
                    .setSongId(reachDatabase.getSongId())
                    .setMetaHash(reachDatabase.getMetaHash())
                    .setSenderId(reachDatabase.getSenderId())
                    .setReceiverId(reachDatabase.getReceiverId())
                    .setUniqueIdReceiver(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE))
                    .setUniqueIdSender(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE))
                    .setLogicalClock(reachDatabase.getLogicalClock())
                    .setOffset(reachDatabase.getProcessed())
                    .setLength(reachDatabase.getLength())
                    .setUrl("")
                    .setSenderIp("")
                    .setMessageType("RELAY").build();

            return new Gson().toJson(connection);
        }

        @Override
        public void run() {

            final MyBoolean myBoolean;
            if (reachDatabase.getSenderId() == StaticData.DEVIKA) {

                //hit cloud
                ProcessManager.submitNetworkRequest(contextReference.get(), fakeResponse(reachDatabase));
                myBoolean = new MyBoolean();
                myBoolean.setGcmexpired(false);
                myBoolean.setOtherGCMExpired(false);
            } else {
                //sending REQ to senderId
                myBoolean = sendGCM(generateRequest(reachDatabase), senderId, receiverId);
            }

            final ReachDatabase.Status status;

            if (myBoolean == null) {
                Log.i("Ayush", "GCM sending resulted in shit");
                status = ReachDatabase.Status.GCM_FAILED;
            } else if (myBoolean.getGcmexpired()) {

                //TODO test
//                final Context context = contextReference.get();
//                if(context == null)
//                    return;
//                final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
//                MiscUtils.updateGCM(SharedPrefUtils.getServerId(preferences), contextReference);
                Log.i("Ayush", "GCM re-registry needed");
                status = ReachDatabase.Status.GCM_FAILED;
            } else if (myBoolean.getOtherGCMExpired()) {
                Log.i("Downloader", "SENDING GCM FAILED " + senderId);
                status = ReachDatabase.Status.GCM_FAILED;
            } else {
                Log.i("Downloader", "GCM SENT " + senderId);
                status = ReachDatabase.Status.NOT_WORKING;
            }

            final String condition = SongHelper.COLUMN_ID + " = ? and " +
                    SongHelper.COLUMN_STATUS + " != ?"; //operation should not be paused !
            final String[] arguments = new String[]{databaseId + "", ReachDatabase.Status.PAUSED_BY_USER.getString()};
            final ContentValues values = new ContentValues();
            values.put(SongHelper.COLUMN_STATUS, status.getValue());

            MiscUtils.useContextFromContext(contextReference, context -> {

                Log.i("Downloader", "Updating DB on GCM sent " + (context.getContentResolver().update(
                        Uri.parse(SongProvider.CONTENT_URI + "/" + databaseId),
                        values, condition, arguments) > 0));
                return null;
            });
        }
        ////////
    }

    /*public synchronized static void checkChatToken(WeakReference<SharedPreferences> preferencesWeakReference,
                                                   WeakReference<Firebase> firebaseWeakReference,
                                                   WeakReference<? extends Activity> toHelpTrack) {

        final SharedPreferences preferences = preferencesWeakReference.get();
        if (preferences == null)
            return;

        final String localToken = SharedPrefUtils.getChatToken(preferences);
        final String localUUID = SharedPrefUtils.getChatUUID(preferences);
        final long serverId = SharedPrefUtils.getServerId(preferences);

        if (serverId == 0)
            return; //shiz

        //if not empty exit
        if (!TextUtils.isEmpty(localToken) && !TextUtils.isEmpty(localUUID))
            return;

        //fetch from server
        final MyString fetchTokenFromServer = MiscUtils.autoRetry(() -> StaticData.USER_API.getChatToken(serverId).execute(), Optional.absent()).orNull();
        if (fetchTokenFromServer == null || TextUtils.isEmpty(fetchTokenFromServer.getString())) {

            MiscUtils.useContextFromContext(toHelpTrack, activity -> {

                ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                        .setCategory("SEVERE ERROR, chat token failed")
                        .setAction("User Id - " + serverId)
                        .setValue(1)
                        .build());
            });
            Log.i("Ayush", "Chat token check failed !");
        } else {

            final Firebase firebase = firebaseWeakReference.get();
            if (firebase == null)
                return;
            firebase.authWithCustomToken(fetchTokenFromServer.getString(), new Firebase.AuthResultHandler() {

                @Override
                public void onAuthenticationError(FirebaseError error) {

                    MiscUtils.useContextFromContext(toHelpTrack, activity -> {

                        ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                                .setCategory("SEVERE ERROR " + error.getDetails())
                                .setAction("User Id - " + serverId)
                                .setValue(1)
                                .build());
                    });
                    Log.e("Ayush", "Login Failed! " + error.getMessage());
                }

                @Override
                public void onAuthenticated(AuthData authData) {

                    final String chatUUID = authData.getUid();
                    //if found save
                    SharedPrefUtils.storeChatUUID(preferences, chatUUID);
                    SharedPrefUtils.storeChatToken(preferences, fetchTokenFromServer.getString());
                    Log.i("Ayush", "Chat authenticated " + chatUUID);

                    final Map<String, Object> userData = new HashMap<>();
                    userData.put("uid", authData.getAuth().get("uid"));
                    userData.put("phoneNumber", authData.getAuth().get("phoneNumber"));
                    userData.put("userName", authData.getAuth().get("userName"));
                    userData.put("imageId", authData.getAuth().get("imageId"));
                    userData.put("lastActivated", 0);
                    userData.put("newMessage", true);

                    final Firebase firebase = firebaseWeakReference.get();
                    if (firebase != null)
                        firebase.child("user").child(chatUUID).setValue(userData);
                }
            });
        }
    }*/

    public static void startDownload(@Nonnull ReachDatabase reachDatabase, Activity activity, View snackView, String page) {

//        final Activity activity = getActivity();

        final ContentResolver contentResolver = activity.getContentResolver();

        if (contentResolver == null)
            return;

        /**
         * DISPLAY_NAME, ACTUAL_NAME, SIZE & DURATION all can not be same, effectively its a hash
         */

        final Cursor cursor = contentResolver.query(
                SongProvider.CONTENT_URI,
                SongCursorHelper.SONG_HELPER.getProjection(),
                SongHelper.COLUMN_DISPLAY_NAME + " = ? and " +
                        SongHelper.COLUMN_ACTUAL_NAME + " = ? and " +
                        SongHelper.COLUMN_SIZE + " = ? and " +
                        SongHelper.COLUMN_DURATION + " = ?",
                new String[]{
                        reachDatabase.getDisplayName(),
                        reachDatabase.getActualName(),
                        reachDatabase.getLength() + "",
                        reachDatabase.getDuration() + ""},
                null);

        if (cursor != null) {

            if (cursor.moveToFirst()) {

                final Song song = SongCursorHelper.SONG_HELPER.parse(cursor);
                playSong(song, activity);
                //in both cases close and continue
                cursor.close();
                return;
            }
            cursor.close();
        }

        final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        final String clientName = SharedPrefUtils.getUserName(sharedPreferences);

        //new song
        //We call bulk starter always
        final Uri uri = contentResolver.insert(SongProvider.CONTENT_URI,
                SongHelper.contentValuesCreator(reachDatabase));
        if (uri == null) {

            ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                    .setCategory("Add song failed")
                    .setAction("User Name - " + clientName)
                    .setLabel("Song - " + reachDatabase.getDisplayName() + ", From - " + reachDatabase.getSenderId())
                    .setValue(1)
                    .build());
            return;
        }

        final String[] splitter = uri.toString().split("/");
        if (splitter.length == 0)
            return;

        final long dbId = Long.parseLong(splitter[splitter.length - 1].trim());
        //start this operation
        new Thread(MiscUtils.startDownloadOperation(
                activity,
                reachDatabase,
                reachDatabase.getReceiverId(), //myID
                reachDatabase.getSenderId(),   //the uploaded
                dbId)).start();

        ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                .setCategory("Transaction - Add SongBrainz")
                .setAction("User Name - " + clientName)
                .setLabel(page)
                .setValue(1)
                .build());

        //usage tracking
        final Map<PostParams, String> simpleParams = MiscUtils.getMap(6);
        simpleParams.put(PostParams.USER_ID, reachDatabase.getReceiverId() + "");
        simpleParams.put(PostParams.DEVICE_ID, MiscUtils.getDeviceId(activity));
        simpleParams.put(PostParams.OS, MiscUtils.getOsName());
        simpleParams.put(PostParams.OS_VERSION, Build.VERSION.SDK_INT + "");
        try {
            simpleParams.put(PostParams.APP_VERSION,
                    activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode + "");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        simpleParams.put(PostParams.SCREEN_NAME, "unknown");

        final Map<SongMetadata, String> complexParams = MiscUtils.getMap(9);
        complexParams.put(SongMetadata.SONG_ID, reachDatabase.getSongId() + "");
        complexParams.put(SongMetadata.META_HASH, reachDatabase.getMetaHash());
        complexParams.put(SongMetadata.ARTIST, reachDatabase.getArtistName());
        complexParams.put(SongMetadata.TITLE, reachDatabase.getDisplayName());
        complexParams.put(SongMetadata.DURATION, reachDatabase.getDuration() + "");
        complexParams.put(SongMetadata.SIZE, reachDatabase.getLength() + "");
        complexParams.put(SongMetadata.UPLOADER_ID, reachDatabase.getSenderId() + "");
        complexParams.put(SongMetadata.ALBUM, reachDatabase.getAlbumName());

        try {
            UsageTracker.trackSong(simpleParams, complexParams, UsageTracker.DOWNLOAD_SONG);
        } catch (JSONException ignored) {
        }

        if (SharedPrefUtils.isItFirstTimeDownload(sharedPreferences)) {
            final Intent foreGround = new Intent(activity, ReachActivity.class);
            foreGround.setAction(ReachActivity.OPEN_MANAGER_SONGS_DOWNLOADING);
            foreGround.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(foreGround);
            SharedPrefUtils.putFirstTimeDownload(sharedPreferences, false);
        } else {
            if (snackView != null) {
                final Snackbar snack_bar = Snackbar.make(snackView, "Song added to queue", Snackbar.LENGTH_INDEFINITE);

                snack_bar.getView().setOnClickListener(v -> {
                    snack_bar.dismiss();
                });
                snack_bar.setAction("Open manager", v -> {
                    final Intent foreGround = new Intent(activity, ReachActivity.class);
                    foreGround.setAction(ReachActivity.OPEN_MANAGER_SONGS_DOWNLOADING);
                    foreGround.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    activity.startActivity(foreGround);
                }).show();
            }
        }
    }

    @NonNull
    public static JsonElement get(JsonObject jsonObject, EnumHelper<String> enumHelper, String defaultValue) {

        final JsonElement jsonElement = jsonObject.get(enumHelper.getName());
        if (jsonElement == null || jsonElement.isJsonNull()) //null check
            return new JsonPrimitive(defaultValue); //return a default String value, parse later

        return jsonElement;
    }

    @NonNull
    public static JsonElement get(JsonObject jsonObject, EnumHelper<String> enumHelper) {
        return jsonObject.get(enumHelper.getName());
    }

    public static ThreadPoolExecutor getRejectionExecutor() {

        //an executor for getting stories from server
        return new ThreadPoolExecutor(
                1, //only 1 thread
                1, //only 1 thread
                0L, TimeUnit.MILLISECONDS, //no waiting
                new SynchronousQueue<>(false), //only 1 thread
                new ThreadPoolExecutor.DiscardPolicy()); //ignored
    }

    public static ThreadPoolExecutor getRejectionExecutor(ThreadFactory threadFactory) {

        //an executor for getting stories from server
        return new ThreadPoolExecutor(
                1, //only 1 thread
                1, //only 1 thread
                0L, TimeUnit.MILLISECONDS, //no waiting
                new SynchronousQueue<>(false), //only 1 thread
                threadFactory,
                new ThreadPoolExecutor.DiscardPolicy()); //ignored
    }

    public static <T extends Fragment> boolean isFragmentDead(@Nullable T fragment) {
        return fragment == null || fragment.isDetached() || fragment.isRemoving() || !fragment.isAdded();
    }

    public static void navigateUp(final AppCompatActivity activity) {

        final Intent upIntent = activity.getSupportParentActivityIntent();
        if (upIntent == null)
            return;
        upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(upIntent);
        //TODO
//        activity.finish();
    }

    public static void navigateUpWithPlayer(final AppCompatActivity activity, int time, String ytId) {

        final Intent upIntent = activity.getSupportParentActivityIntent();
        if (upIntent == null)
            return;
        upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        upIntent.setAction(ReachActivity.RESUME_PLAYER);
        upIntent.putExtra("time", time);
        upIntent.putExtra("ytId", ytId);
        activity.startActivity(upIntent);
        //TODO
//        activity.finish();
    }

    public static String calculateSongHash(long userId,
                                           long duration,
                                           long size,
                                           @NonNull String title,
                                           @NonNull HashFunction hashFunction) {
        //TODO: Error here, so added more information to know about the error
        if (userId == 0 || duration == 0 || size == 0 || TextUtils.isEmpty(title)) {
            throw new IllegalArgumentException("Invalid parameters found" + " userId = " + userId
                    + ", duration = " + duration + ", size = " + size + ", title = "+ title);
        }

        return hashFunction.newHasher()
                .putLong(userId)
                .putLong(duration)
                .putLong(size)
                .putUnencodedChars(title)
                .hash().toString();
    }

    public static String calculateAppHash(@NonNull String packageName,
                                          @NonNull HashFunction hashFunction) {

        return hashFunction.hashUnencodedChars(packageName).toString();
    }

    /**
     * If bitmap could not be returned use the original image as is, this should not happen normally
     *
     * @param inputStream the inputStream for the image
     * @return the resized bitmap
     */
    @NonNull
    public static BitmapFactory.Options getRequiredOptions(final InputStream inputStream) {

        // Decode just the boundaries
        final BitmapFactory.Options mBitmapOptions = new BitmapFactory.Options();
        mBitmapOptions.inJustDecodeBounds = true;

        final Bitmap temporary = BitmapFactory.decodeStream(inputStream, null, mBitmapOptions);
        if (temporary != null)
            temporary.recycle();
        if (mBitmapOptions.outHeight == 0 || mBitmapOptions.outWidth == 0)
            return mBitmapOptions; //illegal

        // Calculate inSampleSize
        // Raw height and width of image
        final int height = mBitmapOptions.outHeight;
        final int width = mBitmapOptions.outWidth;
        final int sideLength = 1000;

        int reqHeight = height;
        int reqWidth = width;
        final int inDensity;
        final int inTargetDensity;

        if (height > width) {

            if (height > sideLength) {

                reqHeight = sideLength;
                reqWidth = (width * sideLength) / height;
            }
            inDensity = height;
            inTargetDensity = reqHeight;

        } else if (width > height) {

            if (width > sideLength) {
                reqWidth = sideLength;
                reqHeight = (height * sideLength) / width;
            }
            inDensity = width;
            inTargetDensity = reqWidth;

        } else {

            reqWidth = sideLength;
            reqHeight = sideLength;
            inDensity = height;
            inTargetDensity = reqHeight;
        }

        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth)
                inSampleSize *= 2;
        }

        //now go resize the image to the size you want
        mBitmapOptions.inSampleSize = inSampleSize;
        mBitmapOptions.inDither = true;
        mBitmapOptions.inPreferQualityOverSpeed = true;
        mBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        mBitmapOptions.inJustDecodeBounds = false;
        mBitmapOptions.inScaled = true;
        mBitmapOptions.inDensity = inDensity;
        mBitmapOptions.inTargetDensity = inTargetDensity * mBitmapOptions.inSampleSize;

        /**
         * Generate the compressed image
         * Will load & resize the image to be 1/inSampleSize dimensions
         */
        return mBitmapOptions;
    }

    public static byte[] compressProto(Message message) {

        final ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream compressor = null;
        try {
            compressor = new GZIPOutputStream(arrayOutputStream);
            compressor.write(message.toByteArray());
            compressor.close();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0]; //failed
        } finally {
            MiscUtils.closeQuietly(compressor, arrayOutputStream);
        }

        return arrayOutputStream.toByteArray();
    }

    public static byte[] unCompressBytes(byte[] bytes) {

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        GZIPInputStream unCompressor = null;
        try {
            unCompressor = new GZIPInputStream(byteArrayInputStream);
            ByteStreams.copy(unCompressor, byteArrayOutputStream);
            unCompressor.close();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0]; //failed
        } finally {
            MiscUtils.closeQuietly(unCompressor, byteArrayInputStream, byteArrayOutputStream);
        }

        return byteArrayOutputStream.toByteArray();
    }

//    final HttpTransport transport = new NetHttpTransport();
//    final JsonFactory factory = new JacksonFactory();
//    final GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(activity, StaticData.SCOPE);
//    credential.setSelectedAccountName(SharedPrefUtils.getEmailId(fragment.sharedPreferences));
//    Log.i("Ayush", "Using credential " + credential.getSelectedAccountName());
//
//    final BlahApi businessApi = CloudEndPointsUtils.updateBuilder(new BlahApi.Builder(transport, factory, credential)
//            .setRootUrl("https://1-dot-business-module-dot-able-door-616.appspot.com/_ah/api/")).build();
}