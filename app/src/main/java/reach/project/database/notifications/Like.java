package reach.project.database.notifications;

/**
 * Created by dexter on 06/07/15.
 */
public class Like extends NotificationBase {

    private String songName = "";

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    @Override
    public NotificationBase portData(reach.backend.notifications.notificationApi.model.NotificationBase base) {
        return super.portData(base);
    }

    @Override
    public NotificationBase portData(NotificationBase base) {
        return super.portData(base);
    }

    @Override
    public Types getTypes() {
        return super.getTypes();
    }

    @Override
    public void setTypes(Types types) {
        if (types != Types.LIKE)
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
    public int getExpanded() {
        return super.getExpanded();
    }

    @Override
    public void setExpanded(int expanded) {
        super.setExpanded(expanded);
    }

    @Override
    public int getNotificationId() {
        return super.getNotificationId();
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
        if (!(o instanceof Like)) return false;
        if (!super.equals(o)) return false;

        Like like = (Like) o;

        return !(songName != null ? !songName.equals(like.songName) : like.songName != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (songName != null ? songName.hashCode() : 0);
        return result;
    }
}
