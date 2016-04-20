package reach.project.coreViews.saved_songs;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by gauravsobti on 19/04/16.
 */
public class SavedSongsHelper extends SQLiteOpenHelper {


    private static final String DATABASE_NAME = "reach.database.sql.ReachSavedSongsHelper";
    private static final int DATABASE_VERSION = 1;


    public SavedSongsHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        final String SQL_CREATE_SAVED_SONGS_TABLE = "CREATE TABLE " + SavedSongsContract.SavedSongsEntry.TABLE_NAME + " (" +
                SavedSongsContract.SavedSongsEntry._ID + " INTEGER PRIMARY KEY," +
                SavedSongsContract.SavedSongsEntry.ARTIST_ALBUM_NAME + " TEXT, " +
        SavedSongsContract.SavedSongsEntry.YOUTUBE_ID + " INTEGER NOT NULL, "+
        SavedSongsContract.SavedSongsEntry.DATE_ADDED + " INTEGER NOT NULL, "+
        SavedSongsContract.SavedSongsEntry.DISPLAY_NAME +" TEXT, " +
        SavedSongsContract.SavedSongsEntry.SENDER_ID + " INTEGER NOT NULL, " +
        SavedSongsContract.SavedSongsEntry.SENDER_NAME + " TEXT, " +
        SavedSongsContract.SavedSongsEntry.SONG_NAME + " TEXT, " +
        SavedSongsContract.SavedSongsEntry.TYPE + " INTEGER NOT NULL " +
                " );";

        db.execSQL(SQL_CREATE_SAVED_SONGS_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + SavedSongsContract.SavedSongsEntry.TABLE_NAME);
        onCreate(db);

    }
}
