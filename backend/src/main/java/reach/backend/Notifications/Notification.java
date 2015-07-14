package reach.backend.Notifications;

import com.google.appengine.repackaged.com.google.common.collect.EvictingQueue;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

/**
 * Created by dexter on 06/07/15.
 */

@Cache
@Entity
public class Notification {

    //will be same as ReachUser id
    @Id
    long id = 0;

    //The notification
    EvictingQueue<NotificationBase> notifications = EvictingQueue.create(NotificationBase.DEFAULT_LIST_LIMIT);

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
