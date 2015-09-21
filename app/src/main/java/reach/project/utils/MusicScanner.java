package reach.project.utils;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.SparseBooleanArray;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.squareup.wire.Wire;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import reach.backend.music.musicVisibilityApi.model.JsonMap;
import reach.backend.music.musicVisibilityApi.model.MusicData;
import reach.project.core.StaticData;
import reach.project.music.AlbumArtData;
import reach.project.music.ListOfAlbumArtData;
import reach.project.music.MusicList;
import reach.project.music.albums.Album;
import reach.project.music.artists.Artist;
import reach.project.music.playLists.Playlist;
import reach.project.music.playLists.ReachPlayListHelper;
import reach.project.music.playLists.ReachPlayListProvider;
import reach.project.music.songs.ReachSongHelper;
import reach.project.music.songs.ReachSongProvider;
import reach.project.music.songs.Song;
import reach.project.uploadDownload.ReachDatabase;
import reach.project.uploadDownload.ReachDatabaseHelper;
import reach.project.uploadDownload.ReachDatabaseProvider;

public class MusicScanner extends IntentService {

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

//    //MBID fetcher
//    private final ExecutorService serviceReference;
//    private final ExecutorCompletionService<Optional<MbidReturn>> storeService;
//
//    {
//        //Test maximum number of threads
//        final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
//        final int CORE_POOL_SIZE = CPU_COUNT + 1;
//        final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
//        final int KEEP_ALIVE = 1;
//        /**
//         * An {@link Executor} that can be used to execute tasks in parallel.
//         */
//        serviceReference = Executors.unconfigurableExecutorService(new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), (r, executor) -> {
//            Log.i("Ayush", "Music scanner rejected my runnable :(");
//        }));
////        serviceReference = Executors.newSingleThreadExecutor(); //for testing
//        storeService = new ExecutorCompletionService<>(serviceReference);
//    }

    //genre collection holder
    private final HashSet<String> genreHashSet = new HashSet<>(50);
    protected ContentResolver resolver;
    private long serverId;

    @Override
    public void onDestroy() {
        super.onDestroy();
        resolver = null;
        genreHashSet.clear();
    }

    public MusicScanner() {
        super("MusicScanner");
    }

    private LongSparseArray<Song> getSongListing(Uri uri) {

        ////////////////////persistence holder use songId as key !
        ////////////////////Loading old song data
        final Cursor reachSongInitialCursor = resolver.query(
                ReachSongProvider.CONTENT_URI,
                new String[]{
                        ReachSongHelper.COLUMN_SONG_ID, //0
                        ReachSongHelper.COLUMN_VISIBILITY, //1
                        ReachSongHelper.COLUMN_IS_LIKED, //2
                        ReachSongHelper.COLUMN_ALBUM_ART_DATA}, //3
                ReachSongHelper.COLUMN_USER_ID + " = ?",
                new String[]{serverId + ""},
                null);

        final LongSparseArray<SongPersist> songPersist;
        if (reachSongInitialCursor != null) {

            songPersist = new LongSparseArray<>(reachSongInitialCursor.getCount());
            while (reachSongInitialCursor.moveToNext()) {
                //songId = key, visibility = value;
                final SongPersist persist = new SongPersist();
                persist.visibility = reachSongInitialCursor.getShort(1) == 1;
                persist.liked = reachSongInitialCursor.getShort(2) == 1;
                final byte[] albumArtData = reachSongInitialCursor.getBlob(3);

                if (albumArtData != null && albumArtData.length > 0) {

                    final AlbumArtData artData;
                    try {
                        artData = new Wire(AlbumArtData.class).parseFrom(albumArtData, AlbumArtData.class);
                        if (artData != null)
                            persist.artData = artData;
                    } catch (IOException ignored) {
                    }
                }

                songPersist.append(
                        reachSongInitialCursor.getLong(0),  //songId
                        persist); //visibility
            }
            reachSongInitialCursor.close();
        } else
            songPersist = null;

        ////////////////////Loading new songs
        final Cursor musicCursor = resolver.query(uri, projection, null, null, null);
        if (musicCursor == null)
            return null;

        final LongSparseArray<Song> toSend = new LongSparseArray<>(musicCursor.getCount());
        final HashSet<String> verifiedMusicPaths = new HashSet<>();
        final File[] directories = new File[]{
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        };

        for (File directory : directories)
            if (directory.exists())
                verifiedMusicPaths.addAll(recurseDirectory(directory));

        int songCountTotal = 0;
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
                if (persist.artData != null)
                    builder.albumArtData(persist.artData);
            }

