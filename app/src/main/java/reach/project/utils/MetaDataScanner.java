package reach.project.utils;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.appspot.able_door_616.userApi.UserApi;
import com.appspot.able_door_616.userApi.model.BulkMetaDataUpdate;
import com.appspot.able_door_616.userApi.model.SimpleApp;
import com.appspot.able_door_616.userApi.model.SimpleSong;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import reach.project.apps.App;
import reach.project.apps.AppCursorHelper;
import reach.project.core.StaticData;
import reach.project.music.ReachDatabase;
import reach.project.music.Song;
import reach.project.music.SongCursorHelper;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;

public class MetaDataScanner extends IntentService {

    @NonNull
    private static Map<String, Song> getCurrentSongs(ContentResolver resolver,
                                                     long serverId) {

        final Cursor reachDatabaseCursor = resolver.query(

                SongProvider.CONTENT_URI,
                SongCursorHelper.SONG_HELPER.getProjection(),
                "(" + SongHelper.COLUMN_OPERATION_KIND + " = ? and " + SongHelper.COLUMN_STATUS + " = ?) or " +
                        SongHelper.COLUMN_OPERATION_KIND + " = ?",
                new String[]{
                        ReachDatabase.OperationKind.DOWNLOAD_OP.getString(),
                        ReachDatabase.Status.FINISHED.getString(),
                        ReachDatabase.OperationKind.OWN.getString()},
                SongHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE");


        if (reachDatabaseCursor != null) {

            final Map<String, Song> toReturn = MiscUtils.getMap(reachDatabaseCursor.getCount());

            while (reachDatabaseCursor.moveToNext()) {

                //read the song object
                final Song song = SongCursorHelper.SONG_HELPER.parse(reachDatabaseCursor);
                //set the metaHash if not found
                if (!TextUtils.isEmpty(song.fileHash))
                    toReturn.put(song.fileHash, song);
            }
            reachDatabaseCursor.close();

            return toReturn;
        } else
            return Collections.emptyMap();
    }

    public MetaDataScanner() {
        super("MetaDataScanner");
    }

//    /**
//     * Saves the music data to cloud and locally
//     *
//     * @param myLibrary  songs from myLibrary
//     * @param downloaded downloaded songs
//     * @param apps       iterable of apps
//     * @param genres     all the genres
//     * @param first      true, if this force local DB update is needed
//     */
//    private static void saveMetaData(List<Song> myLibrary,
//                                     List<Song> downloaded,
//                                     List<App> apps,
//                                     Iterable<String> genres,
//                                     boolean first,
//                                     long serverId,
//                                     Context context) {
//
//        final List<Song> total = ImmutableList.copyOf(Iterables.unmodifiableIterable(Iterables.concat(myLibrary, downloaded))); //music
//
////        for (Song song : total)
////                Log.i("Ayush", song.displayName + " " + song.fileHash);
//
////        for (Song song : total)
////            Log.i("Downloader", "Found " + song.displayName + " " + song.songId);
//
//        ////////////////////Sync songs
//        final byte[] musicBlob = new MusicList.Builder()
//                .clientId(serverId)
//                .genres(ImmutableList.copyOf(genres)) //list view of hash set
//                .song(total) //concatenated list
//                .build().toByteArray();
//
//        //return false if same Music found !
//        boolean newMusic = true;
//        try {
//            newMusic = CloudStorageUtils.uploadMetaData(
//                    musicBlob,
//                    MiscUtils.getMusicStorageKey(serverId),
//                    context.getAssets().open("key.p12", AssetManager.ACCESS_RANDOM),
//                    CloudStorageUtils.MUSIC);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        final Cursor localChecker = context.getContentResolver().query(
//                MySongsProvider.CONTENT_URI,
//                new String[]{MySongsHelper.COLUMN_ID}, null, null, null);
//
//        final boolean noOldMusic = localChecker == null || localChecker.getCount() == 0;
//        if (localChecker != null)
//            localChecker.close();
//
//        if (newMusic || first || noOldMusic) {
//
//            //over-write music visibility
//            ScanMusic.createVisibilityMap(total, serverId);
//
//            //over-write local db
//            bulkInsertSongs(
//                    myLibrary, //sync up only myLibrary
//                    context.getContentResolver());
//        }
//
//        ////////////////////Sync Apps
//        final byte[] appBlob = new AppList.Builder()
//                .clientId(serverId)
//                .app(apps).build().toByteArray();
//
//        //return false if same app found !
//        boolean newApps = true;
//        try {
//            newApps = CloudStorageUtils.uploadMetaData(
//                    appBlob,
//                    MiscUtils.getAppStorageKey(serverId),
//                    context.getAssets().open("key.p12"),
//                    CloudStorageUtils.APP);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        if (newApps || first) {
//
//            //over-write app visibility
//            ScanApps.createVisibilityMap(apps, serverId);
//        }
//    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.i("Ayush", "Starting Scan");

        final SharedPreferences sharedPreferences = getSharedPreferences("Reach", MODE_PRIVATE);
        final ContentResolver contentResolver = getContentResolver();
        final PackageManager packageManager = getPackageManager();
        final long serverId = SharedPrefUtils.getServerId(sharedPreferences);

        if (serverId == 0)
            return;

        //get installedApps
        final List<ApplicationInfo> installedApps = MiscUtils.getInstalledApps(packageManager);
        //get song cursor
        final Cursor musicCursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                SongCursorHelper.ANDROID_SONG_HELPER.getProjection(), null, null, null);

