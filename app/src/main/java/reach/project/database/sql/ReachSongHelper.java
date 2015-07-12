package reach.project.database.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import reach.backend.entities.userApi.model.ReachSong;

/**
 * Created by Dexter on 2/14/2015.
 */
public class ReachSongHelper extends SQLiteOpenHelper {

    public static final String SONG_TABLE = "songs";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SONG_ID = "songId";
    public static final String COLUMN_FILE_HASH = "fileHash";
    public static final String COLUMN_DISPLAY_NAME = "displayName";
    public static final String COLUMN_ACTUAL_NAME = "actualName";
    public static final String COLUMN_GENRE = "genre";
    public static final String COLUMN_PATH = "path";
    public static final String COLUMN_ARTIST = "artist";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_ALBUM = "album";
    public static final String COLUMN_FORMATTED_DATE_ADDED = "formattedDateAdded";

    public static final String COLUMN_USER_ID = "userId";
    public static final String COLUMN_SIZE = "size";
    public static final String COLUMN_YEAR = "year";
    public static final String COLUMN_DATE_ADDED = "dateAdded";
    public static final String COLUMN_VISIBILITY = "visibility";

    private static final String DATABASE_NAME = "reach.database.sql.ReachSongHelper";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + SONG_TABLE + "(" + COLUMN_ID
            + " integer primary key autoincrement, " +
            COLUMN_FILE_HASH + " text" + "," +
            COLUMN_SONG_ID + " long" + "," +
            COLUMN_DISPLAY_NAME + " text" + "," +
            COLUMN_ACTUAL_NAME + " text" + "," +
            COLUMN_GENRE + " text" + "," +
            COLUMN_PATH + " text" + "," +
            COLUMN_ARTIST + " text" + "," +
            COLUMN_DURATION + " long" + "," +
            COLUMN_ALBUM + " text" + "," +
            COLUMN_FORMATTED_DATE_ADDED + " text" + "," +

            COLUMN_USER_ID + " long" + "," +
            COLUMN_SIZE + " long" + "," +
            COLUMN_YEAR + " long" + "," +
            COLUMN_DATE_ADDED + " long" + "," +
            COLUMN_VISIBILITY + " short" + " )";

    public static final String[] projection =
            {
                    COLUMN_ID,
                    COLUMN_FILE_HASH,
                    COLUMN_SONG_ID,
                    COLUMN_DISPLAY_NAME,
                    COLUMN_ACTUAL_NAME,
                    COLUMN_GENRE,
                    COLUMN_PATH,
                    COLUMN_ARTIST,
                    COLUMN_DURATION,
                    COLUMN_ALBUM,
                    COLUMN_FORMATTED_DATE_ADDED,

                    COLUMN_USER_ID,
                    COLUMN_SIZE,
                    COLUMN_YEAR,
                    COLUMN_DATE_ADDED,
                    COLUMN_VISIBILITY
            };

    public static ReachSong cursorToProcess(Cursor cursor) {

        final ReachSong reachSongDatabase = new ReachSong();

        reachSongDatabase.setSongId(cursor.getLong(2));
        reachSongDatabase.setDisplayName(cursor.getString(3));
        reachSongDatabase.setActualName(cursor.getString(4));
        reachSongDatabase.setGenre(cursor.getString(5));
        reachSongDatabase.setPath(cursor.getString(6));
        reachSongDatabase.setArtist(cursor.getString(7));
        reachSongDatabase.setDuration(cursor.getLong(8));
        reachSongDatabase.setAlbum(cursor.getString(9));
        reachSongDatabase.setFormattedDataAdded(cursor.getString(10));
        reachSongDatabase.setUserId(cursor.getLong(11));
        reachSongDatabase.setSize(cursor.getLong(12));
        reachSongDatabase.setYear((int) cursor.getLong(13));
        reachSongDatabase.setDateAdded(cursor.getLong(14));
        reachSongDatabase.setVisibility((int) cursor.getShort(15));
        return reachSongDatabase;
    }

    public static ContentValues contentValuesCreator(ReachSong reachSongDatabase) {

        final ContentValues values = new ContentValues();

        values.put(COLUMN_SONG_ID, reachSongDatabase.getSongId());
        values.put(COLUMN_FILE_HASH, "hello_world");
        values.put(COLUMN_DISPLAY_NAME, reachSongDatabase.getDisplayName());
        values.put(COLUMN_ACTUAL_NAME, reachSongDatabase.getActualName());
        values.put(COLUMN_GENRE, reachSongDatabase.getGenre());
        values.put(COLUMN_PATH, reachSongDatabase.getPath());
        values.put(COLUMN_ARTIST, reachSongDatabase.getArtist());
        values.put(COLUMN_DURATION, reachSongDatabase.getDuration());
        values.put(COLUMN_ALBUM, reachSongDatabase.getAlbum());
        values.put(COLUMN_FORMATTED_DATE_ADDED, reachSongDatabase.getFormattedDataAdded());
        values.put(COLUMN_USER_ID, reachSongDatabase.getUserId());
        values.put(COLUMN_SIZE, reachSongDatabase.getSize());
        values.put(COLUMN_YEAR, reachSongDatabase.getYear());
        values.put(COLUMN_DATE_ADDED, reachSongDatabase.getDateAdded());
        values.put(COLUMN_VISIBILITY, reachSongDatabase.getVisibility());
        return values;
    }

    public ReachSongHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(ReachAlbumHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + SONG_TABLE);
        onCreate(db);
    }
}
