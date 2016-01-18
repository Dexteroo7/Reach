package reach.backend.imageServer;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.Date;

/**
 * Created by dexter on 14/01/16.
 */

@Entity
@Index
@Cache
public class ServingUrl {

    @Id
    String id; //the file name in cloud storage

    String servingUrl; //the serving url generated

    long userId; //image came from this user

    Date dateOfCreation; //when was the servingUrl generated

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

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Date getDateOfCreation() {
        return dateOfCreation;
    }

    public void setDateOfCreation(Date dateOfCreation) {
        this.dateOfCreation = dateOfCreation;
    }
}
