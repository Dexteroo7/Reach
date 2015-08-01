package reach.backend.user;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;

import java.util.HashSet;

/**
 * Created by Dexter on 25-03-2015.
 */
@Cache
@Entity
public class SplitMusicContainer {

    @Id
    private Long id;
    @Unindex
    private HashSet<Long> splitIds;

    public HashSet<Long> getSplitIds() {
        return splitIds;
    }

    public void setSplitIds(HashSet<Long> splitIds) {
        this.splitIds = splitIds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
