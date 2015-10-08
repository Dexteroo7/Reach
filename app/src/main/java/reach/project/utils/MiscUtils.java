package reach.project.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.gson.Gson;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import reach.backend.entities.messaging.model.MyBoolean;
import reach.backend.entities.userApi.model.MyString;
import reach.project.core.StaticData;
import reach.project.music.albums.Album;
import reach.project.music.albums.ReachAlbumHelper;
import reach.project.music.albums.ReachAlbumProvider;
import reach.project.music.artists.Artist;
import reach.project.music.artists.ReachArtistHelper;
import reach.project.music.artists.ReachArtistProvider;
import reach.project.music.playLists.Playlist;
import reach.project.music.playLists.ReachPlayListHelper;
import reach.project.music.playLists.ReachPlayListProvider;
import reach.project.music.songs.ReachSongHelper;
import reach.project.music.songs.ReachSongProvider;
import reach.project.music.songs.Song;
import reach.project.reachProcess.auxiliaryClasses.Connection;
import reach.project.reachProcess.reachService.ProcessManager;
import reach.project.uploadDownload.ReachDatabase;
import reach.project.uploadDownload.ReachDatabaseHelper;
import reach.project.uploadDownload.ReachDatabaseProvider;
import reach.project.utils.auxiliaryClasses.DoWork;
import reach.project.utils.auxiliaryClasses.UseContext;
import reach.project.utils.auxiliaryClasses.UseContext2;
import reach.project.utils.auxiliaryClasses.UseContextAndFragment;
import reach.project.utils.auxiliaryClasses.UseFragment;
import reach.project.utils.auxiliaryClasses.UseFragment2;

/**
 * Created by dexter on 1/10/14.
 */

public enum MiscUtils {
    ;

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

