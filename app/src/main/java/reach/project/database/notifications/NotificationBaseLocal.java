package reach.project.database.notifications;

import reach.backend.notifications.notificationApi.model.NotificationBase;

/**
 * Created by dexter on 08/07/15.
 */
public abstract class NotificationBaseLocal {

    public static final byte GET_ALL = 0;
    public static final byte GET_READ = 1;
    public static final byte GET_UN_READ = 2;

    public static final byte UN_READ = 0;
    public static final byte READ = 1;

    private Types types = Types.DEFAULT;

    private String hostName = "";
    private String imageId = "";

    private long hostId = 0;
    private long systemTime = 0;

    private int read = UN_READ;

    public NotificationBaseLocal portData(NotificationBase base) {

        if (base.getTypes().equals(Types.PUSH_ACCEPTED.name()))
            this.setTypes(Types.PUSH_ACCEPTED);
        else if (base.getTypes().equals(Types.PUSH.name()))
            this.setTypes(Types.PUSH);
        else if (base.getTypes().equals(Types.BECAME_FRIENDS.name()))
            this.setTypes(Types.BECAME_FRIENDS);
        else if (base.getTypes().equals(Types.LIKE.name()))
            this.setTypes(Types.LIKE);
        else
            throw new IllegalArgumentException("Illegal notification type detected");

        this.setHostName(base.getHostName());
        this.setImageId(base.getImageId());
        this.setHostId(base.getHostId());
        this.setSystemTime(base.getSystemTime());
        this.setRead(base.getRead());

        return this;
    }

    public NotificationBaseLocal portData(NotificationBaseLocal base) {

        this.setTypes(base.getTypes());
        this.setHostName(base.getHostName());
        this.setImageId(base.getImageId());
        this.setHostId(base.getHostId());
        this.setSystemTime(base.getSystemTime());
        this.setRead(base.getRead());

        return this;
    }

    public Types getTypes() {
        return types;
    }

    public void setTypes(Types types) {
        this.types = types;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public long getHostId() {
        return hostId;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    public long getSystemTime() {
        return systemTime;
    }

    public void setSystemTime(long systemTime) {
        this.systemTime = systemTime;
    }

    public int getRead() {
        return read;
    }

    public void setRead(int read) {
        this.read = read;
    }

    public int getNotificationId() {

        //basically the hashcode
        return hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationBaseLocal)) return false;
        NotificationBaseLocal that = (NotificationBaseLocal) o;
        return hostId == that.hostId && types == that.types;

    }

    @Override
    public int hashCode() {
        int result = types.name().hashCode();
        result = 31 * result + (int) (hostId ^ (hostId >>> 32));
        return result;
    }
}
