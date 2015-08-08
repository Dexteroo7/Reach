package reach.backend.Music;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Serialize;
import com.googlecode.objectify.annotation.Unindex;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dexter on 06/07/15.
 */

@Entity
@Cache
public class MusicData {

    //will be same as ReachUser id
    @Id
    private long id = 0;
    //visibility of songs
    @Unindex
    @Serialize(zip = true)
    private Map<Long, Boolean> visibility = new HashMap<>(500);

    public Map<Long, Boolean> getVisibility() {
        return visibility;
    }

    public void setVisibility(Map<Long, Boolean> visibility) {
        this.visibility = visibility;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
