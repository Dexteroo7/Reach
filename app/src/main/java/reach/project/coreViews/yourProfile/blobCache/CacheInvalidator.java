package reach.project.coreViews.yourProfile.blobCache;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by dexter on 07/11/15.
 */
class CacheInvalidator {

    final AtomicBoolean cacheInvalidated = new AtomicBoolean(false);
    final AtomicBoolean invalidatorOpen = new AtomicBoolean(true);
}
