package reach.project.coreViews;

import android.database.Cursor;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import javax.annotation.Nullable;

import reach.project.music.MySongsHelper;
import reach.project.music.ReachDatabaseHelper;
import reach.project.music.Song;

/**
 * Created by dexter on 21/02/16.
 */
public enum CursorHelper {

    MY_SONGS_HELPER(new String[]{
            MySongsHelper.COLUMN_ID, //0
            MySongsHelper.COLUMN_SONG_ID, //1
            MySongsHelper.COLUMN_META_HASH, //2

            MySongsHelper.COLUMN_DISPLAY_NAME, //3
            MySongsHelper.COLUMN_ACTUAL_NAME, //4
            MySongsHelper.COLUMN_ARTIST, //5
            MySongsHelper.COLUMN_ALBUM, //6

            MySongsHelper.COLUMN_DURATION, //7
            MySongsHelper.COLUMN_SIZE, //8

            MySongsHelper.COLUMN_GENRE, //9
            MySongsHelper.COLUMN_PATH, //10
            MySongsHelper.COLUMN_DATE_ADDED, //11

            MySongsHelper.COLUMN_VISIBILITY, //12
            MySongsHelper.COLUMN_IS_LIKED, //13
            MySongsHelper.COLUMN_YEAR, //14

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
                    .isLiked(cursor.getShort(13) == 1)
                    .year(cursor.getInt(14)).build();
        }
    }),

    DOWNLOADED_HELPER(new String[]{
            ReachDatabaseHelper.COLUMN_ID, //0
            ReachDatabaseHelper.COLUMN_SONG_ID, //1
            ReachDatabaseHelper.COLUMN_META_HASH, //2

            ReachDatabaseHelper.COLUMN_DISPLAY_NAME, //3
            ReachDatabaseHelper.COLUMN_ACTUAL_NAME, //4
            ReachDatabaseHelper.COLUMN_ARTIST, //5
            ReachDatabaseHelper.COLUMN_ALBUM, //6

            ReachDatabaseHelper.COLUMN_DURATION, //7
            ReachDatabaseHelper.COLUMN_SIZE, //8

            ReachDatabaseHelper.COLUMN_GENRE, //9
            ReachDatabaseHelper.COLUMN_PATH, //10
            ReachDatabaseHelper.COLUMN_DATE_ADDED, //11

            ReachDatabaseHelper.COLUMN_VISIBILITY, //12
            ReachDatabaseHelper.COLUMN_IS_LIKED, //13
            ReachDatabaseHelper.COLUMN_SENDER_NAME, //14

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
            ReachDatabaseHelper.COLUMN_ID, //0
            ReachDatabaseHelper.COLUMN_SONG_ID, //1
            ReachDatabaseHelper.COLUMN_META_HASH, //2

            ReachDatabaseHelper.COLUMN_DISPLAY_NAME, //3
            ReachDatabaseHelper.COLUMN_ACTUAL_NAME, //4
            ReachDatabaseHelper.COLUMN_ARTIST, //5
            ReachDatabaseHelper.COLUMN_ALBUM, //6

            ReachDatabaseHelper.COLUMN_DURATION, //7
            ReachDatabaseHelper.COLUMN_SIZE, //8

            ReachDatabaseHelper.COLUMN_GENRE, //9
            ReachDatabaseHelper.COLUMN_PATH, //10
            ReachDatabaseHelper.COLUMN_DATE_ADDED, //11

            ReachDatabaseHelper.COLUMN_VISIBILITY, //12
            ReachDatabaseHelper.COLUMN_IS_LIKED,//13

            //non-song stuff

            COLUMN_SENDER_ID, //2
            COLUMN_PROCESSED, //3
            COLUMN_STATUS, //9
            COLUMN_SENDER_NAME, //11
            COLUMN_LOGICAL_CLOCK, //13


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
    private final Function<Cursor, Song> parser;

    CursorHelper(String[] projection, Function<Cursor, Song> parser) {
        this.projection = projection;
        this.parser = parser;
    }
}
