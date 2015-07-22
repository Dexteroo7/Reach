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
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.analytics.HitBuilders;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.GZIPOutputStream;

import reach.backend.entities.userApi.model.MusicContainer;
import reach.backend.entities.userApi.model.ReachPlayList;
import reach.backend.entities.userApi.model.ReachSong;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.database.ReachAlbum;
import reach.project.database.ReachArtist;
import reach.project.database.contentProvider.ReachPlayListProvider;
import reach.project.database.contentProvider.ReachSongProvider;
import reach.project.database.sql.ReachPlayListHelper;
import reach.project.database.sql.ReachSongHelper;

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

    private final String[] projectionIds = {MediaStore.Audio.Albums._ID};
    /*
    Map of songs and playLists is needed to preserve
     */
    private final LongSparseArray<Short> reachSongVisibility = new LongSparseArray<>();
    private final LongSparseArray<Short> reachPlayListVisibility = new LongSparseArray<>();
    private final LongSparseArray<ReachSong> songSparse = new LongSparseArray<>();
    private final HashSet<String> genreHashSet = new HashSet<>();

    private long serverId;
    private ContentResolver resolver;

    public MusicScanner() {
        super("MusicScanner");
    }

    private Optional<ImmutableSortedSet<ReachSong>> getSongListing(Uri uri) {

        final ImmutableSortedSet.Builder<ReachSong> songBuilder = new ImmutableSortedSet.Builder<>(
                new Comparator<ReachSong>() {
                    @Override
                    public int compare(ReachSong lhs, ReachSong rhs) {
                        return lhs.getDateAdded().compareTo(rhs.getDateAdded());
                    }
                }
        );
        final HashSet<String> verifiedMusicPaths = new HashSet<>();

        final File[] directories = new File[]{
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        };

        for (File directory : directories)
            if (directory.exists())
                verifiedMusicPaths.addAll(recurseDirectory(directory));

        final Cursor musicCursor = resolver.query(uri, projection, null, null, null);
        if (musicCursor == null)
            return Optional.absent();
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
            if (isMusic == 0) continue;

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

            final ReachSong reachSongDatabase = new ReachSong();
            reachSongDatabase.setSongId(songID);
            reachSongDatabase.setUserId(serverId);

            long size = musicCursor.getLong(music_column_size);
            long duration = musicCursor.getLong(music_column_duration);

            if (size == 0 || duration == 0) {
                continue;
            }
            reachSongDatabase.setSize(size);
            reachSongDatabase.setDuration(duration);

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

            reachSongDatabase.setDisplayName(displayName);
            reachSongDatabase.setActualName(actualName);

            final String correctPath = verifyPath(unverifiedPath, actualName, verifiedMusicPaths);
            if (TextUtils.isEmpty(correctPath)) {
                continue;
            }
            reachSongDatabase.setPath(correctPath);

            if (music_column_artist != -1) {

                String artist = musicCursor.getString(music_column_artist);
                if (artist != null && !artist.equals("")) {
                    reachSongDatabase.setArtist(artist);
                }
            }
            if (music_column_album != -1) {

                String album = musicCursor.getString(music_column_album);
                if (album != null && !album.equals("")) {
                    reachSongDatabase.setAlbum(album);
                }
            }

            //load original visibility or default 1 (visible)
            reachSongDatabase.setVisibility((int) reachSongVisibility.get(songID, (short) 2));

            if (reachSongDatabase.getVisibility() == 2) {


                if (filter(reachSongDatabase.getActualName()) ||
                        filter(reachSongDatabase.getDisplayName()) ||
                        filter(reachSongDatabase.getAlbum()) ||
                        filter(reachSongDatabase.getArtist()) ||
                        reachSongDatabase.getSize() > 100 * 1024 * 1024)

                    reachSongDatabase.setVisibility(0);
                else
                    reachSongDatabase.setVisibility(1);
            }

            if (music_column_year != -1)
                reachSongDatabase.setYear(musicCursor.getInt(music_column_year));
            if (music_date_added != -1) {

                long actualDateAdded = musicCursor.getLong(music_date_added);
                reachSongDatabase.setDateAdded(actualDateAdded);
                reachSongDatabase.setFormattedDataAdded(MiscUtils.combinationFormatter(actualDateAdded));
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

            songBuilder.add(reachSongDatabase);
            songSparse.append(reachSongDatabase.getSongId(), reachSongDatabase);
            sendMessage(SONGS, count++);
        }
        musicCursor.close();
        return Optional.of(songBuilder.build());
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

    private ImmutableSet<ReachPlayList> getPlayLists(ReachPlayList defaultPlayList) {

        Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        final String[] columns = {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME,
                MediaStore.Audio.Playlists.DATE_MODIFIED};

        final Cursor playLists = resolver.query(uri, columns, null, null, null);

        if (playLists == null) {
            Log.e("Playlist", "Found no playlists.");
            return null;
        }

        final ImmutableSet.Builder<ReachPlayList> reachPlayListDatabases = new ImmutableSet.Builder<>();

        int count = 0;
        while (playLists.moveToNext()) {

            final int play_list_id = playLists.getColumnIndex(MediaStore.Audio.Playlists._ID);
            final int play_list_name = playLists.getColumnIndex(MediaStore.Audio.Playlists.NAME);
            if (play_list_name == -1 || play_list_id == -1) continue;
            final String playListName = playLists.getString(play_list_name);
            final long playListId = playLists.getLong(play_list_id);
            if (playListName == null || playListName.equals("")) continue;

            final ReachPlayList reachPlayListDatabase = new ReachPlayList();
            //set playListId
            reachPlayListDatabase.setPlayListId(playListId);
            reachPlayListDatabase.setVisibility((int) reachPlayListVisibility.get(playListId, (short) 1));
            reachPlayListDatabase.setPlaylistName(playListName);
            reachPlayListDatabase.setUserId(serverId);

            uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playLists.getLong(play_list_id));
            final Cursor musicCursor = resolver.query(uri, projectionIds, null, null, null);
            final List<String> songIds = new ArrayList<>(musicCursor.getCount());
            while (musicCursor.moveToNext()) {

                final int music_column_id = musicCursor
                        .getColumnIndex(MediaStore.Audio.Media._ID);
                final long songID = musicCursor.getLong(music_column_id);
                final ReachSong reachSongDatabase = songSparse.get(songID);
                if (reachSongDatabase != null) {
                    songIds.add(songID + "");
                }
            }
            reachPlayListDatabase.setReachSongs(songIds);
            int last_modified = playLists.getColumnIndex(MediaStore.Audio.Playlists.DATE_MODIFIED);
            if (last_modified != -1) {
                reachPlayListDatabase.setDateModified(MiscUtils.dateFormatter(playLists.getLong(last_modified)));
            }

            if (reachPlayListDatabase.getReachSongs().size() == 0)
                continue;
            musicCursor.close();
            reachPlayListDatabases.add(reachPlayListDatabase);
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
                        reachSongInitialCursor.getLong(0),  //
                        reachSongInitialCursor.getShort(1));
            }
            reachSongInitialCursor.close();
        }
        ////////////////////Loading PlayList visibilities
        final Cursor reachPlaylistInitialCursor = resolver.query(
                ReachPlayListProvider.CONTENT_URI,
                new String[]{
                        ReachPlayListHelper.COLUMN_PLAY_LIST_ID,
                        ReachPlayListHelper.COLUMN_VISIBILITY},
                ReachPlayListHelper.COLUMN_USER_ID + " = ?",
                new String[]{serverId + ""},
                null);
        if (reachPlaylistInitialCursor != null) {
            while (reachPlaylistInitialCursor.moveToNext()) {
                reachPlayListVisibility.append(
                        reachPlaylistInitialCursor.getLong(0), //playListId
                        reachPlaylistInitialCursor.getShort(1)); //visibility
            }
            reachPlaylistInitialCursor.close();
        }
        ////////////////////Add all the songs
        final ImmutableSortedSet<ReachSong> songs =
                getSongListing(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).orNull();
        if (songs == null || songs.size() == 0) {
            Log.i("Ayush", "Closing Music Scanner");
            sendMessage(FINISHED, -1);
            return;
        }

        sendMessage(ALBUM_ARTIST, -1);
        ////////////////////Adding albums and artists
        final Pair<Collection<ReachAlbum>, Collection<ReachArtist>>
                albums_artists = MiscUtils.getAlbumsAndArtists(songs);
        final Collection<ReachAlbum> reachAlbums = albums_artists.first;
        final Collection<ReachArtist> reachArtists = albums_artists.second;
        ////////////////////Albums and artists added
        ////////////////////Adding playLists
        final ReachPlayList defaultPlayList = new ReachPlayList();
        defaultPlayList.setDateModified("");
        defaultPlayList.setPlaylistName("Latest");
        defaultPlayList.setUserId(serverId);
        defaultPlayList.setVisibility(1);
        defaultPlayList.setPlayListId(-1L);

        final List<String> songIds = new ArrayList<>(20);
        int i = 0;
        for (ReachSong reachSong : songs) {

            if (reachSong.getVisibility() == 1) {
                songIds.add("" + reachSong.getSongId());
                if (++i > 19) break;
            }
        }
        defaultPlayList.setReachSongs(songIds);

        final ImmutableSet<ReachPlayList> playListSet = getPlayLists(defaultPlayList);
        if (playListSet == null) {
            sendMessage(FINISHED, -1);
            return;
        }
        ////////////////////PlayLists Added
        SharedPrefUtils.storeGenres(sharedPreferences, genreHashSet);
        ////////////////////Genres Added

        final boolean newMusic = portData(songs, playListSet, ImmutableList.copyOf(genreHashSet));
        if(!newMusic) {
            
            Log.i("Ayush", "Same music found !");
            sendMessage(FINISHED, -1);
            return;
        }
        
        if (!StaticData.debugMode) {
            ((ReachApplication) getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                    .setCategory("Update Music")
                    .setAction("User - " + serverId)
                    .setAction("User Name - " + SharedPrefUtils.getUserName(getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)))
                    .setValue(1)
                    .build());
        }
        
        final Future<?> musicUpdate = StaticData.threadPool.submit(new Runnable() {
            @Override
            public void run() {

                //update Music onServer, yes shit load of boiler-plate code
                final MusicContainer musicContainer = new MusicContainer();
                musicContainer.setClientId(serverId);
                musicContainer.setGenres(genreHashSet.toString());
                musicContainer.setReachSongs(songs.asList());
                musicContainer.setReachPlayLists(playListSet.asList());
                
                MiscUtils.autoRetry(new DoWork<Void>() {
                    @Override
                    protected Void doWork() throws IOException {

                        Log.i("Ayush", "Updating music");
                        return StaticData.userEndpoint.updateMusic(musicContainer).execute();
                    }
                }, Optional.<Predicate<Void>>absent()).orNull();
                
                sendMessage(FINISHED, -1);
            }
        });
        
        //save to database
        Log.i("Ayush", "Updating songs");
        MiscUtils.bulkInsertSongs(
                songs,
                reachAlbums,
                reachArtists,
                resolver);

        Log.i("Ayush", "Updating playLists");
        MiscUtils.bulkInsertPlayLists(
                playListSet,
                resolver);

        try {
            musicUpdate.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {

            MiscUtils.closeAndIgnore(
                    songIds,
                    reachAlbums,
                    reachArtists
            );

            songSparse.clear();
            reachPlayListVisibility.clear();
            reachSongVisibility.clear();
        }
    }

    private boolean portData(Set<ReachSong> songs,
                             Set<ReachPlayList> playLists,
                             List<String> genres) {

        final List<Song> newSongs = new ArrayList<>(songs.size());
        final List<Playlist> newPlayLists = new ArrayList<>(songs.size());

        final Song.Builder songBuilder = new Song.Builder();
        final Playlist.Builder playListBuilder = new Playlist.Builder();

        for (ReachSong song : songs)
            newSongs.add(songBuilder
                    .songId(song.getSongId())
                    .size(song.getSize())
                    .visibility(song.getVisibility() == 1)
                    .year(song.getYear())
                    .dateAdded(song.getDateAdded())
                    .duration(song.getDuration())
                    .genre(song.getGenre())
                    .displayName(song.getDisplayName())
                    .actualName(song.getActualName())
                    .artist(song.getArtist())
                    .album(song.getAlbum())
                    .albumArtUrl(song.getAlbumArtUrl())
                    .formattedDataAdded(song.getFormattedDataAdded())
                    .fileHash(song.getFileHash())
                    .path(song.getPath())
                    .build());

        for (ReachPlayList reachPlayList : playLists) {

            final List<Long> reachSongsList = new ArrayList<>();
            for (String reachSongString : reachPlayList.getReachSongs())
                reachSongsList.add(Long.valueOf(reachSongString.trim()));
            newPlayLists.add(playListBuilder
                    .visibility(reachPlayList.getVisibility() == 0)
                    .dateModified(reachPlayList.getDateModified())
                    .playlistName(reachPlayList.getPlaylistName())
                    .reachSongs(reachSongsList)
                    .build());
        }

        final byte[] music = new MusicList.Builder()
                .clientId(serverId)
                .genres(genres)
                .song(newSongs)
                .playlist(newPlayLists)
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
            MiscUtils.closeAndIgnore(outputStream, gzipOutputStream);
        }

        Log.i("Ayush", "Compression ratio " + (compressedMusic.length * 100) / music.length);

        InputStream key = null;
        try {
            key = getAssets().open("key.p12");
        } catch (IOException e) {
            e.printStackTrace();
            MiscUtils.closeAndIgnore(key);
            return true;
        }

        //return false if same music found !
        return CloudStorageUtils.uploadMusicData(compressedMusic,
                MiscUtils.getMusicStorageKey(serverId),
                key);
    }
}