package reach.project.yourProfile.blobCache;

import com.google.common.base.Optional;
import com.squareup.wire.Message;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.List;
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
     * @return cache file
     */
    Optional<RandomAccessFile> getCacheFile(File cacheDirectory, String fileName);

    /**
     * Overwrite new entities on-to cache
     *
     * @param cacheDirectory the directory to where create the cache file in
     * @param fileName       the name of cache file
     * @param items          the items to cache
     * @return true if successful
     */
    boolean storeInCache(File cacheDirectory, String fileName, List<T> items);

    /**
     * Check if data source from cache is alive.
     * Once active cache stream is established, cache will not be updated
     *
     * @param randomAccessFile the file to check
     * @return true if dead
     */
    boolean isCacheStreamDead(RandomAccessFile randomAccessFile);

    /**
     * Fetch complete batch of entities from server
     *
     * @return an iterable of entities
     */
    List<T> fetchFromNetwork(Callable<List<T>> networkFetcher);

    /**
     * @param randomAccessFile to read item from
     * @return false signals stream death, end loading
     */
    boolean readItemFromCache(@Nonnull RandomAccessFile randomAccessFile);
}