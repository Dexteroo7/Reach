package reach.project.coreViews.yourProfile.blobCache;

import android.os.AsyncTask;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.squareup.wire.Message;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import reach.project.utils.MiscUtils;

/**
 * Created by dexter on 05/11/15.
 */

public abstract class Cache implements Closeable {

    @SuppressWarnings("FieldCanBeLocal")
    private static int BATCH_SIZE = 10;

    ///////////////
    @Nullable
    private RandomAccessFile cacheAccess = null;
    @Nullable
    private WeakReference<Cache> cacheWeakReference = null;

    //can not call private constructor
    @SuppressWarnings("unused")
    private Cache() {
        cacheInjectorCallbacks = null;
        fileName = null;
        this.executorService = null;
    }

    public Cache(CacheInjectorCallbacks<Message> cacheInjectorCallbacks,
                 CacheType cacheType,
                 long userId) {

        this.cacheInjectorCallbacks = cacheInjectorCallbacks;
//        Log.i("Ayush", "Hashing " + cacheType.name() + " " + userId);
        this.fileName = cacheType.name() + userId;

        //custom thread pool
        final ThreadFactory threadFactory = new ThreadFactoryBuilder() //specify the name
                .setNameFormat("cache_thread " + fileName)
                .setDaemon(false)
                .build();

        this.executorService = MiscUtils.getRejectionExecutor(threadFactory);
        cacheWeakReference = new WeakReference<>(this);
    }

    private final String fileName;

    private final CacheInjectorCallbacks<Message> cacheInjectorCallbacks;

    //marker for loading done
    private final AtomicBoolean loadingDone = new AtomicBoolean(false);
    //marker for cache invalidation
    private final CacheInvalidator cacheInvalidator = new CacheInvalidator();
    //an executor for getting stories from server
    private final ThreadPoolExecutor executorService;

    ///////////////Public interaction

    //check if cache has fully loaded
    public boolean isLoadingDone() {
        return loadingDone.get();
    }

    /**
     * Hook for loading next batch
     */
    public void loadMoreElements(boolean complete) {

        //cache invalidator MUST BE OPEN

//        Log.i("Ayush", "Calling load more elements");

        if (loadingDone.get() || executorService.getActiveCount() > 0)
            return; //do not load if done

        new CacheLoader().executeOnExecutor(executorService,
                fetchFromNetwork(), //network fetcher object
                cacheAccess, //local cache file
                cacheInvalidator.invalidatorOpen.get() && cacheInvalidator.cacheInvalidated.get(), //flag for cache invalidation
                complete, //should load fully or partially
                cacheWeakReference); //cache reference
    }

    /**
     * Hook for cache updater
     */
    public void invalidateCache() {

        //set only if invalidator is open
        if (cacheInvalidator.invalidatorOpen.get()) {

            //mark invalidated
            cacheInvalidator.cacheInvalidated.set(true);
            //clear cache
            if (cacheAccess != null) {

                try {
                    cacheAccess.setLength(0);
                    cacheAccess.close();
                } catch (IOException ignored) {
                }
            }
            //load fresh
            loadingDone.set(false);
            executorService.purge();
        }
    }

    ///////////////

    @Override
    public void close() {

        MiscUtils.closeQuietly(cacheAccess);
        cacheAccess = null;

        if (cacheWeakReference != null)
            cacheWeakReference.clear();
        cacheWeakReference = null;

        executorService.shutdownNow();
    }

    /**
     * please maintain a static final immutable instance of this callable
     *
     * @return a callable that returns collection from network
     */
    protected abstract Callable<List<? extends Message>> fetchFromNetwork();

    //parse from byte array
    protected abstract Message getItem(byte[] source, int offset, int count) throws IOException;

    private static final class CacheLoader extends AsyncTask<Object, Void, Object[]> implements CacheLoaderController<Message> {

        @Nonnull
        private byte[] byteBuffer = new byte[300]; //reusable readable byte buffer
        private int currentSize = 0;

