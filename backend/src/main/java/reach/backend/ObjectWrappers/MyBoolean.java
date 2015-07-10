package reach.backend.ObjectWrappers;

/**
 * Created by dexter on 4/11/14.
 */

public class MyBoolean {

    public boolean isGCMExpired() {
        return isGCMExpired;
    }

    final boolean isGCMExpired, isOtherGCMExpired;

    public boolean isOtherGCMExpired() {
        return isOtherGCMExpired;
    }

    public MyBoolean(boolean isGCMExpired, boolean isOtherGCMExpired) {
        this.isGCMExpired = isGCMExpired;
        this.isOtherGCMExpired = isOtherGCMExpired;
    }
}
