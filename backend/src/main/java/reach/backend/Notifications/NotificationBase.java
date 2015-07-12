package reach.backend.Notifications;

/**
 * Created by dexter on 08/07/15.
 */
public class NotificationBase {

    private Types types = Types.DEFAULT;

    private long hostId = 0;
    private long systemTime = 0;

    private String hostName = "";
    private String imageId = "";

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationBase)) return false;

        NotificationBase that = (NotificationBase) o;

        if (hostId != that.hostId) return false;
        if (systemTime != that.systemTime) return false;
        if (types != that.types) return false;
        if (!hostName.equals(that.hostName)) return false;
        return imageId.equals(that.imageId);

    }

    @Override
    public int hashCode() {
        int result = types.hashCode();
        result = 31 * result + (int) (hostId ^ (hostId >>> 32));
        result = 31 * result + (int) (systemTime ^ (systemTime >>> 32));
        result = 31 * result + hostName.hashCode();
        result = 31 * result + imageId.hashCode();
        return result;
    }
}
