package reach.project.music;

import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.google.common.base.Function;
import com.google.common.hash.Hashing;

import org.joda.time.DateTime;

import java.io.File;

import javax.annotation.Nullable;

import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.utils.MiscUtils;

/**
 * Created by dexter on 23/02/16.
 */
public enum SongCursorHelper {

    ANDROID_SONG_HELPER(new String[]{
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
            MediaStore.Audio.Media.DATE_MODIFIED //10
    }, new Function<Cursor, Song.Builder>() {

        @Nullable
        @Override
        public Song.Builder apply(@Nullable Cursor musicCursor) {

            if (musicCursor == null)
                return null;

            final int music_column_name = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.TITLE);
            final int music_column_actual_name = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
            final int music_column_id = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media._ID);
            final int is_music = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.IS_MUSIC);
            final int music_column_size = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.SIZE);
            final int music_column_duration = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.DURATION);
            final int music_column_file = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.DATA);

            if (music_column_name == -1 || is_music == -1 ||
                    music_column_id == -1 || music_column_actual_name == -1 ||
                    music_column_size == -1 || music_column_duration == -1 || music_column_file == -1)
                return null;

            final int isMusic = musicCursor.getInt(is_music);
            if (isMusic == 0)
                return null; //skip non-music files

            final int music_column_artist = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.ARTIST);
            final int music_column_album = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.ALBUM);
            final int music_column_year = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.YEAR);
            final int music_date_added = musicCursor
                    .getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED);

            final Song.Builder builder = new Song.Builder();
            final long androidSongID = musicCursor.getLong(music_column_id);
            builder.songId(androidSongID);

            final long size = musicCursor.getLong(music_column_size);
            final long duration = musicCursor.getLong(music_column_duration);

            if (size == 0 || duration == 0)
                return null;

            builder.size(size);
            builder.duration(duration);

            final String songPath = musicCursor.getString(music_column_file);
            if (TextUtils.isEmpty(songPath) || !isFileValid(songPath))
                return null;

            final String displayName = musicCursor.getString(music_column_name);
            if (TextUtils.isEmpty(displayName))
                return null;

            final String actualName = musicCursor.getString(music_column_actual_name);
            if (TextUtils.isEmpty(actualName))
                return null;

            builder.path(songPath);
            builder.displayName(displayName);
            builder.actualName(actualName);

            if (music_column_artist != -1)
                builder.artist(musicCursor.getString(music_column_artist));

            if (music_column_album != -1)
                builder.album(musicCursor.getString(music_column_album));

            if (music_column_year != -1)
                builder.year(musicCursor.getInt(music_column_year));

            if (music_date_added != -1)
                builder.dateAdded(musicCursor.getLong(music_date_added));

            return builder;
        }
    }),

    SONG_HELPER(new String[]{
            SongHelper.COLUMN_ID, //0
            SongHelper.COLUMN_UNIQUE_ID, //1
            SongHelper.COLUMN_META_HASH, //2

            SongHelper.COLUMN_DISPLAY_NAME, //3
            SongHelper.COLUMN_ACTUAL_NAME, //4
            SongHelper.COLUMN_ARTIST, //5
            SongHelper.COLUMN_ALBUM, //6

            SongHelper.COLUMN_DURATION, //7
            SongHelper.COLUMN_SIZE, //8

            SongHelper.COLUMN_GENRE, //9
            SongHelper.COLUMN_PATH, //10
            SongHelper.COLUMN_DATE_ADDED, //11

            SongHelper.COLUMN_VISIBILITY, //12
            SongHelper.COLUMN_IS_LIKED, //13
            SongHelper.COLUMN_USER_NAME, //14

    }, new Function<Cursor, Song>() {
        @Nullable
        @Override
        public Song apply(@Nullable Cursor cursor) {

            if (cursor == null || cursor.isClosed())
                throw new IllegalArgumentException("Invalid cursor found");

            return new Song.Builder()
                    .songId(cursor.getLong(1))
                    .fileHash(cursor.getString(2))
                    .displayName(cursor.getString(3))
                    .actualName(cursor.getString(4))
                    .artist(cursor.getString(5))
                    .album(cursor.getString(6))
                    .duration(cursor.getLong(7))
                    .size(cursor.getLong(8))
                    .genre(cursor.getString(9))
                    .path(cursor.getString(10))
                    .dateAdded(cursor.getLong(11))
                    .visibility(cursor.getShort(12) == 1)
                    .isLiked(cursor.getString(13).equals("1") || cursor.getString(13).equals("true")).build();
        }
    }),

    DOWNLOADING_HELPER(new String[]{

            SongHelper.COLUMN_ID, //0
            SongHelper.COLUMN_SONG_ID, //1
            SongHelper.COLUMN_UNIQUE_ID, //2
            SongHelper.COLUMN_META_HASH, //3

            SongHelper.COLUMN_DISPLAY_NAME, //4
            SongHelper.COLUMN_ACTUAL_NAME, //5
            SongHelper.COLUMN_ARTIST, //6
            SongHelper.COLUMN_ALBUM, //7

            SongHelper.COLUMN_DURATION, //8
            SongHelper.COLUMN_SIZE, //9

            SongHelper.COLUMN_GENRE, //10
            SongHelper.COLUMN_PATH, //11
            SongHelper.COLUMN_DATE_ADDED, //12

            SongHelper.COLUMN_VISIBILITY, //13
            SongHelper.COLUMN_IS_LIKED, //14

            SongHelper.COLUMN_RECEIVER_ID, //15
            SongHelper.COLUMN_SENDER_ID, //16
            SongHelper.COLUMN_USER_NAME, //17
            SongHelper.COLUMN_ONLINE_STATUS, //18
            SongHelper.COLUMN_OPERATION_KIND, //19
            SongHelper.COLUMN_LOGICAL_CLOCK, //20
            SongHelper.COLUMN_PROCESSED, //21
            SongHelper.COLUMN_STATUS, //22
            SongHelper.COLUMN_ALBUM_ART_DATA

    }, new Function<Cursor, ReachDatabase>() {
        @Nullable
        @Override
        public ReachDatabase apply(@Nullable Cursor cursor) {

            if (cursor == null || cursor.isClosed())
                throw new IllegalArgumentException("Invalid cursor found");

            final String liked = cursor.getString(14);
            final ReachDatabase reachDatabase = new ReachDatabase.Builder()
                    .setId(cursor.getLong(0))
                    .setSongId(cursor.getLong(1))
                    .setUniqueId(cursor.getLong(2))
                    .setMetaHash(cursor.getString(3))
                    .setDisplayName(cursor.getString(4))
                    .setActualName(cursor.getString(5))
                    .setArtistName(cursor.getString(6))
                    .setAlbumName(cursor.getString(7))
                    .setDuration(cursor.getLong(8))
                    .setLength(cursor.getLong(9))
                    .setGenre(cursor.getString(10))
                    .setDateAdded(new DateTime(cursor.getLong(12)))
                    .setReceiverId(cursor.getLong(15))
                    .setSenderId(cursor.getLong(16))
                    .setUserName(cursor.getString(17))
                    .setOperationKind(ReachDatabase.OperationKind.getFromValue(cursor.getShort(19)))
                    .setAlbumArtData(new byte[0])
                    .setLiked(!TextUtils.isEmpty(liked) && (liked.equals("1") || liked.equals("true")))
                    .setOnlineStatus(ReachFriendsHelper.Status.getFromValue(Short.parseShort(cursor.getString(18))))
                    .setVisibility(cursor.getShort(13) == 1)
                    .setLogicalClock(cursor.getShort(20))
                    .setPath(cursor.getString(11))
                    .setProcessed(cursor.getLong(21))
                    .setStatus(ReachDatabase.Status.getFromValue(cursor.getShort(22))).build();

            reachDatabase.setLastActive(0);
            reachDatabase.setReference(0);

            return reachDatabase;
        }
    }),

    DOWNLOADING_TO_SONG_HELPER(new String[]{

            SongHelper.COLUMN_ID, //0
            SongHelper.COLUMN_SONG_ID, //1
            SongHelper.COLUMN_UNIQUE_ID, //2
            SongHelper.COLUMN_META_HASH, //3

            SongHelper.COLUMN_DISPLAY_NAME, //4
            SongHelper.COLUMN_ACTUAL_NAME, //5
            SongHelper.COLUMN_ARTIST, //6
            SongHelper.COLUMN_ALBUM, //7

            SongHelper.COLUMN_DURATION, //8
            SongHelper.COLUMN_SIZE, //9

            SongHelper.COLUMN_GENRE, //10
            SongHelper.COLUMN_PATH, //11
            SongHelper.COLUMN_DATE_ADDED, //12

            SongHelper.COLUMN_VISIBILITY, //13
            SongHelper.COLUMN_IS_LIKED, //14

            SongHelper.COLUMN_RECEIVER_ID, //15
            SongHelper.COLUMN_SENDER_ID, //16
            SongHelper.COLUMN_USER_NAME, //17
            SongHelper.COLUMN_ONLINE_STATUS, //18
            SongHelper.COLUMN_OPERATION_KIND, //19
            SongHelper.COLUMN_LOGICAL_CLOCK, //20
            SongHelper.COLUMN_PROCESSED, //21
            SongHelper.COLUMN_STATUS //22

    }, new Function<Cursor, Song>() {
        @Nullable
        @Override
        public Song apply(@Nullable Cursor cursor) {

            if (cursor == null || cursor.isClosed())
                throw new IllegalArgumentException("Invalid cursor found");

            return new Song.Builder()
                    .songId(cursor.getLong(2))
                    .fileHash(cursor.getString(3))
                    .displayName(cursor.getString(4))
                    .actualName(cursor.getString(5))
                    .artist(cursor.getString(6))
                    .album(cursor.getString(7))
                    .duration(cursor.getLong(8))
                    .size(cursor.getLong(9))
                    .genre(cursor.getString(10))
                    .path(cursor.getString(11))
                    .dateAdded(cursor.getLong(12))
                    .visibility(cursor.getShort(13) == 1)
                    .isLiked(cursor.getString(14).equals("1") || cursor.getString(14).equals("true")).build();
        }
    });

    private static boolean isFileValid(String songPath) {

        final File file = new File(songPath);
        return file.exists() && file.isFile() && file.length() > 0;
    }

    private static boolean filter(String name) {

        return TextUtils.isEmpty(name) ||
                (name.startsWith("AUD") ||
                        MiscUtils.containsIgnoreCase(name, "AudioRecording") ||
                        MiscUtils.containsIgnoreCase(name, "AudioTrack") ||
                        MiscUtils.containsIgnoreCase(name, "WhatsApp"));
    }

