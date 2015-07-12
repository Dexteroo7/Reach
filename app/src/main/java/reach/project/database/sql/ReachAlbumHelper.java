package reach.project.database.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import reach.project.database.ReachAlbumDatabase;

/**
 * Created by Dexter on 2/14/2015.
 */
public class ReachAlbumHelper extends SQLiteOpenHelper {

    public static final String ALBUM_TABLE = "albums";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_ALBUM = "album";
    public static final String COLUMN_ARTIST = "artist";
    public static final String COLUMN_USER_ID = "userId";
    public static final String COLUMN_SIZE = "size";

    private static final String DATABASE_NAME = "reach.database.sql.ReachAlbumHelper";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + ALBUM_TABLE + "(" + COLUMN_ID
            + " integer primary key autoincrement, " +
            COLUMN_ALBUM + " text" + "," +
            COLUMN_ARTIST + " text" + "," +
            COLUMN_USER_ID + " long" + "," +
            COLUMN_SIZE + " int" + " )";

    public static final String[] projection =
            {
                    COLUMN_ID,
                    COLUMN_ALBUM,
                    COLUMN_ARTIST,
                    COLUMN_USER_ID,
                    COLUMN_SIZE
            };

    public static ReachAlbumDatabase cursorToProcess(Cursor cursor) {

        final ReachAlbumDatabase reachAlbumDatabase = new ReachAlbumDatabase();
        reachAlbumDatabase.setId(cursor.getLong(0));
        reachAlbumDatabase.setAlbumName(cursor.getString(1));
        reachAlbumDatabase.setArtist(cursor.getString(2));
        reachAlbumDatabase.setUserId(cursor.getLong(3));
        reachAlbumDatabase.setSize(cursor.getInt(4));
        return reachAlbumDatabase;
    }

    public static ContentValues contentValuesCreator(ReachAlbumDatabase reachAlbumDatabase) {

        final ContentValues values = new ContentValues();
        if(reachAlbumDatabase.getId() != -1)
            values.put(COLUMN_ID, reachAlbumDatabase.getId());

        values.put(COLUMN_ALBUM, reachAlbumDatabase.getAlbumName());
        values.put(COLUMN_ARTIST, reachAlbumDatabase.getArtist());
        values.put(COLUMN_SIZE, reachAlbumDatabase.getSize());
        values.put(COLUMN_USER_ID, reachAlbumDatabase.getUserId());
        return values;
    }

    public ReachAlbumHelper(Context context) {
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
        db.execSQL("DROP TABLE IF EXISTS " + ALBUM_TABLE);
        onCreate(db);
    }
}
