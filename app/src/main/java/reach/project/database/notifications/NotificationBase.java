package reach.project.database.notifications;

import reach.backend.entities.userApi.model.ReachUser;

/**
 * Created by dexter on 08/07/15.
 */
public abstract class NotificationBase {

    private Types types = Types.DEFAULT;
    private long hostId = 0;
    private long systemTime = 0;
    private String hostName = "";
    private String imageId = "";
    private short read = 0;
    private short expanded = 1;

    public NotificationBase portData(reach.backend.notifications.notificationApi.model.NotificationBase base) {

        this.setHostName(base.getHostName());
        this.setRead(base.getRead().shortValue());
        this.setHostId(base.getHostId());
        this.setImageId(base.getImageId());
        this.setSystemTime(base.getSystemTime());
        this.setExpanded((short) 0);

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
        return this;
    }

    public short getExpanded() {
        return expanded;
    }

    public void setExpanded(short expanded) {
        this.expanded = expanded;
    }

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

    public short getRead() {
        return read;
    }

    public void setRead(short read) {
        this.read = read;
    }

    public void addBasicData(ReachUser user) {
        setRead((short) 0);
        setSystemTime(System.currentTimeMillis());
        setImageId(user.getImageId());
        setHostId(user.getId());
        setHostName(user.getUserName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationBase)) return false;

        NotificationBase that = (NotificationBase) o;

        if (hostId != that.hostId) return false;
        if (systemTime != that.systemTime) return false;
        if (read != that.read) return false;
        if (types != that.types) return false;
        if (!hostName.equals(that.hostName)) return false;
        return !(imageId != null ? !imageId.equals(that.imageId) : that.imageId != null);

    }

    @Override
    public int hashCode() {
        int result = types.hashCode();
        result = 31 * result + (int) (hostId ^ (hostId >>> 32));
        result = 31 * result + (int) (systemTime ^ (systemTime >>> 32));
        result = 31 * result + hostName.hashCode();
        result = 31 * result + (imageId != null ? imageId.hashCode() : 0);
        result = 31 * result + (int) read;
        return result;
    }
}
