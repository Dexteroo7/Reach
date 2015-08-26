package reach.project.utils;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
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
import android.support.v4.util.SparseArrayCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.analytics.HitBuilders;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.localytics.android.Localytics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import reach.backend.music.musicVisibilityApi.model.JsonMap;
import reach.backend.music.musicVisibilityApi.model.MusicData;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachPlayListProvider;
import reach.project.database.contentProvider.ReachSongProvider;
import reach.project.database.sql.ReachPlayListHelper;
import reach.project.database.sql.ReachSongHelper;
import reach.project.utils.auxiliaryClasses.DoWork;
import reach.project.utils.auxiliaryClasses.MusicList;
import reach.project.utils.auxiliaryClasses.Playlist;
import reach.project.utils.auxiliaryClasses.ReachAlbum;
import reach.project.utils.auxiliaryClasses.ReachArtist;
import reach.project.utils.auxiliaryClasses.Song;

public class MusicScanner extends IntentService {

    private final String[] projection = {
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

    /*
    Map of songs and playLists is needed to preserve
     */
    private final LongSparseArray<Short> reachSongVisibility = new LongSparseArray<>();
    private final SparseArrayCompat<Short> reachPlayListVisibility = new SparseArrayCompat<>();
    private final LongSparseArray<Song> songSparse = new LongSparseArray<>();
    private final HashSet<String> genreHashSet = new HashSet<>();

    //counter
    private int visibleSongs = 0;
    private long serverId;
    private ContentResolver resolver;

    public MusicScanner() {
        super("MusicScanner");
    }

    //TODO save track number
    private ImmutableList<Song> getSongListing(Uri uri) {

        final List<Song> toSend = new ArrayList<>();
        final HashSet<String> verifiedMusicPaths = new HashSet<>();

        final File[] directories = new File[]{
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        };

        for (File directory : directories)
            if (directory.exists())
                verifiedMusicPaths.addAll(recurseDirectory(directory));

        final Cursor musicCursor = resolver.query(uri, projection, null, null, null);
        if (musicCursor == null)
            return null;

        int count = 0;
        while (musicCursor.moveToNext()) {

            int music_column_name = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.TITLE);
            int music_column_actual_name = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
            int music_column_id = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media._ID);
            int is_music = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.IS_MUSIC);
            if (music_column_name == -1 || is_music == -1 ||
                    music_column_id == -1 || music_column_actual_name == -1) continue;
            int isMusic = musicCursor.getInt(is_music);
            if (isMusic == 0)
                continue; //skip non-music files

