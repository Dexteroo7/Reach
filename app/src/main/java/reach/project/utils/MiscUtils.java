package reach.project.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.util.SimpleArrayMap;
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

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.gson.Gson;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import reach.backend.entities.messaging.model.MyBoolean;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachAlbumProvider;
import reach.project.database.contentProvider.ReachArtistProvider;
import reach.project.database.contentProvider.ReachDatabaseProvider;
import reach.project.database.contentProvider.ReachPlayListProvider;
import reach.project.database.contentProvider.ReachSongProvider;
import reach.project.database.sql.ReachAlbumHelper;
import reach.project.database.sql.ReachArtistHelper;
import reach.project.database.sql.ReachDatabaseHelper;
import reach.project.database.sql.ReachPlayListHelper;
import reach.project.database.sql.ReachSongHelper;
import reach.project.reachProcess.auxiliaryClasses.Connection;
import reach.project.reachProcess.reachService.ProcessManager;
import reach.project.utils.auxiliaryClasses.DoWork;
import reach.project.utils.auxiliaryClasses.Playlist;
import reach.project.utils.auxiliaryClasses.ReachAlbum;
import reach.project.utils.auxiliaryClasses.ReachArtist;
import reach.project.utils.auxiliaryClasses.ReachDatabase;
import reach.project.utils.auxiliaryClasses.Song;
import reach.project.utils.auxiliaryClasses.UseContext;
import reach.project.utils.auxiliaryClasses.UseFragment;

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

    public static String dateFormatter(final long seconds) {

        return new SimpleDateFormat("dd-MM-yyyy HH-mm-ss", Locale.getDefault()).format(
                new Date(seconds));
    }

    public static void closeAndIgnore(Collection... collections) {
        if (collections == null || collections.length == 0)
            return;
        for (Collection collection : collections)
            if (collection != null)
                collection.clear();
    }

    public static void closeAndIgnore(Closeable... closeables) {
        for (Closeable closeable : closeables)
            if (closeable != null)
                try {
                    closeable.close();
                } catch (IOException ignored) {
                }
    }

    public static void closeAndIgnore(Closeable closeable) {
        if (closeable != null)
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
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

    public static void setEmptyTextforListView(ListView listView, String emptyText) {

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
                                       Set<ReachAlbum> reachAlbums,
                                       Set<ReachArtist> reachArtists,
                                       ContentResolver contentResolver,
                                       long serverId) {

        //Add all songs
        final ContentValues[] songs = new ContentValues[songList.size()];
        final ContentValues[] albums = new ContentValues[reachAlbums.size()];
        final ContentValues[] artists = new ContentValues[reachArtists.size()];

        int i = 0;
        for (Song song : songList) {
            songs[i++] = ReachSongHelper.contentValuesCreator(song, serverId);
        }
        i = 0;
        Log.i("Ayush", "Songs Inserted " + contentResolver.bulkInsert(ReachSongProvider.CONTENT_URI, songs));

        if (reachAlbums.size() > 0) {
            for (ReachAlbum reachAlbum : reachAlbums)
                albums[i++] = ReachAlbumHelper.contentValuesCreator(reachAlbum);
            Log.i("Ayush", "Albums Inserted " + contentResolver.bulkInsert(ReachAlbumProvider.CONTENT_URI, albums));
            i = 0;
        }
        if (reachArtists.size() > 0) {
            for (ReachArtist reachArtist : reachArtists)
                artists[i++] = ReachArtistHelper.contentValuesCreator(reachArtist);
            Log.i("Ayush", "Artists Inserted " + contentResolver.bulkInsert(ReachArtistProvider.CONTENT_URI, artists));
        }
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
        } else Log.i("Ayush", "NO playLists to save");
    }

    public static Pair<Set<ReachAlbum>, Set<ReachArtist>> getAlbumsAndArtists(Iterable<Song> songs, long serverId) {

        final Set<ReachAlbum> albums = new HashSet<>();
        final Set<ReachArtist> artists = new HashSet<>();
        final SimpleArrayMap<String, ReachAlbum> albumMap = new SimpleArrayMap<>();
        final SimpleArrayMap<String, ReachArtist> artistMap = new SimpleArrayMap<>();

        for (Song song : songs) {

            //don't consider invisible files
            if (!song.visibility)
                continue;

            if (!TextUtils.isEmpty(song.album)) {

                ReachAlbum album = albumMap.get(song.album);
                if (album == null)
                    albums.add(albumMap.put(song.album, album = new ReachAlbum()));
                else {
                    album.setAlbumName(song.album);
                    album.setUserId(serverId);
                    album.setArtist(song.artist);
                }
                album.incrementSize();
            }

            if (!TextUtils.isEmpty(song.artist)) {

                ReachArtist artist = artistMap.get(song.artist);
                if (artist == null)
                    artists.add(artistMap.put(song.artist, artist = new ReachArtist()));
                else {
                    artist.setArtistName(song.artist);
                    artist.setUserID(serverId);
                    artist.setAlbum(song.album);
                }
                artist.incrementSize();
            }
        }
        ///////////////////////
        return new Pair<>(albums, artists);
    }

    public static MyBoolean sendGCM(final String message, final long hostId, final long clientId) {

        return MiscUtils.autoRetry(() -> {
            Log.i("Downloader", "Sending message " + message);
            return StaticData.messagingEndpoint.sendMessage(message, hostId, clientId).execute();
        }, Optional.<Predicate<MyBoolean>>of(input -> input == null)).orNull();
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

//    public static <T extends Context, Result> Optional<Result> useContextFromContext(final WeakReference<T> reference,
//                                                                          final UseContext<Result, T>... tasks) {
//
//        final boolean isActivity = reference.get() instanceof Activity;
//
//        Optional<Result> result = Optional.absent();
//        for (UseContext<Result, T> task : tasks) {
//
//            final T context;
//
//            final boolean contextLost = (context = reference.get()) == null;
//            if (contextLost)
//                return Optional.absent();
//            final boolean activityIsFinishing = (isActivity && ((Activity) context).isFinishing());
//            if (activityIsFinishing)
//                return Optional.absent();
//
//            result = Optional.fromNullable(task.work(context));
//        }
//
//        return result;
//    }

    public static <T extends Context, Result> Optional<Result> useContextFromContext(final WeakReference<T> reference,
                                                                                     final UseContext<Result, T> task) {

        final T context;
        if (reference == null || (context = reference.get()) == null)
            return Optional.absent();

        final boolean isActivity = context instanceof Activity;
        final boolean activityIsFinishing = (isActivity && ((Activity) context).isFinishing());
        if (activityIsFinishing)
            return Optional.absent();

        return Optional.fromNullable(task.work(context));
    }

    public static <T extends Fragment, Result> Optional<Result> useContextFromFragment(final WeakReference<T> reference,
                                                                                       final UseContext<Result, Activity> task) {

        final T fragment;
        if (reference == null || (fragment = reference.get()) == null)
            return Optional.absent();

        final Activity activity = fragment.getActivity();
        if (activity == null || activity.isFinishing())
            return Optional.absent();

        return Optional.fromNullable(task.work(activity));
    }

    public static <T extends Fragment, Result> Optional<Result> useFragment(final WeakReference<T> reference,
                                                                            final UseFragment<Result, T> task) {

        final T fragment;
        if (reference == null || (fragment = reference.get()) == null)
            return Optional.absent();

        final Activity activity = fragment.getActivity();
        if (activity == null || activity.isFinishing())
            return Optional.absent();

        return Optional.fromNullable(task.work(fragment));
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
//        final Activity activity = fragment.getActivity();
//        if (activity.isFinishing())
//            return;
//
//        activity.runOnUiThread(() -> task.work(activity));
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
        }, Optional.<Predicate<String>>of(TextUtils::isEmpty)).orNull();

        if (TextUtils.isEmpty(regId) || regId.equals("QUIT"))
            return false;
        //if everything is fine, send to server
        Log.i("Ayush", "Uploading newGcmId to server");
        final Boolean result = autoRetry(() -> {

            StaticData.userEndpoint.setGCMId(id, regId).execute();
            Log.i("Ayush", regId.substring(0, 5) + "NEW GCM ID AFTER CHECK");
            return true;
        }, Optional.<Predicate<Boolean>>absent()).orNull();
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
//                final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
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
    }
}