//    public static final Function<Pair<Song.Builder, ContentResolver>, Song.Builder> SET_GENRE = new Function<Pair<Song.Builder, ContentResolver>, Song.Builder>() {
//
//        private final String[] genresProjection = {
//                MediaStore.Audio.Genres.NAME,
//                MediaStore.Audio.Genres._ID};
//
//        @Nullable
//        @Override
//        public Song.Builder apply(@Nullable Pair<Song.Builder, ContentResolver> input) {
//
//            if (input == null)
//                return null;
//
//            final Song.Builder songBuilder = input.first;
//            final ContentResolver resolver = input.second;
//
//            if (songBuilder == null || resolver == null)
//                return songBuilder;
//
//            final Cursor genresCursor = resolver.query(
//                    MediaStore.Audio.Genres.getContentUriForAudioId("external", songBuilder.songId.intValue()),
//                    genresProjection, null, null, null);
//
//            if (genresCursor != null && genresCursor.moveToFirst()) {
//
//                String listString = "";
//                while (genresCursor.moveToNext()) {
//                    final String genre = genresCursor.getString(0);
//                    if (!TextUtils.isEmpty(genre))
//                        listString += genre + "\t";
//                }
//                songBuilder.genre(listString);
//            }
//
//            if (genresCursor != null)
//                genresCursor.close();
//
//            return songBuilder;
//        }
//    };

    public static final Function<Pair<Song.Builder, Long>, Song.Builder> GENERATE_HASH = new Function<Pair<Song.Builder, Long>, Song.Builder>() {
        @Nullable
        @Override
        public Song.Builder apply(@Nullable Pair<Song.Builder, Long> input) {

            if (input == null)
                return null;

            final Song.Builder songBuilder = input.first;
            final long serverId = input.second;

            //generate the fileHash
            songBuilder.fileHash(MiscUtils.calculateSongHash(
                    serverId,
                    songBuilder.duration,
                    songBuilder.size,
                    songBuilder.displayName,
                    Hashing.sipHash24()));

            return songBuilder;
        }
    };

    public static final Function<Song.Builder, Song.Builder> DEFAULT_VISIBILITY = new Function<Song.Builder, Song.Builder>() {

        @Nullable
        @Override
        public Song.Builder apply(@Nullable Song.Builder builder) {

            if (builder == null)
                return null;

            final boolean hide;
            if (builder.size > 100 * 1024 * 1024 || //100mb big file
                    builder.duration > 60 * 60 * 1000 || //1hour big file
                    builder.size < 400 * 1024 || //400kb very small file
                    builder.duration < 40 * 1000) //40 seconds very small file
                hide = true;
            else {

                final String displayName = builder.displayName;
                final String actualName = builder.actualName;
                final String albumName = builder.album;
                final String artistName = builder.artist;
                hide = filter(displayName) && filter(actualName) && filter(albumName) && filter(artistName);
            }

            if (hide)
                builder.visibility(false);
            else
                builder.visibility(true);

            return builder;
        }
    };

    public static final Function<Song.Builder, Song> SONG_BUILDER = new Function<Song.Builder, Song>() {
        @Nullable
        @Override
        public Song apply(@Nullable Song.Builder input) {
            return input != null ? input.build() : null;
        }
    };

    private final String[] projection;
    private final Function<Cursor, ?> parser;

    SongCursorHelper(String[] projection, Function<Cursor, ?> parser) {
        this.projection = projection;
        this.parser = parser;
    }

    public String[] getProjection() {
        return projection;
    }

    public Function<Cursor, ?> getParser() {
        return parser;
    }
}
