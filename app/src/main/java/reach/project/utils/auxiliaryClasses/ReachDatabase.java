package reach.project.utils.auxiliaryClasses;

/**
 * Created by dexter on 14/9/14.
 */
public class ReachDatabase {

    private long id = -1; //0
    private long songId = 0; //1
    private long receiverId = 0; //2
    private long senderId = 0; //3

    private short operationKind = 0;//4 (0 = download, 1 = upload)

    private String path = "hello_world"; //5
    private String senderName = "hello_world"; //6
    private String onlineStatus = "hello_world"; //7
    private String artistName = "hello_world"; //8

    private boolean isLiked = false; //9

    private String displayName = "hello_world"; //10
    private String actualName = "hello_world"; //11

    private long length = 0; //12
    private long processed = 0; //13
    private long added = 0; //14
    private long duration = 0; //15

    private short logicalClock = 0; //16
    private short status = 0; //17

    private long lastActive = 0; //not in sql
    private long reference = 0; //not in sql

    //types of status
    public static final short NOT_WORKING = 0;
    public static final short WORKING = 1;
    public static final short RELAY = 2;
    public static final short FINISHED = 3;
    public static final short GCM_FAILED = 4;        //only applicable for download
    public static final short FILE_NOT_FOUND = 5;    //404 from host
    public static final short FILE_NOT_CREATED = 6;  //weird error
    public static final short PAUSED_BY_USER = 7;    //paused by client
//    public static final short PAUSED_BY_HOST = 8;    //paused by host
//    public static final short HOST_GONE = 9;    //paused by host

    public static void isPresent(short status) throws IllegalArgumentException {
        if (status < 0 || status > 7)
            throw new IllegalArgumentException("Wrong status !");
        //else all good
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }


    public String getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(String onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public String getActualName() {
        return actualName;
    }

    public void setActualName(String actualName) {
        this.actualName = actualName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(long receiverId) {
        this.receiverId = receiverId;
    }

    public long getSenderId() {
        return senderId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public long getSongId() {
        return songId;
    }

    public void setSongId(long songId) {
        this.songId = songId;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getAdded() {
        return added;
    }

    public void setAdded(long added) {
        this.added = added;
    }

    public short getLogicalClock() {
        return logicalClock;
    }

    public void setLogicalClock(short logicalClock) {
        this.logicalClock = logicalClock;
    }

    public long getLastActive() {
        return lastActive;
    }

    public void setLastActive(long lastActive) {
        this.lastActive = lastActive;
    }

    public long getProcessed() {
        return processed;
    }

    public void setProcessed(long processed) {
        this.processed = processed;
    }

    public short getOperationKind() {
        return operationKind;
    }

    public void setOperationKind(short operationKind) {
        this.operationKind = operationKind;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "ReachDatabase{" +
                ", songId=" + songId +
                ", receiverId=" + receiverId +
                ", senderId=" + senderId +
                ", operationKind=" + operationKind +
                ", displayName='" + displayName + '\'' +
                ", actualName='" + actualName + '\'' +
                ", length=" + length +
                ", added=" + added +
                '}';
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ReachDatabase that = (ReachDatabase) o;
        return added == that.added &&
                length == that.length &&
                operationKind == that.operationKind &&
                receiverId == that.receiverId &&
                senderId == that.senderId &&
                songId == that.songId;
    }

    @Override
    public int hashCode() {
        int result = (int) (songId ^ (songId >>> 32));
        result = 31 * result + (int) (receiverId ^ (receiverId >>> 32));
        result = 31 * result + (int) (senderId ^ (senderId >>> 32));
        result = 31 * result + (int) operationKind;
        result = 31 * result + (int) (length ^ (length >>> 32));
        result = 31 * result + (int) (added ^ (added >>> 32));
        return result;
    }

    public long getReference() {
        return reference;
    }

    public void setReference(long reference) {
        this.reference = reference;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setIsLiked(boolean isLiked) {
        this.isLiked = isLiked;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
