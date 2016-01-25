package reach.backend.ObjectWrappers;

import java.util.List;
import java.util.Map;

/**
 * Created by dexter on 23/01/16.
 */
public class BulkManualNotification {

    Map<String, String> messages;
    List<Long> ids;

    public BulkManualNotification() {
    }

    public Map<String, String> getMessages() {
        return messages;
    }

    public void setMessages(Map<String, String> messages) {
        this.messages = messages;
    }

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
