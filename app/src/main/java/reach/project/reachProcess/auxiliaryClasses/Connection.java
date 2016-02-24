package reach.project.reachProcess.auxiliaryClasses;

/**
 * Created by dexter on 20/9/14.
 */
public class Connection {

    private final long senderId;
    private final long songId;
    private final long receiverId;
    /**
    SenderId, receiverId and songId parameters are used to uniquely identify reachDatabase
     **/
    private final long uniqueIdReceiver;
    private final long uniqueIdSender;
    private final int logicalClock;
    private final long offset;
    private final long length;
    private final String url;

    private String senderIp;
    private String messageType;

    public String getUrl() {
        return url;
    }

    public void setSenderIp(String senderIp) {
        this.senderIp = senderIp;
    }

    public String getSenderIp() {
        return senderIp;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageType() {
        return messageType;
    }

    public long getLength() {
        return length;
    }

    public long getOffset() {
        return offset;
    }

    public int getLogicalClock() {
        return logicalClock;
    }

    public long getUniqueIdSender() {
        return uniqueIdSender;
    }

    public long getUniqueIdReceiver() {
        return uniqueIdReceiver;
    }

    public long getReceiverId() {
        return receiverId;
    }

    public long getSongId() {
        return songId;
    }

    public long getSenderId() {
        return senderId;
    }

    public Connection(String messageType, long senderId, long receiverId,
                      long songId, long offset,
                      long length, long uniqueIdReceiver,
                      long uniqueIdSender, int logicalClock,
                      String url) {

        this.messageType = messageType;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.songId = songId;
        this.offset = offset;
        this.length = length;
        this.uniqueIdReceiver = uniqueIdReceiver;
        this.uniqueIdSender = uniqueIdSender;
        this.logicalClock = logicalClock;
        this.url = url;
    }

    @Override
    public String toString() {
        return "Connection{" +
                "receiverId=" + receiverId +
                ", senderId=" + senderId +
                ", length=" + length +
                ", messageType='" + messageType + '\'' +
                ", offset=" + offset +
                ", logicalClock=" + logicalClock +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Connection)) return false;

        Connection that = (Connection) o;

        if (getSenderId() != that.getSenderId()) return false;
        if (getSongId() != that.getSongId()) return false;
        if (getReceiverId() != that.getReceiverId()) return false;
        if (getUniqueIdReceiver() != that.getUniqueIdReceiver()) return false;
        if (getUniqueIdSender() != that.getUniqueIdSender()) return false;
        if (getLogicalClock() != that.getLogicalClock()) return false;
        if (getOffset() != that.getOffset()) return false;
        if (getLength() != that.getLength()) return false;
        if (getUrl() != null ? !getUrl().equals(that.getUrl()) : that.getUrl() != null)
            return false;
        if (getSenderIp() != null ? !getSenderIp().equals(that.getSenderIp()) : that.getSenderIp() != null)
            return false;
        return !(getMessageType() != null ? !getMessageType().equals(that.getMessageType()) : that.getMessageType() != null);

    }

    @Override
    public int hashCode() {
        int result = (int) (getSenderId() ^ (getSenderId() >>> 32));
        result = 31 * result + (int) (getSongId() ^ (getSongId() >>> 32));
        result = 31 * result + (int) (getReceiverId() ^ (getReceiverId() >>> 32));
        result = 31 * result + (int) (getUniqueIdReceiver() ^ (getUniqueIdReceiver() >>> 32));
        result = 31 * result + (int) (getUniqueIdSender() ^ (getUniqueIdSender() >>> 32));
        result = 31 * result + (int) getLogicalClock();
        result = 31 * result + (int) (getOffset() ^ (getOffset() >>> 32));
        result = 31 * result + (int) (getLength() ^ (getLength() >>> 32));
        result = 31 * result + (getUrl() != null ? getUrl().hashCode() : 0);
        result = 31 * result + (getSenderIp() != null ? getSenderIp().hashCode() : 0);
        result = 31 * result + (getMessageType() != null ? getMessageType().hashCode() : 0);
        return result;
    }
}