package reach.backend.Transactions;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;

/**
 * Created by Dexter on 12-04-2015.
 */

@Entity
@Cache
@Index
public class CompletedOperations {

    //these 3 will identify this element
    @Id
    private Long id;
    private long senderId; //uploader
    private long receiver; //downloader
    private String songName;

    @Unindex
    private long songSize;
    @Unindex
    private long time;
    @Unindex
    private long hits = 0;

    public long getReceiver() {
        return receiver;
    }

    public void setReceiver(long receiver) {
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

    public long getSenderId() {
        return senderId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompletedOperations)) return false;

        CompletedOperations that = (CompletedOperations) o;

        if (getSenderId() != that.getSenderId()) return false;
        if (getSongSize() != that.getSongSize()) return false;
        return getSongName().equals(that.getSongName());
    }

    @Override
    public int hashCode() {
        int result = (int) (getSenderId() ^ (getSenderId() >>> 32));
        result = 31 * result + getSongName().hashCode();
        result = 31 * result + (int) (getSongSize() ^ (getSongSize() >>> 32));
        return result;
    }
}
