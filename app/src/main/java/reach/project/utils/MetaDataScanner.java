package reach.project.utils;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Environment;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.squareup.wire.Wire;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import reach.backend.applications.appVisibilityApi.model.AppVisibility;
import reach.backend.applications.classifiedAppsApi.model.StringList;
import reach.backend.music.musicVisibilityApi.model.JsonMap;
import reach.backend.music.musicVisibilityApi.model.MusicData;
import reach.project.apps.App;
import reach.project.apps.AppList;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.music.AlbumArtData;
import reach.project.music.MusicList;
import reach.project.music.MySongsHelper;
import reach.project.music.MySongsProvider;
import reach.project.music.Song;

public class MetaDataScanner extends IntentService {

    private static final String[] projection = {
            MediaStore.Audio.Media._ID, //0
            MediaStore.Audio.Media.IS_MUSIC, //1
            MediaStore.Audio.Media.DATA, //2
            MediaStore.Audio.Media.TITLE, //3
            MediaStore.Audio.Media.DISPLAY_NAME, //4
            MediaStore.Audio.Media.SIZE, //5
            MediaStore.Audio.Media.ARTIST, //6
            MediaStore.Audio.Media.DURATION, //7
            MediaStore.Audio.Media.ALBUM, //8
            MediaStore.Audio.Media.YEAR, //9
            MediaStore.Audio.Media.DATE_ADDED}; //10

    private static final String[] reachDatabaseProjection = {
            ReachDatabaseHelper.COLUMN_UNIQUE_ID, //0
            ReachDatabaseHelper.COLUMN_ALBUM_ART_DATA, //1
            ReachDatabaseHelper.COLUMN_DISPLAY_NAME, //2
            ReachDatabaseHelper.COLUMN_ACTUAL_NAME, //3
            ReachDatabaseHelper.COLUMN_ARTIST, //4
            ReachDatabaseHelper.COLUMN_ALBUM, //5
            ReachDatabaseHelper.COLUMN_DURATION,//6
            ReachDatabaseHelper.COLUMN_SIZE, //7
            ReachDatabaseHelper.COLUMN_GENRE, //8
            ReachDatabaseHelper.COLUMN_PATH, //9
            ReachDatabaseHelper.COLUMN_DATE_ADDED, //10
            ReachDatabaseHelper.COLUMN_VISIBILITY //11
    };

    private enum ScanMusic {

        ;

        //genre collection holder
        private static final HashSet<String> genreHashSet = new HashSet<>(50);

