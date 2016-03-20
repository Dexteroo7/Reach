package reach.project.music;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.hash.Hashing;

import org.joda.time.DateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.utils.ContentType;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.HandOverMessage;

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

            SongHelper.COLUMN_PROCESSED, //14
            SongHelper.COLUMN_SENDER_ID, //15
            SongHelper.COLUMN_USER_NAME //16

    }, new Function<Cursor, Song>() {
        @Nullable
        @Override
        public Song apply(@Nullable Cursor cursor) {

            if (cursor == null || cursor.isClosed())
                throw new IllegalArgumentException("Invalid cursor found");

            final String liked = cursor.getString(13);
            final boolean isLiked = !TextUtils.isEmpty(liked) && (liked.equals("1") || liked.equals("true"));

            final Song song = new Song.Builder()
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
                    .isLiked(isLiked).build(); //13

            song.setProcessed(cursor.getLong(14));
            song.setSenderId(cursor.getLong(15));
            song.setSenderName(cursor.getString(16));

            return song;

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

    }, new Function<Cursor, ReachDatabase>() {
        @Nullable
        @Override
        public ReachDatabase apply(@Nullable Cursor cursor) {

            if (cursor == null || cursor.isClosed())
                throw new IllegalArgumentException("Invalid cursor found");

            final String liked = cursor.getString(14);
            final boolean isLiked = !TextUtils.isEmpty(liked) && (liked.equals("1") || liked.equals("true"));

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
                    .setPath(cursor.getString(11))
                    .setDateAdded(new DateTime(cursor.getLong(12)))
                    .setVisibility(cursor.getShort(13) == 1)
                    .setLiked(isLiked) //14
                    .setReceiverId(cursor.getLong(15))
                    .setSenderId(cursor.getLong(16))
                    .setUserName(cursor.getString(17))
                    .setOnlineStatus(ReachFriendsHelper.Status.getFromValue(Short.parseShort(cursor.getString(18))))
                    .setOperationKind(ReachDatabase.OperationKind.getFromValue(cursor.getShort(19)))
                    .setLogicalClock(cursor.getShort(20))
                    .setProcessed(cursor.getLong(21))
                    .setStatus(ReachDatabase.Status.getFromValue(cursor.getShort(22)))
                    .setAlbumArtData(new byte[0]).build();

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
            SongHelper.COLUMN_STATUS, //22

    }, new Function<Cursor, Song>() {
        @Nullable
        @Override
        public Song apply(@Nullable Cursor cursor) {

            if (cursor == null || cursor.isClosed())
                throw new IllegalArgumentException("Invalid cursor found");

            final String liked = cursor.getString(14);
            final boolean isLiked = !TextUtils.isEmpty(liked) && (liked.equals("1") || liked.equals("true"));

            final Song song = new Song.Builder()
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
                    .isLiked(isLiked).build();

            song.setProcessed(cursor.getLong(21));
            song.setSenderId(cursor.getLong(16));
            song.setSenderName(cursor.getString(17));

            return song;
        }
    });

    private final String[] projection;
    private final Function<Cursor, ?> parser;

    SongCursorHelper(String[] projection, Function<Cursor, ?> parser) {
        this.projection = projection;
        this.parser = parser;
    }

    public String[] getProjection() {
        return projection;
    }

    @Nonnull
    public <T> T parse(@Nonnull Cursor cursor) {
        return (T) parser.apply(cursor);
    }

    /////////////////////////////////////////

    private static boolean isFileValid(String songPath) {

        final File file = new File(songPath);
        return file.exists() && file.isFile() && file.length() > 0;
    }

    private static boolean isPersonal(String name) {

        return TextUtils.isEmpty(name) ||
                (name.startsWith("AUD") ||
                        MiscUtils.containsIgnoreCase(name, "AudioRecording") ||
                        MiscUtils.containsIgnoreCase(name, "AudioTrack") ||
                        MiscUtils.containsIgnoreCase(name, "WhatsApp"));
    }

    /////////////////////////////////////////

    public static List<Song> getSongs(@Nullable Cursor musicCursor,

                                      long serverId,
                                      @Nonnull ContentResolver contentResolver,
                                      @Nonnull Set<String> fillGenres,
                                      @Nonnull Map<String, EnumSet<ContentType.State>> oldStates,
                                      @Nonnull HandOverMessage<Integer> handOverMessage) {

        if (musicCursor == null || musicCursor.getCount() == 0 || serverId <= 0) {

            Log.i("Ayush", "Meta-data sync failed");
            return Collections.emptyList();
        } else {

            final Function<Song.Builder, Song.Builder> fixer = Functions.compose(
                    getOldStatePersister(oldStates), //second function to apply
                    getSongHashFixer(serverId)); //first function to apply

            int counter = 0;
            final List<Song> toReturn = new ArrayList<>(musicCursor.getCount());
            while (musicCursor.moveToNext()) {

                //get the songBuilder
                Song.Builder songBuilder = SongCursorHelper.ANDROID_SONG_HELPER.parse(musicCursor);
                //apply default visibility
                songBuilder = DEFAULT_VISIBILITY.apply(songBuilder);
                //fix song hash and apply oldState
                songBuilder = fixer.apply(songBuilder);

                if (songBuilder != null) {

                    setGenres(songBuilder, fillGenres, contentResolver);
                    toReturn.add(songBuilder.build());
                    handOverMessage.handOverMessage(++counter);
                }
            }

            musicCursor.close();
            return toReturn;
        }
    }

    public static List<Song> getSongs(@Nullable Cursor musicCursor,

                                      long serverId,
                                      @Nonnull ContentResolver contentResolver,
                                      @Nonnull Set<String> fillGenres) {

        if (musicCursor == null || musicCursor.getCount() == 0 || serverId <= 0) {

            Log.i("Ayush", "Meta-data sync failed");
            return Collections.emptyList();
        } else {

            //set up the function
            final Function<Song.Builder, Song.Builder> songHashFixer = getSongHashFixer(serverId);

            final List<Song> toReturn = new ArrayList<>(musicCursor.getCount());
            while (musicCursor.moveToNext()) {

                //get the songBuilder
                Song.Builder songBuilder = SongCursorHelper.ANDROID_SONG_HELPER.parse(musicCursor);
                //apply default visibility
                songBuilder = DEFAULT_VISIBILITY.apply(songBuilder);
                //fix song hash
                songBuilder = songHashFixer.apply(songBuilder);

                if (songBuilder != null) {

                    setGenres(songBuilder, fillGenres, contentResolver);
                    toReturn.add(songBuilder.build());
                }
            }

            musicCursor.close();
            return toReturn;
        }
    }

    private static final Function<Song.Builder, Song.Builder> DEFAULT_VISIBILITY = new Function<Song.Builder, Song.Builder>() {

        @Nullable
        @Override
        public Song.Builder apply(@Nullable Song.Builder builder) {

            if (builder == null)
                return null;

            final boolean hideBySize =
                    builder.size > 100 * 1024 * 1024 ||  //100mb big file
                            builder.duration > 60 * 60 * 1000 || //1hour big file
                            builder.size < 500 * 1024 ||         //500kb very small file
                            builder.duration < 50 * 1000;        //50 seconds very small file

            final boolean hideByName =
                    isPersonal(builder.displayName) ||
                            isPersonal(builder.actualName) ||
                            isPersonal(builder.album) ||
                            isPersonal(builder.artist);

            if (hideByName || hideBySize)
                builder.visibility(false);
            else
                builder.visibility(true);

            return builder;
        }
    };

    private static Function<Song.Builder, Song.Builder> getSongHashFixer(final long serverId) {

        return new Function<Song.Builder, Song.Builder>() {
            @Nullable
            @Override
            public Song.Builder apply(@Nullable Song.Builder song) {

                if (song == null)
                    return null;

                //set only if not present
                if (TextUtils.isEmpty(song.fileHash) || song.fileHash.equals("hello_world"))
                    song.fileHash(MiscUtils.calculateSongHash(serverId, song.duration, song.size, song.displayName, Hashing.sipHash24()));
                return song;
            }
        };
    }

    private static Function<Song.Builder, Song.Builder> getOldStatePersister(final Map<String, EnumSet<ContentType.State>> persistStates) {

        return new Function<Song.Builder, Song.Builder>() {
            @Nullable
            @Override
            public Song.Builder apply(@Nullable Song.Builder input) {

                if (input == null)
                    return null;

                final String metaHash = input.fileHash;
                if (TextUtils.isEmpty(metaHash))
                    throw new IllegalStateException("Plz set all metaHashes");

                final EnumSet<ContentType.State> oldStates = persistStates.get(metaHash);
                //do not change visibility if not found
                if (oldStates != null)
                    input.visibility(oldStates.contains(ContentType.State.VISIBLE));
                //default to false if no old state found
                input.isLiked(oldStates != null && oldStates.contains(ContentType.State.LIKED));

                return input;
            }
        };
    }

    private static final String[] GENRES_PROJECTION = {
            MediaStore.Audio.Genres.NAME,
            MediaStore.Audio.Genres._ID};

    /**
     * This method will set the required genres in the builder
     * and return the found genres in a set
     *
     * @param resolver ContentResolver
     */
    private static void setGenres(@Nonnull Song.Builder songBuilder,
                                  @Nonnull Set<String> fillHere,
                                  @Nonnull ContentResolver resolver) {

        final Cursor genresCursor = resolver.query(
                MediaStore.Audio.Genres.getContentUriForAudioId("external", songBuilder.songId.intValue()),
                GENRES_PROJECTION, null, null, null);

        if (genresCursor != null && genresCursor.getCount() > 0) {

            String currentGenres = "";
            while (genresCursor.moveToNext()) {

                String genre = genresCursor.getString(0);
                if (genre != null)
                    genre = genre.trim();
                if (!TextUtils.isEmpty(genre)) {

                    fillHere.add(genre);
                    currentGenres += genre + "\t";
                }
            }

            if (!TextUtils.isEmpty(currentGenres))
                songBuilder.genre(currentGenres);
        }

        if (genresCursor != null)
            genresCursor.close();
    }
}