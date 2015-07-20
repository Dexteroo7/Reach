package reach.backend.User;

/**
 * Created by dexter on 28/1/15.
 */
public class ReachFriend {

    private final long id;
    private final int numberofSongs;
    private final String phoneNumber;
    private final String userName;
    private final String imageId;
    private final String statusSong;

    private final String genres;
    private short status = 3;
    /*
    0 - offline and permission-request granted
    1 - online and permission-request granted
    2 - request sent but permission not granted
    3 - request not sent and permission not granted
    */
    private long lastSeen = 1;
    private short networkType = 0;

    /**
     * The ONLINE_LIMIT is 30 seconds
     * We keep last seen currentTime for 30 minutes.
     * Know the difference
     */

    public ReachFriend(ReachUser host, long lastSeen, ReachUser client) {

        if ((client.getMyReach() != null && client.getMyReach().contains(host.getId())) ||
                (host.getMyReach() != null && host.getMyReach().contains(client.getId()))) {

            if (lastSeen > ReachUser.ONLINE_LIMIT) {
                //less than 30 seconds
                this.status = 0;     //offline and permission-request granted
            } else this.status = 1;  //online and permission-request granted
        } else if ((client.getSentRequests() != null && client.getSentRequests().contains(host.getId())) ||
                (host.getReceivedRequests() != null && host.getReceivedRequests().contains(client.getId())))
            this.status = 2; //request sent but permission not granted
        else
            this.status = 3; //request not sent and permission not granted

        this.id = host.getId();
        this.lastSeen = lastSeen;
        this.phoneNumber = host.getPhoneNumber();
        this.userName = host.getUserName();
        this.statusSong = host.getStatusSong();
        this.imageId = host.getImageId();
        this.genres = host.getGenres();
        this.numberofSongs = host.getNumberOfSongs();
    }

    public int getNumberofSongs() {
        return numberofSongs;
    }

    public short getNetworkType() {
        return networkType;
    }

    public void setNetworkType(short networkType) {
        this.networkType = networkType;
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public String getGenres() {
        return genres;
    }

    public String getImageId() {
        return imageId;
    }

    public String getStatusSong() {
        return statusSong;
    }

    public String getUserName() {
        return userName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReachFriend)) return false;

        ReachFriend that = (ReachFriend) o;

        if (getId() != that.getId()) return false;
        if (!getPhoneNumber().equals(that.getPhoneNumber())) return false;
        return getUserName().equals(that.getUserName());

    }

    @Override
    public int hashCode() {
        int result = (int) (getId() ^ (getId() >>> 32));
        result = 31 * result + getPhoneNumber().hashCode();
        result = 31 * result + getUserName().hashCode();
        return result;
    }
}
