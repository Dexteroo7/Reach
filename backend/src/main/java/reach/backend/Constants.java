package reach.backend;

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
}