        @NonNull
        private static List<Song> getSongListing(ContentResolver resolver, Cursor musicCursor, long serverId) {

            if (musicCursor == null)
                return Collections.emptyList();
            ////////////////////persistence holder use songId as key !

            final Cursor reachSongInitialCursor = resolver.query(
                    MySongsProvider.CONTENT_URI,
                    new String[]{
                            MySongsHelper.COLUMN_SONG_ID, //0
                            MySongsHelper.COLUMN_VISIBILITY, //1
                            MySongsHelper.COLUMN_IS_LIKED},
                    null, null, null);


            LongSparseArray<SongPersist> songPersist = null;
            //try local first
            if (reachSongInitialCursor != null) {

                songPersist = new LongSparseArray<>(reachSongInitialCursor.getCount());
                while (reachSongInitialCursor.moveToNext()) {
                    //songId = key, visibility = value;
                    final SongPersist persist = new SongPersist();
                    persist.visibility = reachSongInitialCursor.getShort(1) == 1;
                    persist.liked = reachSongInitialCursor.getShort(2) == 1;

                    songPersist.append(
                            reachSongInitialCursor.getLong(0),  //songId
                            persist); //visibility
                }
                reachSongInitialCursor.close();
            }

            //if no visibility found in table look in cloud
            if (songPersist == null || songPersist.size() == 0) {

                Log.i("Ayush", "Fetching visibility data");
                //fetch visibility data
                final MusicData visibility = MiscUtils.autoRetry(() -> StaticData.MUSIC_VISIBILITY_API.get(serverId).execute(), Optional.absent()).orNull();
                final JsonMap visibilityMap;
                if (visibility == null || (visibilityMap = visibility.getVisibility()) == null || visibilityMap.isEmpty())
                    Log.i("Ayush", "no visibility data found on cloud");
                else {

                    songPersist = new LongSparseArray<>(visibilityMap.size());
                    //cloud visibility found
                    for (Map.Entry<String, Object> objectEntry : visibilityMap.entrySet()) {

                        if (objectEntry == null) {
                            //TODO track
                            Log.i("Ayush", "objectEntry was null");
                            continue;
                        }

                        final String key = objectEntry.getKey();
                        final Object value = objectEntry.getValue();

                        if (TextUtils.isEmpty(key) || !TextUtils.isDigitsOnly(key) || value == null || !(value instanceof Boolean)) {
                            //TODO track
                            Log.i("Ayush", "Found shit data inside visibilityMap " + key + " " + value);
                            continue;
                        }

                        //persist data found
                        final SongPersist persist = new SongPersist();
                        persist.visibility = (Boolean) value;
                        persist.liked = false; //TODO persist later

                        songPersist.append(
                                Long.parseLong(key),  //songId
                                persist); //visibility
                    }
                    visibilityMap.clear();
                }
            }

            ////////////////////Loading new songs
            final List<Song> toSend = new ArrayList<>(musicCursor.getCount());
            final HashSet<String> verifiedMusicPaths = new HashSet<>();
            final File[] directories = new File[]{
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            };

            for (File directory : directories)
                if (directory.exists())
                    verifiedMusicPaths.addAll(recurseDirectory(directory));

            while (musicCursor.moveToNext()) {

                int music_column_name = musicCursor
                        .getColumnIndex(MediaStore.Audio.Media.TITLE);
                int music_column_actual_name = musicCursor
                        .getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
                int music_column_id = musicCursor
                        .getColumnIndex(MediaStore.Audio.Media._ID);
                int is_music = musicCursor
                        .getColumnIndex(MediaStore.Audio.Media.IS_MUSIC);
                int music_column_size = musicCursor
                        .getColumnIndex(MediaStore.Audio.Media.SIZE);
                int music_column_duration = musicCursor
                        .getColumnIndex(MediaStore.Audio.Media.DURATION);
                int music_column_file = musicCursor
                        .getColumnIndex(MediaStore.Audio.Media.DATA);

                if (music_column_name == -1 || is_music == -1 ||
                        music_column_id == -1 || music_column_actual_name == -1 ||
                        music_column_size == -1 || music_column_duration == -1 || music_column_file == -1)
                    continue;
                int isMusic = musicCursor.getInt(is_music);
                if (isMusic == 0)
                    continue; //skip non-music files

                int music_column_artist = musicCursor
                        .getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int music_column_album = musicCursor
                        .getColumnIndex(MediaStore.Audio.Media.ALBUM);
                int music_column_year = musicCursor
                        .getColumnIndex(MediaStore.Audio.Media.YEAR);
                int music_date_added = musicCursor
                        .getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);

                final Song.Builder builder = new Song.Builder();
                final long songID = musicCursor.getLong(music_column_id);
                builder.songId(songID);

                final long size = musicCursor.getLong(music_column_size);
                final long duration = musicCursor.getLong(music_column_duration);

                if (size == 0 || duration == 0)
                    continue;

                builder.size(size);
                builder.duration(duration);

                String unverifiedPath = musicCursor.getString(music_column_file);
                if (TextUtils.isEmpty(unverifiedPath)) {
                    continue;
                }
                String displayName = musicCursor.getString(music_column_name);
                if (TextUtils.isEmpty(displayName)) {
                    continue;
                }
                String actualName = musicCursor.getString(music_column_actual_name);
                if (TextUtils.isEmpty(actualName)) {
                    continue;
                }

                builder.displayName(displayName);
                builder.actualName(actualName);

                final String correctPath = verifyPath(unverifiedPath, actualName, verifiedMusicPaths);
                if (TextUtils.isEmpty(correctPath))
                    continue;

//            reachSongDatabase.setFileHash(MiscUtils.quickHash(actualName, displayName, duration, size));
                builder.path(correctPath);

                if (music_column_artist != -1) {

                    String artist = musicCursor.getString(music_column_artist);
                    if (artist != null && !artist.equals("")) {
                        builder.artist(artist);
                    }
                }

                if (music_column_album != -1) {

                    String album = musicCursor.getString(music_column_album);
                    if (album != null && !album.equals("")) {
                        builder.album(album);
                    }
                }

                if (music_column_year != -1)
                    builder.year(musicCursor.getInt(music_column_year));
                if (music_date_added != -1) {

                    final long actualDateAdded = musicCursor.getLong(music_date_added);
                    builder.dateAdded(actualDateAdded);
                }

                final Cursor genresCursor;
                final String[] genresProjection = {
                        MediaStore.Audio.Genres.NAME,
                        MediaStore.Audio.Genres._ID};

                genresCursor = resolver.query(
                        MediaStore.Audio.Genres.getContentUriForAudioId("external", (int) songID),
                        genresProjection, null, null, null);

                if (genresCursor != null && genresCursor.moveToFirst()) {

                    while (genresCursor.moveToNext()) {
                        final String genre = genresCursor.getString(0);
                        if (TextUtils.isEmpty(genre))
                            continue;
                        genreHashSet.add(genre);
                    }
                }

                if (genresCursor != null)
                    genresCursor.close();

                final SongPersist persist = songPersist != null ? songPersist.get(builder.songId, null) : null;
                //load visibility
                if (persist == null) {

                    if (filter(builder.actualName) ||
                            filter(builder.displayName) ||
                            filter(builder.album) ||
                            filter(builder.artist) ||
                            builder.size > 100 * 1024 * 1024 || //100mb
                            builder.duration > 60 * 60 * 1000 || //1hour
                            builder.size < 400 * 1024 || //400kb
                            builder.duration < 40 * 1000) //40 seconds

                        builder.visibility(false);
                    else
                        builder.visibility(true);

                    builder.isLiked(false);

                    final AlbumArtData.Builder artBuilder = new AlbumArtData.Builder();
                    artBuilder.albumArtUrl("hello_world"); //put some shit
                    artBuilder.artistMBID("hello_world"); //put some shit
                    artBuilder.releaseGroupMBID("hello_world"); //put some shit
                    artBuilder.title(builder.displayName);
                    artBuilder.release(builder.album);
                    artBuilder.artist(builder.artist);
                    artBuilder.duration(builder.duration);

                    builder.albumArtData(artBuilder.build());

                } else {

                    builder.visibility(persist.visibility);
                    builder.isLiked(persist.liked);
                }

                toSend.add(builder.build());
                sendMessage(SCANNING_FILES, files++);
            }

