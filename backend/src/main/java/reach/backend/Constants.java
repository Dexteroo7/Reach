package reach.backend;

import java.util.Comparator;

import reach.backend.ObjectWrappers.App;
import reach.backend.ObjectWrappers.Song;

/**
 * Created by dexter on 13/12/15.
 */
public enum Constants {
    ;

    public static String BASE_IP = "http://52.74.117.248:8080/";

    public static String BASE_LOG_TRANSACTION = BASE_IP + "campaign/transaction?";
    public static String BASE_LOG_NEW_FRIEND = BASE_IP + "campaign/newFriends?";

    public static String USER_ID = "userId";
    public static String FRIEND_ID = "friendId";
    public static String SONG_ID = "songId";

    public static final long devikaId = 5666668701286400L;
    public static final String devikaPhoneNumber = "8860872102";

    //sort by recently added
    public static final Comparator<Song> dateAddedComparatorMusic = new Comparator<Song>() {
        @Override
        public int compare(Song lhs, Song rhs) {

            final Long a = lhs.dateAdded == null ? 0 : lhs.dateAdded;
            final Long b = rhs.dateAdded == null ? 0 : rhs.dateAdded;
            return a.compareTo(b);
        }
    };

    //sort by recently added
    public static final Comparator<App> dateAddedComparatorApps = new Comparator<App>() {
        @Override
        public int compare(App lhs, App rhs) {

            final Long a = lhs.installDate == null ? 0 : lhs.installDate;
            final Long b = rhs.installDate == null ? 0 : rhs.installDate;
            return a.compareTo(b);
        }
    };
}
