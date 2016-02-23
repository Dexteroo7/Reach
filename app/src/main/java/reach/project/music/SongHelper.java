package reach.project.music;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.hash.Hashing;

import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.utils.MiscUtils;

/**
 * Created by Dexter on 2/14/2015.
 */
public class SongHelper extends SQLiteOpenHelper {

    /**
     * COLUMN_ID : local dataBase table id, used exclusively for referencing locally
     * COLUMN_SONG_ID : songId of where the song is located, used for referencing the MySongsHelper table (upload/download both)
     * COLUMN_META_HASH : hash of displayName, duration and size and userId
     * COLUMN_UNIQUE_ID : random UUID
     */

    public static final String REACH_TABLE = "reach";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SONG_ID = "songId";
    public static final String COLUMN_META_HASH = "metaHash";

    public static final String COLUMN_DISPLAY_NAME = "displayName"; //title
    public static final String COLUMN_ACTUAL_NAME = "actualName";
    public static final String COLUMN_ARTIST = "globalIp"; //artist
    public static final String COLUMN_ALBUM = "album"; //artist

    public static final String COLUMN_DURATION = "lastActive"; //duration
    public static final String COLUMN_SIZE = "length"; //size

    public static final String COLUMN_GENRE = "genre";
    public static final String COLUMN_ALBUM_ART_DATA = "albumArtData";
    public static final String COLUMN_PATH = "path";
    public static final String COLUMN_DATE_ADDED = "added";

    public static final String COLUMN_VISIBILITY = "visibility";
    public static final String COLUMN_IS_LIKED = "globalPort";

    public static final String COLUMN_RECEIVER_ID = "receiverId"; //in-case of downloads = myId
    public static final String COLUMN_SENDER_ID = "senderId"; // in-case of uploads = myId

    //non song stuff
    public static final String COLUMN_UNIQUE_ID = "uniqueId";
    public static final String COLUMN_SENDER_NAME = "localIp";
    public static final String COLUMN_ONLINE_STATUS = "localPort";
    public static final String COLUMN_OPERATION_KIND = "operationKind";
    public static final String COLUMN_LOGICAL_CLOCK = "logicalClock";
    public static final String COLUMN_PROCESSED = "processed";
    public static final String COLUMN_STATUS = "status";

    private static final String DATABASE_NAME = "reach.database.sql.ReachDatabaseHelper";
    private static final int DATABASE_VERSION = 4;

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
                    COLUMN_UNIQUE_ID, //22
                    COLUMN_META_HASH //23
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
            reachDatabase.setIsLiked(liked.equals("1") || liked.equals("true"));

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

        //set the metaHash if absent
        if (TextUtils.isEmpty(reachDatabase.getMetaHash()))
            reachDatabase.setMetaHash(MiscUtils.calculateSongHash(
                    reachDatabase.getReceiverId(), reachDatabase.getDuration(),
                    reachDatabase.getLength(), reachDatabase.getDisplayName(),
                    Hashing.sipHash24()));
        values.put(COLUMN_META_HASH, reachDatabase.getMetaHash());

        Log.i("Ayush", "Saving reach database with hash " + reachDatabase.getMetaHash());

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

    public SongHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + REACH_TABLE + "(" + COLUMN_ID
            + " integer primary key autoincrement, " +

