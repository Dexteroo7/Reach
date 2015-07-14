package reach.project.database.notifications;

import reach.backend.entities.userApi.model.ReachUser;

/**
 * Created by dexter on 06/07/15.
 */
public class Push extends NotificationBase {

    public String getPushContainer() {
        return pushContainer;
    }

    public void setPushContainer(String pushContainer) {
        this.pushContainer = pushContainer;
    }

    private String pushContainer = ""; //pushContainer compressed

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
    public long getHostId() {
        return super.getHostId();
    }

    @Override
    public void setHostId(long hostId) {
        super.setHostId(hostId);
    }

    @Override
    public void addBasicData(ReachUser user) {
        super.addBasicData(user);
    }

    @Override
    public short getRead() {
        return super.getRead();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Push)) return false;
        if (!super.equals(o)) return false;

        Push push = (Push) o;

        return !(pushContainer != null ? !pushContainer.equals(push.pushContainer) : push.pushContainer != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (pushContainer != null ? pushContainer.hashCode() : 0);
        return result;
    }
}
