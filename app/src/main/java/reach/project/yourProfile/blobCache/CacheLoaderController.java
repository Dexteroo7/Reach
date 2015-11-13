package reach.project.yourProfile.blobCache;

import com.google.common.base.Optional;
import com.squareup.wire.Message;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

/**
 * Created by dexter on 05/11/15.
 */
public interface CacheLoaderController<T extends Message> {

    /**
     * Return a cache stream from given file
     *
     * @param cacheDirectory the directory to where create the cache file in
     * @param fileName       the name of the cache file if present
     * @return cache stream
     */
    Optional<DataInputStream> getCacheStream(File cacheDirectory, String fileName);

    /**
     * Overwrite new entities on-to cache
     *
     * @param cacheDirectory the directory to where create the cache file in
     * @param fileName       the name of cache file
     * @param items          the items to cache
     * @return true if successful
     */
    boolean storeInCache(File cacheDirectory, String fileName, Collection<T> items);

    /**
     * Check if data source from cache is alive.
     * Once active cache stream is established, cache will not be updated
     *
     * @param stream the stream to check
     * @return true if dead
     */
    boolean isCacheStreamDead(InputStream stream);

    /**
     * Fetch complete batch of entities from server
     *
     * @return an iterable of entities
     */
    Collection<T> fetchFromNetwork(Callable<Collection<T>> networkFetcher);

    /**
     * @param inputStream to read item from
     * @return absent signals stream death, end loading
     */
    boolean readItemIntoList(@Nonnull DataInputStream inputStream);
}