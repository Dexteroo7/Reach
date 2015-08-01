package reach.backend.transactions;

import java.util.HashSet;

/**
 * Created by Dexter on 09-03-2015.
 */
public class CompletedOperation {

    //these 3 will identify this element
    private String songName;
    private long songSize;
    private long senderId;
    private long time;
    private long hits = 0;
    private HashSet<Long> receiver;

    public CompletedOperation(String songName, long songSize, long senderId) {
        this.senderId = senderId;
        this.songName = songName;
        this.songSize = songSize;
    }

    public HashSet<Long> getReceiver() {
        return receiver;
    }

    public void setReceiver(HashSet<Long> receiver) {
        this.receiver = receiver;
    }

    public long getHits() {
        return hits;
    }

    public void setHits(long hits) {
        this.hits = hits;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getSenderId() {
        return senderId;
    }

    public long getSongSize() {
        return songSize;
    }

    public String getSongName() {
        return songName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final CompletedOperation that = (CompletedOperation) o;
        return senderId == that.senderId && songSize == that.songSize && !(songName != null ? !songName.equals(that.songName) : that.songName != null);

    }

    @Override
    public int hashCode() {
        int result = songName != null ? songName.hashCode() : 0;
        result = 31 * result + (int) (songSize ^ (songSize >>> 32));
        result = 31 * result + (int) (senderId ^ (senderId >>> 32));
        return result;
    }
}
