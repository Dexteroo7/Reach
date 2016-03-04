package reach.project.utils;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import reach.project.apps.App;
import reach.project.apps.AppCursorHelper;
import reach.project.music.MySongsHelper;
import reach.project.music.MySongsProvider;
import reach.project.music.ReachDatabase;
import reach.project.music.Song;
import reach.project.music.SongCursorHelper;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;

public class MetaDataScanner extends IntentService {


    @NonNull
    private static List<Song> getCurrentSongs(ContentResolver resolver,
                                              long serverId) {

        final Cursor reachDatabaseCursor = resolver.query(
                SongProvider.CONTENT_URI,
                SongCursorHelper.SONG_HELPER.getProjection(),
                SongHelper.COLUMN_OPERATION_KIND + " = ? and " +
                        SongHelper.COLUMN_STATUS + " = ?",
                new String[]{
                        ReachDatabase.OperationKind.DOWNLOAD_OP.getString(),
                        ReachDatabase.Status.FINISHED.getString()}, null);


        if (reachDatabaseCursor != null) {

            final List<Song.Builder> toReturn = new ArrayList<>(reachDatabaseCursor.getCount());
            final Function<Cursor, Song.Builder> parser = SongCursorHelper.SONG_HELPER.getParser();
            final Function<Song.Builder, Song.Builder> songHashFixer = SongCursorHelper.getSongHashFixer(serverId);

            while (reachDatabaseCursor.moveToNext()) {

                //read the song object
                Song.Builder songBuilder = parser.apply(reachDatabaseCursor);
                //set the metaHash if not found
                songBuilder = songHashFixer.apply(songBuilder);
                if (songBuilder != null)
                    toReturn.add(songBuilder);
            }
            reachDatabaseCursor.close();

            //build and return
            return FluentIterable.from(toReturn)
                    .transform(SongCursorHelper.SONG_BUILDER)
                    .filter(Predicates.notNull()).toList();
        } else
            return Collections.emptyList();
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

    private static void bulkInsertSongs(Collection<Song> reachSongs,
                                        ContentResolver contentResolver) {

        //Add all songs
        final ContentValues[] songs = new ContentValues[reachSongs.size()];

        int i = 0;
        if (reachSongs.size() > 0) {

            for (Song song : reachSongs)
                songs[i++] = MySongsHelper.contentValuesCreator(song);
            i = 0; //reset counter
            Log.i("Ayush", "Songs Inserted " + contentResolver.bulkInsert(MySongsProvider.CONTENT_URI, songs));
        } else
            contentResolver.delete(MySongsProvider.CONTENT_URI, null, null);
    }

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

        //get the song builders
        final List<Song.Builder> songBuilders = SongCursorHelper.getSongs(
                musicCursor,
                Collections.emptyMap(),
                serverId,
                contentResolver,
                genres,
                message -> {//ignored
                });

        //get the current songs
        final List<Song> currentSongs = getCurrentSongs(contentResolver, serverId);

        //get the app builders
        final List<App.Builder> appBuilders = AppCursorHelper.getApps(
                installedApps,
                packageManager,
                message -> { //ignored
                },
                Collections.emptyMap());

        //get current app package visibilities
        final Map<String, Boolean> packageVisibility = SharedPrefUtils.getPackageVisibilities(sharedPreferences);

        //TODO apply app visibility
        //TODO add new songs to final list

//        saveMetaData(
//                songs, //myLibrary music
//                downloaded, //downloaded music
//                appList, //apps
//                ScanMusic.GENRE_HASH_SET, //genres
//                intent.getBooleanExtra("first", true),
//                serverId,
//                this);

        //locally store the genres
        SharedPrefUtils.storeGenres(sharedPreferences, genres);
    }
}