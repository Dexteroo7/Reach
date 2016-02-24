package reach.project.reachProcess.auxiliaryClasses;

/**
 * Created by dexter on 20/9/14.
 */
public class Connection {

    //SenderId, receiverId, metaHash and songId parameters are used to uniquely identify reachDatabase
    private final long songId;
    private final String metaHash;

    private final long senderId;
    private final long receiverId;
    private final long uniqueIdReceiver;
    private final long uniqueIdSender;
    private final int logicalClock;
    private final long offset;
    private final long length;
    private final String url;

    private String senderIp;
    private String messageType;

    private Connection(long songId, String metaHash, long senderId, long receiverId, long uniqueIdReceiver,
                       long uniqueIdSender, int logicalClock, long offset, long length, String url) {
        
        this.songId = songId;
        this.metaHash = metaHash;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.uniqueIdReceiver = uniqueIdReceiver;
        this.uniqueIdSender = uniqueIdSender;
        this.logicalClock = logicalClock;
        this.offset = offset;
        this.length = length;
        this.url = url;
    }

    public long getSongId() {
        return songId;
    }

    public String getMetaHash() {
        return metaHash;
    }

    public long getSenderId() {
        return senderId;
    }

    public long getReceiverId() {
        return receiverId;
    }

    public long getUniqueIdReceiver() {
        return uniqueIdReceiver;
    }

    public long getUniqueIdSender() {
        return uniqueIdSender;
    }

    public int getLogicalClock() {
        return logicalClock;
    }

    public long getOffset() {
        return offset;
    }

    public long getLength() {
        return length;
    }

    public String getUrl() {
        return url;
    }

    public String getSenderIp() {
        return senderIp;
    }

    public void setSenderIp(String senderIp) {
        this.senderIp = senderIp;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Connection that = (Connection) o;

        if (songId != that.songId) return false;
        if (senderId != that.senderId) return false;
        if (receiverId != that.receiverId) return false;
        if (uniqueIdReceiver != that.uniqueIdReceiver) return false;
        if (uniqueIdSender != that.uniqueIdSender) return false;
        if (logicalClock != that.logicalClock) return false;
        if (offset != that.offset) return false;
        if (length != that.length) return false;
        if (metaHash != null ? !metaHash.equals(that.metaHash) : that.metaHash != null)
            return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        if (senderIp != null ? !senderIp.equals(that.senderIp) : that.senderIp != null)
            return false;
        return messageType != null ? messageType.equals(that.messageType) : that.messageType == null;

    }

    @Override
    public int hashCode() {
        int result = (int) (songId ^ (songId >>> 32));
        result = 31 * result + (metaHash != null ? metaHash.hashCode() : 0);
        result = 31 * result + (int) (senderId ^ (senderId >>> 32));
        result = 31 * result + (int) (receiverId ^ (receiverId >>> 32));
        result = 31 * result + (int) (uniqueIdReceiver ^ (uniqueIdReceiver >>> 32));
        result = 31 * result + (int) (uniqueIdSender ^ (uniqueIdSender >>> 32));
        result = 31 * result + logicalClock;
        result = 31 * result + (int) (offset ^ (offset >>> 32));
        result = 31 * result + (int) (length ^ (length >>> 32));
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (senderIp != null ? senderIp.hashCode() : 0);
        result = 31 * result + (messageType != null ? messageType.hashCode() : 0);
        return result;
    }

    public static final class Builder {

        private long songId;
        private String metaHash;

        private long senderId;
        private long receiverId;
        private long uniqueIdReceiver;
        private long uniqueIdSender;
        private int logicalClock;
        private long offset;
        private long length;
        private String url;

        private String senderIp;
        private String messageType;

        public Builder setMessageType(String messageType) {
            this.messageType = messageType;
            return this;
        }

        public Builder setSenderIp(String senderIp) {
            this.senderIp = senderIp;
            return this;
        }

        public Builder setSongId(long songId) {
            this.songId = songId;
            return this;
        }

        public Builder setMetaHash(String metaHash) {
            this.metaHash = metaHash;
            return this;
        }

        public Builder setSenderId(long senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder setReceiverId(long receiverId) {
            this.receiverId = receiverId;
            return this;
        }

        public Builder setUniqueIdReceiver(long uniqueIdReceiver) {
            this.uniqueIdReceiver = uniqueIdReceiver;
            return this;
        }

        public Builder setUniqueIdSender(long uniqueIdSender) {
            this.uniqueIdSender = uniqueIdSender;
            return this;
        }

        public Builder setLogicalClock(int logicalClock) {
            this.logicalClock = logicalClock;
            return this;
        }

        public Builder setOffset(long offset) {
            this.offset = offset;
            return this;
        }

        public Builder setLength(long length) {
            this.length = length;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Connection build() {

            final Connection connection = new Connection(songId, metaHash, senderId, receiverId, uniqueIdReceiver,
                    uniqueIdSender, logicalClock, offset, length ,url);
            connection.setSenderIp(senderIp);
            connection.setMessageType(messageType);

            return connection;
        }
    }
}