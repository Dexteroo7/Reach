package reach.project.database.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import reach.backend.entities.userApi.model.Friend;
import reach.project.database.ReachFriend;

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
    public static final String COLUMN_STATUS_SONG = "statusSong"; //un-used
    public static final String COLUMN_NETWORK_TYPE = "networkType";
    public static final String COLUMN_NUMBER_OF_SONGS = "numberOfSongs";
    public static final String COLUMN_HASH = "hash";

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
            COLUMN_GENRES + " blob" + "," +
            COLUMN_IMAGE_ID + " text" + "," +
            COLUMN_STATUS_SONG + " text" + "," +
            COLUMN_NETWORK_TYPE + " short" + "," +
            COLUMN_NUMBER_OF_SONGS + " int" + "," +
            COLUMN_LAST_SEEN + " long" + "," +
            COLUMN_STATUS + " short" + "," +
            COLUMN_HASH + " int" + " )";

    public static final String[] projection =
            {
                    COLUMN_ID, //0
                    COLUMN_PHONE_NUMBER, //1
                    COLUMN_USER_NAME, //2
                    COLUMN_GENRES, //3
                    COLUMN_IMAGE_ID, //4
                    COLUMN_STATUS_SONG, //5
                    COLUMN_NETWORK_TYPE, //6
                    COLUMN_NUMBER_OF_SONGS, //7
                    COLUMN_LAST_SEEN, //8
                    COLUMN_STATUS, //9
                    COLUMN_HASH //10
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

        return new ReachFriend(
                cursor.getLong(0), //serverId
                cursor.getString(1), //phoneNumber
                cursor.getString(2), //userName
                cursor.getBlob(3), //genres
                cursor.getString(4), //imageId
                cursor.getString(5), //statusSong
                cursor.getShort(6), //networkType
                cursor.getInt(7), //numberOfSongs
                cursor.getLong(8), //lastSeen
                cursor.getShort(9), //status
                cursor.getInt(10) //hash
        );
    }

    public static ContentValues contentValuesCreator(ReachFriend reachFriend) {

        final ContentValues values = new ContentValues();
        if(reachFriend.getServerId() != -1)
            values.put(COLUMN_ID, reachFriend.getServerId());

        values.put(COLUMN_PHONE_NUMBER, reachFriend.getPhoneNumber());
        values.put(COLUMN_USER_NAME, reachFriend.getUserName());
        values.put(COLUMN_GENRES, reachFriend.getGenres());
        values.put(COLUMN_IMAGE_ID, reachFriend.getImageId());
        values.put(COLUMN_STATUS_SONG, reachFriend.getStatusSong());
        values.put(COLUMN_NETWORK_TYPE, reachFriend.getNetworkType());
        values.put(COLUMN_NUMBER_OF_SONGS, reachFriend.getNumberOfSongs());
        values.put(COLUMN_LAST_SEEN, reachFriend.getLastSeen());
        values.put(COLUMN_STATUS, reachFriend.getStatus());
        values.put(COLUMN_HASH, reachFriend.getHash());
        return values;
    }

    public static ContentValues contentValuesCreator(Friend reachFriend) {

        final ContentValues values = new ContentValues();
        if(reachFriend.getId() != -1)
            values.put(COLUMN_ID, reachFriend.getId());

        values.put(COLUMN_PHONE_NUMBER, reachFriend.getPhoneNumber());
        values.put(COLUMN_USER_NAME, reachFriend.getUserName());
        values.put(COLUMN_GENRES, new byte[]{});
        values.put(COLUMN_IMAGE_ID, reachFriend.getImageId());
        values.put(COLUMN_STATUS_SONG, "hello_world");
        values.put(COLUMN_NETWORK_TYPE, 0);
        values.put(COLUMN_NUMBER_OF_SONGS, reachFriend.getNumberOfSongs());
        values.put(COLUMN_LAST_SEEN, reachFriend.getLastSeen());
        values.put(COLUMN_STATUS, reachFriend.getStatus());
        values.put(COLUMN_HASH, reachFriend.getHash());
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
        Log.w(ReachFriendsHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + FRIENDS_TABLE);
        onCreate(db);
    }
}
