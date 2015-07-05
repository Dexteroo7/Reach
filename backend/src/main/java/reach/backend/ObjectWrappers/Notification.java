package reach.backend.ObjectWrappers;

import com.google.appengine.repackaged.com.google.gson.Gson;

import java.io.Serializable;

/**
 * Created by dexter on 16/12/14.
 */
public class Notification implements Serializable {

    private static final long serialVersionUID = 0L;

    public static final short PERMISSION_REQUEST_RECEIVED = 0;
    public static final short REPLY_ACCEPTED = 1;
    public static final short REPLY_REJECTED = 2;

    public static final short PERMISSION_REQUEST_SENT = 3;
    public static final short REQUEST_ACCEPTED = 4;
    public static final short REQUEST_REJECTED = 5;

    private final short NotificationType;
    private final String otherPartyId;

    public short getNotificationType() {
        return NotificationType;
    }

    public String getOtherPartyId() {
        return otherPartyId;
    }

    public Notification(short notificationType, String otherPartyId) {

        this.NotificationType = notificationType;
        this.otherPartyId = otherPartyId;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this, Notification.class);
    }
}