            toSend.append(builder.songId, builder.build());
            sendMessage(SONGS, songCountTotal++);
        }

        musicCursor.close();
        return toSend;
    }

    private HashSet<String> recurseDirectory(File file) {

        final HashSet<String> files = new HashSet<>();

        if (file.isDirectory()) {

            for (File song : file.listFiles())
                if (song.exists()) //recurse
                    files.addAll(recurseDirectory(song));

        } else if (file.isFile() && file.getName().toLowerCase().contains(".mp3"))
            files.add(file.getPath().trim());
        return files;
    }

    private String verifyPath(String path, String fileName, Iterable<String> verifiedMusicPaths) {

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

    private boolean filter(String name) {

        return (name.startsWith("AUD") ||
                MiscUtils.containsIgnoreCase(name, "AudioRecording") ||
                MiscUtils.containsIgnoreCase(name, "AudioTrack") ||
                MiscUtils.containsIgnoreCase(name, "WhatsApp") ||
                MiscUtils.containsIgnoreCase(name, "Recording"));
    }

    private List<Playlist.Builder> getPlayLists(Playlist.Builder defaultPlayList,
                                                LongSparseArray<Song> songSparse) {

        ////////////////////persistence holder use playListName.hashCode() as key !
        ////////////////////Loading PlayList visibilities
        final Cursor reachPlaylistInitialCursor = resolver.query(
                ReachPlayListProvider.CONTENT_URI,
                new String[]{
                        ReachPlayListHelper.COLUMN_PLAY_LIST_NAME,
                        ReachPlayListHelper.COLUMN_VISIBILITY},
                ReachPlayListHelper.COLUMN_USER_ID + " = ?",
                new String[]{serverId + ""},
                null);
        final SparseBooleanArray playListPersist;
        if (reachPlaylistInitialCursor != null) {

            playListPersist = new SparseBooleanArray(reachPlaylistInitialCursor.getCount());
            while (reachPlaylistInitialCursor.moveToNext()) {

                playListPersist.append(
                        reachPlaylistInitialCursor.getString(0).hashCode(), //playListName
                        reachPlaylistInitialCursor.getShort(1) == 1); //visibility
            }
            reachPlaylistInitialCursor.close();
        } else
            playListPersist = null;

        final Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        final String[] columns = {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME,
                MediaStore.Audio.Playlists.DATE_MODIFIED};
        final Cursor playLists = resolver.query(uri, columns, null, null, null);
        if (playLists == null) {
            Log.e("Playlist", "Found no play lists.");
            return null;
        }

        final List<Playlist.Builder> toSend = new ArrayList<>(playLists.getCount());
        int count = 0;
        while (playLists.moveToNext()) {

            final int play_list_id = playLists.getColumnIndex(MediaStore.Audio.Playlists._ID);
            final int play_list_name = playLists.getColumnIndex(MediaStore.Audio.Playlists.NAME);
            if (play_list_name == -1 || play_list_id == -1)
                continue;

            final String playListName = playLists.getString(play_list_name);
            if (TextUtils.isEmpty(playListName))
                continue;

            final Playlist.Builder builder = new Playlist.Builder();
            builder.visibility(playListPersist != null && playListPersist.get(playListName.hashCode(), false));
            builder.playlistName(playListName);
            Log.i("Ayush", playListName);

            final Cursor musicCursor = resolver.query(
                    MediaStore.Audio.Playlists.Members.getContentUri("external", playLists.getLong(play_list_id)), //specify the URI
                    new String[]{MediaStore.Audio.Playlists.Members.AUDIO_ID}, //specify the projection
                    null, null, null);

            final ImmutableList.Builder<AlbumArtData> dataBuilder = new ImmutableList.Builder<>();
            final ImmutableList.Builder<Long> songIds = new ImmutableList.Builder<>();

            while (musicCursor.moveToNext()) {

                final long songID = musicCursor.getLong(0);
                final Song song = songSparse.get(songID);
                if (song != null) {

                    songIds.add(songID);
                    if (song.albumArtData != null)
                        dataBuilder.add(song.albumArtData);
                }
            }

            builder.reachSongs(songIds.build());
            if (builder.reachSongs.isEmpty()) {
                Log.i("Ayush", "Playlist song ids not present");
                continue;
            }
            builder.listOfAlbumArtData(new ListOfAlbumArtData.Builder().albumArtData(dataBuilder.build()).build());

            int last_modified = playLists.getColumnIndex(MediaStore.Audio.Playlists.DATE_MODIFIED);
            if (last_modified != -1) {
                builder.dateModified(MiscUtils.dateFormatter(playLists.getLong(last_modified)));
            }

            musicCursor.close();
            toSend.add(builder);
            sendMessage(PLAY_LISTS, count++);
        }
        playLists.close();

        toSend.add(defaultPlayList);
        return toSend;
    }

    private Messenger messenger;
    public static int SONGS = 0;
    public static int PLAY_LISTS = 1;
    public static int ALBUM_ARTIST = 2;
    public static int FINISHED = 3;

    private void sendMessage(int what, int arg1) {

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

    @Override
    protected void onHandleIntent(Intent intent) {

        messenger = intent.getParcelableExtra("messenger");
        Log.i("Ayush", "Starting Scan");
        final SharedPreferences sharedPreferences = getSharedPreferences("Reach", MODE_MULTI_PROCESS);
        serverId = SharedPrefUtils.getServerId(sharedPreferences);
        resolver = getContentResolver();

        if (serverId == 0) {
            sendMessage(FINISHED, -1);
            return;
        }

        ////////////////////Add all the songs
        final LongSparseArray<Song> songs = getSongListing(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        if (songs == null || songs.size() == 0) {

            //TODO remove from server ?
            Log.i("Ayush", "Closing Music Scanner");
            sendMessage(FINISHED, -1);
            return;
        }
        sendMessage(ALBUM_ARTIST, -1);

        ////////////////////Adding playLists
        final Playlist.Builder defaultPlayList = new Playlist.Builder();
        defaultPlayList.dateModified("");
        defaultPlayList.playlistName("Latest");
        defaultPlayList.visibility(true);

        final ImmutableList.Builder<AlbumArtData> dataBuilder = new ImmutableList.Builder<>();
        final ImmutableList.Builder<Long> songIds = new ImmutableList.Builder<>();
        int i = 0;
        for (int j = 0; j < songs.size(); j++) {
            // get the object by the key.
            final Song song = songs.valueAt(i);
            if (song == null)
                continue;
            if (song.visibility) {

                songIds.add(song.songId);
                if (song.albumArtData != null)
                    dataBuilder.add(song.albumArtData);
                if (++i > 19) break;
            }
        }

        defaultPlayList.reachSongs(songIds.build());
        defaultPlayList.listOfAlbumArtData(new ListOfAlbumArtData.Builder().albumArtData(dataBuilder.build()).build());

        final List<Playlist.Builder> playLists = getPlayLists(defaultPlayList, songs);
        if (playLists == null) {
            sendMessage(FINISHED, -1);
            return;
        }
        Log.i("Ayush", "Play lists found " + playLists.size());
        ////////////////////PlayLists Added
        SharedPrefUtils.storeGenres(sharedPreferences, genreHashSet);

        final boolean first = intent.getBooleanExtra("first", true);


        //pre insert
        saveMusicData(songs, playLists, genreHashSet, first);
        sendMessage(FINISHED, -1); //send finished, so that UI can continue
    }

    /**
     * Saves the music data to cloud and locally
     *
     * @param songs     LongSparseArray of myLibrarySongs
     * @param playLists List of playLists on device
     * @param genres    all the genres
     * @param first     true, if this force local DB update is needed
     */
    private void saveMusicData(LongSparseArray<Song> songs,
                               List<Playlist.Builder> playLists,
                               Iterable<String> genres,
                               boolean first) {

        if (songs == null) {
            Log.i("Ayush", "Nothing new in MBID store");
            return;
        }

        final ImmutableList.Builder<Song> myLibraryBuilder = new ImmutableList.Builder<>();
        final ImmutableList.Builder<Playlist> myPlayListsBuilder = new ImmutableList.Builder<>();

        //get playLists
        for (Playlist.Builder playlistBuilder : playLists) {

            final ListOfAlbumArtData.Builder builder = new ListOfAlbumArtData.Builder();
            final ImmutableList.Builder<AlbumArtData> dataBuilder = new ImmutableList.Builder<>();

            for (Long songId : playlistBuilder.reachSongs) {

                final Song song = songs.get(songId, null);
                if (song == null || song.albumArtData == null)
                    continue;

                dataBuilder.add(song.albumArtData);
            }

            builder.albumArtData(dataBuilder.build());
            myPlayListsBuilder.add(playlistBuilder.build());
        }

        //get songs
        for (int i = 0; i < songs.size(); i++) {

            final Song song = songs.valueAt(i);
            if (song == null)
                continue;
            myLibraryBuilder.add(song);
        }

        /**
         * Insert playLists and songs into cloud blob
         */
        final ImmutableList<Playlist> myPlayLists = myPlayListsBuilder.build();
        final ImmutableList<Song> myLibrary = myLibraryBuilder.build();
        final Iterable<Song> combinedView = Iterables.unmodifiableIterable(Iterables.concat(myLibrary, LocalUtils.getDownloadedSongs(resolver)));

        final byte[] music = new MusicList.Builder()
                .clientId(serverId)
                .genres(ImmutableList.copyOf(genres)) //list view of hash set
                .song(ImmutableList.copyOf(combinedView)) //concatenated list
                .playlist(myPlayLists) //normal list
                .build().toByteArray();

        final InputStream key;
        try {
            key = getAssets().open("key.p12");
        } catch (IOException e) {
            e.printStackTrace();
            return; // what to do ? this should probably not happen
        }

        //return false if same Music found !
        final boolean newMusic = CloudStorageUtils.uploadMusicData(
                music,
                MiscUtils.getMusicStorageKey(serverId),
                key);

        /**
         * if no new music is found and not a first time operations quit
         * OR
         * if drainage is true ! (local DB already exists)
         */
        if (!newMusic && !first) {

            Log.i("Ayush", "Same Music found !");
            return;
        }

        //update music visibility
        LocalUtils.createVisibilityMap(combinedView, serverId);

        /**
         * Commit to local DB now !
         */
        final List<Song> myLibrarySongs = myLibraryBuilder.build();
        final Pair<ArrayMap<String, Album>, ArrayMap<String, Artist>> albums_artists = MiscUtils.getAlbumsAndArtists(myLibrarySongs, serverId);
        //save songs, albums & artists to database
        MiscUtils.bulkInsertSongs(
                myLibrarySongs,
                albums_artists.first,
                albums_artists.second,
                resolver, serverId);
        //save playLists to database
        MiscUtils.bulkInsertPlayLists(myPlayLists, resolver, serverId);
    }

    private static final class SongPersist {

        boolean visibility, liked;
        AlbumArtData artData;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SongPersist)) return false;

            SongPersist that = (SongPersist) o;

            if (visibility != that.visibility) return false;
            if (liked != that.liked) return false;
            return !(artData != null ? !artData.equals(that.artData) : that.artData != null);

        }

        @Override
        public int hashCode() {
            int result = (visibility ? 1 : 0);
            result = 31 * result + (liked ? 1 : 0);
            result = 31 * result + (artData != null ? artData.hashCode() : 0);
            return result;
        }
    }

    private enum LocalUtils {
        ;

        public static ImmutableList<Song> getDownloadedSongs(ContentResolver resolver) {

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
        public static void createVisibilityMap(Iterable<Song> songs,
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

                StaticData.musicVisibility.insert(finalVisibleSongs, musicData).execute();
                return null;
            }, Optional.<Predicate<Void>>absent());
        }
    }
}

