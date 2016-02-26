package reach.project.music;

import android.database.Cursor;
import android.text.TextUtils;

import com.google.common.base.Function;

import org.joda.time.DateTime;

import javax.annotation.Nullable;

/**
 * Created by dexter on 23/02/16.
 */
public enum SongCursorHelper {

    SONG_HELPER(new String[]{
            SongHelper.COLUMN_ID, //0
            SongHelper.COLUMN_SONG_ID, //1
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
                    .setOnlineStatus(cursor.getString(18))
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

    public Function<Cursor, ?> getParser() {
        return parser;
    }
}
