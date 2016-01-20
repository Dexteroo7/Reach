package reach.backend.applications;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Serialize;
import com.googlecode.objectify.annotation.Unindex;

import java.util.HashMap;
import java.util.Map;

/**
 * User defined visibility list, over-rides default hidden list
 * Created by dexter on 19/11/15.
 */

@Entity
@Cache
public class AppVisibility {

    //will be same as ReachUser id
    @Id
    private Long id = 0L;
    //visibility of songs
    @Unindex
    @Serialize(zip = true)
    private Map<String, Boolean> visibility = new HashMap<>(500);
    //presence of packages
    @Unindex
    @Serialize(zip = true)
    private Map<String, Boolean> presence = new HashMap<>(500);

    public Map<String, Boolean> getVisibility() {
        return visibility;
    }

    public void setVisibility(Map<String, Boolean> visibility) {
        this.visibility = visibility;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Map<String, Boolean> getPresence() {
        return presence;
    }

    public void setPresence(Map<String, Boolean> presence) {
        this.presence = presence;
    }
}