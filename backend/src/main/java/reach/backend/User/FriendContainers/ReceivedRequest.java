package reach.backend.User.FriendContainers;

/**
 * Created by dexter on 17/07/15.
 */
public class ReceivedRequest {

    public static String[] projectReceivedRequest = new String[]{
            "numberOfSongs",
            "numberOfApps",
            "userName",
            "imageId",
            "phoneNumber"};

    private final long id;
    private final int numberOfSongs;
    private final int numberOfApps;

    private final String phoneNumber;
    private final String userName;
    private final String imageId;

    public String getImageId() {
        return imageId;
    }

    public String getUserName() {
        return userName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public int getNumberOfSongs() {
        return numberOfSongs;
    }

    public long getId() {
        return id;
    }

    public ReceivedRequest(long id, int numberOfSongs, int numberOfApps,
                           String phoneNumber, String userName, String imageId) {
        this.id = id;
        this.numberOfSongs = numberOfSongs;
        this.numberOfApps = numberOfApps;
        this.phoneNumber = phoneNumber;
        this.userName = userName;
        this.imageId = imageId;
    }

    public int getNumberOfApps() {
        return numberOfApps;
    }
}
