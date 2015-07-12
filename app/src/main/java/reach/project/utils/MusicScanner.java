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
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.analytics.HitBuilders;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.GZIPOutputStream;

import reach.backend.entities.userApi.model.MusicContainer;
import reach.backend.entities.userApi.model.ReachPlayList;
import reach.backend.entities.userApi.model.ReachSong;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.database.ReachAlbumDatabase;
import reach.project.database.ReachArtistDatabase;
import reach.project.database.contentProvider.ReachPlayListProvider;
import reach.project.database.contentProvider.ReachSongProvider;
import reach.project.database.sql.ReachPlayListHelper;
import reach.project.database.sql.ReachSongHelper;

public class MusicScanner extends IntentService {

    private final String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED };

    private final String[] projectionIds = {MediaStore.Audio.Albums._ID};
    private long serverId;
    /*
    Map of songs and playLists is needed to preserve
     */
    private final LongSparseArray<Short> reachSongVisibility = new LongSparseArray<>();
    private final LongSparseArray<Short> reachPlayListVisibility = new LongSparseArray<>();
    private final LongSparseArray<ReachSong> songSparse = new LongSparseArray<>();

    private final HashSet<ReachSong> songHashSet = new HashSet<>();
    private final HashSet<String> verifiedMusicPaths = new HashSet<>();
    private final HashSet<String> genreHashSet = new HashSet<>();

    private final ArrayList<ReachSong> songArray = new ArrayList<>();


    public MusicScanner() {
        super("MusicScanner");
    }

    private HashSet<ReachPlayList> getPlayLists() {

        final ContentResolver resolver = getContentResolver();
        Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;

        final String[] columns = {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME,
                MediaStore.Audio.Playlists.DATE_MODIFIED };

        final Cursor playLists = resolver.query(uri, columns, null, null, null);

        if (playLists == null) {
            Log.e("Playlist", "Found no playlists.");
            return null;
        }

        final HashSet<ReachPlayList> reachPlayListDatabases = new HashSet<>(playLists.getCount());

        int count = 0;
        while (playLists.moveToNext()) {

            final int play_list_id = playLists.getColumnIndex(MediaStore.Audio.Playlists._ID);
            final int play_list_name = playLists.getColumnIndex(MediaStore.Audio.Playlists.NAME);
            if(play_list_name == -1 || play_list_id == -1) continue;
            final String playListName = playLists.getString(play_list_name);
            final long playListId = playLists.getLong(play_list_id);
            if(playListName == null || playListName.equals("")) continue;

            final ReachPlayList reachPlayListDatabase = new ReachPlayList();
            //set playListId
            reachPlayListDatabase.setPlayListId(playListId);
            reachPlayListDatabase.setVisibility((int)reachPlayListVisibility.get(playListId, (short)1));
            reachPlayListDatabase.setPlaylistName(playListName);
            reachPlayListDatabase.setUserId(serverId);

            uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playLists.getLong(play_list_id));
            final Cursor musicCursor = getContentResolver().query(uri, projectionIds, null, null, null);
            final List<String> songIds = new ArrayList<>(musicCursor.getCount());
            while (musicCursor.moveToNext()) {

                final int music_column_id = musicCursor
                        .getColumnIndex(MediaStore.Audio.Media._ID);
                final long songID = musicCursor.getLong(music_column_id);
                final ReachSong reachSongDatabase = songSparse.get(songID);
                if(reachSongDatabase != null) {
                    songIds.add(songID+"");
                }
            }
            reachPlayListDatabase.setReachSongs(songIds);
            int last_modified = playLists.getColumnIndex(MediaStore.Audio.Playlists.DATE_MODIFIED);
            if(last_modified != -1) {
                reachPlayListDatabase.setDateModified(MiscUtils.dateFormatter(playLists.getLong(last_modified)));
            }

            if(reachPlayListDatabase.getReachSongs().size() == 0)
                continue;
            musicCursor.close();
            reachPlayListDatabases.add(reachPlayListDatabase);
            if(messenger != null) {
                final Message message = Message.obtain();
                message.what = PLAY_LISTS;
                message.arg1 = count++;
                try {
                    messenger.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        playLists.close();
        return reachPlayListDatabases;
    }

    private void getSongListing(Uri uri) {

        if(verifiedMusicPaths.size() == 0)
            getMusicFiles();
        final Cursor musicCursor = getContentResolver().query(uri, projection, null, null, null);
        if(musicCursor == null)
            return;
        int count = 0;
        while(musicCursor.moveToNext()) {


            int music_column_name = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.TITLE);
            int music_column_actual_name = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
            int music_column_id = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media._ID);
            int is_music = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.IS_MUSIC);
            if(music_column_name == -1 || is_music == -1 || music_column_id == -1 || music_column_actual_name == -1) continue;
            int isMusic = musicCursor.getInt(is_music);
            if(isMusic == 0) continue;

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

            if(music_column_size == -1 || music_column_duration == -1)
                continue;

            long songID = musicCursor.getLong(music_column_id);

            final ReachSong reachSongDatabase = new ReachSong();
            reachSongDatabase.setSongId(songID);
            reachSongDatabase.setUserId(serverId);

            long size = musicCursor.getLong(music_column_size);
            long duration = musicCursor.getLong(music_column_duration);

            if(size == 0 || duration == 0) {
                continue;
            }
            reachSongDatabase.setSize(size);
            reachSongDatabase.setDuration(duration);

            String unverifiedPath = musicCursor.getString(music_column_file);
            if(unverifiedPath == null || unverifiedPath.equals("")) {
                continue;
            }
            String displayName = musicCursor.getString(music_column_name);
            if(displayName == null || displayName.equals("")) {
                continue;
            }
            String actualName = musicCursor.getString(music_column_actual_name);
            if(actualName == null || actualName.equals("")) {
                continue;
            }

            reachSongDatabase.setDisplayName(displayName);
            reachSongDatabase.setActualName(actualName);

            final String correctPath = verifyPath(unverifiedPath, actualName);
            if(correctPath == null || correctPath.equals("")) {
                continue;
            }
            reachSongDatabase.setPath(correctPath);

            if(music_column_artist != -1) {

                String artist = musicCursor.getString(music_column_artist);
                if(artist != null && !artist.equals("")) {
                    reachSongDatabase.setArtist(artist);
                }
            }
            if(music_column_album != -1) {

                String album = musicCursor.getString(music_column_album);
                if(album != null && !album.equals("")) {
                    reachSongDatabase.setAlbum(album);
                }
            }

            //load original visibility or default 1 (visible)
            reachSongDatabase.setVisibility((int) reachSongVisibility.get(songID, (short) 2));

            if(reachSongDatabase.getVisibility() == 2) {

                if (MiscUtils.filter(reachSongDatabase.getActualName()) ||
                    MiscUtils.filter(reachSongDatabase.getDisplayName()) ||
                    MiscUtils.filter(reachSongDatabase.getAlbum()) ||
                    MiscUtils.filter(reachSongDatabase.getArtist()) ||
                    reachSongDatabase.getSize() > 100 * 1024 * 1024)

                    reachSongDatabase.setVisibility(0);
                    else
                    reachSongDatabase.setVisibility(1);
            }

            if(music_column_year != -1)
                reachSongDatabase.setYear(musicCursor.getInt(music_column_year));
            if(music_date_added != -1) {

                long actualDateAdded = musicCursor.getLong(music_date_added);
                reachSongDatabase.setDateAdded(actualDateAdded);
                reachSongDatabase.setFormattedDataAdded(MiscUtils.combinationFormatter(actualDateAdded));
            }

            final Cursor genresCursor;
            final String[] genresProjection = {
                    MediaStore.Audio.Genres.NAME,
                    MediaStore.Audio.Genres._ID};

            genresCursor = getContentResolver().
                    query(MediaStore.Audio.Genres.getContentUriForAudioId("external", (int) songID), genresProjection, null, null, null );

            if (genresCursor != null && genresCursor.moveToFirst()) {

                int genre_column_index = genresCursor.getColumnIndexOrThrow
                        (MediaStore.Audio.Genres.NAME);
                String info = "";
                do {
                    String genre = genresCursor.getString(genre_column_index);
                    if(genre == null || genre.equals("")) continue;
                    genre = genre.trim().replace(" ", "-");
                    genreHashSet.add(genre);
                    info += genre + ", ";
                } while (genresCursor.moveToNext());
                if(!info.equals("")) reachSongDatabase.setGenre(info);
                else reachSongDatabase.setGenre("Unknown Genre");
            } else reachSongDatabase.setGenre("Unknown genre");

            if(genresCursor != null)
                genresCursor.close();

            if(songHashSet.add(reachSongDatabase)) {
                songSparse.append(reachSongDatabase.getSongId(), reachSongDatabase);
                songArray.add(reachSongDatabase);
            }

            if(messenger != null) {
                final Message message = Message.obtain();
                message.what = SONGS;
                message.arg1 = count++;
                try {
                    messenger.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        musicCursor.close();
    }

    private String verifyPath(String path, String fileName) {

        File file = new File(path);

        if(!file.exists() || !(file.length() > 0) || !file.isFile()) {

            path = "";
            for(String correctPath : verifiedMusicPaths) {

                if(correctPath.endsWith(fileName.trim()) || correctPath.endsWith(fileName.trim()+".mp3")) {

                    path = correctPath;
                    file = new File(path);
                    if(file.exists() && file.length() > 0 && file.isFile()) break;
                }
            }
        }
        return path;
    }

    private void getMusicFiles () {

        final HashSet<String> musicPaths = new HashSet<>();
        if(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).exists())
            for (final File music : Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).listFiles())
                musicPaths.add(music.getPath());
        if(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).exists())
            for (final File music : Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).listFiles())
                musicPaths.add(music.getPath());

        for(String path : musicPaths) {
            verifiedMusicPaths.addAll(getBaseFiles(new File(path)));
        }
    }

    private HashSet<String> getBaseFiles(File file) {

        final HashSet<String> songs = new HashSet<>();
        if(file.exists() && file.isDirectory()) {
            for(File song : file.listFiles()) {
                songs.addAll(getBaseFiles(song));
            }
        } else if(file.getName().toLowerCase().endsWith(".mp3")) {
            songs.add(file.getPath().trim());
        }
        return songs;
    }

    private Messenger messenger;
    public static int SONGS = 0;
    public static int PLAY_LISTS = 1;
    public static int ALBUM_ARTIST = 2;
    public static int FINISHED = 3;

    private void finished(){

        if(messenger != null) {
            final Message message = Message.obtain();
            message.what = FINISHED;
            try {
                messenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        stopForeground(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        final Intent foreGround = new Intent(this, MusicScanner.class);
        foreGround.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        /*PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, foreGround, 0);
        NotificationCompat.Builder note =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_icon_notif)
                        .setContentTitle("Searching for music")
                        .setOngoing(true)
                        .setContentIntent(pendingIntent);
        startForeground(StaticData.MUSIC_SCANNER_NOTIFICATION, note.build());*/

        messenger = intent.getParcelableExtra("messenger");

        Log.i("Ayush", "Starting Scan");
        final SharedPreferences sharedPreferences = getSharedPreferences("Reach", MODE_MULTI_PROCESS);
        serverId = SharedPrefUtils.getServerId(sharedPreferences);
        if(serverId == 0) {
            finished();
            return;
        }
        ////////////////////Loading song visibilities
        final Cursor reachSongInitialCursor = getContentResolver().query(
                ReachSongProvider.CONTENT_URI,
                new String[]{
                        ReachSongHelper.COLUMN_SONG_ID,
                        ReachSongHelper.COLUMN_VISIBILITY},
                ReachSongHelper.COLUMN_USER_ID + " = ?",
                new String[]{serverId + ""},
                null);
        if(reachSongInitialCursor != null) {
            while (reachSongInitialCursor.moveToNext()) {
                //songId = key, visibility = value;
                reachSongVisibility.append(
                        reachSongInitialCursor.getLong(0),  //
                        reachSongInitialCursor.getShort(1));
            }
            reachSongInitialCursor.close();
        }
        ////////////////////Loading PlayList visibilities
        final Cursor reachPlaylistInitialCursor = getContentResolver().query(
                        ReachPlayListProvider.CONTENT_URI,
                        new String[]{
                                ReachPlayListHelper.COLUMN_PLAY_LIST_ID,
                                ReachPlayListHelper.COLUMN_VISIBILITY},
                        ReachPlayListHelper.COLUMN_USER_ID + " = ?",
                        new String[]{serverId + ""},
                        null);
        if(reachPlaylistInitialCursor != null) {
            while (reachPlaylistInitialCursor.moveToNext()) {
                reachPlayListVisibility.append(
                        reachPlaylistInitialCursor.getLong(0), //playListId
                        reachPlaylistInitialCursor.getShort(1)); //visibility
            }
            reachPlaylistInitialCursor.close();
        }
        ////////////////////Add all the songs
        getSongListing(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        if(songArray.size() == 0 || songHashSet.size() == 0) {
            Log.i("Ayush", "Closing Music Scanner");
            finished();
            return;
        }
        if(messenger != null) {
            final Message message = Message.obtain();
            message.what = ALBUM_ARTIST;
            try {
                messenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        ////////////////////Adding albums and artists
        final Pair<Collection<ReachAlbumDatabase>, Collection<ReachArtistDatabase>>
                albums_artists = MiscUtils.getAlbumsAndArtists(songHashSet);
        final Collection<ReachAlbumDatabase> reachAlbumDatabases = albums_artists.first;
        final Collection<ReachArtistDatabase> reachArtistDatabases = albums_artists.second;
        ////////////////////Albums and artists added
        ////////////////////Adding playLists
        final ReachPlayList defaultPlayList = new ReachPlayList();
        defaultPlayList.setDateModified("");
        defaultPlayList.setPlaylistName("Latest");
        defaultPlayList.setUserId(serverId);
        defaultPlayList.setVisibility(1);
        defaultPlayList.setPlayListId(-1L);

        final List<String> songIds = new ArrayList<>(20);
        Log.d("Ashish",songArray.size()+" - original");
        Collections.sort(songArray, new Comparator<ReachSong>() {
            @Override
            public int compare(ReachSong lhs, ReachSong rhs) {
                return lhs.getDateAdded().compareTo(rhs.getDateAdded());
            }
        });
        Log.d("Ashish",songArray.size()+" - sorted");
        int i=0;
        for(ReachSong reachSong : songArray) {

            if(reachSong.getVisibility() == 1) {
                songIds.add("" + reachSong.getSongId());
                if (++i > 19) break;
            }
        }
        defaultPlayList.setReachSongs(songIds);

        final HashSet<ReachPlayList> playListSet = getPlayLists();
        if(playListSet == null) {
            finished();
            return;
        }
        playListSet.add(defaultPlayList);
        ////////////////////PlayLists Added
        final StringBuilder stringBuilder = new StringBuilder(genreHashSet.size());
        for(String genre : genreHashSet)
            stringBuilder.append(genre.trim()).append(" ");
        Log.i("Ayush", "User genres = " + stringBuilder.toString());
        final String genres = stringBuilder.toString();
        //save the genres
        SharedPrefUtils.storeGenres(sharedPreferences.edit(), genres);
        final int songHash = SharedPrefUtils.getSongCodeForUser(serverId, sharedPreferences);
        final int playListHash = SharedPrefUtils.getPlayListCodeForUser(serverId, sharedPreferences);
        ////////////////////save to server
        if(songHashSet.hashCode() == songHash &&
           playListSet.hashCode() == playListHash) {

            finished();
            return;
        }
        Log.i("Ayush", "Updating songs " + songHashSet.hashCode() + " " + songHash);
        if (!StaticData.debugMode) {
            ((ReachApplication)getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                    .setCategory("Update Music")
                    .setAction("User - " + serverId)
                    .setAction("User Name - " + SharedPrefUtils.getUserName(getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)))
                    .setValue(1)
                    .build());
        }
        final Future<?> musicUpdate = StaticData.threadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    List<Song> songList = new ArrayList<>();
                    for (ReachSong reachSong : songArray) {
                        boolean vis = reachSong.getVisibility() != 0;
                        songList.add(new Song.Builder()
                                .songId(reachSong.getSongId())
                                .size(reachSong.getSize())
                                .visibility(vis)
                                .year(reachSong.getYear())
                                .dateAdded(reachSong.getDateAdded())
                                .duration(reachSong.getDuration())
                                .genre(reachSong.getGenre())
                                .displayName(reachSong.getDisplayName())
                                .actualName(reachSong.getActualName())
                                .artist(reachSong.getArtist())
                                .album(reachSong.getAlbum())
                                .albumArtUrl(reachSong.getAlbumArtUrl())
                                .formattedDataAdded(reachSong.getFormattedDataAdded())
                                .fileHash(reachSong.getFileHash())
                                .path(reachSong.getPath())
                                .build());
                    }
                    List<Playlist> playlistList = new ArrayList<>();
                    for (ReachPlayList reachPlayList : playListSet) {
                        List<Long> reachSongsList = new ArrayList<>();
                        List<String> reachSongsStringList = reachPlayList.getReachSongs();
                        for (String reachSongString : reachSongsStringList) {
                            reachSongsList.add(Long.valueOf(reachSongString));
                        }
                        boolean vis = reachPlayList.getVisibility() != 0;
                        playlistList.add(new Playlist.Builder()
                                .visibility(vis)
                                .dateModified(reachPlayList.getDateModified())
                                .playlistName(reachPlayList.getPlaylistName())
                                .reachSongs(reachSongsList)
                                .build());
                    }
                    List<String> genreList = new ArrayList<>();
                    genreList.add(genres);
                    MusicList musicList = new MusicList.Builder()
                            .clientId(serverId)
                            .genres(genreList)
                            .song(songList)
                            .playlist(playlistList)
                            .build();

                    byte[] data = musicList.toByteArray();

                    Log.d("Ashish", data.length + " - orig size");
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(data.length);

                    GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
                    gzipOutputStream.write(data);
                    gzipOutputStream.close();

                    Log.d("Ashish", byteArrayOutputStream.toByteArray().length + " - compr size");
                    byteArrayOutputStream.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                //update Music onServer, yes shit load of boiler-plate code
                final MusicContainer musicContainer = new MusicContainer();
                musicContainer.setClientId(serverId);
                musicContainer.setGenres(genres);
                musicContainer.setReachSongs(songArray);
                musicContainer.setReachPlayLists(ImmutableList.copyOf(playListSet));
                MiscUtils.autoRetry(new DoWork<Void>() {
                    @Override
                    protected Void doWork() throws IOException {

                        Log.i("Ayush", "Updating music");
                        return StaticData.userEndpoint.updateMusic(musicContainer).execute();
                    }
                }, Optional.<Predicate<Void>>absent()).orNull();
                if(messenger != null) {
                    final Message message = Message.obtain();
                    message.what = FINISHED;
                    try {
                        messenger.send(message);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        //save to database
        MiscUtils.bulkInsertSongs(
                songHashSet,
                reachAlbumDatabases,
                reachArtistDatabases,
                getContentResolver());

        if(playListSet.hashCode() != playListHash) {

            SharedPrefUtils.storePlayListCodeForUser(serverId, playListSet.hashCode(), sharedPreferences.edit());
            Log.i("Ayush", "Updating playLists " + playListSet.hashCode() + " " + playListHash);
            MiscUtils.bulkInsertPlayLists(
                    playListSet,
                    getContentResolver());
        }

        try {
            musicUpdate.get();
            SharedPrefUtils.storeSongCodeForUser(serverId, songHashSet.hashCode(), sharedPreferences.edit());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {

            playListSet.clear();
            songSparse.clear();
            songHashSet.clear();
            songArray.clear();
            reachAlbumDatabases.clear();
            reachArtistDatabases.clear();
            reachPlayListVisibility.clear();
            reachSongVisibility.clear();
        }
    }
}