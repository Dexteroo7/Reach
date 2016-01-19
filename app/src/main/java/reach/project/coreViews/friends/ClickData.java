package reach.project.coreViews.friends;

/**
 * Created by dexter on 19/01/16.
 */
public class ClickData {

    private static ClickData ourInstance = new ClickData();

    public static ClickData getInstance() {
        return ourInstance;
    }

    private ClickData() {
    }

    public long friendId;
    public short status, networkType;
}