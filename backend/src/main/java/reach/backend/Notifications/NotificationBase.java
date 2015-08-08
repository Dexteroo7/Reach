package reach.backend.Notifications;

/**
 * Created by dexter on 08/07/15.
 */
public class NotificationBase {

    public static final int DEFAULT_LIST_LIMIT = 150;

    public static final int NEW = -1;
    public static final int UN_READ = 0;
    public static final int READ = 1;

    private Types types = Types.DEFAULT;
    private long hostId = 0;
    private long systemTime = 0;
    private int read = NEW; //using int to accommodate more cases !

    private String hostName = ""; //can change, we don't save this in the data store
    private String imageId = ""; //can change, we don't save this in the data store

    public Types getTypes() {
        return types;
    }

    public void setTypes(Types types) {
        this.types = types;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public long getSystemTime() {
        return systemTime;
    }

    public void setSystemTime(long systemTime) {
        this.systemTime = systemTime;
    }

    public long getHostId() {
        return hostId;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    public int getNotificationId() {

        //basically the hashcode
        if(this.types == null || this.types == Types.DEFAULT || this.hostId == 0 || this.systemTime == 0)
            throw new IllegalStateException("Notification uninitialized !");

        int result = types.name().hashCode();
        result = 31 * result + (int) (hostId ^ (hostId >>> 32));
        result = 31 * result + (int) (systemTime ^ (systemTime >>> 32));
        return result;
    }

    public void setNotificationId(int notificationId) {
        //nothing to do here ! :)
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationBase)) return false;

        NotificationBase base = (NotificationBase) o;

        if (hostId != base.hostId) return false;
        if (systemTime != base.systemTime) return false;
        return types == base.types;

    }

    @Override
    public int hashCode() {
        int result = types.hashCode();
        result = 31 * result + (int) (hostId ^ (hostId >>> 32));
        result = 31 * result + (int) (systemTime ^ (systemTime >>> 32));
        return result;
    }

    public int getRead() {
        return read;
    }

    public void setRead(int read) {
        this.read = read;
    }
}
