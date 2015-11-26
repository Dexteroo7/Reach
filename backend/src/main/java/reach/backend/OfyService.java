package reach.backend;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;

/**
 * Objectify service wrapper so we can statically register our persistence classes
 * More on Objectify here : https://code.google.com/p/objectify-appengine/
 */
public class OfyService {

    public static String BASE_LOG_TRANSACTION = "http://54.169.227.37:8080/campaign/transaction?";
    public static String BASE_LOG_NEW_FRIEND = "http://54.169.227.37:8080/campaign/newFriends?";

    public static String USER_ID = "userId";
    public static String FRIEND_ID = "friendId";
    public static String SONG_ID = "songId";

    public static final long devikaId = 5666668701286400L;
    public static final String devikaPhoneNumber = "8860872102";

//    static {
//        ObjectifyService.register(ReachUser.class);
//        ObjectifyService.register(FeedBack.class);
//        ObjectifyService.register(CompletedOperations.class);
//        ObjectifyService.register(MusicData.class);
//        ObjectifyService.register(Notification.class);
//
//        ObjectifyService.register(Like.class);
//        ObjectifyService.register(BecameFriends.class);
//        ObjectifyService.register(Push.class);
//        ObjectifyService.register(PushAccepted.class);
//    }

    public static Objectify ofy() {
        return ObjectifyService.ofy();
    }

//    private static ObjectifyFactory factory() {
//        return ObjectifyService.factory();
//    }

    public static long longHash(String string) {
        long h = 1125899906842597L; // prime
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = 31*h + string.charAt(i);
        }
        return h;
    }
}