            musicCursor.close();
            return toSend;
        }

        private static HashSet<String> recurseDirectory(File file) {

            final HashSet<String> files = new HashSet<>();

            if (file == null)
                return files;

            if (file.isDirectory()) {

                final File[] toIterate;

                if ((toIterate = file.listFiles()) == null || toIterate.length == 0)
                    return files;

                for (File song : toIterate)
                    if (song != null && song.exists()) //recurse
                        files.addAll(recurseDirectory(song));

            } else if (file.isFile())
                files.add(file.getPath().trim());
            return files;
        }

        private static String verifyPath(String path, String fileName, Iterable<String> verifiedMusicPaths) {

            File file = new File(path);

            if (file.exists() && file.isFile() && file.length() > 0)
                return path; //file is OK

            Log.i("Ayush", path + " invalid");

            //file not OK
            path = "";
            for (String newPath : verifiedMusicPaths)
                if (newPath.endsWith(fileName.trim()) || newPath.endsWith(fileName.trim() + ".mp3")) {

                    file = new File(newPath);
                    if (file.exists() && file.isFile() && file.length() > 0) {
                        path = newPath;
                        break;
                    }
                }
            return path;
        }

        private static boolean filter(String name) {

            return TextUtils.isEmpty(name) ||
                    (name.startsWith("AUD") ||
                            MiscUtils.containsIgnoreCase(name, "AudioRecording") ||
                            MiscUtils.containsIgnoreCase(name, "AudioTrack") ||
                            MiscUtils.containsIgnoreCase(name, "WhatsApp") ||
                            MiscUtils.containsIgnoreCase(name, "Recording"));

        }