        @Override
        protected Object[] doInBackground(Object... params) {

            if (params == null || params.length != 5)
                throw new IllegalArgumentException("Failed to give reference to required objects");

            if (!(params[0] instanceof Callable &&
                    (params[1] == null || params[1] instanceof RandomAccessFile) && //if null ignore check
                    params[2] instanceof Boolean &&
                    params[3] instanceof Boolean &&
                    params[4] instanceof WeakReference))
                throw new IllegalArgumentException("Required objects not of expected type");

            @SuppressWarnings("unchecked")
            final Callable<List<Message>> networkFetcher = (Callable<List<Message>>) params[0];
            @Nullable RandomAccessFile randomAccessFile = (RandomAccessFile) params[1];

            final boolean cacheInvalidated = (boolean) params[2];
            final boolean loadFully = (boolean) params[3];
            final boolean networkLoad;

            @SuppressWarnings("unchecked")
            final WeakReference<Cache> cacheWeakReference = (WeakReference<Cache>) params[4];

            /**
             * If cache stream is dead, try to open new one
             */
            if (!cacheInvalidated && isCacheStreamDead(randomAccessFile)) {

                final Cache cache;
                //noinspection unchecked
                if ((cache = cacheWeakReference.get()) == null) {
                    MiscUtils.closeQuietly(randomAccessFile);
                    return null; //buffer destroyed
                }

                final Optional<RandomAccessFile> streamOptional = getCacheFile(cache.cacheInjectorCallbacks.getCacheDirectory(), cache.fileName);
                if (streamOptional.isPresent())
                    randomAccessFile = streamOptional.get();
                cache.cacheAccess = randomAccessFile; // set this stream as cache stream
                Log.i("Ayush", "Loading cache stream " + cache.fileName + " " + (cache.cacheAccess != null));
            }

            /**
             * The batch of items to return
             */
            final List<Message> itemsToReturn;
            /**
             * If cache stream is still dead or external input says cache is invalidated, fetch from network.
             * This should guarantee a cache stream next time loader is started.
             * Right now cache won't be utilized as we already fetched everything from network.
             * Items will be overwritten to cache file.
             */
            if (cacheInvalidated || isCacheStreamDead(randomAccessFile)) {

                Log.i("Ayush", "Fetching from network");
                itemsToReturn = fetchFromNetwork(networkFetcher);
                networkLoad = true;

                final Cache cache;
                //noinspection unchecked
                if ((cache = cacheWeakReference.get()) == null) {
                    MiscUtils.closeQuietly(randomAccessFile);
                    return null; //buffer destroyed
                }

                Log.i("Ayush", "Fetched " + itemsToReturn.size() + " " + cache.fileName);

                //display as is, set true for this turn irrespective of failure
                cache.loadingDone.set(true);
                //cache can not be invalidated as fetched from network
                cache.cacheInvalidator.cacheInvalidated.set(false);
                cache.cacheInvalidator.invalidatorOpen.set(false);  //freeze invalidator
                //store in cache
                final boolean result = storeInCache(cache.cacheInjectorCallbacks.getCacheDirectory(), cache.fileName, itemsToReturn);
                Log.i("Ayush", "Storing in cache " + result);
                //no need to check for visibilityMap here ?

            } else {

                /**
                 * Cache stream was found. Continue loading from cache.
                 * EOF / Exception will signal end of loading.
                 */

                Log.i("Ayush", "Streaming from cache");
                itemsToReturn = new ArrayList<>(BATCH_SIZE);
                networkLoad = false;

                for (int index = 0; index < BATCH_SIZE || loadFully; index++) {

                    //get instance here to make sure not dead
                    final Cache cache;
                    //noinspection unchecked
                    if ((cache = cacheWeakReference.get()) == null) {
                        MiscUtils.closeQuietly(randomAccessFile);
                        return null; //buffer destroyed
                    }

                    if (readItemFromCache(randomAccessFile)) {

                        //byte buffer and current size singleton instance
                        final Message item;
                        try {
                            item = cache.getItem(byteBuffer, 0, currentSize);
                        } catch (IOException e) {
                            e.printStackTrace();
                            //stream dead, mark loading done
                            cache.loadingDone.set(true);
                            break;
                        }
                        //handle visibility
                        if (cache.cacheInjectorCallbacks.verifyItemVisibility(item))
                            itemsToReturn.add(item);

                    } else {

                        //stream dead, mark loading done
                        cache.loadingDone.set(true);
                        break;
                    }
                }
            }

            //either loaded from network or fetched in 1 go from cache
            final boolean fullLoad = networkLoad || loadFully || cacheInvalidated;
            return new Object[]{itemsToReturn, fullLoad, cacheWeakReference};
        }

        @Override
        protected void onPostExecute(Object[] params) {

            super.onPostExecute(params);

            if (params == null)
                return;

            if (params.length != 3)
                throw new IllegalArgumentException("Failed to give reference to required objects");

            if (!(params[0] instanceof List &&
                    params[1] instanceof Boolean &&
                    params[2] instanceof WeakReference))
                throw new IllegalArgumentException("Required objects not of expected type");

            @SuppressWarnings("unchecked") final List<Message> itemsToReturn = (List<Message>) params[0];
            @SuppressWarnings("unchecked") final WeakReference<Cache> cacheWeakReference = (WeakReference<Cache>) params[2];
            final boolean fullLoad = (boolean) params[1];

            final Cache cache;
            //noinspection unchecked
            if ((cache = cacheWeakReference.get()) != null)
                //make the insertion, removal not supported bug
                cache.cacheInjectorCallbacks.injectElements(new ArrayList<>(itemsToReturn), fullLoad, cache.loadingDone.get());
        }

