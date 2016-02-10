package com.reach.backend;


public class Constants {

    public static final String WEB_CLIENT_ID = "528178870551-m0emotumn7gb4r9hted42p58tr3s8ukt.apps.googleusercontent.com";
    public static final String ASHISH_DEBUG_KEY = "528178870551-l65e1l1621dk3prvfhdjnm7h2i7i59k4.apps.googleusercontent.com";
    public static final String AYUSH_DEBUG_KEY = "528178870551-qsb1sprlv2s6mt3tkdsticgmr64k776j.apps.googleusercontent.com";
    public static final String ANDROID_CLIENT_ID = "528178870551-8824ij4gv9e6dnfgg01ve0f99dt8facf.apps.googleusercontent.com";
    public static final String ANDROID_AUDIENCE = WEB_CLIENT_ID;

    public static final String EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email";

    public static String BASE_IP = "http://52.74.117.248:8080/";

    public static String BASE_LOG_TRANSACTION = BASE_IP + "campaign/transaction?";
    public static String BASE_LOG_NEW_FRIEND = BASE_IP + "campaign/newFriends?";

    public static String USER_ID = "userId";
    public static String FRIEND_ID = "friendId";
    public static String SONG_ID = "songId";

    public static final long devikaId = 5666668701286400L;
    public static final String devikaPhoneNumber = "8860872102";

    //sort by recently added
//    public static final Comparator<Song> dateAddedComparatorMusic = new Comparator<Song>() {
//        @Override
//        public int compare(Song lhs, Song rhs) {
//
//            final Long a = lhs.dateAdded == null ? 0 : lhs.dateAdded;
//            final Long b = rhs.dateAdded == null ? 0 : rhs.dateAdded;
//            return a.compareTo(b);
//        }
//    };
//
//    //sort by recently added
//    public static final Comparator<App> dateAddedComparatorApps = new Comparator<App>() {
//        @Override
//        public int compare(App lhs, App rhs) {
//
//            final Long a = lhs.installDate == null ? 0 : lhs.installDate;
//            final Long b = rhs.installDate == null ? 0 : rhs.installDate;
//            return a.compareTo(b);
//        }
//    };
}