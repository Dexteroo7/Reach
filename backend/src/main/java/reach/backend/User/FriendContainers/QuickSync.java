package reach.backend.User.FriendContainers;

import java.util.List;
import java.util.Map;

/**
 * Created by dexter on 17/07/15.
 */
public class QuickSync {

    //status change map
    private final Map<Long, Short> newStatus;
    //new newFriends found
    private final List<Friend> newFriends;
    //received requests (can be improved BUT will not be too big hence ignore)
    private final List<ReceivedRequest> receivedRequests;

    public List<ReceivedRequest> getReceivedRequests() {
        return receivedRequests;
    }

    public List<Friend> getNewFriends() {
        return newFriends;
    }

    public Map<Long, Short> getNewStatus() {
        return newStatus;
    }

    public QuickSync(Map<Long, Short> newStatus,
                     List<Friend> newFriends,
                     List<ReceivedRequest> receivedRequests) {

        this.newStatus = newStatus;
        this.newFriends = newFriends;
        this.receivedRequests = receivedRequests;
    }
}
