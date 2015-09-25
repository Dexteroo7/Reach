package reach.project.uploadDownload;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import reach.project.reachProcess.auxiliaryClasses.MusicData;

/**
 * Created by Dexter on 2/14/2015.
 */
public class ReachDatabaseHelper extends SQLiteOpenHelper {

    /**
     * COLUMN_ID : local dataBase table id, used exclusively for referencing locally
     * COLUMN_SONG_ID : songId of where the song is located, used for referencing the reachSong table (upload/download both)
     * COLUMN_UNIQUE_ID : a uniqueId which is used when this track will be uploaded (only downloads)
     */

    public static final String REACH_TABLE = "reach";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SONG_ID = "songId";
    public static final String COLUMN_UNIQUE_ID = "uniqueId";

    public static final String COLUMN_RECEIVER_ID = "receiverId"; //in-case of downloads = myId
    public static final String COLUMN_SENDER_ID = "senderId"; // in-case of uploads = myId

    public static final String COLUMN_DISPLAY_NAME = "displayName"; //title
    public static final String COLUMN_ACTUAL_NAME = "actualName";
    public static final String COLUMN_ARTIST = "globalIp"; //artist
    public static final String COLUMN_ALBUM = "album"; //artist
    public static final String COLUMN_DURATION = "lastActive"; //duration
    public static final String COLUMN_SIZE = "length"; //size
    public static final String COLUMN_GENRE = "genre";
    public static final String COLUMN_PATH = "path";
    public static final String COLUMN_ALBUM_ART_DATA = "albumArtData";

    public static final String COLUMN_DATE_ADDED = "added";
    public static final String COLUMN_VISIBILITY = "visibility";

    //non song stuff
    public static final String COLUMN_SENDER_NAME = "localIp";
    public static final String COLUMN_ONLINE_STATUS = "localPort";
    public static final String COLUMN_OPERATION_KIND = "operationKind";
    public static final String COLUMN_LOGICAL_CLOCK = "logicalClock";
    public static final String COLUMN_PROCESSED = "processed";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_IS_LIKED = "globalPort";

    private static final String DATABASE_NAME = "reach.database.sql.ReachDatabaseHelper";
    private static final int DATABASE_VERSION = 3;

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
            COLUMN_ARTIST + " text" + "," +
            COLUMN_ALBUM + " text" + "," +
            COLUMN_IS_LIKED + " text" + "," +
            COLUMN_GENRE + " text" + "," +

            COLUMN_DISPLAY_NAME + " text" + "," +
            COLUMN_ACTUAL_NAME + " text" + "," +
            COLUMN_ALBUM_ART_DATA + " blob" + "," +

            COLUMN_SIZE + " long" + "," +
            COLUMN_PROCESSED + " long" + "," +
            COLUMN_DATE_ADDED + " long" + "," +
            COLUMN_DURATION + " long" + "," +
            COLUMN_UNIQUE_ID + " long" + "," +

            COLUMN_LOGICAL_CLOCK + " short" + "," +
            COLUMN_VISIBILITY + " short" + "," +
            COLUMN_STATUS + " short" + " )";

        public static final String[] projection =
            {
                    COLUMN_ID, //0

                    COLUMN_SONG_ID, //1
                    COLUMN_RECEIVER_ID, //2
                    COLUMN_SENDER_ID, //3
                    COLUMN_OPERATION_KIND, //4
                    //strings
                    COLUMN_PATH, //5
                    COLUMN_SENDER_NAME, //6
                    COLUMN_ONLINE_STATUS, //7
                    COLUMN_ARTIST, //8
                    COLUMN_IS_LIKED, //9
                    //strings
                    COLUMN_DISPLAY_NAME, //10
                    COLUMN_ACTUAL_NAME, //11
                    //longs
                    COLUMN_SIZE, //12
                    COLUMN_PROCESSED, //13
                    COLUMN_DATE_ADDED, //14
                    COLUMN_DURATION, //15
                    //shorts
                    COLUMN_LOGICAL_CLOCK, //16
                    COLUMN_STATUS, //17
                    COLUMN_ALBUM, //18
                    COLUMN_GENRE, //19
                    COLUMN_ALBUM_ART_DATA, //20
                    COLUMN_VISIBILITY, //21
                    COLUMN_UNIQUE_ID
            };

    /**
     * Operation kind :
     * 0 = download;
     * 1 = upload;
     */
    public static ReachDatabase cursorToProcess(Cursor cursor) {

        final ReachDatabase reachDatabase = new ReachDatabase();

        reachDatabase.setId(cursor.getLong(0));
        reachDatabase.setSongId(cursor.getLong(1));
        reachDatabase.setReceiverId(cursor.getLong(2));
        reachDatabase.setSenderId(cursor.getLong(3));

        reachDatabase.setOperationKind(cursor.getShort(4));

        reachDatabase.setPath(cursor.getString(5));
        reachDatabase.setSenderName(cursor.getString(6));
        reachDatabase.setOnlineStatus(cursor.getString(7));
        reachDatabase.setArtistName(cursor.getString(8));

        final String liked = cursor.getString(9);
        if (TextUtils.isEmpty(liked))
            reachDatabase.setIsLiked(false);
        else
            reachDatabase.setIsLiked(liked.equals("1"));

        reachDatabase.setDisplayName(cursor.getString(10));
        reachDatabase.setActualName(cursor.getString(11));

        reachDatabase.setLength(cursor.getLong(12));
        reachDatabase.setProcessed(cursor.getLong(13));
        reachDatabase.setAdded(cursor.getLong(14));
        reachDatabase.setDuration(cursor.getLong(15));

        reachDatabase.setLogicalClock(cursor.getShort(16));
        reachDatabase.setStatus(cursor.getShort(17));

        reachDatabase.setLastActive(0); //reset
        reachDatabase.setReference(0);  //reset

        return reachDatabase;
    }

