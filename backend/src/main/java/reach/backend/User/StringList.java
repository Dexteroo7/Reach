package reach.backend.User;

import java.util.List;

/**
 * Created by dexter on 03/11/15.
 */
public class StringList {

    private List<String> stringList;
    private long userId;

    public StringList() {

    }

    public List<String> getStringList() {
        return stringList;
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }
}
