package reach.backend.imageServer;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

/**
 * Created by dexter on 14/01/16.
 */

@Entity
@Cache
public class ServingUrl {

    @Id
    String id; //the file name in cloud storage
    String servingUrl; //the serving url generated

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServingUrl() {
        return servingUrl;
    }

    public void setServingUrl(String servingUrl) {
        this.servingUrl = servingUrl;
    }
}
