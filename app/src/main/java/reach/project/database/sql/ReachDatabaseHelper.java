package reach.project.database.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import reach.project.utils.auxiliaryClasses.ReachDatabase;

/**
 * Created by Dexter on 2/14/2015.
 */
public class ReachDatabaseHelper extends SQLiteOpenHelper {

    public static final String REACH_TABLE = "reach";
    public static final String COLUMN_ID = "_id";

    public static final String COLUMN_SONG_ID = "songId";
    public static final String COLUMN_RECEIVER_ID = "receiverId";
    public static final String COLUMN_SENDER_ID = "senderId";
    public static final String COLUMN_OPERATION_KIND = "operationKind";

    public static final String COLUMN_PATH = "path";

    public static final String COLUMN_SENDER_NAME = "localIp";
    public static final String COLUMN_ONLINE_STATUS = "localPort";
    public static final String COLUMN_ARTIST_NAME = "globalIp";
    public static final String COLUMN_IS_LIKED = "globalPort";

    public static final String COLUMN_DISPLAY_NAME = "displayName";
    public static final String COLUMN_ACTUAL_NAME = "actualName";

    public static final String COLUMN_LENGTH = "length";
    public static final String COLUMN_PROCESSED = "processed";
    public static final String COLUMN_ADDED = "added";
    public static final String COLUMN_LOGICAL_CLOCK = "logicalClock";
    public static final String COLUMN_ACTIVATED = "lastActive";
    public static final String COLUMN_STATUS = "status";

    private static final String DATABASE_NAME = "reach.database.sql.ReachDatabaseHelper";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + REACH_TABLE + "(" + COLUMN_ID
            + " integer primary key autoincrement, " +

            COLUMN_SONG_ID + " long" + "," +
            COLUMN_RECEIVER_ID + " long" + "," +
            COLUMN_SENDER_ID + " long" + "," +
            COLUMN_OPERATION_KIND + " short" + "," +

            COLUMN_PATH + " text" + "," +

            COLUMN_SENDER_NAME + " text" + "," +
            COLUMN_ONLINE_STATUS + " text" + "," +
            COLUMN_ARTIST_NAME + " text" + "," +
            COLUMN_IS_LIKED + " text" + "," +

            COLUMN_DISPLAY_NAME + " text" + "," +
            COLUMN_ACTUAL_NAME + " text" + "," +

            COLUMN_LENGTH + " long" + "," +
            COLUMN_PROCESSED + " long" + "," +
            COLUMN_ADDED + " long" + "," +
            COLUMN_ACTIVATED + " long" + "," +

            COLUMN_LOGICAL_CLOCK + " short" + "," +
            COLUMN_STATUS + " short" + " )";

    public static final String[] projection =
            {
                    COLUMN_ID,

                    COLUMN_SONG_ID,
                    COLUMN_RECEIVER_ID,
                    COLUMN_SENDER_ID,
                    COLUMN_OPERATION_KIND,
                    //strings
                    COLUMN_PATH,
                    COLUMN_SENDER_NAME,
                    COLUMN_ONLINE_STATUS,
                    COLUMN_ARTIST_NAME,
                    COLUMN_IS_LIKED,
                    //strings
                    COLUMN_DISPLAY_NAME,
                    COLUMN_ACTUAL_NAME,
                    //longs
                    COLUMN_LENGTH,
                    COLUMN_PROCESSED,
                    COLUMN_ADDED,
                    COLUMN_ACTIVATED,
                    //shorts
                    COLUMN_LOGICAL_CLOCK,
                    COLUMN_STATUS,
            };
    /**
    Operation kind :
        0 = download;
        1 = upload;
     */
    public static ReachDatabase cursorToProcess(Cursor cursor) {

        final ReachDatabase reachDatabase = new ReachDatabase();

        int i=0;
        reachDatabase.setId(cursor.getLong(i++));

        reachDatabase.setSongId(cursor.getLong(i++));
        reachDatabase.setReceiverId(cursor.getLong(i++));
        reachDatabase.setSenderId(cursor.getLong(i++));
        reachDatabase.setOperationKind(cursor.getShort(i++));

        reachDatabase.setPath(cursor.getString(i++));
        reachDatabase.setSenderName(cursor.getString(i++));
        reachDatabase.setOnlineStatus(cursor.getString(i++));
        reachDatabase.setArtistName(cursor.getString(i++));

        final String liked = cursor.getString(i++);
        if(TextUtils.isEmpty(liked))
            reachDatabase.setIsLiked(false);
        if(liked.equals("true"))
            reachDatabase.setIsLiked(true);
        else
        reachDatabase.setIsLiked(false);

        reachDatabase.setDisplayName(cursor.getString(i++));
        reachDatabase.setActualName(cursor.getString(i++));

        reachDatabase.setLength(cursor.getLong(i++));
        reachDatabase.setProcessed(cursor.getLong(i++));
        reachDatabase.setAdded(cursor.getLong(i++));

        final long activated = cursor.getLong(i++);
        if(activated == 0)
            reachDatabase.setIsActivated(false);
        else
            reachDatabase.setIsActivated(true);

        reachDatabase.setLogicalClock(cursor.getShort(i++));
        reachDatabase.setStatus(cursor.getShort(i));

        return reachDatabase;
    }

    public static ContentValues putActivated(boolean activated, ContentValues values) {
        values.put(COLUMN_ACTIVATED, activated ? 1 : 0); //must put pong
        return values;
    }

    public static ContentValues putLiked(boolean liked, ContentValues values) {
        values.put(COLUMN_ACTIVATED, liked ? "true" : "false"); //must put pong
        return values;
    }

    public static ContentValues contentValuesCreator(ReachDatabase reachDatabase) {

        final ContentValues values = new ContentValues();
        if(reachDatabase.getId() != -1)
            values.put(COLUMN_ID, reachDatabase.getId());

        values.put(COLUMN_SONG_ID, reachDatabase.getSongId());
        values.put(COLUMN_RECEIVER_ID, reachDatabase.getReceiverId());
        values.put(COLUMN_SENDER_ID, reachDatabase.getSenderId());
        values.put(COLUMN_OPERATION_KIND, reachDatabase.getOperationKind());

        values.put(COLUMN_PATH, reachDatabase.getPath());
        values.put(COLUMN_SENDER_NAME, reachDatabase.getSenderName());
        values.put(COLUMN_ONLINE_STATUS, reachDatabase.getOnlineStatus());
        values.put(COLUMN_ARTIST_NAME, reachDatabase.getArtistName());
        values.put(COLUMN_IS_LIKED, reachDatabase.isLiked()+""); //must put string

        values.put(COLUMN_DISPLAY_NAME, reachDatabase.getDisplayName());
        values.put(COLUMN_ACTUAL_NAME, reachDatabase.getActualName());

        values.put(COLUMN_LENGTH, reachDatabase.getLength());
        values.put(COLUMN_PROCESSED, reachDatabase.getProcessed());
        values.put(COLUMN_ADDED, reachDatabase.getAdded());
        values.put(COLUMN_ACTIVATED, reachDatabase.isActivated() ? 1 : 0); //must put pong

        values.put(COLUMN_LOGICAL_CLOCK, reachDatabase.getLogicalClock());
        values.put(COLUMN_STATUS, reachDatabase.getStatus());

        return values;
    }

    public ReachDatabaseHelper (Context context) {
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
        db.execSQL("DROP TABLE IF EXISTS " + REACH_TABLE);
        onCreate(db);
    }
}
