package reach.backend.objectWrappers;

/**
 * Created by dexter on 4/11/14.
 */

public class MyBoolean {

    final boolean isGCMExpired, isOtherGCMExpired;

    public MyBoolean(boolean isGCMExpired, boolean isOtherGCMExpired) {
        this.isGCMExpired = isGCMExpired;
        this.isOtherGCMExpired = isOtherGCMExpired;
    }

    public boolean isGCMExpired() {
        return isGCMExpired;
    }

    public boolean isOtherGCMExpired() {
        return isOtherGCMExpired;
    }
}
