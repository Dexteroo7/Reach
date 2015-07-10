package reach.backend.Entities;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

/**
 * Created by dexter on 11/11/14.
 */
@Entity
@Cache
public class ReachInvite {

    @Id private Long id;
    @Index private String clientId;
    @Index private String phoneNumber;
    @Index private String name;

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClientId() {
        return clientId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }
}
