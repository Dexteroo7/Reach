package reach.backend.notifications;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Serialize;
import com.googlecode.objectify.annotation.Unindex;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by dexter on 06/07/15.
 */
@Entity
@Unindex
public class Notification {

    //will be same as ReachUser id
    @Id
    private long id = 0;
    //The notification
    @Serialize(zip = true)
    private Set<NotificationBase> notifications = Collections.newSetFromMap(new LinkedHashMap<NotificationBase, Boolean>() {
        protected boolean removeEldestEntry(Map.Entry<NotificationBase, Boolean> eldest) {
            return size() > NotificationBase.DEFAULT_LIST_LIMIT;
        }
    });

    public Set<NotificationBase> getNotifications() {
        return notifications;
    }

    public void setNotifications(Set<NotificationBase> notifications) {
        this.notifications = notifications;
    }

    public void initializeCollection() {

        this.notifications = Collections.newSetFromMap(new LinkedHashMap<NotificationBase, Boolean>() {
            protected boolean removeEldestEntry(Map.Entry<NotificationBase, Boolean> eldest) {
                return size() > NotificationBase.DEFAULT_LIST_LIMIT;
            }
        });
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
