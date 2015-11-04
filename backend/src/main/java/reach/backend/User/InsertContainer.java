package reach.backend.User;

/**
 * Created by dexter on 02/10/15.
 */
public class InsertContainer {

    private final long userId;

    public String getFireBaseToken() {
        return fireBaseToken;
    }

    public long getUserId() {
        return userId;
    }

    private final String fireBaseToken;

    public InsertContainer(long userId, String fireBaseToken) {
        this.userId = userId;
        this.fireBaseToken = fireBaseToken;
    }
}
