package reach.project.notificationCentre.notifications;

/**
 * Created by dexter on 08/07/15.
 */
public class PushAccepted extends NotificationBaseLocal {

    private String firstSongName = "";
    private int size = 0;

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getFirstSongName() {
        return firstSongName;
    }

    public void setFirstSongName(String firstSongName) {
        this.firstSongName = firstSongName;
    }

    @Override
    public NotificationBaseLocal portData(reach.backend.notifications.notificationApi.model.NotificationBase base) {
        return super.portData(base);
    }

    @Override
    public NotificationBaseLocal portData(NotificationBaseLocal base) {
        return super.portData(base);
    }

    @Override
    public Types getTypes() {
        return super.getTypes();
    }

    @Override
    public void setTypes(Types types) {

        if (types != Types.PUSH_ACCEPTED)
            throw new IllegalStateException("Illegal type");
        super.setTypes(types);
    }

    @Override
    public String getImageId() {
        return super.getImageId();
    }

    @Override
    public void setImageId(String imageId) {
        super.setImageId(imageId);
    }

    @Override
    public String getHostName() {
        return super.getHostName();
    }

    @Override
    public void setHostName(String hostName) {
        super.setHostName(hostName);
    }

    @Override
    public long getSystemTime() {
        return super.getSystemTime();
    }

    @Override
    public void setSystemTime(long systemTime) {
        super.setSystemTime(systemTime);
    }

    @Override
    public int getRead() {
        return super.getRead();
    }

    @Override
    public void setRead(int read) {
        super.setRead(read);
    }

    @Override
    public int getNotificationId() {
        return hashCode();
    }

    @Override
    public long getHostId() {
        return super.getHostId();
    }

    @Override
    public void setHostId(long hostId) {
        super.setHostId(hostId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PushAccepted)) return false;
        if (!super.equals(o)) return false;

        PushAccepted that = (PushAccepted) o;

        if (size != that.size) return false;
        return !(firstSongName != null ? !firstSongName.equals(that.firstSongName) : that.firstSongName != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (firstSongName != null ? firstSongName.hashCode() : 0);
        result = 31 * result + size;
        return result;
    }
}
