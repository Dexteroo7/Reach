package reach.backend.User.FriendContainers;

import java.util.HashSet;

import reach.backend.User.ReachUser;

/**
 * Created by dexter on 17/07/15.
 */
public class Friend {

    public static String[] projectNewFriend = new String[]{
            "numberOfSongs",
            "userName",
            "imageId",
            "phoneNumber"};

    private final long id;
    private final int hash;
    private final int numberOfSongs; //can change
    private final String userName;   //can change
    private final String imageId;    //can change
    private final String phoneNumber;
    /*
        0 - offline and permission-request granted
        1 - online and permission-request granted
        2 - request sent but permission not granted
        3 - request not sent and permission not granted
    */
    private final short status;      //can change
    private final long lastSeen;     //can change

    /**
     * Used to mark a friend as dead (hash = 0)
     * @param id the id who died
     * @param dead HAS TO BE TRUE if using this constructor
     */
    public Friend(long id, boolean dead) {

        if(!dead)
            throw new IllegalArgumentException("Dead has to be true if using this constructor");

        this.id = id;
        this.hash = 0;
        numberOfSongs = 0;
        userName = null;
        imageId = null;
        phoneNumber = null;
        status = 0;
        lastSeen = 0;
    }

    /**
     * To be used when host is a new friends with status 3
     * @param host the friend
     */
    public Friend(ReachUser host) {

        this.id = host.getId();
        this.phoneNumber = host.getPhoneNumber();
        this.userName = host.getUserName();
        this.imageId = host.getImageId();
        this.numberOfSongs = host.getNumberOfSongs();
        this.lastSeen = ReachUser.ONLINE_LIMIT + 1;
        this.status = 3;
        //in-case dirty check was not set compute
        this.hash = host.getDirtyCheck() == 0 ? host.computeDirtyHash() : host.getDirtyCheck();
        if(this.hash == 0)
            throw  new IllegalArgumentException("Hash should not be zero " + this.userName);
    }

    /**
     * General purpose constructor
     * @param host the friend
     * @param myReach list of friends with status < 2
     * @param sentRequests list of friends with status 2
     * @param lastSeen lastSeen parameter (decides status 01)
     */
    public Friend(ReachUser host,
                  HashSet<Long> myReach,
                  HashSet<Long> sentRequests,
                  long lastSeen) {

        this.id = host.getId();
        this.phoneNumber = host.getPhoneNumber();
        this.userName = host.getUserName();
        this.imageId = host.getImageId();
        this.numberOfSongs = host.getNumberOfSongs();
        this.lastSeen = lastSeen;
        //in-case dirty check was not set compute
        this.hash = host.getDirtyCheck() == 0 ? host.computeDirtyHash() : host.getDirtyCheck();
        if(this.hash == 0)
            throw  new IllegalArgumentException("Hash should not be zero " + this.userName);

        if (myReach != null && myReach.contains(id))
            if (lastSeen > ReachUser.ONLINE_LIMIT)
                this.status = 0;     //offline and permission-request granted
            else
                this.status = 1;  //online and permission-request granted
        else if (sentRequests != null && sentRequests.contains(id))
            this.status = 2; //request sent but permission not granted
        else
            this.status = 3; //request not sent and permission not granted
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public short getStatus() {
        return status;
    }

    public String getImageId() {
        return imageId;
    }

    public String getUserName() {
        return userName;
    }

    public int getNumberOfSongs() {
        return numberOfSongs;
    }

    public long getId() {
        return id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public int getHash() {
        return hash;
    }
}
