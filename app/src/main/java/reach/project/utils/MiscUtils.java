package reach.project.utils;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.gson.Gson;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import reach.backend.entities.messaging.model.MyBoolean;
import reach.backend.entities.userApi.model.ReachPlayList;
import reach.backend.entities.userApi.model.ReachSong;
import reach.project.core.StaticData;
import reach.project.database.ReachAlbum;
import reach.project.database.ReachArtist;
import reach.project.database.ReachDatabase;
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

/**
 * Created by dexter on 1/10/14.
 */

//TODO fix nonsense string wraps
public enum MiscUtils {
    ;

    public static String combinationFormatter(final long millis) {

        final long seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
                - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        final long hours = TimeUnit.MILLISECONDS.toHours(millis);

        final String toSend = (hours == 0 ? "00" : hours < 10 ? hours : hours) + ":" +
                (minutes == 0 ? "00" : minutes < 10 ? minutes : minutes) + ":" +
                (seconds == 0 ? "00" : seconds < 10 ? seconds : seconds);

        return toSend.startsWith("00:") ? toSend.substring(3) : toSend;
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
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

    public static String generateInitials(String name) {

        try {
            final String[] splitter = name.trim().split(" ");
            if (splitter.length > 1)
                return (String.valueOf(splitter[0].charAt(0)) + splitter[1].charAt(0)).toUpperCase();
            else
                return String.valueOf(splitter[0].charAt(0)).toUpperCase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "A";
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

    public static void bulkInsertSongs(Set<ReachSong> reachSongDatabases,
                                       Collection<ReachAlbum> reachAlbums,
                                       Collection<ReachArtist> reachArtists,
                                       ContentResolver contentResolver) {

        //Add all songs
        final ContentValues[] songs = new ContentValues[reachSongDatabases.size()];
        final ContentValues[] albums = new ContentValues[reachAlbums.size()];
        final ContentValues[] artists = new ContentValues[reachArtists.size()];

        int i = 0;
        for (ReachSong reachSongDatabase : reachSongDatabases) {
            songs[i++] = ReachSongHelper.contentValuesCreator(reachSongDatabase);
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

    public static void bulkInsertPlayLists(Set<ReachPlayList> reachPlayListDatabases,
                                           ContentResolver contentResolver) {

        if (reachPlayListDatabases != null && !reachPlayListDatabases.isEmpty()) {

            final ContentValues[] playLists = new ContentValues[reachPlayListDatabases.size()];
            int i = 0;
            for (ReachPlayList reachPlayListDatabase : reachPlayListDatabases) {
                playLists[i++] = ReachPlayListHelper.contentValuesCreator(reachPlayListDatabase);
            }
            Log.i("Ayush", "PlayLists Inserted " + contentResolver.bulkInsert(ReachPlayListProvider.CONTENT_URI, playLists));
        } else Log.i("Ayush", "NO playLists to save");
    }

    public static Pair<Collection<ReachAlbum>, Collection<ReachArtist>> getAlbumsAndArtists
            (Iterable<ReachSong> reachSongs) {

        final Map<String, ReachAlbum> reachAlbumDatabaseHashMap = new HashMap<>();
        final Map<String, ReachArtist> reachArtistDatabaseHashMap = new HashMap<>();

        for (ReachSong reachSong : reachSongs) {

            //don't consider invisible files
            if(reachSong.getVisibility() == 0)
                continue;

            if (!TextUtils.isEmpty(reachSong.getAlbum())) {

                final ReachAlbum reachAlbum;

                if (reachAlbumDatabaseHashMap.containsKey(reachSong.getAlbum()))
                    reachAlbum = reachAlbumDatabaseHashMap.get(reachSong.getAlbum());
                else {
                    reachAlbum = new ReachAlbum();
                    reachAlbum.setAlbumName(reachSong.getAlbum());
                    reachAlbum.setUserId(reachSong.getUserId());
                    reachAlbum.setArtist(reachSong.getArtist());
                }
                reachAlbum.incrementSize();
                reachAlbumDatabaseHashMap.put(reachAlbum.getAlbumName(), reachAlbum);
            }

            if (reachSong.getArtist() != null && !reachSong.getArtist().equals("")) {

                final ReachArtist reachArtist;

                if (reachArtistDatabaseHashMap.containsKey(reachSong.getArtist()))
                    reachArtist = reachArtistDatabaseHashMap.get(reachSong.getArtist());
                else {
                    reachArtist = new ReachArtist();
                    reachArtist.setArtistName(reachSong.getArtist());
                    reachArtist.setUserID(reachSong.getUserId());
                    reachArtist.setAlbum(reachSong.getAlbum());
                }
                reachArtist.incrementSize();
                reachArtistDatabaseHashMap.put(reachArtist.getArtistName(), reachArtist);
            }
        }
        ///////////////////////
        return new Pair<>(reachAlbumDatabaseHashMap.values(),
                reachArtistDatabaseHashMap.values());
    }

    public static void keyCleanUp(SelectionKey selectionKey) {

        if (selectionKey == null || !selectionKey.isValid())
            return;
        Log.i("Downloader", "Running cleanUp");
        try {
            if (selectionKey.channel() != null)
                selectionKey.channel().close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            selectionKey.cancel();
        }
    }

    public static ContentProviderOperation getUpdateOperation(ContentValues contentValues, long id) {
        return ContentProviderOperation
                .newUpdate(Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + id))
                .withValues(contentValues)
                .withSelection(ReachDatabaseHelper.COLUMN_ID + " = ? and " + ReachDatabaseHelper.COLUMN_STATUS + " != ?", new String[]{id + "", "" + ReachDatabase.PAUSED_BY_USER})
                .build();
    }

    public synchronized static StartDownloadOperation startDownloadOperation(ReachDatabase reachDatabase, ContentResolver contentResolver) {
        return new StartDownloadOperation(reachDatabase, contentResolver);
    }

    public synchronized static Runnable startBulkDownloadOperation(List<ReachDatabase> reachDatabases, ContentResolver contentResolver) {
        return new StartBulkDownloadOperation(reachDatabases, contentResolver);
    }

    public static MyBoolean sendGCM(final String message, final long hostId, final long clientId) {

        return MiscUtils.autoRetry(new DoWork<MyBoolean>() {
            @Override
            protected MyBoolean doWork() throws IOException {
                Log.i("Downloader", "Sending message " + message);
                return StaticData.messagingEndpoint.messagingEndpoint()
                        .sendMessage(message, hostId, clientId).execute();
            }
        }, Optional.<Predicate<MyBoolean>>of(new Predicate<MyBoolean>() {
            @Override
            public boolean apply(@Nullable MyBoolean input) {
                return input == null;
            }
        })).orNull();
    }

    public static File getReachDirectory() {

        final File file = new File(Environment.getExternalStorageDirectory(), ".Reach");
        if (file.isDirectory()) {
            Log.i("Downloader", "Using getExternalStorageDirectory");
            return file;
        } else if (file.mkdir()) {
            Log.i("Downloader", "Creating and using getExternalStorageDirectory");
            return file;
        }
        return null;
    }

    public static long stringToLongHashCode(String string) {

        long h = 1125899906842597L; // prime
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = 31 * h + string.charAt(i);
        }
        return h;
    }

//    public static int getReachDatabaseCount(ContentResolver contentResolver) {
//
//        final Cursor countCursor;
//        countCursor = contentResolver.query(
//                ReachDatabaseProvider.CONTENT_URI,
//                new String[]{ReachDatabaseHelper.COLUMN_ID},
//                null, null, null);
//        if(countCursor == null)
//            return 0;
//        try {
//            return countCursor.getCount();
//        } finally {
//            countCursor.close();
//        }
//    }
//
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

    public static boolean updateGCM(final long id, final WeakReference reference) {

        final String regId = autoRetry(new DoWork<String>() {
            @Override
            protected String doWork() throws IOException {

                final Context context = (Context) reference.get();
                if (context == null)
                    return "QUIT";
                return GoogleCloudMessaging.getInstance(context)
                        .register("528178870551");
            }
        }, Optional.<Predicate<String>>of(new Predicate<String>() {
            @Override
            public boolean apply(@Nullable String input) {
                return TextUtils.isEmpty(input);
            }
        })).orNull();

        if (TextUtils.isEmpty(regId) || regId.equals("QUIT"))
            return false;
        //if everything is fine, send to server
        Log.i("Ayush", "Uploading newGcmId to server");
        final Boolean result = autoRetry(new DoWork<Boolean>() {
            @Override
            protected Boolean doWork() throws IOException {

                StaticData.userEndpoint.setGCMId(id, regId).execute();
                Log.i("Ayush", regId.substring(0, 5) + "NEW GCM ID AFTER CHECK");
                return true;
            }
        }, Optional.<Predicate<Boolean>>absent()).orNull();
        //set locally
        return !(result == null || !result);
    }

    /**
     * Performs a task, retries upon failure with exponential back-off.
     * Kindly don't use on UI thread.
     *
     * @param <T>       the return type of task
     * @param task      the task that needs to be performed
     * @param predicate the extra condition for failure
     * @return the result/output of performing the task
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Optional.absent();
    }

    public static String getMusicStorageKey(long serverId) {

        return serverId + "MUSIC";
    }

    public static String getHistoryStoragekey(long serverId) {
        return serverId + "HISTORY";
    }

    /**
     * Performs a task, retries upon failure with exponential back-off.
     * This is to be used if returned value is of no importance other than checking for failure.
     * Automatically delegates to separate thread.
     *
     * @param task      the task that needs to be performed
     * @param predicate the extra condition for failure
     * @param <T>       the return type of task
     */
    public static <T> void autoRetryAsync(@NonNull final DoWork<T> task,
                                          @NonNull final Optional<Predicate<T>> predicate) {

        StaticData.threadPool.submit(new Runnable() {
            @Override
            public void run() {
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
            }
        });
    }

    private static class StartBulkDownloadOperation implements Runnable {

        private final List<ReachDatabase> reachDatabases;
        private final ContentResolver contentResolver;

        private StartBulkDownloadOperation(List<ReachDatabase> reachDatabases, ContentResolver contentResolver) {
            this.reachDatabases = reachDatabases;
            this.contentResolver = contentResolver;
        }

        @Override
        public void run() {

            final ArrayList<ContentProviderOperation> operations =
                    new ArrayList<>();

            for (ReachDatabase reachDatabase : reachDatabases) {

                final ContentValues values = new ContentValues();
                if (reachDatabase.getProcessed() >= reachDatabase.getLength()) {

                    values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.FINISHED);
                    operations.add(getUpdateOperation(values, reachDatabase.getId()));
                    continue;
                }

                final String message = "CONNECT" + new Gson().toJson
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

                final MyBoolean myBoolean = sendGCM(message, reachDatabase.getSenderId(), reachDatabase.getReceiverId());
                if (myBoolean == null) {
                    Log.i("Ayush", "GCM sending resulted in shit");
                    values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.GCM_FAILED);
                } else if (myBoolean.getGcmexpired()) {
                    Log.i("Ayush", "GCM re-registry needed");
                    values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.GCM_FAILED);
                } else if (myBoolean.getOtherGCMExpired()) {
                    Log.i("Downloader", "SENDING GCM FAILED " + reachDatabase.getSenderId());
                    values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.GCM_FAILED);
                } else {
                    Log.i("Downloader", "GCM SENT " + reachDatabase.getSenderId());
                    values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.NOT_WORKING);
                }
                operations.add(getUpdateOperation(values, reachDatabase.getId()));
            }
            try {
                Log.i("Downloader", "Starting Download op " + operations.size());
                contentResolver.applyBatch(ReachDatabaseProvider.AUTHORITY, operations);
            } catch (RemoteException | OperationApplicationException e) {
                e.printStackTrace();
            }
        }
    }

    private static class StartDownloadOperation implements Runnable {

        private final ReachDatabase reachDatabase;
        private final ContentResolver contentResolver;

        private StartDownloadOperation(ReachDatabase reachDatabase, ContentResolver contentResolver) {
            this.reachDatabase = reachDatabase;
            this.contentResolver = contentResolver;
        }

        @Override
        public void run() {

            final String message = "CONNECT" + new Gson().toJson
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

            final MyBoolean myBoolean = sendGCM(message, reachDatabase.getSenderId(), reachDatabase.getReceiverId());
            final ContentValues values = new ContentValues();
            values.put(ReachDatabaseHelper.COLUMN_LOGICAL_CLOCK, reachDatabase.getLogicalClock());

            if (myBoolean == null) {
                Log.i("Ayush", "GCM sending resulted in shit");
                values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.GCM_FAILED);
            } else if (myBoolean.getGcmexpired()) {
                Log.i("Ayush", "GCM re-registry needed");
                values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.GCM_FAILED);
            } else if (myBoolean.getOtherGCMExpired()) {
                Log.i("Downloader", "SENDING GCM FAILED " + reachDatabase.getSenderId());
                values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.GCM_FAILED);
            } else {
                Log.i("Downloader", "GCM SENT " + reachDatabase.getSenderId());
                values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.NOT_WORKING);
            }
            Log.i("Downloader", "Updating DB on GCM sent " + contentResolver.update(
                    Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + reachDatabase.getId()),
                    values,
                    ReachDatabaseHelper.COLUMN_ID + " = ?",
                    new String[]{reachDatabase.getId() + ""}));
        }
    }
}
