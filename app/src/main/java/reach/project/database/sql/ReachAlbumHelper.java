package reach.project.database.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import reach.project.utils.auxiliaryClasses.ReachAlbum;

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

    public static ReachAlbum cursorToProcess(Cursor cursor) {

        final ReachAlbum reachAlbum = new ReachAlbum();
        reachAlbum.setId(cursor.getLong(0));
        reachAlbum.setAlbumName(cursor.getString(1));
        reachAlbum.setArtist(cursor.getString(2));
        reachAlbum.setUserId(cursor.getLong(3));
        reachAlbum.setSize(cursor.getInt(4));
        return reachAlbum;
    }

    public static ContentValues contentValuesCreator(ReachAlbum reachAlbum) {

        final ContentValues values = new ContentValues();
        if(reachAlbum.getId() != -1)
            values.put(COLUMN_ID, reachAlbum.getId());

        values.put(COLUMN_ALBUM, reachAlbum.getAlbumName());
        values.put(COLUMN_ARTIST, reachAlbum.getArtist());
        values.put(COLUMN_SIZE, reachAlbum.getSize());
        values.put(COLUMN_USER_ID, reachAlbum.getUserId());
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
