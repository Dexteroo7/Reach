package reach.project.music;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import reach.project.reachProcess.auxiliaryClasses.MusicData;

/**
 * Created by Dexter on 2/14/2015.
 */
public class MySongsHelper extends SQLiteOpenHelper {

    public static final String SONG_TABLE = "songs";
    public static final String COLUMN_ID = "_id";

    //identity of the song (meta-data hash ?)
    public static final String COLUMN_SONG_ID = "songId";
    public static final String COLUMN_META_HASH = "metaHash";

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
    private static final int DATABASE_VERSION = 5;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + SONG_TABLE + "(" + COLUMN_ID
            + " integer primary key autoincrement, " +
            COLUMN_SONG_ID + " long" + "," +
            COLUMN_META_HASH + " text" + "," +

            COLUMN_DISPLAY_NAME + " text" + "," +
            COLUMN_ACTUAL_NAME + " text" + "," +
            COLUMN_GENRE + " text" + "," +
            COLUMN_PATH + " text" + "," +
            COLUMN_ARTIST + " text" + "," +
            COLUMN_DURATION + " long" + "," +

            COLUMN_ALBUM + " text" + "," +
            COLUMN_ALBUM_ART_DATA + " blob" + "," +

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

                    COLUMN_SIZE,
                    COLUMN_YEAR,
                    COLUMN_DATE_ADDED,
                    COLUMN_VISIBILITY,
                    COLUMN_META_HASH
            };
    private static final String BLANK = "";

    public static ContentValues contentValuesCreator(Song song) {

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

        if (!TextUtils.isEmpty(song.fileHash))
            values.put(COLUMN_META_HASH, song.fileHash);

        values.put(COLUMN_SIZE, song.size);
        values.put(COLUMN_YEAR, song.year);
        values.put(COLUMN_DATE_ADDED, song.dateAdded);
        values.put(COLUMN_VISIBILITY, song.visibility);

        return values;
    }

    public static final String[] DISK_LIST = new String[]{ //count = 8
            COLUMN_SONG_ID, //0
            COLUMN_SIZE, //1
            COLUMN_PATH, //2
            COLUMN_DISPLAY_NAME, //3
            COLUMN_ARTIST, //4
            COLUMN_DURATION, //5
            COLUMN_ALBUM, //6
            COLUMN_ID, //7
            COLUMN_ALBUM_ART_DATA, //8
            COLUMN_ACTUAL_NAME, //9
            COLUMN_DATE_ADDED, //10
            COLUMN_VISIBILITY, //11
            COLUMN_IS_LIKED, //12
            COLUMN_META_HASH, //13
            COLUMN_GENRE //14
    };

    public static final String[] SONG_LIST = new String[]{

            COLUMN_SONG_ID, //0

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

    public static Song convertMusicDataToSong(final MusicData data){

        return new Song.Builder()
                .album(data.getAlbumName())
                .artist(data.getArtistName())
                .dateAdded(data.getDateAdded())
                .displayName(data.getDisplayName())
                .duration(data.getDuration())
                .fileHash(data.getMetaHash())
                .isLiked(data.isLiked())
                .path(data.getPath())
                .songId(data.getId())
                .size(data.getLength())
                .build();

    }

    //DISK_LIST specific !
    public static MusicData getMusicData(final Cursor cursor, final long serverId) {

        return new MusicData(
                cursor.getLong(0), //songId
                cursor.getString(13), //meta-hash
                cursor.getLong(1), //length
                serverId, //senderId
                cursor.getLong(1), //processed = length
                cursor.getLong(9), //date added
                cursor.getString(2), //path
                cursor.getString(3), //displayName
                cursor.getString(4), //artistName
                cursor.getString(6), //albumName
                cursor.getShort(12) == 1, //liked
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
        onCreate(database);
    }
}
