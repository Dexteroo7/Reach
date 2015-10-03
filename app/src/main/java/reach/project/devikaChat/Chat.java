package reach.project.devikaChat;

/**
 * @author greg
 * @since 6/21/13
 */
public class Chat {

    private String message = "hello_world";
    private long timestamp = 0;
    private long userId = 0;
    boolean read = false; //if true, double tick
    boolean isDevika = false; //if true, then devika

    // Required default constructor for Firebase object mapping
    @SuppressWarnings("unused")
    public Chat() {
    }

    public boolean isDevika() {
        return isDevika;
    }

    public void setIsDevika(boolean isDevika) {
        this.isDevika = isDevika;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}