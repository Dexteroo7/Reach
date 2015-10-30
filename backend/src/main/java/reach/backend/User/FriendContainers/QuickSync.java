package reach.backend.User.FriendContainers;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by dexter on 17/07/15.
 */
public class QuickSync {

    //status to change, partial update
    private final Map<Long, Short> newStatus;
    //friends to insert
    private final Set<Friend> newFriends;
    //friends whose data is to be updated
    private final List<Friend> toUpdate;

    public List<Friend> getToUpdate() {
        return toUpdate;
    }

    public Set<Friend> getNewFriends() {
        return newFriends;
    }

    public Map<Long, Short> getNewStatus() {
        return newStatus;
    }

    public QuickSync(Map<Long, Short> newStatus, Set<Friend> newFriends, List<Friend> toUpdate) {

        this.newStatus = newStatus;
        this.newFriends = newFriends;
        this.toUpdate = toUpdate;
    }
}
