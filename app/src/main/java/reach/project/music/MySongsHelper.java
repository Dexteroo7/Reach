package reach.project.music;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import reach.project.reachProcess.auxiliaryClasses.MusicData;

/**
 * Created by Dexter on 2/14/2015.
 */
public class MySongsHelper extends SQLiteOpenHelper {

    public static final String SONG_TABLE = "songs";
    public static final String COLUMN_ID = "_id";

    public static final String COLUMN_SONG_ID = "songId";
    public static final String COLUMN_USER_ID = "userId";

    public static final String COLUMN_DISPLAY_NAME = "displayName"; //title
    public static final String COLUMN_ACTUAL_NAME = "actualName";
    public static final String COLUMN_ARTIST = "artist"; //artist
    public static final String COLUMN_ALBUM = "album"; //album
    public static final String COLUMN_DURATION = "duration"; //duration
    public static final String COLUMN_SIZE = "size";
    public static final String COLUMN_GENRE = "genre";
    public static final String COLUMN_ALBUM_ART_DATA = "albumArtData";
    public static final String COLUMN_PATH = "path";
    public static final String COLUMN_YEAR = "year";
    public static final String COLUMN_DATE_ADDED = "dateAdded";
    public static final String COLUMN_VISIBILITY = "visibility";
    public static final String COLUMN_IS_LIKED = "isLiked";

    private static final String DATABASE_NAME = "reach.database.sql.MySongsHelper";
    private static final int DATABASE_VERSION = 3;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + SONG_TABLE + "(" + COLUMN_ID
            + " integer primary key autoincrement, " +
            COLUMN_SONG_ID + " long" + "," +
            COLUMN_DISPLAY_NAME + " text" + "," +
            COLUMN_ACTUAL_NAME + " text" + "," +
            COLUMN_GENRE + " text" + "," +
            COLUMN_PATH + " text" + "," +
            COLUMN_ARTIST + " text" + "," +
            COLUMN_DURATION + " long" + "," +
            COLUMN_ALBUM + " text" + "," +
            COLUMN_ALBUM_ART_DATA + " blob" + "," +

            COLUMN_USER_ID + " long" + "," +
            COLUMN_SIZE + " long" + "," +
            COLUMN_YEAR + " long" + "," +
            COLUMN_DATE_ADDED + " long" + "," +
            COLUMN_IS_LIKED + " short" + "," +
            COLUMN_VISIBILITY + " short" + " )";

    public static final String[] projection =
            {
                    COLUMN_ID,
                    COLUMN_IS_LIKED,

                    COLUMN_SONG_ID,
                    COLUMN_DISPLAY_NAME,
                    COLUMN_ACTUAL_NAME,
                    COLUMN_GENRE,
                    COLUMN_PATH,
                    COLUMN_ARTIST,
                    COLUMN_DURATION,
                    COLUMN_ALBUM,
                    COLUMN_ALBUM_ART_DATA,

                    COLUMN_USER_ID,
                    COLUMN_SIZE,
                    COLUMN_YEAR,
                    COLUMN_DATE_ADDED,
                    COLUMN_VISIBILITY
            };

    public static ContentValues contentValuesCreator(Song song,
                                                     long serverId) {

        final ContentValues values = new ContentValues();

        values.put(COLUMN_IS_LIKED, song.isLiked);
        values.put(COLUMN_SONG_ID, song.songId);

        values.put(COLUMN_DISPLAY_NAME, song.displayName);
        values.put(COLUMN_ACTUAL_NAME, song.actualName);
        values.put(COLUMN_GENRE, song.genre);
        values.put(COLUMN_PATH, song.path);
        values.put(COLUMN_ARTIST, song.artist);
        values.put(COLUMN_DURATION, song.duration);
        values.put(COLUMN_ALBUM, song.album);
        if (song.albumArtData != null)
            values.put(COLUMN_ALBUM_ART_DATA, song.albumArtData.toByteArray());

        values.put(COLUMN_USER_ID, serverId);
        values.put(COLUMN_SIZE, song.size);
        values.put(COLUMN_YEAR, song.year);
        values.put(COLUMN_DATE_ADDED, song.dateAdded);
        values.put(COLUMN_VISIBILITY, song.visibility);

        return values;
    }

    public static final String[] DISK_LIST = new String[]{ //count = 8
            MySongsHelper.COLUMN_SONG_ID, //0
            MySongsHelper.COLUMN_SIZE, //1
            MySongsHelper.COLUMN_PATH, //2
            MySongsHelper.COLUMN_DISPLAY_NAME, //3
            MySongsHelper.COLUMN_ARTIST, //4
            MySongsHelper.COLUMN_DURATION, //5
            MySongsHelper.COLUMN_ALBUM, //6
            MySongsHelper.COLUMN_ID, //7
            MySongsHelper.COLUMN_ALBUM_ART_DATA, //8
            MySongsHelper.COLUMN_ACTUAL_NAME, //9
            MySongsHelper.COLUMN_DATE_ADDED //9
    };

    //DISK_LIST specific !
    public static MusicData getMusicData(final Cursor cursor, final long serverId) {

        return new MusicData(
                cursor.getLong(0), //songId
                cursor.getLong(1), //length
                serverId, //senderId
                cursor.getLong(1), //processed = length
                cursor.getString(2), //path
                cursor.getString(3), //displayName
                cursor.getString(4), //artistName
                false, //liked
                cursor.getLong(5), //duration
                (byte) 1); //type
    }

    public MySongsHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(MySongsHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + SONG_TABLE);
//        database.execSQL("ALTER TABLE " + SONG_TABLE + " ADD COLUMN " + COLUMN_ARTIST_MBID + " text");
//        database.execSQL("ALTER TABLE " + SONG_TABLE + " ADD COLUMN " + COLUMN_RELEASE_GROUP_MBID + " text");
//        database.execSQL("ALTER TABLE " + SONG_TABLE + " ADD COLUMN " + COLUMN_IS_LIKED + " short");
        onCreate(database);
    }
}