            COLUMN_SONG_ID + " long" + "," +
            COLUMN_META_HASH + " text" + "," +

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

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {

        Log.w(SongHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
//        database.execSQL("DROP TABLE IF EXISTS " + REACH_TABLE);
        if (newVersion > oldVersion)
            database.execSQL("ALTER TABLE " + REACH_TABLE + " ADD COLUMN " + COLUMN_META_HASH + " text");
//        database.execSQL("ALTER TABLE " + REACH_TABLE + " ADD COLUMN " + COLUMN_ALBUM + " text");
//        database.execSQL("ALTER TABLE " + REACH_TABLE + " ADD COLUMN " + COLUMN_GENRE + " text");
//        database.execSQL("ALTER TABLE " + REACH_TABLE + " ADD COLUMN " + COLUMN_VISIBILITY + " short");
//        database.execSQL("ALTER TABLE " + REACH_TABLE + " ADD COLUMN " + COLUMN_UNIQUE_ID + " long");
//        onCreate(database);
    }

    public static final String[] MUSIC_DATA_LIST = new String[]{ //count = 14

            COLUMN_ID, //0
            COLUMN_SIZE, //1
            COLUMN_SENDER_ID, //2
            COLUMN_PROCESSED, //3
            COLUMN_PATH, //4
            COLUMN_DISPLAY_NAME, //5
            COLUMN_ARTIST, //6
            COLUMN_IS_LIKED, //7
            COLUMN_DURATION, //8

            ///////////////

            COLUMN_STATUS, //9
            COLUMN_OPERATION_KIND, //10
            COLUMN_SENDER_NAME, //11
            COLUMN_RECEIVER_ID, //12
            COLUMN_LOGICAL_CLOCK, //13
            COLUMN_SONG_ID, //14
            COLUMN_ALBUM, //15
            COLUMN_ACTUAL_NAME, //16
            COLUMN_DATE_ADDED, //17
            COLUMN_VISIBILITY, //18
            COLUMN_META_HASH //19
    };

    public static final String[] SONG_LIST = new String[]{

            COLUMN_UNIQUE_ID, //0

            COLUMN_DISPLAY_NAME, //1
            COLUMN_ACTUAL_NAME, //2
            COLUMN_ALBUM, //3
            COLUMN_ARTIST, //4

            COLUMN_SIZE, //5
            COLUMN_DURATION, //6
            COLUMN_VISIBILITY, //7
            COLUMN_PATH, //8
            COLUMN_DATE_ADDED, //9
            COLUMN_GENRE, //10
            COLUMN_IS_LIKED, //11
    };

    private final String[] projectionDownloaded =
            {
                    SongHelper.COLUMN_ID, //0

                    SongHelper.COLUMN_UNIQUE_ID, //1

                    SongHelper.COLUMN_DISPLAY_NAME, //2
                    SongHelper.COLUMN_ACTUAL_NAME, //3

                    SongHelper.COLUMN_ARTIST, //4
                    SongHelper.COLUMN_ALBUM, //5

                    SongHelper.COLUMN_DURATION, //6
                    SongHelper.COLUMN_SIZE, //7

                    SongHelper.COLUMN_VISIBILITY, //8
                    SongHelper.COLUMN_GENRE, //9
            };

    public static MusicData getMusicData(final Cursor cursor) {

        final String temp = cursor.getString(7);

        return new MusicData(
                cursor.getLong(0), //id
                cursor.getString(19), //meta-hash
                cursor.getLong(1), //length
                cursor.getLong(2), //senderId
                cursor.getLong(3), //processed
                cursor.getLong(17), //date added
                cursor.getString(4), //path
                cursor.getString(5), //displayName
                cursor.getString(6), //artistName
                cursor.getString(15), //album
                !TextUtils.isEmpty(temp) && temp.equals("1"), //liked
                cursor.getLong(8), //duration
                MusicData.Type.DOWNLOADED); //type
    }

    public static Song getSong(final Cursor cursor) {

        if (cursor.getColumnCount() != SONG_LIST.length)
            throw new IllegalArgumentException("Provided cursor of invalid length");

        final String liked = cursor.getString(11);
        final boolean isLiked = !TextUtils.isEmpty(liked) && liked.equals("1");

        return new Song.Builder()
                .size(cursor.getLong(5))
                .visibility(cursor.getShort(7) == 1)
                .path(cursor.getString(8))
                .duration(cursor.getLong(6))
                .actualName(cursor.getString(2))
                .album(cursor.getString(3))
                .albumArtData(new AlbumArtData.Builder().build())
                .artist(cursor.getString(4))
                .dateAdded(cursor.getLong(9))
                .displayName(cursor.getString(1))
                .fileHash("")
                .genre(cursor.getString(10))
                .isLiked(isLiked)
                .songId(cursor.getLong(0)).build();
    }
}