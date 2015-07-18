//package reach.backend.User.FriendContainers;
//
//import java.util.HashSet;
//
//import reach.backend.User.ReachUser;
//
///**
// * Created by dexter on 17/07/15.
// */
//public class Friend {
//
//    public static String[] projectNewFriend = new String[]{
//            "numberOfSongs",
//            "userName",
//            "imageId",
//            "phoneNumber"};
//
//    private final long id;
//    private final int numberOfSongs;
//    private final String phoneNumber;
//    private final String userName;
//    private final String imageId;
//    /*
//        0 - offline and permission-request granted
//        1 - online and permission-request granted
//        2 - request sent but permission not granted
//        3 - request not sent and permission not granted
//    */
//    private final short status;
//    private final long lastSeen;
//
//    /**
//     * To be used when host is a new friends with status 3
//     * @param host the friend
//     */
//    public Friend(ReachUser host) {
//
//        this.id = host.getId();
//        this.phoneNumber = host.getPhoneNumber();
//        this.userName = host.getUserName();
//        this.imageId = host.getImageId();
//        this.numberOfSongs = host.getNumberOfSongs();
//        this.lastSeen = ReachUser.ONLINE_LIMIT + 1;
//        this.status = 3;
//    }
//
//    /**
//     * General purpose constructor
//     * @param host the friend
//     * @param myReach list of friends with status < 2
//     * @param sentRequests list of friends with status 2
//     * @param lastSeen lastSeen parameter (decides status 01)
//     */
//    public Friend(ReachUser host,
//                  HashSet<Long> myReach,
//                  HashSet<Long> sentRequests,
//                  long lastSeen) {
//
//        this.id = host.getId();
//        this.phoneNumber = host.getPhoneNumber();
//        this.userName = host.getUserName();
//        this.imageId = host.getImageId();
//        this.numberOfSongs = host.getNumberOfSongs();
//        this.lastSeen = lastSeen;
//
//        if (myReach != null && myReach.contains(id))
//            if (lastSeen > ReachUser.ONLINE_LIMIT)
//                this.status = 0;     //offline and permission-request granted
//            else
//                this.status = 1;  //online and permission-request granted
//        else if (sentRequests != null && sentRequests.contains(id))
//            this.status = 2; //request sent but permission not granted
//        else
//            this.status = 3; //request not sent and permission not granted
//    }
//
//    public long getLastSeen() {
//        return lastSeen;
//    }
//
//    public short getStatus() {
//        return status;
//    }
//
//    public String getImageId() {
//        return imageId;
//    }
//
//    public String getUserName() {
//        return userName;
//    }
//
//    public int getNumberOfSongs() {
//        return numberOfSongs;
//    }
//
//    public long getId() {
//        return id;
//    }
//
//    public String getPhoneNumber() {
//        return phoneNumber;
//    }
//}