        //genres get filled here
        final Set<String> genres = MiscUtils.getSet(100);

        //get the deviceSongs
        final List<Song> deviceSongs = SongCursorHelper.getSongs(
                musicCursor,
                serverId,
                contentResolver,
                genres);

        final Map<String, Song> currentSongs = getCurrentSongs(contentResolver, serverId);
        final ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        for (Song song : deviceSongs)
            if (!currentSongs.containsKey(song.fileHash)) {

                Log.i("Ayush", "Found new song " + song.displayName);
                final ContentValues contentValues = SongHelper.contentValuesCreator(song, serverId);
                final ContentProviderOperation insert = ContentProviderOperation
                        .newInsert(SongProvider.CONTENT_URI)
                        .withValues(contentValues)
                        .build();
                operations.add(insert);
                currentSongs.put(song.fileHash, song);
            }

        //insert new songs into DB
        try {
            if (operations.size() > 0)
                contentResolver.applyBatch(SongProvider.AUTHORITY, operations);
        } catch (RemoteException | OperationApplicationException ignored) {
        }

        //get current visible apps
        final Map<String, Boolean> packageVisibility = SharedPrefUtils.getPackageVisibilities(sharedPreferences);
        final Set<Map.Entry<String, Boolean>> packageEntries = packageVisibility.entrySet();
        final Set<String> visiblePackages = MiscUtils.getSet(50);
        for (Map.Entry<String, Boolean> entry : packageEntries)
            if (entry.getValue())
                visiblePackages.add(entry.getKey());


        //get the deviceApps
        final List<App> deviceApps = AppCursorHelper.getApps(
                installedApps,
                packageManager,
                visiblePackages);

        final List<SimpleSong> simpleSongs = new ArrayList<>(currentSongs.size());
        final List<SimpleApp> simpleApps = new ArrayList<>(deviceApps.size());

        final Set<Map.Entry<String, Song>> entries = currentSongs.entrySet();
        for (Map.Entry<String, Song> entry : entries)
            simpleSongs.add(new Song.Builder(entry.getValue()).buildSimple());

        for (App app : deviceApps)
            simpleApps.add(new App.Builder(app).buildSimple());

        final BulkMetaDataUpdate bulkMetaDataUpdate = new BulkMetaDataUpdate();
        bulkMetaDataUpdate.setGenres(ImmutableList.copyOf(genres));
        bulkMetaDataUpdate.setSimpleApps(simpleApps);
        bulkMetaDataUpdate.setSimpleSongs(simpleSongs);

        Log.i("Ayush", "BulkMetaDataUpdate " + simpleApps.size() + " apps and " + simpleSongs.size() + " songs");

        final HttpTransport transport = new NetHttpTransport();
        final JsonFactory factory = new JacksonFactory();
        final GoogleAccountCredential credential = GoogleAccountCredential
                .usingAudience(this, StaticData.SCOPE)
                .setSelectedAccountName(SharedPrefUtils.getEmailId(sharedPreferences));
        Log.d("CodeVerification", credential.getSelectedAccountName());

        final UserApi userApi = CloudEndPointsUtils.updateBuilder(new UserApi.Builder(transport, factory, credential))
                .setRootUrl("https://1-dot-client-module-dot-able-door-616.appspot.com/_ah/api/").build();

        try {
            userApi.updateMetaData(serverId, bulkMetaDataUpdate).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        SharedPrefUtils.storeGenres(sharedPreferences, genres);
    }
}