package reach.project.coreViews.friends;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import reach.backend.entities.userApi.model.Friend;

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
    public static final String COLUMN_NEW_SONGS = "statusSong"; //hack reuse
    public static final String COLUMN_NETWORK_TYPE = "networkType";
    public static final String COLUMN_NUMBER_OF_SONGS = "numberOfSongs";
    public static final String COLUMN_HASH = "hash";

    public static final String COLUMN_LAST_SEEN = "lastSeen";
    public static final String COLUMN_STATUS = "status";

    private static final String DATABASE_NAME = "reach.database.sql.ReachFriendsHelper";
    private static final int DATABASE_VERSION = 2;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + FRIENDS_TABLE + "(" + COLUMN_ID
            + " integer primary key autoincrement, " +
            COLUMN_PHONE_NUMBER + " text" + "," +
            COLUMN_USER_NAME + " text" + "," +
            COLUMN_GENRES + " blob" + "," +
            COLUMN_IMAGE_ID + " text" + "," +
            COLUMN_NEW_SONGS + " text" + "," +
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
                    COLUMN_NEW_SONGS, //5
                    COLUMN_NETWORK_TYPE, //6
                    COLUMN_NUMBER_OF_SONGS, //7
                    COLUMN_LAST_SEEN, //8
                    COLUMN_STATUS, //9
                    COLUMN_HASH //10
            };

    public static final short ONLINE_REQUEST_GRANTED = 0;
    public static final short OFFLINE_REQUEST_GRANTED = 1;
    public static final short REQUEST_SENT_NOT_GRANTED = 2;
    public static final short REQUEST_NOT_SENT = 3;

    /**
     * Use this when inserting a fresh new friend
     * @param reachFriend the friend to add
     * @return contentValues
     */
    public static ContentValues contentValuesCreator(Friend reachFriend) {

        final ContentValues values = new ContentValues();
        if(reachFriend.getId() != -1)
            values.put(COLUMN_ID, reachFriend.getId());

        values.put(COLUMN_PHONE_NUMBER, reachFriend.getPhoneNumber());
        values.put(COLUMN_USER_NAME, reachFriend.getUserName());
        values.put(COLUMN_GENRES, new byte[]{}); //by default don't store genres
        values.put(COLUMN_IMAGE_ID, reachFriend.getImageId());
        values.put(COLUMN_NEW_SONGS, 0); //by default 0 new songs (fresh new insert)
        values.put(COLUMN_NETWORK_TYPE, 0); //wifi by default
        values.put(COLUMN_NUMBER_OF_SONGS, reachFriend.getNumberOfSongs());
        values.put(COLUMN_LAST_SEEN, reachFriend.getLastSeen());
        values.put(COLUMN_STATUS, reachFriend.getStatus());
        values.put(COLUMN_HASH, reachFriend.getHash());
        return values;
    }

    /**
     * Use this when updating an old friend (requires newSong parameter)
     * @param reachFriend the friend to add (actually update by overwriting)
     * @param oldSongCount the number of old songCount (REQUIRED)
     * @return contentValues
     */
    public static ContentValues contentValuesCreator(Friend reachFriend, int oldSongCount) {

        final ContentValues values = new ContentValues();
        if(reachFriend.getId() != -1)
            values.put(COLUMN_ID, reachFriend.getId());

        values.put(COLUMN_PHONE_NUMBER, reachFriend.getPhoneNumber());
        values.put(COLUMN_USER_NAME, reachFriend.getUserName());
        values.put(COLUMN_GENRES, new byte[]{}); //by default don't store genres
        values.put(COLUMN_IMAGE_ID, reachFriend.getImageId());
        values.put(COLUMN_NEW_SONGS, reachFriend.getNumberOfSongs() - oldSongCount); //ignore if negative
        values.put(COLUMN_NETWORK_TYPE, 0); //wifi by default
        values.put(COLUMN_NUMBER_OF_SONGS, reachFriend.getNumberOfSongs());
        values.put(COLUMN_LAST_SEEN, reachFriend.getLastSeen());
        values.put(COLUMN_STATUS, reachFriend.getStatus());
        values.put(COLUMN_HASH, reachFriend.getHash());
        return values;
    }

    /**
     * THis is to be used when adding new friend from notificationApi
     * @param reachFriend the friend to add
     * @return content values
     */
    public static ContentValues contentValuesCreator(reach.backend.notifications.notificationApi.model.Friend reachFriend) {

        final ContentValues values = new ContentValues();
        if(reachFriend.getId() != -1)
            values.put(COLUMN_ID, reachFriend.getId());

        values.put(COLUMN_PHONE_NUMBER, reachFriend.getPhoneNumber());
        values.put(COLUMN_USER_NAME, reachFriend.getUserName());
        values.put(COLUMN_GENRES, new byte[]{}); //genres are stored elsewhere
        values.put(COLUMN_IMAGE_ID, reachFriend.getImageId());
        values.put(COLUMN_NEW_SONGS, 0); //0 new songs by default
        values.put(COLUMN_NETWORK_TYPE, 0); //Wifi by default
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