//    /**
//     * Simply drains the mbid store and updates the builders
//     *
//     * @param songs sparse array of song builders that will be updated
//     * @return updated builders
//     * @throws InterruptedException if shit happens
//     */
//    private LongSparseArray<Song.Builder> drainStore(LongSparseArray<Song.Builder> songs) throws InterruptedException {
//
//        if (storeCount == 0)
//            return null;
//
//
//        final ArrayList<ContentProviderOperation> operations = new ArrayList<>(storeCount);
//
//        //now drain the store
//        int validCount = 0;
//        long lastSync = System.currentTimeMillis();
//        for (int i = 0; i < storeCount; i++) {
//
//            final Future<Optional<MbidReturn>> mbidOptionalFuture = storeService.take();
//            final Optional<MbidReturn> mbidOptional;
//            try {
//                mbidOptional = mbidOptionalFuture.get();
//            } catch (ExecutionException e) {
//                e.printStackTrace();
//                Log.i("Ayush", "No album art found " + e.getLocalizedMessage());
//                //TRACK ?
//                continue;
//            }
//            if (!mbidOptional.isPresent())
//                continue;
//
//            final MbidReturn mbidReturn = mbidOptional.get();
//            final Song.Builder song = songs.get(mbidReturn.songId, null);
//            if (song == null)
//                continue;
//
//            song.releaseGroupMBID = mbidReturn.releaseGroupMbid;
//            song.artistMBID = mbidReturn.artistMbid;
//
//            final long currentTime = System.currentTimeMillis();
//            if (currentTime - lastSync > 5000 && operations.size() > 0) {
//
//                //commit to localDB
//                try {
//                    resolver.applyBatch(ReachSongProvider.AUTHORITY, operations);
//                } catch (RemoteException | OperationApplicationException ignored) {
//                } finally {
//                    operations.clear();
//                    lastSync = currentTime;
//                }
//            }
//
//            final ContentValues values = new ContentValues(2);
//            values.put(ReachSongHelper.COLUMN_RELEASE_GROUP_MBID, song.releaseGroupMBID);
//            values.put(ReachSongHelper.COLUMN_ARTIST_MBID, song.artistMBID);
//
//            operations.add(ContentProviderOperation
//                    .newUpdate(ReachSongProvider.CONTENT_URI)
//                    .withValues(values)
//                    .withSelection(ReachSongHelper.COLUMN_USER_ID + " = ? and " +
//                                    ReachSongHelper.COLUMN_SONG_ID + " = ?",
//                            new String[]{serverId + "", song.songId + ""}).build());
//            validCount++;
//        }
//
//        //store has served its purpose
//        serviceReference.shutdownNow();
//
//        if (validCount == 0)
//            return null;
//
//        if (operations.size() > 0)
//            try {
//                resolver.applyBatch(ReachSongProvider.AUTHORITY, operations);
//            } catch (RemoteException | OperationApplicationException ignored) {
//            } finally {
//                operations.clear();
//            }
//
//        return songs;
//    }


