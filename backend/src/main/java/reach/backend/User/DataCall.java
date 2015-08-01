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

        private final String userName;
        private final String phoneNumber;
        private final long songCount;
        private final long friendsCount;
        private final boolean isAlive;

        public Statistics(String userName, String phoneNumber, long songCount, long friendsCount, boolean isAlive) {
            this.userName = userName;
            this.phoneNumber = phoneNumber;
            this.songCount = songCount;
            this.friendsCount = friendsCount;
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
    }

    final class Solution {



        public int solution(int[] A) {

            if (A == null || A.length == 0)
                return 0;

            if (A.length == 1)
                return A[0];



            return 0;
        }
    }
}
