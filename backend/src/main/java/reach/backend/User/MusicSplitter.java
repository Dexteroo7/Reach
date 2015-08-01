package reach.backend.user;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;

import java.util.HashSet;

/**
 * Created by Dexter on 24-03-2015.
 */

@Cache
@Entity
public class MusicSplitter {

    @Id
    private Long id;
    @Unindex
    private HashSet<ReachSong> reachSongList;

    public HashSet<ReachSong> getReachSongList() {
        return reachSongList;
    }

    public void setReachSongList(HashSet<ReachSong> reachSongList) {
        this.reachSongList = reachSongList;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
