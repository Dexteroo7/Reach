package reach.project.devikaChat;

/**
 * @author dexter
 * @since 6/21/13
 */
public class Chat {

    public static final byte ADMIN = 0;
    public static final byte NOT_ADMIN = 1;

    public static final byte PENDING = 0;
    public static final byte SENT_TO_SERVER = 1;
    public static final byte UN_READ = 2;
    public static final byte READ = 3;

    private String message = "hello_world";
    private long timestamp = 0;

    private byte status = PENDING;
    private byte admin = NOT_ADMIN;

    // Required default constructor for Firebase object mapping
    @SuppressWarnings("unused")
    public Chat() {
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

    public byte getAdmin() {
        return admin;
    }

    public void setAdmin(byte admin) {
        this.admin = admin;
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }
}