        @NonNull
        private static ImmutableList<Song> getDownloadedSongs(ContentResolver resolver) {

            final ImmutableList.Builder<Song> downloadBuilder = new ImmutableList.Builder<>();
            final Cursor reachDatabaseCursor = resolver.query(
                    ReachDatabaseProvider.CONTENT_URI,
                    reachDatabaseProjection,
                    ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ? and " +
                            ReachDatabaseHelper.COLUMN_STATUS + " = ?",
                    new String[]{"0", ReachDatabase.FINISHED + ""}, null);

            if (reachDatabaseCursor != null) {

                while (reachDatabaseCursor.moveToNext()) {

                    final Song.Builder builder = new Song.Builder();
                    builder.songId(reachDatabaseCursor.getLong(0));
                    if (builder.songId == 0 || builder.songId == -1)
                        continue; //no unique Id found, can not process

                    final byte[] albumArtData = reachDatabaseCursor.getBlob(1);
                    if (albumArtData != null && albumArtData.length > 0) {

                        final AlbumArtData artData;
                        try {
                            artData = new Wire(AlbumArtData.class).parseFrom(albumArtData, AlbumArtData.class);
                            if (artData != null)
                                builder.albumArtData(artData);
                        } catch (IOException ignored) {
                        }
                    }

                    builder.displayName(reachDatabaseCursor.getString(2));
                    builder.actualName(reachDatabaseCursor.getString(3));
                    builder.artist(reachDatabaseCursor.getString(4));
                    builder.album(reachDatabaseCursor.getString(5));
                    builder.duration(reachDatabaseCursor.getLong(6));
                    builder.size(reachDatabaseCursor.getLong(7));
                    builder.genre(reachDatabaseCursor.getString(8));
                    builder.path(reachDatabaseCursor.getString(9));
                    builder.dateAdded(reachDatabaseCursor.getLong(10));
                    builder.visibility(reachDatabaseCursor.getShort(11) == 1);
                    downloadBuilder.add(builder.build());
                }
                reachDatabaseCursor.close();
            }

            return downloadBuilder.build();
        }

        /**
         * Save the latest visibility data to the cloud
         *
         * @param songs    whose virility needs to be committed
         * @param serverId the id of the user
         */
        private static void createVisibilityMap(Iterable<Song> songs,
                                                long serverId) {

            final JsonMap visibilityMap = new JsonMap();

            int visibleSongs = 0;
            for (Song song : songs) {

                visibilityMap.put(song.songId + "", song.visibility);
                if (song.visibility)
                    visibleSongs++;
            }

            final MusicData musicData = new MusicData();
            musicData.setVisibility(visibilityMap);
            musicData.setId(serverId);

            //update music visibility data
            final int finalVisibleSongs = visibleSongs;
            MiscUtils.autoRetry(() -> {

                StaticData.MUSIC_VISIBILITY_API.insert(finalVisibleSongs, musicData).execute();
                return null;
            }, Optional.absent());
        }

        private static final class SongPersist {

