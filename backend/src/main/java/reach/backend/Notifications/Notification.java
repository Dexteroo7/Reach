package reach.backend.Notifications;

import com.google.appengine.repackaged.com.google.common.collect.EvictingQueue;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Serialize;

/**
 * Created by dexter on 06/07/15.
 */

@Cache
@Entity
@Index
public class Notification {

    //will be same as ReachUser id
    @Id
    long id = 0;

    //The notification in JSON format
    @Serialize(zip = true)
    EvictingQueue<NotificationBase> notifications = EvictingQueue.create(25);

    public EvictingQueue<NotificationBase> getNotifications() {
        return notifications;
    }

    public void setNotifications(EvictingQueue<NotificationBase> notifications) {
        this.notifications = notifications;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