        @Override
        public Optional<RandomAccessFile> getCacheFile(File cacheDirectory, String fileName) {

            //sanity check
            if (cacheDirectory == null || !cacheDirectory.exists() || !cacheDirectory.isDirectory())
                return Optional.absent();

            Log.i("Ayush", "Opening cache stream " + cacheDirectory + " " + fileName);

            //get file
            final RandomAccessFile randomAccessFile;
            try {
                randomAccessFile = new RandomAccessFile(cacheDirectory + "/" + fileName, "r");
            } catch (IOException e) {
                e.printStackTrace();
                return Optional.absent(); //nothing to do here, bad shit happened
            }

            //check length of file
            try {
                if (randomAccessFile.length() == 0)
                    return Optional.absent();
                else
                    Log.i("Ayush", "Found cache file " + randomAccessFile.length() + " bytes");
            } catch (IOException e) {
                e.printStackTrace();
                MiscUtils.closeQuietly(randomAccessFile);
                return Optional.absent();
            }

            //return cache stream
            return Optional.of(randomAccessFile);
        }

        @Override
        public boolean storeInCache(File cacheDirectory, String fileName, List<Message> items) {

            //sanity check
            if (cacheDirectory == null || !cacheDirectory.exists() || !cacheDirectory.isDirectory() || items == null || items.isEmpty())
                return false;

            Log.i("Ayush", "Storing into " + cacheDirectory + " " + fileName + " " + items.size());

            final RandomAccessFile randomAccessFile;
            try {
                randomAccessFile = new RandomAccessFile(cacheDirectory + "/" + fileName, "rw");
            } catch (IOException e) {
                e.printStackTrace();
                return false; //nothing to do here, bad shit happened
            }

            try {
                randomAccessFile.setLength(0); //reset the size
            } catch (IOException e) {
                e.printStackTrace();
                MiscUtils.closeQuietly(randomAccessFile);
            }

            //write to file
            for (Message item : items) {

                currentSize = item.getSerializedSize();
                //resize the byte array
                if (byteBuffer.length < currentSize) {
                    byteBuffer = new byte[currentSize + 1];
                    Log.i("Ayush", "Allocating new byte array " + currentSize + 1);
                }

                //get bytes
                item.writeTo(byteBuffer);

                /**
                 * write to file
                 * first write the size, then write the object
                 */
                try {
                    randomAccessFile.writeInt(currentSize);
                    randomAccessFile.write(byteBuffer, 0, currentSize);
                } catch (IOException e) {

                    e.printStackTrace();
                    try {
                        randomAccessFile.setLength(0); //reset whole file
                        randomAccessFile.close();
                    } catch (IOException ignored) {
                    }
                    return false;
                }
            }

            MiscUtils.closeQuietly(randomAccessFile);
            return true;
        }

        @Override
        public boolean isCacheStreamDead(RandomAccessFile randomAccessFile) {

            //basic check
            if (randomAccessFile == null)
                return true;

            //strict check
            try {
                return randomAccessFile.length() == 0; //all checks passed
            } catch (IOException e) {

                e.printStackTrace();
                Log.e("Ayush", e.getLocalizedMessage());
                MiscUtils.closeQuietly(randomAccessFile);
                return true; //stream seems to be dead
            }
        }

        @Override
        public List<Message> fetchFromNetwork(Callable<List<Message>> networkFetcher) {

            return MiscUtils.autoRetry(() -> {

                try {
                    return networkFetcher.call();
                } catch (Exception e) {

                    e.printStackTrace();
                    if (e instanceof IOException)
                        throw (IOException) e;
                    else
                        return null;
                }
            }, Optional.absent()).or(Collections.emptyList());
        }

        @Override
        public boolean readItemFromCache(@Nonnull RandomAccessFile randomAccessFile) {

            try {
                currentSize = randomAccessFile.readInt();
            } catch (IOException e) {

                MiscUtils.closeQuietly(randomAccessFile);
                e.printStackTrace();
                return false; //exit loader, do not retry
            }

            //resize the byte array
            if (byteBuffer.length < currentSize) {
                byteBuffer = new byte[currentSize + 1];
                Log.i("Ayush", "Allocating new byte array " + currentSize + 1);
            }

            //read the object
            try {
                randomAccessFile.readFully(byteBuffer, 0, currentSize);
            } catch (IOException e) {

                MiscUtils.closeQuietly(randomAccessFile);
                e.printStackTrace();
                return false; //exit loader, do not retry
            }

            return true; //read successful
        }
    }
}