            int music_column_size = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.SIZE);
            int music_column_artist = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int music_column_duration = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.DURATION);
            int music_column_album = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int music_column_year = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.YEAR);
            int music_date_added = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);
            int music_column_file = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.DATA);

            if (music_column_size == -1 || music_column_duration == -1)
                continue;

            long songID = musicCursor.getLong(music_column_id);

            final Song.Builder builder = new Song.Builder();
            builder.songId(songID);

            long size = musicCursor.getLong(music_column_size);
            long duration = musicCursor.getLong(music_column_duration);

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

            //load original visibility or default 1 (visible)
            final int visibility = (int) reachSongVisibility.get(songID, (short) 2);

            if (visibility == 2) {

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
                    builder.visibility(-1 < visibleSongs++); //increment and mark as true
            } else
                builder.visibility(visibility == 1 && -1 < visibleSongs++); //increment and mark as true

            if (music_column_year != -1)
                builder.year(musicCursor.getInt(music_column_year));
            if (music_date_added != -1) {

                long actualDateAdded = musicCursor.getLong(music_date_added);
                builder.dateAdded(actualDateAdded);
                builder.formattedDataAdded(MiscUtils.combinationFormatter(actualDateAdded));
            }

            final Cursor genresCursor;
            final String[] genresProjection = {
                    MediaStore.Audio.Genres.NAME,
                    MediaStore.Audio.Genres._ID};

            genresCursor = resolver.
                    query(MediaStore.Audio.Genres.getContentUriForAudioId("external", (int) songID), genresProjection, null, null, null);

            if (genresCursor != null && genresCursor.moveToFirst()) {

                int genre_column_index = genresCursor.getColumnIndex(MediaStore.Audio.Genres.NAME);
                if (genre_column_index != -1) {

                    while (genresCursor.moveToNext()) {
                        final String genre = genresCursor.getString(genre_column_index);
                        if (TextUtils.isEmpty(genre))
                            continue;
                        genreHashSet.add(genre);
                    }
                }
            }

            if (genresCursor != null)
                genresCursor.close();

            final Song song = builder.build();
            toSend.add(song);
            songSparse.append(song.songId, song);
            sendMessage(SONGS, count++);
        }

        musicCursor.close();
        Collections.sort(toSend, (lhs, rhs) -> lhs.dateAdded.compareTo(rhs.dateAdded));
        return ImmutableList.copyOf(toSend);
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

    private ImmutableList<Playlist> getPlayLists(Playlist defaultPlayList) {

        Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        final String[] columns = {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME,
                MediaStore.Audio.Playlists.DATE_MODIFIED};

        final Cursor playLists = resolver.query(uri, columns, null, null, null);

        if (playLists == null) {
            Log.e("Playlist", "Found no play lists.");
            return null;
        }

        final ImmutableList.Builder<Playlist> reachPlayListDatabases = new ImmutableList.Builder<>();

        int count = 0;
        while (playLists.moveToNext()) {

            final int play_list_id = playLists.getColumnIndex(MediaStore.Audio.Playlists._ID);
            final int play_list_name = playLists.getColumnIndex(MediaStore.Audio.Playlists.NAME);
            if (play_list_name == -1 || play_list_id == -1) continue;
            final String playListName = playLists.getString(play_list_name);
            if (playListName == null || playListName.equals("")) continue;

            final Playlist.Builder builder = new Playlist.Builder();
            //set playListId
            builder.visibility((int) reachPlayListVisibility.get(playListName.hashCode(), (short) 1) == 1);
            builder.playlistName(playListName);

            Log.i("Ayush", playListName);

            final Cursor musicCursor = resolver.query(
                    MediaStore.Audio.Playlists.Members.getContentUri("external", playLists.getLong(play_list_id)), //specify the URI
                    new String[]{MediaStore.Audio.Playlists.Members.AUDIO_ID}, //specify the projection
                    null, null, null);
            final List<Long> songIds = new ArrayList<>(musicCursor.getCount());
            while (musicCursor.moveToNext()) {

//                final int music_column_id = musicCursor
//                        .getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID);
                final long songID = musicCursor.getLong(0);
                final Song song = songSparse.get(songID);
                if (song != null)
                    songIds.add(songID);
            }

            if (songIds.isEmpty()) {
                Log.i("Ayush", "Song ids not present");
                continue;
            }

            builder.reachSongs(songIds);
            int last_modified = playLists.getColumnIndex(MediaStore.Audio.Playlists.DATE_MODIFIED);
            if (last_modified != -1) {
                builder.dateModified(MiscUtils.dateFormatter(playLists.getLong(last_modified)));
            }

            musicCursor.close();
            reachPlayListDatabases.add(builder.build());
            sendMessage(PLAY_LISTS, count++);
        }
        playLists.close();

        reachPlayListDatabases.add(defaultPlayList);
        return reachPlayListDatabases.build();
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
        ////////////////////Loading song visibilities
        final Cursor reachSongInitialCursor = resolver.query(
                ReachSongProvider.CONTENT_URI,
                new String[]{
                        ReachSongHelper.COLUMN_SONG_ID,
                        ReachSongHelper.COLUMN_VISIBILITY},
                ReachSongHelper.COLUMN_USER_ID + " = ?",
                new String[]{serverId + ""},
                null);
        if (reachSongInitialCursor != null) {
            while (reachSongInitialCursor.moveToNext()) {
                //songId = key, visibility = value;
                reachSongVisibility.append(
                        reachSongInitialCursor.getLong(0),  //songId
                        reachSongInitialCursor.getShort(1)); //visibility
            }
            reachSongInitialCursor.close();
        }
        ////////////////////Loading PlayList visibilities
        final Cursor reachPlaylistInitialCursor = resolver.query(
                ReachPlayListProvider.CONTENT_URI,
                new String[]{
                        ReachPlayListHelper.COLUMN_PLAY_LIST_NAME,
                        ReachPlayListHelper.COLUMN_VISIBILITY},
                ReachPlayListHelper.COLUMN_USER_ID + " = ?",
                new String[]{serverId + ""},
                null);
        if (reachPlaylistInitialCursor != null) {
            while (reachPlaylistInitialCursor.moveToNext()) {
                reachPlayListVisibility.append(
                        reachPlaylistInitialCursor.getString(0).hashCode(), //playListName
                        reachPlaylistInitialCursor.getShort(1)); //visibility
            }
            reachPlaylistInitialCursor.close();
        }
        ////////////////////Add all the songs
        final ImmutableList<Song> songs =
                getSongListing(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        if (songs == null || songs.isEmpty()) {

            //TODO remove from server ?
            Log.i("Ayush", "Closing Music Scanner");
            sendMessage(FINISHED, -1);
            return;
        }

        sendMessage(ALBUM_ARTIST, -1);
        final Pair<ArrayMap<String, ReachAlbum>, ArrayMap<String, ReachArtist>>
                albums_artists = MiscUtils.getAlbumsAndArtists(songs, serverId);

        ////////////////////Adding playLists
        final Playlist.Builder defaultPlayList = new Playlist.Builder();
        defaultPlayList.dateModified("");
        defaultPlayList.playlistName("Latest");
        defaultPlayList.visibility(true);

        final List<Long> songIds = new ArrayList<>(20);
        int i = 0;
        for (Song reachSong : songs) {

            if (reachSong.visibility) {
                songIds.add(reachSong.songId);
                if (++i > 19) break;
            }
        }
        defaultPlayList.reachSongs(songIds);

        final ImmutableList<Playlist> playListSet = getPlayLists(defaultPlayList.build());
        if (playListSet == null) {
            sendMessage(FINISHED, -1);
            return;
        }

        Log.i("Ayush", "Play lists found " + playListSet.size());
        ////////////////////PlayLists Added
        SharedPrefUtils.storeGenres(sharedPreferences, genreHashSet);
        ////////////////////Genres Added

        final boolean newMusic = portData(songs, playListSet, ImmutableList.copyOf(genreHashSet));
        if (!(newMusic || intent.getBooleanExtra("first", true))) {

            Log.i("Ayush", "Same Music found !");
            sendMessage(FINISHED, -1);
            return;
        }

        if (!StaticData.debugMode) {
            ((ReachApplication) getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                    .setCategory("Update Music")
                    .setAction("User Name - " + SharedPrefUtils.getUserName(getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)))
                    .setValue(1)
                    .build());
            Map<String, String> tagValues = new HashMap<>();
            tagValues.put("User Name", SharedPrefUtils.getUserName(getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)));
            Localytics.tagEvent("Update Music", tagValues);
        }

        //update music visibility
        createVisibilityMap(songs);

        //save songs, albums & artists to database
        Log.i("Ayush", "Updating songs");
        MiscUtils.bulkInsertSongs(
                songs,
                albums_artists.first,
                albums_artists.second,
                resolver, serverId);

        //save playLists to database
        Log.i("Ayush", "Updating playLists");
        MiscUtils.bulkInsertPlayLists(
                playListSet,
                resolver, serverId);

        songSparse.clear();
        reachPlayListVisibility.clear();
        reachSongVisibility.clear();

        sendMessage(FINISHED, -1);
    }

    private boolean portData(List<Song> songs,
                             List<Playlist> playLists,
                             List<String> genres) {

        final byte[] music = new MusicList.Builder()
                .clientId(serverId)
                .genres(genres)
                .song(songs)
                .playlist(playLists)
                .build().toByteArray();

        final byte[] compressedMusic;

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(music.length);
        GZIPOutputStream gzipOutputStream = null;

        try {

            gzipOutputStream = new GZIPOutputStream(outputStream);
            gzipOutputStream.write(music);
            gzipOutputStream.close();
            compressedMusic = outputStream.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            return true;
        } finally {
            MiscUtils.closeQuietly(outputStream, gzipOutputStream);
        }

        //TODO track
        Log.i("Ayush", "Compression ratio " + (compressedMusic.length * 100) / music.length);

        final InputStream key;
        try {
            key = getAssets().open("key.p12");
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }

        //return false if same Music found !
        return CloudStorageUtils.uploadMusicData(compressedMusic,
                MiscUtils.getMusicStorageKey(serverId),
                key);
    }

    private void createVisibilityMap(Iterable<Song> songs) {

        final JsonMap visibilityMap = new JsonMap();
        for (Song song : songs)
            visibilityMap.put(song.songId + "", song.visibility);

        final MusicData musicData = new MusicData();
        musicData.setVisibility(visibilityMap);
        musicData.setId(serverId);

        //update music visibility data
        MiscUtils.autoRetry(new DoWork<Void>() {
            @Override
            public Void doWork() throws IOException {

                StaticData.musicVisibility.insert(visibleSongs, musicData).execute();
                return null;
            }
        }, Optional.<Predicate<Void>>absent());
    }
}