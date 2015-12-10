package reach.backend;

import javax.annotation.Nullable;

/**
 * Created by dexter on 26/10/15.
 */
public enum TextUtils {;

    /**
     * Returns true if the string is null or 0-length.
     *
     * @param str the string to be examined
     * @return true if str is null or zero length
     */
    public static boolean isEmpty(@Nullable CharSequence str) {
        return str == null || str.length() == 0;
    }
}
