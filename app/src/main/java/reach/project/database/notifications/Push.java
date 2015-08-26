package reach.project.database.notifications;

/**
 * Created by dexter on 06/07/15.
 */
public class Push extends NotificationBaseLocal {

    private int size = 0;
    private String firstSongName = "";
    private String customMessage = "";
    private String pushContainer = ""; //compressed Base64

    public String getPushContainer() {
        return pushContainer;
    }

    public void setPushContainer(String pushContainer) {
        this.pushContainer = pushContainer;
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
    public void setTypes(Types types) { //fix ?
        if (types != Types.PUSH)
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

    public String getCustomMessage() {
        return customMessage;
    }

    public void setCustomMessage(String customMessage) {
        this.customMessage = customMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Push)) return false;
        if (!super.equals(o)) return false;

        Push push = (Push) o;

        if (size != push.size) return false;
        if (firstSongName != null ? !firstSongName.equals(push.firstSongName) : push.firstSongName != null)
            return false;
        if (customMessage != null ? !customMessage.equals(push.customMessage) : push.customMessage != null)
            return false;
        return !(pushContainer != null ? !pushContainer.equals(push.pushContainer) : push.pushContainer != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + size;
        result = 31 * result + (firstSongName != null ? firstSongName.hashCode() : 0);
        result = 31 * result + (customMessage != null ? customMessage.hashCode() : 0);
        result = 31 * result + (pushContainer != null ? pushContainer.hashCode() : 0);
        return result;
    }
}
