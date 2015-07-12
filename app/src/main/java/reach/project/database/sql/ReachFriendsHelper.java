package reach.project.database.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import reach.backend.entities.userApi.model.ReachFriend;

/**
 * Created by Dexter on 2/14/2015.
 */
public class ReachFriendsHelper extends SQLiteOpenHelper {

    public static final String FRIENDS_TABLE = "friends";
    public static final String COLUMN_ID = "_id";

    public static final String COLUMN_PHONE_NUMBER = "phoneNumber";
    public static final String COLUMN_USER_NAME = "userName";
    public static final String COLUMN_GENRES = "genres";
    public static final String COLUMN_IMAGE_ID = "imageId";
    public static final String COLUMN_STATUS_SONG = "statusSong";
    public static final String COLUMN_NETWORK_TYPE = "networkType";
    public static final String COLUMN_NUMBER_OF_SONGS = "numberOfSongs";

    public static final String COLUMN_LAST_SEEN = "lastSeen";
    public static final String COLUMN_STATUS = "status";

    private static final String DATABASE_NAME = "reach.database.sql.ReachFriendsHelper";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + FRIENDS_TABLE + "(" + COLUMN_ID
            + " integer primary key autoincrement, " +
            COLUMN_PHONE_NUMBER + " text" + "," +
            COLUMN_USER_NAME + " text" + "," +
            COLUMN_GENRES + " text" + "," +
            COLUMN_IMAGE_ID + " text" + "," +
            COLUMN_STATUS_SONG + " text" + "," +
            COLUMN_NETWORK_TYPE + " short" + "," +
            COLUMN_NUMBER_OF_SONGS + " int" + "," +
            COLUMN_LAST_SEEN + " long" + "," +
            COLUMN_STATUS + " short" + " )";

    public static final String[] projection =
            {
                    COLUMN_ID,
                    COLUMN_PHONE_NUMBER,
                    COLUMN_USER_NAME,
                    COLUMN_GENRES,
                    COLUMN_IMAGE_ID,
                    COLUMN_STATUS_SONG,
                    COLUMN_NETWORK_TYPE,
                    COLUMN_NUMBER_OF_SONGS,
                    COLUMN_LAST_SEEN,
                    COLUMN_STATUS,
            };

    public static short OFFLINE_REQUEST_GRANTED = 0;
    public static short ONLINE_REQUEST_GRANTED = 1;
    public static short REQUEST_SENT_NOT_GRANTED = 2;
    public static short REQUEST_NOT_SENT = 3;

    /*
    0 - offline and permission-request granted
    1 - online and permission-request granted
    2 - request sent but permission not granted
    3 - request not sent and permission not granted
    */

    public static ReachFriend cursorToProcess(Cursor cursor) {

        int i=0;
        final ReachFriend reachFriendsDatabase = new ReachFriend();
        reachFriendsDatabase.setId(cursor.getLong(i++));
        reachFriendsDatabase.setPhoneNumber(cursor.getString(i++));
        reachFriendsDatabase.setUserName(cursor.getString(i++));
        reachFriendsDatabase.setGenres(cursor.getString(i++));
        reachFriendsDatabase.setImageId(cursor.getString(i++));
        reachFriendsDatabase.setStatusSong(cursor.getString(i++));
        reachFriendsDatabase.setNetworkType((int) cursor.getShort(i++));
        reachFriendsDatabase.setNumberofSongs(cursor.getInt(i++));

        reachFriendsDatabase.setLastSeen(cursor.getLong(i++));
        reachFriendsDatabase.setStatus((int) cursor.getShort(i));
        return reachFriendsDatabase;
    }

    public static ContentValues contentValuesCreator(ReachFriend reachFriendsDatabase) {

        final ContentValues values = new ContentValues();
        if(reachFriendsDatabase.getId() != -1)
            values.put(COLUMN_ID, reachFriendsDatabase.getId());

        values.put(COLUMN_PHONE_NUMBER, reachFriendsDatabase.getPhoneNumber());
        values.put(COLUMN_USER_NAME, reachFriendsDatabase.getUserName());
        values.put(COLUMN_GENRES, reachFriendsDatabase.getGenres());
        values.put(COLUMN_IMAGE_ID, reachFriendsDatabase.getImageId());
        values.put(COLUMN_STATUS_SONG, reachFriendsDatabase.getStatusSong());
        values.put(COLUMN_NETWORK_TYPE, reachFriendsDatabase.getNetworkType());
        values.put(COLUMN_NUMBER_OF_SONGS, reachFriendsDatabase.getNumberofSongs());
        values.put(COLUMN_LAST_SEEN, reachFriendsDatabase.getLastSeen());
        values.put(COLUMN_STATUS, reachFriendsDatabase.getStatus());
        return values;
    }

    public ReachFriendsHelper(Context context) {
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
        db.execSQL("DROP TABLE IF EXISTS " + FRIENDS_TABLE);
        onCreate(db);
    }
}
