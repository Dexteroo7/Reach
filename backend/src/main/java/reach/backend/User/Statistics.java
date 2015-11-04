package reach.backend.User;

/**
 * Created by dexter on 22/10/15.
 */

/**
 * Created by dexter on 09/07/15.
 */
public class Statistics {

    private final long userId;

    private final long timeCreated;

    private final int songCount;
    private final int friendsCount;
    private final int sentRequests;
    private final int pendingRequests;

    private final int uploadCount;
    private final int downloadCount;

    private final boolean isAlive;

    private final String userName;
    private final String phoneNumber;


    public Statistics(long userId,
                      long timeCreated,
                      String userName,
                      String phoneNumber,
                      int songCount,
                      int friendsCount,
                      int sentRequests,
                      int pendingRequests,
                      int uploadCount,
                      int downloadCount,
                      boolean isAlive) {

        this.userId = userId;
        this.timeCreated = timeCreated;

        this.userName = userName;
        this.phoneNumber = phoneNumber;
        this.songCount = songCount;
        this.friendsCount = friendsCount;
        this.sentRequests = sentRequests;
        this.pendingRequests = pendingRequests;
        this.isAlive = isAlive;

        this.uploadCount = uploadCount;
        this.downloadCount = downloadCount;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public long getFriendsCount() {
        return friendsCount;
    }

    public long getSongCount() {
        return songCount;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getUserName() {
        return userName;
    }

    public long getUserId() {
        return userId;
    }

    public int getSentRequests() {
        return sentRequests;
    }

    public int getPendingRequests() {
        return pendingRequests;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public int getUploadCount() {
        return uploadCount;
    }

    public long getTimeCreated() {
        return timeCreated;
    }
}