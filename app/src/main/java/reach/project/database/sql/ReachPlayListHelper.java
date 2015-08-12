package reach.project.database.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.List;

import reach.project.utils.auxiliaryClasses.Playlist;

/**
 * Created by Dexter on 2/14/2015.
 */
public class ReachPlayListHelper extends SQLiteOpenHelper {

    public static final String PLAY_LIST_TABLE = "playLists";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PLAY_LIST_NAME = "playList";
    public static final String COLUMN_DATE_MODIFIED = "dateModified";
    public static final String COLUMN_USER_ID = "userId";
    public static final String COLUMN_SIZE = "size";
    public static final String COLUMN_VISIBILITY = "visibility";
    public static final String COLUMN_ARRAY_OF_SONG_IDS = "arrayOfSongIds";

    private static final String DATABASE_NAME = "reach.database.sql.ReachPlayListHelper";
    private static final int DATABASE_VERSION = 2;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + PLAY_LIST_TABLE + "(" + COLUMN_ID
            + " integer primary key autoincrement, " +
            COLUMN_PLAY_LIST_NAME + " text" + "," +
            COLUMN_DATE_MODIFIED + " text" + "," +
            COLUMN_ARRAY_OF_SONG_IDS + " blob" + "," +
            COLUMN_VISIBILITY + " short" + "," +
            COLUMN_USER_ID + " long" + "," +
            COLUMN_SIZE + " int" + " )";

    public static final String[] projection =
            {
                    COLUMN_ID, //0
                    COLUMN_PLAY_LIST_NAME, //1
                    COLUMN_DATE_MODIFIED, //2
                    COLUMN_ARRAY_OF_SONG_IDS, //3
                     COLUMN_VISIBILITY, //4
                    COLUMN_USER_ID, //5
                    COLUMN_SIZE, //6
            };

    public static byte[] toBytes(long data) {

        return new byte[]{
                (byte) ((data >> 56) & 0xff),
                (byte) ((data >> 48) & 0xff),
                (byte) ((data >> 40) & 0xff),
                (byte) ((data >> 32) & 0xff),
                (byte) ((data >> 24) & 0xff),
                (byte) ((data >> 16) & 0xff),
                (byte) ((data >> 8) & 0xff),
                (byte) ((data) & 0xff),
        };
    }

    public static byte[] toBytes(List<Long> data) {

        if (data == null || data.isEmpty())
            return new byte[0];

        final byte[] bytes = new byte[data.size() * 8];
        for (int i = 0; i < data.size(); i++)
            System.arraycopy(toBytes(data.get(i)), 0, bytes, i * 8, 8);
        return bytes;
    }

    public static long toLong(byte[] data) {

        if (data == null || data.length != 8)
            return 0x0;
        // ----------
        // (Below) convert to longs before shift because digits
        //         are lost with ints beyond the 32-bit limit
        return (long) (0xff & data[0]) << 56 |
                (long) (0xff & data[1]) << 48 |
                (long) (0xff & data[2]) << 40 |
                (long) (0xff & data[3]) << 32 |
                (long) (0xff & data[4]) << 24 |
                (long) (0xff & data[5]) << 16 |
                (long) (0xff & data[6]) << 8 |
                (long) (0xff & data[7]);
    }

    public static long[] toLongArray(byte[] data) {

        if (data == null || data.length == 0 || data.length % 8 != 0)
            return new long[0];
        // ----------
        long[] longs = new long[data.length / 8];
        for (int i = 0; i < longs.length; i++) {
            longs[i] = toLong(new byte[]{
                    data[(i * 8)],
                    data[(i * 8) + 1],
                    data[(i * 8) + 2],
                    data[(i * 8) + 3],
                    data[(i * 8) + 4],
                    data[(i * 8) + 5],
                    data[(i * 8) + 6],
                    data[(i * 8) + 7],
            });
        }
        return longs;
    }

    public static String[] toStringArray(byte[] data) {

        Log.i("Ayush", "Play lists " + data);

        if (data == null || data.length == 0 || data.length % 8 != 0)
            return new String[0];
        // ----------
        String[] strings = new String[data.length / 8];

        for (int i = 0; i < strings.length; i++) {
            strings[i] = toLong(new byte[]{
                    data[(i * 8)],
                    data[(i * 8) + 1],
                    data[(i * 8) + 2],
                    data[(i * 8) + 3],
                    data[(i * 8) + 4],
                    data[(i * 8) + 5],
                    data[(i * 8) + 6],
                    data[(i * 8) + 7],
            }) + "";
        }
        return strings;
    }

    public static ContentValues contentValuesCreator(Playlist playlist, long serverId) {

        final ContentValues values = new ContentValues();

        values.put(COLUMN_PLAY_LIST_NAME, playlist.playlistName);
        values.put(COLUMN_DATE_MODIFIED, playlist.dateModified);
        values.put(COLUMN_VISIBILITY, (short) (playlist.visibility ? 1 : 0));
        values.put(COLUMN_USER_ID, serverId);

        values.put(COLUMN_SIZE, playlist.reachSongs.size());
        values.put(COLUMN_ARRAY_OF_SONG_IDS, toBytes(playlist.reachSongs));
        return values;
    }

    public ReachPlayListHelper(Context context) {
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
        db.execSQL("DROP TABLE IF EXISTS " + PLAY_LIST_TABLE);
        onCreate(db);
    }
}