            boolean visibility, liked;

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof SongPersist)) return false;

                SongPersist that = (SongPersist) o;

                return visibility == that.visibility && liked == that.liked;

            }

            @Override
            public int hashCode() {
                int result = (visibility ? 1 : 0);
                result = 31 * result + (liked ? 1 : 0);
                return result;
            }
        }
    }

    private enum ScanApps {
        ;

        @NonNull
        public static List<App> getPackages(long userId,
                                            SharedPreferences preferences,
                                            PackageManager packageManager,
                                            List<ApplicationInfo> applicationInfoList) {

            if (applicationInfoList == null || applicationInfoList.isEmpty())
                return Collections.emptyList();

            //fetch local visibility
            final Map<String, Boolean> packageVisibility = SharedPrefUtils.getPackageVisibilities(preferences);
            final List<String> newPackages = new ArrayList<>();

            /**
             * Add packages which are new from local perspective
             */
            for (ApplicationInfo applicationInfo : applicationInfoList)
                if (!packageVisibility.containsKey(applicationInfo.packageName))
                    newPackages.add(applicationInfo.packageName);

            /**
             * Remove package from new list if found on server
             */
            if (!newPackages.isEmpty()) {

                Log.i("Ayush", "Found " + newPackages.size() + " new packages");
                final reach.backend.applications.appVisibilityApi.model.JsonMap visibilityMap = MiscUtils.autoRetry(() ->
                        StaticData.APP_VISIBILITY_API.get(userId).execute().getVisibility(), Optional.absent()).orNull();

                if (visibilityMap != null && !visibilityMap.isEmpty()) {

                    final Set<Map.Entry<String, Object>> entrySet = visibilityMap.entrySet();
                    Log.i("Ayush", "Found " + entrySet.size() + " persisted visibility");

                    for (Map.Entry<String, Object> entry : entrySet) {

                        final String packageName = entry.getKey();
                        final Object visibility = entry.getValue();

                        //ignore if already present locally
                        if (TextUtils.isEmpty(packageName) ||
                                visibility == null ||
                                packageVisibility.containsKey(packageName))
                            continue;

                        final boolean visible;
                        if (visibility instanceof Boolean)
                            visible = (boolean) visibility;
                        else {
                            visible = false;
                            Log.i("Ayush", "Found junk data");
                        }

                        Log.i("Ayush", "Marking " + packageName + " as " + visible);
                        //add new visibility and remove from new
                        packageVisibility.put(packageName, visible);
                        newPackages.remove(packageName);

                    }
                }
            }

            /**
             * If some really new packages are there get the default visibility
             */
            if (!newPackages.isEmpty()) {

                Log.i("Ayush", "Found " + newPackages.size() + " really new packages");
                final StringList request = new StringList();
                request.setUserId(userId);
                request.setStringList(newPackages);

                final StringList defaultHidden = MiscUtils.autoRetry(() ->
                        StaticData.CLASSIFIED_APPS_API.getDefaultState(request).execute(), Optional.absent()).orNull();

                final List<String> hiddenPackages;
                if (defaultHidden != null && (hiddenPackages = defaultHidden.getStringList()) != null && !hiddenPackages.isEmpty()) {

                    for (String hiddenPackage : hiddenPackages) {
                        packageVisibility.put(hiddenPackage, false);
                        newPackages.remove(hiddenPackage);
                        Log.i("Ayush", "Marking " + hiddenPackage + " as not visible");
                    }
                }
            }

            /**
             * Remaining packages will not have a visibility
             */
            //any remaining packages in "newPackages" will be marked as visible
//            for (String newPackage : newPackages)
//                packageVisibility.put(newPackage, true);

            final List<App> applicationsFound = new ArrayList<>();
            for (ApplicationInfo applicationInfo : applicationInfoList) {

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
                    appBuilder.visible(true); //default as true
                else
                    appBuilder.visible(visibility);

                applicationsFound.add(appBuilder.build());
                sendMessage(SCANNING_FILES, files++);
            }

            //update the package visibility
            SharedPrefUtils.overWritePackageVisibility(preferences, packageVisibility);

            return applicationsFound;
        }

        /**
         * Save the latest visibility data to the cloud
         *
         * @param apps     whose visibility needs to be committed
         * @param serverId the id of the user
         */
        private static void createVisibilityMap(Iterable<App> apps,
                                                long serverId) {

            final reach.backend.applications.appVisibilityApi.model.JsonMap visibilityMap = new reach.backend.applications.appVisibilityApi.model.JsonMap();

            for (App app : apps)
                visibilityMap.put(app.packageName, app.visible);

            final AppVisibility appVisibility = new AppVisibility();
            appVisibility.setVisibility(visibilityMap);
            appVisibility.setId(serverId);

            //update music visibility data
            MiscUtils.autoRetry(() -> {

                StaticData.APP_VISIBILITY_API.insert(appVisibility).execute();
                return null;
            }, Optional.absent());
        }
    }

    public MetaDataScanner() {
        super("MetaDataScanner");
    }

    public static int SCANNING_FILES = 0;
    public static int UPLOADING = 1;
    public static int FINISHED = 2;
    public static int TOTAL_EXPECTED = 3;

    private static int files = 0;

    @Nullable
    private static Messenger messenger = null;

    private synchronized static void sendMessage(int what, int arg1) {

        if (messenger != null) {

            final Message message = Message.obtain();
            message.what = what;
            message.arg1 = arg1;
            try {
                messenger.send(message);
            } catch (RemoteException ignored) {
            }
        }
    }

    /**
     * Saves the music data to cloud and locally
     *
     * @param myLibrary  songs from myLibrary
     * @param downloaded downloaded songs
     * @param apps       iterable of apps
     * @param genres     all the genres
     * @param first      true, if this force local DB update is needed
     */
    private static void saveMetaData(List<Song> myLibrary,
                                     List<Song> downloaded,
                                     List<App> apps,
                                     Iterable<String> genres,
                                     boolean first,
                                     long serverId,
                                     Context context) {

        final InputStream key;
        try {
            key = context.getAssets().open("key.p12");
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            return; // what to do ? this should probably not happen
        }

        final List<Song> total = ImmutableList.copyOf(Iterables.unmodifiableIterable(Iterables.concat(myLibrary, downloaded))); //music

        for (Song song : total)
            Log.i("Downloader", "Found " + song.displayName + " " + song.songId);

        ////////////////////Sync songs
        //TODO verify empty case being handled
        final byte[] musicBlob = new MusicList.Builder()
                .clientId(serverId)
                .genres(ImmutableList.copyOf(genres)) //list view of hash set
                .song(total) //concatenated list
                .build().toByteArray();

        //return false if same Music found !
        final boolean newMusic = CloudStorageUtils.uploadMetaData(
                musicBlob,
                MiscUtils.getMusicStorageKey(serverId),
                key,
                CloudStorageUtils.MUSIC);

        if (newMusic || first) {

            //over-write music visibility
            ScanMusic.createVisibilityMap(total, serverId);

            //over-write local db
            MiscUtils.bulkInsertSongs(
                    myLibrary, //sync up only myLibrary
                    context.getContentResolver());
        }

        ////////////////////Sync Apps
        final byte[] appBlob = new AppList.Builder()
                .clientId(serverId)
                .app(apps).build().toByteArray();

        //return false if same app found !
        final boolean newApps = CloudStorageUtils.uploadMetaData(
                appBlob,
                MiscUtils.getAppStorageKey(serverId),
                key,
                CloudStorageUtils.APP);

        if (newApps || first) {

            //over-write app visibility
            ScanApps.createVisibilityMap(apps, serverId);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        messenger = intent.getParcelableExtra("messenger");
        Log.i("Ayush", "Starting Scan");

        final SharedPreferences sharedPreferences = getSharedPreferences("Reach", MODE_PRIVATE);
        final ContentResolver resolver = getContentResolver();
        final PackageManager packageManager = getPackageManager();

        final long serverId = SharedPrefUtils.getServerId(sharedPreferences);

        if (serverId == 0) {
            sendMessage(FINISHED, -1);
            return;
        }

        final List<ApplicationInfo> applications = MiscUtils.getInstalledApps(packageManager);
        final Cursor musicCursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null);

        sendMessage(TOTAL_EXPECTED, applications.size() + (musicCursor != null ? musicCursor.getCount() : 0));

        ////////////////////Get my library songs
        final List<Song> songs = ScanMusic.getSongListing(resolver, musicCursor, serverId);
        ////////////////////Get downloaded songs
        final List<Song> downloaded = ScanMusic.getDownloadedSongs(resolver);
        ////////////////////Get applications
        final List<App> appList = ScanApps.getPackages(serverId, sharedPreferences, packageManager, applications);
        ////////////////////Put into server

        sendMessage(UPLOADING, -1); //start uploading

        saveMetaData(
                songs, //myLibrary music
                downloaded, //downloaded music
                appList, //apps
                ScanMusic.genreHashSet, //genres
                intent.getBooleanExtra("first", true),
                serverId,
                this);

        //locally store the genres
        SharedPrefUtils.storeGenres(sharedPreferences, ScanMusic.genreHashSet);
        sendMessage(FINISHED, -1); //send finished, so that UI can continue
        messenger = null;
    }
}