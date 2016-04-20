package reach.project.coreViews.saved_songs;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by gauravsobti on 19/04/16.
 */
public class SavedSongsContract {



    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "reach.project.coreViews.saved_songs.SavedSongsProvider";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths (appended to base content URI for possible URI's)
    // For instance, content://com.example.android.sunshine.app/weather/ is a valid path for
    // looking at weather data. content://com.example.android.sunshine.app/givemeroot/ will fail,
    // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
    // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
    public static final String PATH_SAVED_SONGS = "saved_songs";

    public static final class SavedSongsEntry implements BaseColumns{

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SAVED_SONGS).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_SAVED_SONGS;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_SAVED_SONGS;

        public static final String TABLE_NAME = "saved_songs";

        public static final String YOUTUBE_ID = "youtube_id";
        public static final String DATE_ADDED = "date_added";
        public static final String SENDER_NAME = "sender_name";
        public static final String SENDER_ID = "sender_id";
        //Type of saved songs: history or user_saved
        public static final String TYPE = "type";
        public static final String SONG_NAME = "song_name";
        public static final String ARTIST_ALBUM_NAME = "artist_album_name";
        public static final String DISPLAY_NAME = "display_name";


        public static String [] projection = new String[]{
                YOUTUBE_ID, //0
                DATE_ADDED ,//1
                SENDER_NAME ,//2
                SENDER_ID ,//3
                TYPE ,//4
                SONG_NAME ,//5
                ARTIST_ALBUM_NAME ,//6
                DISPLAY_NAME,//7
                _ID//8
        };


        public static Uri buildSavedSongUri(long _id) {
            return ContentUris.withAppendedId(CONTENT_URI, _id);
        }
        public static Uri buildSavedSongUri(String youtube_id) {
            return CONTENT_URI.buildUpon().appendPath(youtube_id).build();
        }



    }

}