//    private static final class MbidFetcher implements Callable<Optional<MbidReturn>> {
//
//        final long songId;
//        final String title, artist, album;
//
//        private MbidFetcher(long songId, String title, String artist, String album) {
//
//            this.songId = songId;
//            this.title = title;
//            this.artist = artist;
//            this.album = album;
//        }
//
//        @Override
//        public Optional<MbidReturn> call() throws UnsupportedEncodingException {
//
//            //sanity check
//            if (TextUtils.isEmpty(title) && TextUtils.isEmpty(artist) && TextUtils.isEmpty(album))
//                return Optional.absent();
//
//            final String queryString = SongBrainz.createQuery(title, artist, album);
//            final Optional<JsonNode> nodeOptional = SongBrainz.fetchMBRecordingJSONFromQuery(queryString);
//            if (nodeOptional.isPresent()) {
//
//                final JsonNode node = nodeOptional.get();
//                final String releaseGroupMbid = SongBrainz.getReleaseGroupMBID(node);
//                final String artistMbid = SongBrainz.getArtistMBID(node);
//                return Optional.of(new MbidReturn(songId, releaseGroupMbid, artistMbid));
//            }
//            return Optional.absent();
//        }
//    }


//    private static final class MbidReturn {
//
//        final long songId;
//        final String releaseGroupMbid, artistMbid;
//
//        private MbidReturn(long songId, String releaseGroupMbid, String artistMbid) {
//            this.songId = songId;
//            this.releaseGroupMbid = releaseGroupMbid;
//            this.artistMbid = artistMbid;
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (!(o instanceof MbidReturn)) return false;
//            MbidReturn that = (MbidReturn) o;
//            if (songId != that.songId) return false;
//            return !(releaseGroupMbid != null ? !releaseGroupMbid.equals(that.releaseGroupMbid) : that.releaseGroupMbid != null) &&
//                    !(artistMbid != null ? !artistMbid.equals(that.artistMbid) : that.artistMbid != null);
//        }
//
//        @Override
//        public int hashCode() {
//            int result = (int) (songId ^ (songId >>> 32));
//            result = 31 * result + (releaseGroupMbid != null ? releaseGroupMbid.hashCode() : 0);
//            result = 31 * result + (artistMbid != null ? artistMbid.hashCode() : 0);
//            return result;
//        }
//    }