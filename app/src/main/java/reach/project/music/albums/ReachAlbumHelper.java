package reach.project.music.albums;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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

    public static final String COLUMN_RELEASE_GROUP_MBID = "releaseGroupMbid";

    private static final String DATABASE_NAME = "reach.database.sql.ReachAlbumHelper";
    private static final int DATABASE_VERSION = 2;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + ALBUM_TABLE + "(" + COLUMN_ID
            + " integer primary key autoincrement, " +
            COLUMN_ALBUM + " text" + "," +
            COLUMN_ARTIST + " text" + "," +
            COLUMN_RELEASE_GROUP_MBID + " text" + "," +
            COLUMN_USER_ID + " long" + "," +
            COLUMN_SIZE + " int" + " )";

    public static final String[] projection =
            {
                    COLUMN_ID,
                    COLUMN_ALBUM,
                    COLUMN_ARTIST,
                    COLUMN_USER_ID,
                    COLUMN_SIZE,
                    COLUMN_RELEASE_GROUP_MBID
            };

    public static Album cursorToProcess(Cursor cursor) {

        final Album album = new Album();
        album.setId(cursor.getLong(0));
        album.setAlbumName(cursor.getString(1));
        album.setArtist(cursor.getString(2));
        album.setUserId(cursor.getLong(3));
        album.setSize(cursor.getInt(4));
        return album;
    }

    public static ContentValues contentValuesCreator(Album album) {

        final ContentValues values = new ContentValues();
        if(album.getId() != -1)
            values.put(COLUMN_ID, album.getId());

        values.put(COLUMN_ALBUM, album.getAlbumName());
        values.put(COLUMN_ARTIST, album.getArtist());
        values.put(COLUMN_SIZE, album.getSize());
        values.put(COLUMN_USER_ID, album.getUserId());
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
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
//        Log.w(ReachAlbumHelper.class.getName(),
//                "Upgrading database from version " + oldVersion + " to "
//                        + newVersion + ", which will destroy all old data");
//        db.execSQL("DROP TABLE IF EXISTS " + ALBUM_TABLE);
        database.execSQL("ALTER TABLE " + ALBUM_TABLE + " ADD COLUMN " + COLUMN_RELEASE_GROUP_MBID + " text");
//        onCreate(database);
    }
}