    public static String capitalizeFirst(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
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

    public static String generateInitials(String name) {

        name = name.trim();
        if (TextUtils.isEmpty(name))
            return "A";

        final String[] splitter = name.split(" ");
        switch (splitter.length) {

            case 0:
                return "A";
            case 1:
                return (splitter[0].charAt(0) + "").toUpperCase();
            case 2:
                return (splitter[0].charAt(0) + "" + splitter[1].charAt(0)).toUpperCase();
            default:
                return "A";
        }
    }

    public static boolean containsIgnoreCase(CharSequence str, CharSequence searchStr) {

        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(searchStr))
            return false;
        int len = searchStr.length();
        int max = str.length() - len;
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

    public static void setEmptyTextforGridView(GridView gridView, String emptyText) {

        if (gridView.getContext() == null)
            return;
        final TextView emptyTextView = new TextView(gridView.getContext());
        emptyTextView.setText(emptyText);

        final ViewParent parent = gridView.getParent();
        if (parent == null ||
                parent.getClass() == null ||
                TextUtils.isEmpty(parent.getClass().getName()))
            return;
        final String parentType = parent.getClass().getName();

        if (parentType.equals("android.widget.FrameLayout")) {
            emptyTextView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            gridView.setEmptyView(emptyTextView);
            final FrameLayout frameLayout = (FrameLayout) parent;
            frameLayout.removeViewAt(1);
            frameLayout.addView(emptyTextView);
        } else if (parentType.equals("android.widget.RelativeLayout")) {

            final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            emptyTextView.setLayoutParams(layoutParams);
            gridView.setEmptyView(emptyTextView);
            final RelativeLayout relativeLayout = (RelativeLayout) parent;
            relativeLayout.removeViewAt(1);
            relativeLayout.addView(emptyTextView);
        }
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

    public static GridView addLoadingToGridView(GridView gridView) {

        final ProgressBar loading = new ProgressBar(gridView.getContext());
        loading.setIndeterminate(true);
        final ViewParent parent = gridView.getParent();
        final String parentType = parent.getClass().getName();
        if (parentType.equals("android.widget.FrameLayout")) {
            loading.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            gridView.setEmptyView(loading);
            final FrameLayout frameLayout = (FrameLayout) parent;
            frameLayout.addView(loading);
        } else if (parentType.equals("android.widget.RelativeLayout")) {

            final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            loading.setLayoutParams(layoutParams);
            gridView.setEmptyView(loading);
            final RelativeLayout relativeLayout = (RelativeLayout) parent;
            relativeLayout.addView(loading);
        }
        return gridView;
    }

    public static void deleteSongs(long userId, ContentResolver contentResolver) {

        contentResolver.delete(
                ReachSongProvider.CONTENT_URI,
                ReachSongHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId + ""});

        contentResolver.delete(
                ReachAlbumProvider.CONTENT_URI,
                ReachAlbumHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId + ""});

        contentResolver.delete(
                ReachArtistProvider.CONTENT_URI,
                ReachArtistHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId + ""});
    }

    public static void deletePlayLists(long userId, ContentResolver contentResolver) {

        contentResolver.delete(
                ReachPlayListProvider.CONTENT_URI,
                ReachPlayListHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId + ""});
    }

    public static void bulkInsertSongs(List<Song> songList,
                                       ArrayMap<String, Album> reachAlbums,
                                       ArrayMap<String, Artist> reachArtists,
                                       ContentResolver contentResolver,
                                       long serverId) {

        //Add all songs
        final ContentValues[] songs = new ContentValues[songList.size()];
        final ContentValues[] albums = new ContentValues[reachAlbums.size()];
        final ContentValues[] artists = new ContentValues[reachArtists.size()];

        int i = 0;
        if (songList.size() > 0) {

            for (Song song : songList)
                songs[i++] = ReachSongHelper.contentValuesCreator(song, serverId);
            i = 0;
            Log.i("Ayush", "Songs Inserted " + contentResolver.bulkInsert(ReachSongProvider.CONTENT_URI, songs));
        } else
            contentResolver.delete(ReachSongProvider.CONTENT_URI,
                    ReachSongHelper.COLUMN_USER_ID + " = ?",
                    new String[]{serverId + ""});

        if (reachAlbums.size() > 0) {

            for (int j = 0; j < reachAlbums.size(); j++) {

                final Album album = reachAlbums.valueAt(j);
                if (album != null)
                    albums[i++] = ReachAlbumHelper.contentValuesCreator(album);
            }
            Log.i("Ayush", "Albums Inserted " + contentResolver.bulkInsert(ReachAlbumProvider.CONTENT_URI, albums));
            i = 0;
        } else
            contentResolver.delete(ReachAlbumProvider.CONTENT_URI,
                    ReachAlbumHelper.COLUMN_USER_ID + " = ?",
                    new String[]{serverId + ""});

        if (reachArtists.size() > 0) {

            for (int j = 0; i < reachArtists.size(); j++) {

                final Artist artist = reachArtists.valueAt(j);
                if (artist != null)
                    artists[i++] = ReachArtistHelper.contentValuesCreator(artist);
            }
            Log.i("Ayush", "Artists Inserted " + contentResolver.bulkInsert(ReachArtistProvider.CONTENT_URI, artists));
        } else
            contentResolver.delete(ReachArtistProvider.CONTENT_URI,
                    ReachArtistHelper.COLUMN_USER_ID + " = ?",
                    new String[]{serverId + ""});
    }

    public static void bulkInsertPlayLists(List<Playlist> playlistList,
                                           ContentResolver contentResolver,
                                           long serverId) {

        if (playlistList != null && !playlistList.isEmpty()) {

            final ContentValues[] playLists = new ContentValues[playlistList.size()];
            int i = 0;
            for (Playlist playlist : playlistList) {
                playLists[i++] = ReachPlayListHelper.contentValuesCreator(playlist, serverId);
            }
            Log.i("Ayush", "PlayLists Inserted " + contentResolver.bulkInsert(ReachPlayListProvider.CONTENT_URI, playLists));
        } else
            contentResolver.delete(ReachPlayListProvider.CONTENT_URI,
                    ReachPlayListHelper.COLUMN_USER_ID + " = ?",
                    new String[]{serverId + ""});
    }

    public static Pair<ArrayMap<String, Album>, ArrayMap<String, Artist>> getAlbumsAndArtists(Iterable<Song> songs, long serverId) {

        final ArrayMap<String, Album> albumMap = new ArrayMap<>();
        final ArrayMap<String, Artist> artistMap = new ArrayMap<>();

        for (Song song : songs) {

            //don't consider invisible files
            if (!song.visibility)
                continue;

            if (!TextUtils.isEmpty(song.album)) {

                Album album = albumMap.get(song.album);
                if (album == null)
                    albumMap.put(song.album, album = new Album());
                album.setAlbumName(song.album);
                album.setUserId(serverId);
                album.setArtist(song.artist);
                album.incrementSize();
            }

            if (!TextUtils.isEmpty(song.artist)) {

                Artist artist = artistMap.get(song.artist);
                if (artist == null)
                    artistMap.put(song.artist, artist = new Artist());
                artist.setArtistName(song.artist);
                artist.setUserID(serverId);
                artist.setAlbum(song.album);
                artist.incrementSize();
            }
        }

        Log.i("Ayush", "Found " + albumMap.size() + " " + artistMap.size());
        ///////////////////////
        return new Pair<>(albumMap, artistMap);
    }

    public static MyBoolean sendGCM(final String message, final long hostId, final long clientId) {

        return MiscUtils.autoRetry(() -> {
            Log.i("Downloader", "Sending message " + message);
            return StaticData.messagingEndpoint.sendMessage(message, hostId, clientId).execute();
        }, Optional.of(input -> input == null)).orNull();
    }

