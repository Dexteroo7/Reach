package reach.backend.Transactions;

import java.util.HashSet;

/**
 * Created by dexter on 28/07/15.
 */
public class UploadHistory {

    private String songName;
    private long songSize;
    private long senderId; //the person who uploaded the song
    private long time;
    private long hits = 0;
    private HashSet<Long> receiver; //people who requested this song

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

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public long getSongSize() {
        return songSize;
    }

    public void setSongSize(long songSize) {
        this.songSize = songSize;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UploadHistory)) return false;

        UploadHistory that = (UploadHistory) o;

        if (songSize != that.songSize) return false;
        if (senderId != that.senderId) return false;
        if (time != that.time) return false;
        if (hits != that.hits) return false;
        if (songName != null ? !songName.equals(that.songName) : that.songName != null)
            return false;
        return !(receiver != null ? !receiver.equals(that.receiver) : that.receiver != null);

    }

    @Override
    public int hashCode() {
        int result = songName != null ? songName.hashCode() : 0;
        result = 31 * result + (int) (songSize ^ (songSize >>> 32));
        result = 31 * result + (int) (senderId ^ (senderId >>> 32));
        result = 31 * result + (int) (time ^ (time >>> 32));
        result = 31 * result + (int) (hits ^ (hits >>> 32));
        result = 31 * result + (receiver != null ? receiver.hashCode() : 0);
        return result;
    }
}