    public static ContentValues contentValuesCreator(ReachDatabase reachDatabase) {

        final ContentValues values = new ContentValues();
        if (reachDatabase.getId() != -1)
            values.put(COLUMN_ID, reachDatabase.getId());

        values.put(COLUMN_SONG_ID, reachDatabase.getSongId());
        values.put(COLUMN_RECEIVER_ID, reachDatabase.getReceiverId());
        values.put(COLUMN_SENDER_ID, reachDatabase.getSenderId());
        values.put(COLUMN_OPERATION_KIND, reachDatabase.getOperationKind());

        values.put(COLUMN_PATH, reachDatabase.getPath());
        values.put(COLUMN_SENDER_NAME, reachDatabase.getSenderName());
        values.put(COLUMN_ONLINE_STATUS, reachDatabase.getOnlineStatus());
        values.put(COLUMN_ARTIST, reachDatabase.getArtistName());
        values.put(COLUMN_IS_LIKED, reachDatabase.isLiked() ? 1 : 0); //must put string

        values.put(COLUMN_DISPLAY_NAME, reachDatabase.getDisplayName());
        values.put(COLUMN_ACTUAL_NAME, reachDatabase.getActualName());

        values.put(COLUMN_SIZE, reachDatabase.getLength());
        values.put(COLUMN_PROCESSED, reachDatabase.getProcessed());
        values.put(COLUMN_DATE_ADDED, reachDatabase.getAdded());
        values.put(COLUMN_DURATION, reachDatabase.getDuration());

        values.put(COLUMN_LOGICAL_CLOCK, reachDatabase.getLogicalClock());
        values.put(COLUMN_STATUS, reachDatabase.getStatus());

        values.put(COLUMN_ALBUM_ART_DATA, reachDatabase.getAlbumArtData());
        values.put(COLUMN_ALBUM, reachDatabase.getAlbumName());
        values.put(COLUMN_GENRE, reachDatabase.getGenre());
        values.put(COLUMN_VISIBILITY, reachDatabase.getVisibility());

        values.put(COLUMN_UNIQUE_ID, reachDatabase.getUniqueId());

        return values;
    }

    public static final String[] ADAPTER_LIST = new String[]{ //count = 14

            ReachDatabaseHelper.COLUMN_ID, //0
            ReachDatabaseHelper.COLUMN_SIZE, //1
            ReachDatabaseHelper.COLUMN_SENDER_ID, //2
            ReachDatabaseHelper.COLUMN_PROCESSED, //3
            ReachDatabaseHelper.COLUMN_PATH, //4
            ReachDatabaseHelper.COLUMN_DISPLAY_NAME, //5
            ReachDatabaseHelper.COLUMN_ARTIST, //6
            ReachDatabaseHelper.COLUMN_IS_LIKED, //7
            ReachDatabaseHelper.COLUMN_DURATION, //8

            ///////////////

            ReachDatabaseHelper.COLUMN_STATUS, //9
            ReachDatabaseHelper.COLUMN_OPERATION_KIND, //10
            ReachDatabaseHelper.COLUMN_SENDER_NAME, //11
            ReachDatabaseHelper.COLUMN_RECEIVER_ID, //12
            ReachDatabaseHelper.COLUMN_LOGICAL_CLOCK, //13
            ReachDatabaseHelper.COLUMN_SONG_ID, //14
            ReachDatabaseHelper.COLUMN_ALBUM_ART_DATA //15
    };

    public static MusicData getMusicData(final Cursor cursor) {

        final boolean liked;
        final String temp = cursor.getString(7);
        liked = !TextUtils.isEmpty(temp) && temp.equals("1");

        return new MusicData(
                cursor.getLong(0), //id
                cursor.getLong(1), //length
                cursor.getLong(2), //senderId
                cursor.getLong(3), //processed
                cursor.getString(4), //path
                cursor.getString(5), //displayName
                cursor.getString(6), //artistName
                liked, //liked
                cursor.getLong(8), //duration
                (byte) 0); //type
    }

    public ReachDatabaseHelper(Context context) {
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
//        db.execSQL("DROP TABLE IF EXISTS " + REACH_TABLE);
        database.execSQL("ALTER TABLE " + REACH_TABLE + " ADD COLUMN " + COLUMN_ALBUM_ART_DATA + " blob");
        database.execSQL("ALTER TABLE " + REACH_TABLE + " ADD COLUMN " + COLUMN_ALBUM + " text");
        database.execSQL("ALTER TABLE " + REACH_TABLE + " ADD COLUMN " + COLUMN_GENRE + " text");
        database.execSQL("ALTER TABLE " + REACH_TABLE + " ADD COLUMN " + COLUMN_VISIBILITY + " short");
        database.execSQL("ALTER TABLE " + REACH_TABLE + " ADD COLUMN " + COLUMN_UNIQUE_ID + " long");
        //onCreate(database);
    }
}
