package reach.project.database.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import reach.project.database.ReachArtist;

/**
 * Created by Dexter on 2/14/2015.
 */
public class ReachArtistHelper extends SQLiteOpenHelper {

    public static final String ARTIST_TABLE = "artists";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_ALBUM = "album";
    public static final String COLUMN_ARTIST = "artist";
    public static final String COLUMN_USER_ID = "userId";
    public static final String COLUMN_SIZE = "size";

    private static final String DATABASE_NAME = "reach.database.sql.ReachArtistHelper";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + ARTIST_TABLE + "(" + COLUMN_ID
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

    public static ReachArtist cursorToProcess(Cursor cursor) {

        final ReachArtist reachArtist = new ReachArtist();
        reachArtist.setId(cursor.getLong(0));
        reachArtist.setAlbum(cursor.getString(1));
        reachArtist.setArtistName(cursor.getString(2));
        reachArtist.setUserID(cursor.getLong(3));
        reachArtist.setSize(cursor.getInt(4));
        return reachArtist;
    }

    public static ContentValues contentValuesCreator(ReachArtist reachArtist) {

        final ContentValues values = new ContentValues();
        if(reachArtist.getId() != -1)
            values.put(COLUMN_ID, reachArtist.getId());

        values.put(COLUMN_ALBUM, reachArtist.getAlbum());
        values.put(COLUMN_ARTIST, reachArtist.getArtistName());
        values.put(COLUMN_SIZE, reachArtist.getSize());
        values.put(COLUMN_USER_ID, reachArtist.getUserID());
        return values;
    }    

    public ReachArtistHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(ReachArtistHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + ARTIST_TABLE);
        onCreate(db);
    }
}
