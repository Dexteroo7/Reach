package reach.backend;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by dexter on 13/12/15.
 */
public enum MiscUtils {
    ;

    public static void closeQuietly(Closeable... closeables) {
        for (Closeable closeable : closeables)
            if (closeable != null)
                try {
                    closeable.close();
                } catch (IOException ignored) {
                }
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null)
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
    }
}
