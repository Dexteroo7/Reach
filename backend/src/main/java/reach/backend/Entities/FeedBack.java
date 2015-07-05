package reach.backend.Entities;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;

/**
 * Created by dexter on 11/11/14.
 */
@Entity
@Cache
public class FeedBack {

    @Id private Long id;
    @Unindex private String reply1;
    @Unindex private String reply2;
    @Unindex private String reply3;
    @Unindex private String clientId;

    public void setReply1(String reply1) {
        this.reply1 = reply1;
    }

    public void setReply2(String reply2) {
        this.reply2 = reply2;
    }

    public void setReply3(String reply3) {
        this.reply3 = reply3;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public long getId() {
        return id;
    }

    public String getReply1() {
        return reply1;
    }

    public String getReply2() {
        return reply2;
    }

    public String getReply3() {
        return reply3;
    }

    public String getClientId() {
        return clientId;
    }

}
