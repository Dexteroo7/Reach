package reach.backend.ObjectWrappers;

/**
 * Created by dexter on 20/10/15.
 */
public class HotStats {

    private final int totalAccounts;
    private final int totalActiveAccounts;

    private final int totalUniqueAccounts;
    private final int totalUniqueActiveAccounts;

    public HotStats(int totalAccounts,
                    int totalActiveAccounts,
                    int totalUniqueAccounts,
                    int totalUniqueActiveAccounts) {
        this.totalAccounts = totalAccounts;
        this.totalActiveAccounts = totalActiveAccounts;
        this.totalUniqueAccounts = totalUniqueAccounts;
        this.totalUniqueActiveAccounts = totalUniqueActiveAccounts;
    }

    public int getTotalAccounts() {
        return totalAccounts;
    }

    public int getTotalActiveAccounts() {
        return totalActiveAccounts;
    }

    public int getTotalUniqueAccounts() {
        return totalUniqueAccounts;
    }

    public int getTotalUniqueActiveAccounts() {
        return totalUniqueActiveAccounts;
    }
}
