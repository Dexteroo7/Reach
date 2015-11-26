package reach.project.coreViews.yourProfile.blobCache;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by dexter on 07/11/15.
 */
public class CacheInvalidator {

    public final AtomicBoolean cacheInvalidated = new AtomicBoolean(false);
    public final AtomicBoolean invalidatorOpen = new AtomicBoolean(true);
}
