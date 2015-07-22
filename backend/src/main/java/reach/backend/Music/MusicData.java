package reach.backend.Music;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Serialize;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dexter on 06/07/15.
 */

@Cache
@Entity
public class MusicData {

    //will be same as ReachUser id
    @Id
    private long id = 0;
    //visibility of songs
    @Serialize(zip = true)
    Map<Integer, Boolean> visibility = new HashMap<>(500);


    public Map<Integer, Boolean> getVisibility() {
        return visibility;
    }

    public void setVisibility(Map<Integer, Boolean> visibility) {
        this.visibility = visibility;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
