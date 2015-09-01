package reach.project.music.artists;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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

    public static final String COLUMN_ARTIST_MBID = "artistMbid";

    private static final String DATABASE_NAME = "reach.database.sql.ReachArtistHelper";
    private static final int DATABASE_VERSION = 2;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + ARTIST_TABLE + "(" + COLUMN_ID
            + " integer primary key autoincrement, " +
            COLUMN_ALBUM + " text" + "," +
            COLUMN_ARTIST + " text" + "," +
            COLUMN_ARTIST_MBID + " text" + "," +
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

    public static Artist cursorToProcess(Cursor cursor) {

        final Artist artist = new Artist();
        artist.setId(cursor.getLong(0));
        artist.setAlbum(cursor.getString(1));
        artist.setArtistName(cursor.getString(2));
        artist.setUserID(cursor.getLong(3));
        artist.setSize(cursor.getInt(4));
        return artist;
    }

    public static ContentValues contentValuesCreator(Artist artist) {

        final ContentValues values = new ContentValues();
        if(artist.getId() != -1)
            values.put(COLUMN_ID, artist.getId());

        values.put(COLUMN_ALBUM, artist.getAlbum());
        values.put(COLUMN_ARTIST, artist.getArtistName());
        values.put(COLUMN_SIZE, artist.getSize());
        values.put(COLUMN_USER_ID, artist.getUserID());
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
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {

//        Log.w(ReachArtistHelper.class.getName(),
//                "Upgrading database from version " + oldVersion + " to "
//                        + newVersion + ", which will destroy all old data");
//        db.execSQL("DROP TABLE IF EXISTS " + ARTIST_TABLE);
        database.execSQL("ALTER TABLE " + ARTIST_TABLE + " ADD COLUMN " + COLUMN_ARTIST_MBID + " text");
//        onCreate(database);
    }
}
