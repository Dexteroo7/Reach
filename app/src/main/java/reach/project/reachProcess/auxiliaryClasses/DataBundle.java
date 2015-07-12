package reach.project.reachProcess.auxiliaryClasses;

import java.util.Date;

/**
 * Created by dexter on 21/9/14.
 */
public class DataBundle {

    public long getBytesProcessed() {
        return bytesProcessed;
    }

    public void setBytesProcessed(long bytesProcessed) {
        this.bytesProcessed = bytesProcessed;
    }

    public long getSongId() {
        return songId;
    }

    public long getLength() {
        return length;
    }

    public long getLastActive() {
        return lastActive;
    }

    public void setLastActive(long lastActive) {
        this.lastActive = lastActive;
    }

    public long getReceiverId() {
        return receiverId;
    }

    public long getSenderId() {
        return senderId;
    }

    public long getId() {
        return id;
    }

    public long getReference() {
        return reference;
    }


    private long bytesProcessed;
    private long lastActive;

    private final long id;
    private final long length;
    private final long songId;
    private final long receiverId;
    private final long senderId;
    private final long reference;

    public DataBundle(long id, long songId, long receiverId, long senderId, long length, long bytesProcessed, long reference) {

        this.id = id;
        this.songId = songId;
        this.receiverId = receiverId;
        this.senderId = senderId;
        this.length = length;
        this.bytesProcessed = bytesProcessed;
        this.lastActive = new Date().getTime();
        this.reference = reference;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof DataBundle)) return false;

        DataBundle that = (DataBundle) o;
        if (getBytesProcessed() != that.getBytesProcessed()) return false;
        if (getLastActive() != that.getLastActive()) return false;
        if (getId() != that.getId()) return false;
        if (getLength() != that.getLength()) return false;
        if (getSongId() != that.getSongId()) return false;
        if (getReceiverId() != that.getReceiverId()) return false;
        if (getSenderId() != that.getSenderId()) return false;
        return getReference() == that.getReference();

    }

    @Override
    public int hashCode() {
        int result = (int) (getBytesProcessed() ^ (getBytesProcessed() >>> 32));
        result = 31 * result + (int) (getLastActive() ^ (getLastActive() >>> 32));
        result = 31 * result + (int) (getId() ^ (getId() >>> 32));
        result = 31 * result + (int) (getLength() ^ (getLength() >>> 32));
        result = 31 * result + (int) (getSongId() ^ (getSongId() >>> 32));
        result = 31 * result + (int) (getReceiverId() ^ (getReceiverId() >>> 32));
        result = 31 * result + (int) (getSenderId() ^ (getSenderId() >>> 32));
        result = 31 * result + (int) (getReference() ^ (getReference() >>> 32));
        return result;
    }
}
