package reach.project.database.notifications;

import reach.backend.entities.userApi.model.ReachUser;

/**
 * Created by dexter on 08/07/15.
 */
public class BecameFriends extends NotificationBase {

    @Override
    public Types getTypes() {
        return super.getTypes();
    }

    @Override
    public void setTypes(Types types) {
        if (types != Types.BECAME_FRIENDS)
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
}