//    /**
//     * //TODO improve/fix/replace this hack
//     * @return The current localIP
//     * @throws IOException
//     * @throws InterruptedException
//     */
//    public static InetAddress getLocalIp() throws IOException, InterruptedException {
//
//        final Socket temp = new Socket();
//        final InetSocketAddress google = new InetSocketAddress("www.google.com", 80);
//        final InetAddress localIpAddress;
//        temp.connect(google);
//        temp.setSoLinger(true, 1);
//        temp.setReuseAddress(true);
//        localIpAddress = temp.getLocalAddress();
//        temp.close();
//        Thread.sleep(1000L);
//        return localIpAddress;
//    }

    public static <Param extends Context, Result> Optional<Result> useContextFromContext(final WeakReference<Param> reference,
                                                                                         final UseContext<Result, Param> task) {

        final Param context;
        if (reference == null || (context = reference.get()) == null)
            return Optional.absent();

        if (context instanceof Activity && ((Activity) context).isFinishing())
            return Optional.absent();

        return Optional.fromNullable(task.work(context));
    }

    public static <Param extends Context> void useContextFromContext(final WeakReference<Param> reference,
                                                                     final UseContext2<Param> task) {

        final Param context;
        if (reference == null || (context = reference.get()) == null)
            return;

        if (context instanceof Activity && ((Activity) context).isFinishing())
            return;

        task.work(context);
    }

    @SuppressWarnings("unchecked")
    public static <Param1 extends Context, Param2 extends Fragment, Result> Optional<Result> useContextFromFragment(final WeakReference<Param2> reference,
                                                                                                                    final UseContext<Result, Param1> task) {

        final Param2 fragment;
        if (reference == null || (fragment = reference.get()) == null)
            return Optional.absent();

        final Activity activity = fragment.getActivity();
        if (activity == null || activity.isFinishing())
            return Optional.absent();

        return Optional.fromNullable(task.work((Param1) activity));
    }

    @SuppressWarnings("unchecked")
    public static <Param1 extends Context, Param2 extends Fragment> void useContextFromFragment(final WeakReference<Param2> reference,
                                                                                                final UseContext2<Param1> task) {

        final Param2 fragment;
        if (reference == null || (fragment = reference.get()) == null)
            return;

        final Activity activity = fragment.getActivity();
        if (activity != null)
            task.work((Param1) activity);
    }

    @SuppressWarnings("unchecked")
    public static <Param1 extends Context, Param2 extends Fragment> void useContextAndFragment(final WeakReference<Param2> reference,
                                                                                               final UseContextAndFragment<Param1, Param2> task) {

        final Param2 fragment;
        if (reference == null || (fragment = reference.get()) == null)
            return;

        final Activity activity = fragment.getActivity();
        if (activity != null && !activity.isFinishing())
            task.work((Param1) activity, fragment);
    }

    public static <Param extends Fragment, Result> Optional<Result> useFragment(final WeakReference<Param> reference,
                                                                                final UseFragment<Result, Param> task) {

        final Param fragment;
        if (reference == null || (fragment = reference.get()) == null)
            return Optional.absent();

        final Activity activity = fragment.getActivity();
        if (activity == null || activity.isFinishing())
            return Optional.absent();

        return Optional.fromNullable(task.work(fragment));
    }

    public static <Param extends Fragment> void useFragment(final WeakReference<Param> reference,
                                                            final UseFragment2<Param> task) {

        final Param fragment;
        if (reference == null || (fragment = reference.get()) == null)
            return;

        final Activity activity = fragment.getActivity();
        if (activity != null && !activity.isFinishing())
            task.work(fragment);
    }

