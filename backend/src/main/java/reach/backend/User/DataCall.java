package reach.backend.user;

import com.google.common.collect.ImmutableList;

/**
 * Created by dexter on 09/07/15.
 */
public class DataCall {

    private final ImmutableList<Statistics> immutableList;
    private final String nextCursor;

    public DataCall(ImmutableList<Statistics> immutableList, String nextCursor) {
        this.immutableList = immutableList;
        this.nextCursor = nextCursor;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public ImmutableList<Statistics> getImmutableList() {
        return immutableList;
    }

    /**
     * Created by dexter on 09/07/15.
     */
    public static class Statistics {

        private final long userId;
        private final int songCount;
        private final int friendsCount;
        private final int sentRequests;
        private final int pendingRequests;

        private final boolean isAlive;

        private final String userName;
        private final String phoneNumber;

        public Statistics(long userId, String userName, String phoneNumber, int songCount, int friendsCount, int sentRequests, int pendingRequests, boolean isAlive) {
            this.userId = userId;
            this.userName = userName;
            this.phoneNumber = phoneNumber;
            this.songCount = songCount;
            this.friendsCount = friendsCount;
            this.sentRequests = sentRequests;
            this.pendingRequests = pendingRequests;
            this.isAlive = isAlive;
        }

        public boolean isAlive() {
            return isAlive;
        }

        public long getFriendsCount() {
            return friendsCount;
        }

        public long getSongCount() {
            return songCount;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public String getUserName() {
            return userName;
        }

        public long getUserId() {
            return userId;
        }

        public int getSentRequests() {
            return sentRequests;
        }

        public int getPendingRequests() {
            return pendingRequests;
        }
    }
}
