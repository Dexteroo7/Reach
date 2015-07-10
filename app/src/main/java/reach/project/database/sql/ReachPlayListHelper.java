package reach.project.database.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;

import reach.backend.entities.userApi.model.ReachPlayList;

/**
 * Created by Dexter on 2/14/2015.
 */
public class ReachPlayListHelper extends SQLiteOpenHelper {

    public static final String PLAY_LIST_TABLE = "playLists";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PLAY_LIST_ID  = "playListId";
    public static final String COLUMN_PLAY_LIST_NAME = "playList";
    public static final String COLUMN_DATE_MODIFIED = "dateModified";
    public static final String COLUMN_USER_ID = "userId";
    public static final String COLUMN_SIZE = "size";
    public static final String COLUMN_VISIBILITY = "visibility";
    public static final String COLUMN_ARRAY_OF_SONG_IDS = "arrayOfSongIds";

    private static final String DATABASE_NAME = "reach.database.sql.ReachPlayListHelper";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + PLAY_LIST_TABLE + "(" + COLUMN_ID
            + " integer primary key autoincrement, " +
            COLUMN_PLAY_LIST_ID + " long" + "," +
            COLUMN_PLAY_LIST_NAME + " text" + "," +
            COLUMN_DATE_MODIFIED + " text" + "," +
            COLUMN_ARRAY_OF_SONG_IDS + " text" + "," +
            COLUMN_VISIBILITY + " short" + "," +
            COLUMN_USER_ID + " long" + "," +
            COLUMN_SIZE + " int" + " )";

    public static final String[] projection =
            {
                    COLUMN_ID,
                    COLUMN_PLAY_LIST_ID,
                    COLUMN_PLAY_LIST_NAME,
                    COLUMN_DATE_MODIFIED,
                    COLUMN_ARRAY_OF_SONG_IDS,
                    COLUMN_VISIBILITY,
                    COLUMN_USER_ID,
                    COLUMN_SIZE,
            };

    public static ReachPlayList cursorToProcess(Cursor cursor) {

        final ReachPlayList reachPlayListDatabase = new ReachPlayList();
        reachPlayListDatabase.setPlayListId(cursor.getLong(1));
        reachPlayListDatabase.setPlaylistName(cursor.getString(2));
        reachPlayListDatabase.setDateModified(cursor.getString(3));
        if(!TextUtils.isEmpty(cursor.getString(4)))
            reachPlayListDatabase.setReachSongs(Arrays.asList(cursor.getString(4).split(" ")));
        reachPlayListDatabase.setVisibility((int) cursor.getShort(5));
        reachPlayListDatabase.setUserId(cursor.getLong(6));
        return reachPlayListDatabase;
    }

    public static ContentValues contentValuesCreator(ReachPlayList reachPlayListDatabase) {

        final ContentValues values = new ContentValues();

        values.put(COLUMN_PLAY_LIST_ID, reachPlayListDatabase.getPlayListId());
        values.put(COLUMN_PLAY_LIST_NAME, reachPlayListDatabase.getPlaylistName());
        values.put(COLUMN_DATE_MODIFIED, reachPlayListDatabase.getDateModified());
        final StringBuilder stringBuilder = new StringBuilder();
        if(reachPlayListDatabase.getReachSongs() != null && reachPlayListDatabase.getReachSongs().size() > 0) {
            for (String songId : reachPlayListDatabase.getReachSongs())
                stringBuilder.append(songId).append(" ");
            values.put(COLUMN_ARRAY_OF_SONG_IDS, stringBuilder.toString().trim());
        } else
            values.put(COLUMN_ARRAY_OF_SONG_IDS, "");
        values.put(COLUMN_VISIBILITY, reachPlayListDatabase.getVisibility());
        values.put(COLUMN_USER_ID, reachPlayListDatabase.getUserId());
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