//    public static <T extends Activity> void runOnUiThread(final WeakReference<T> reference,
//                                                          final UseContext<Void, T> task) {
//
//        final T activity;
//        if (reference == null || (activity = reference.get()) == null || activity.isFinishing())
//            return;
//
//        activity.runOnUiThread(() -> task.work(activity));
//    }
//
//    public static <T extends Fragment> void runOnUiThreadFragment(final WeakReference<T> reference,
//                                                                  final UseContext<Void, Activity> task) {
//
//        final T fragment;
//        if (reference == null || (fragment = reference.get()) == null)
//            return;
//
//        final Activity activity = fragment.getActivity();
//        if (activity == null || activity.isFinishing())
//            return;
//
//        activity.runOnUiThread(() -> task.work(activity));
//    }
//
//    public static <T extends Fragment> void runOnUiThreadFragment(final WeakReference<T> reference,
//                                                                  final UseFragment<Void, T> task) {
//
//        final T fragment;
//        if (reference == null || (fragment = reference.get()) == null)
//            return;
//
//        final Activity activity = fragment.getActivity();
//        if (activity == null || activity.isFinishing())
//            return;
//
//        activity.runOnUiThread(() -> task.work(fragment));
//    }

    /**
     * @param id        id of the person to update gcm of
     * @param reference the context reference
     * @param <T>       something which extends context
     * @return false : failed, true : OK
     */
    public static <T extends Context> boolean updateGCM(final long id, final WeakReference<T> reference) {

        final String regId = autoRetry(() -> {

            final Context context;
            if (reference == null || (context = reference.get()) == null)
                return "QUIT";
            return GoogleCloudMessaging.getInstance(context)
                    .register("528178870551");
        }, Optional.of(TextUtils::isEmpty)).orNull();

        if (TextUtils.isEmpty(regId) || regId.equals("QUIT"))
            return false;
        //if everything is fine, send to server
        Log.i("Ayush", "Uploading newGcmId to server");
        final Boolean result = autoRetry(() -> {

            StaticData.userEndpoint.setGCMId(id, regId).execute();
            Log.i("Ayush", regId.substring(0, 5) + "NEW GCM ID AFTER CHECK");
            return true;
        }, Optional.absent()).orNull();
        //set locally
        return !(result == null || !result);
    }

    public static <T> boolean isOnline(T stuff) {

        if (stuff == null)
            return false;

        final Context context;
        if (stuff instanceof Context)
            context = (Context) stuff;
        else if (stuff instanceof Fragment)
            context = ((Fragment) stuff).getActivity();
        else
            return false;

        final NetworkInfo networkInfo =
                ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
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
                return Optional.fromNullable(resultAfterWork);
            } catch (InterruptedException | UnknownHostException | NullPointerException e) {
                e.printStackTrace();
                return Optional.absent();
            } catch (GoogleJsonResponseException e) {
                if (e.getLocalizedMessage().contains("404"))
                    return Optional.absent();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Optional.absent();
    }

    public static String getMusicStorageKey(long serverId) {

        return serverId + "MUSIC";
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

        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {

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
                } catch (InterruptedException | UnknownHostException e) {
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static File compressImage(File image) throws IOException {

        // Decode just the boundaries
        final BitmapFactory.Options mBitmapOptions = new BitmapFactory.Options();
        mBitmapOptions.inJustDecodeBounds = true;

        final FileInputStream fileInputStream = new FileInputStream(image);
        final Bitmap temporary = BitmapFactory.decodeStream(fileInputStream, null, mBitmapOptions);
        if (temporary != null)
            temporary.recycle();

        // Calculate inSampleSize
        // Raw height and width of image
        final int height = mBitmapOptions.outHeight;
        final int width = mBitmapOptions.outWidth;
        final int sideLength = 800;
        closeQuietly(fileInputStream);

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
        mBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        mBitmapOptions.inJustDecodeBounds = false;
        mBitmapOptions.inScaled = true;
        mBitmapOptions.inDensity = inDensity;
        mBitmapOptions.inTargetDensity = inTargetDensity * mBitmapOptions.inSampleSize;

        final File tempFile = File.createTempFile("compressed_profile_photo", ".tmp");
        final RandomAccessFile accessFile = new RandomAccessFile(tempFile, "rws");
        accessFile.setLength(0);
        accessFile.close();

        final FileInputStream fInputStream = new FileInputStream(image);
        final FileOutputStream fileOutputStream = new FileOutputStream(tempFile);

        // will load & resize the image to be 1/inSampleSize dimensions
        Log.i("Ayush", "Starting compression");
        final Bitmap bitmap = BitmapFactory.decodeStream(fInputStream, null, mBitmapOptions);
        bitmap.compress(Bitmap.CompressFormat.WEBP, 80, fileOutputStream);
        fileOutputStream.flush();
        closeQuietly(fInputStream, fileOutputStream);
        Log.i("Ayush", "Result " + bitmap.getHeight() + " " + bitmap.getWidth());
        bitmap.recycle();

        //noinspection ResultOfMethodCallIgnored
        image.delete();
        Log.i("Ayush", "Returning image");
        return tempFile;
    }

    private static final StringBuffer buffer = new StringBuffer();
    private static final String baseURL = "http://ec2-52-24-99-153.us-west-2.compute.amazonaws.com:8080/getImage/small?";

    public synchronized static String getAlbumArt(String album, String artist, String song) throws UnsupportedEncodingException {

        buffer.setLength(0);
        buffer.append(baseURL);
        if (!TextUtils.isEmpty(album)) {

            buffer.append("album=").append(Uri.encode(album));
            if (!TextUtils.isEmpty(artist))
                buffer.append("&artist=").append(Uri.encode(artist));
            if (!TextUtils.isEmpty(song))
                buffer.append("&song=").append(Uri.encode(song));
        } else if (!TextUtils.isEmpty(artist)) {

            buffer.append("artist=").append(Uri.encode(artist));
            if (!TextUtils.isEmpty(song))
                buffer.append("&song=").append(Uri.encode(song));
        } else if (!TextUtils.isEmpty(song))
            buffer.append("song=").append(Uri.encode(song));

        final String toReturn = buffer.toString();
//        Log.i("Ayush", toReturn);
        return toReturn;
    }

    public synchronized static StartDownloadOperation startDownloadOperation(Context context,
                                                                             ReachDatabase reachDatabase,
                                                                             long receiverId,
                                                                             long senderId,
                                                                             long databaseId) {
        return new StartDownloadOperation(context, reachDatabase, receiverId, senderId, databaseId);
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

            return "CONNECT" + new Gson().toJson
                    (new Connection(
                            ////Constructing connection object
                            "REQ",
                            reachDatabase.getSenderId(),
                            reachDatabase.getReceiverId(),
                            reachDatabase.getSongId(),
                            reachDatabase.getProcessed(),
                            reachDatabase.getLength(),
                            UUID.randomUUID().getMostSignificantBits(),
                            UUID.randomUUID().getMostSignificantBits(),
                            reachDatabase.getLogicalClock(), ""));
        }

        private String fakeResponse(ReachDatabase reachDatabase) {

            return new Gson().toJson
                    (new Connection(
                            ////Constructing connection object
                            "RELAY",
                            reachDatabase.getSenderId(),
                            reachDatabase.getReceiverId(),
                            reachDatabase.getSongId(),
                            reachDatabase.getProcessed(),
                            reachDatabase.getLength(),
                            UUID.randomUUID().getMostSignificantBits(),
                            UUID.randomUUID().getMostSignificantBits(),
                            reachDatabase.getLogicalClock(), ""));
        }

        @Override
        public void run() {

            final MyBoolean myBoolean;
            if (reachDatabase.getSenderId() == StaticData.devika) {

                //hit cloud
                ProcessManager.submitNetworkRequest(contextReference.get(), fakeResponse(reachDatabase));
                myBoolean = new MyBoolean();
                myBoolean.setGcmexpired(false);
                myBoolean.setOtherGCMExpired(false);
            } else {
                //sending REQ to senderId
                myBoolean = sendGCM(generateRequest(reachDatabase), senderId, receiverId);
            }

            final short status;

            if (myBoolean == null) {
                Log.i("Ayush", "GCM sending resulted in shit");
                status = ReachDatabase.GCM_FAILED;
            } else if (myBoolean.getGcmexpired()) {

                //TODO test
//                final Context context = contextReference.get();
//                if(context == null)
//                    return;
//                final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
//                MiscUtils.updateGCM(SharedPrefUtils.getServerId(preferences), contextReference);
                Log.i("Ayush", "GCM re-registry needed");
                status = ReachDatabase.GCM_FAILED;
            } else if (myBoolean.getOtherGCMExpired()) {
                Log.i("Downloader", "SENDING GCM FAILED " + senderId);
                status = ReachDatabase.GCM_FAILED;
            } else {
                Log.i("Downloader", "GCM SENT " + senderId);
                status = ReachDatabase.NOT_WORKING;
            }

            final String condition = ReachDatabaseHelper.COLUMN_ID + " = ? and " +
                    ReachDatabaseHelper.COLUMN_STATUS + " != ?"; //operation should not be paused !
            final String[] arguments = new String[]{databaseId + "", ReachDatabase.PAUSED_BY_USER + ""};
            final ContentValues values = new ContentValues();
            values.put(ReachDatabaseHelper.COLUMN_STATUS, status);

            MiscUtils.useContextFromContext(contextReference, context -> {

                Log.i("Downloader", "Updating DB on GCM sent " + (context.getContentResolver().update(
                        Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + databaseId),
                        values, condition, arguments) > 0));
                return null;
            });
        }
        ////////
    }

    public synchronized static void checkChatToken(WeakReference<SharedPreferences> preferencesWeakReference,
                                                   WeakReference<Firebase> firebaseWeakReference) {

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
        final MyString fetchTokenFromServer = MiscUtils.autoRetry(() -> StaticData.userEndpoint.getChatToken(serverId).execute(), Optional.absent()).orNull();
        if (fetchTokenFromServer == null || TextUtils.isEmpty(fetchTokenFromServer.getString())) {
            //TODO track
            Log.i("Ayush", "Chat token check failed !");
        } else {

            final Firebase.AuthResultHandler authHandler = new Firebase.AuthResultHandler() {

                @Override
                public void onAuthenticationError(FirebaseError error) {
                    Log.e("Ayush", "Login Failed! " + error.getMessage());
                }

                @Override
                public void onAuthenticated(AuthData authData) {

                    final String chatUUID = authData.getUid();
                    //if found save
                    final SharedPreferences preferences = preferencesWeakReference.get();
                    if (preferences == null)
                        return;

                    SharedPrefUtils.storeChatUUID(preferences, fetchTokenFromServer.getString());
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
            };

            final Firebase firebase = firebaseWeakReference.get();
            if (firebase != null)
                firebase.authWithCustomToken(fetchTokenFromServer.getString(), authHandler);
        }
    }
}
