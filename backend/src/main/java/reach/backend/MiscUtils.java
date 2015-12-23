package reach.backend;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Key;

import java.io.Closeable;
import java.io.IOException;

import reach.backend.User.ReachUser;

/**
 * Created by dexter on 13/12/15.
 */
public enum MiscUtils {
    ;

    public static final Blob emptyBlob = new Blob(new byte[]{});

    public static long longHash(String string) {
        long h = 1125899906842597L; // prime
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = 31 * h + string.charAt(i);
        }
        return h;
    }

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

    public static ImmutableList.Builder<Key> getKeyBuilder(Iterable<Long> ids) {

        final ImmutableList.Builder<Key> dirtyCheck = new ImmutableList.Builder<>();
        for (Long id : ids)
            dirtyCheck.add(Key.create(ReachUser.class, id));
        return dirtyCheck;
    }

    public static ImmutableList.Builder<Key> getKeyBuilder(Long... ids) {

        final ImmutableList.Builder<Key> dirtyCheck = new ImmutableList.Builder<>();
        for (Long id : ids)
            dirtyCheck.add(Key.create(ReachUser.class, id));
        return dirtyCheck;
    }

    public static long computeLastSeen(MemcacheService syncCache, long userId) {

        final byte [] value = (byte[]) syncCache.get(userId);

        final long currentTime = System.currentTimeMillis();
        final long lastSeen;
        if (value == null || value.length == 0)
            lastSeen = currentTime;
        else {
            final String val = new String(value);
            if (val.equals(""))
                lastSeen = currentTime;
            else
                lastSeen = currentTime - Long.parseLong(val);
        }
        return lastSeen;
    }
}
