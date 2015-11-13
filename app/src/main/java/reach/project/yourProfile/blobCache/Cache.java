package reach.project.yourProfile.blobCache;

import android.os.AsyncTask;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.squareup.wire.Message;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import reach.project.utils.MiscUtils;

/**
 * Created by dexter on 05/11/15.
 */

/**
 * TODO
 * 1) delete the file instead of just setting length to 0
 * 2) confirm object cleanup (streams)
 */

public abstract class Cache implements Closeable {

    @SuppressWarnings("FieldCanBeLocal")
    private static int BATCH_SIZE = 10;

    ///////////////
    @Nullable
    private DataInputStream streamFromCache = null;
    @Nullable
    private Map<Long, Boolean> visibilityMap = null;
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
        this.fileName = Objects.hash(cacheType, userId) + "";

        //custom thread pool
        final ThreadFactory threadFactory = new ThreadFactoryBuilder() //specify the name
                .setNameFormat("cache_thread " + fileName)
                .setPriority(Thread.MAX_PRIORITY)
                .setDaemon(false)
                .setUncaughtExceptionHandler((thread, ex) -> {
                    //TODO track and ?restart
                    ex.getLocalizedMessage();
//                new FetchNextBatch<>().executeOnExecutor(executorService);
                })
                .build();

        this.executorService = new ThreadPoolExecutor(
                1, //only 1 thread
                1, //only 1 thread
                0L, TimeUnit.MILLISECONDS, //no waiting
                new SynchronousQueue<>(false), //only 1 thread
                threadFactory,
                (r, executor) -> {
                    //TODO track and ignore
                });

        cacheWeakReference = new WeakReference<>(this);
    }

    private final String fileName;

    private final CacheInjectorCallbacks<Message> cacheInjectorCallbacks;

    //marker for loading done
    private final AtomicBoolean loadingDone = new AtomicBoolean(false);
    //marker for cache invalidation
    private final CacheInvalidator cacheInvalidator = new CacheInvalidator();

    //an executor for getting stories from server
    private final ExecutorService executorService;

    ///////////////Public interaction

    /**
     * Hook for item validation policy injection
     *
     * @param visibilityMap of validation policy
     */
    public void injectVisibilityMap(Map<Long, Boolean> visibilityMap) {
        this.visibilityMap = visibilityMap;
    }

    /**
     * Hook for loading next batch
     */
    public void loadMoreElements() {

        //cache invalidator MUST BE OPEN

        if (loadingDone.get())
            return; //do not load if done

        CacheLoader.getInstance().executeOnExecutor(executorService,
                fetchFromNetwork(), //network fetcher object
                streamFromCache, //local cache stream
                cacheInvalidator.cacheInvalidated.get() && cacheInvalidator.invalidatorOpen.get(), //flag for cache invalidation
                cacheWeakReference); //cache reference
    }

    /**
     * Hook for cache updater
     */
    public void invalidateCache() {

        //set only if invalidator is open
        if (cacheInvalidator.invalidatorOpen.get())
            cacheInvalidator.cacheInvalidated.set(true);
    }

    ///////////////

    @Override
    public void close() throws IOException {
        if (streamFromCache != null)
            streamFromCache.close();
    }

    /**
     * please maintain a static final immutable instance of this callable
     *
     * @return a callable that returns collection from network
     */
    protected abstract Callable<Collection<? extends Message>> fetchFromNetwork();

    //parse from byte array
    protected abstract Message getItem(byte[] source, int offset, int count) throws IOException;

    protected abstract void signalExternalInjection(boolean complete);

    protected abstract void loadingDone();

    private static final class CacheLoader extends AsyncTask<Object, Void, Void> implements CacheLoaderController<Message> {

        private static final CacheLoader cacheLoader = new CacheLoader();

        public static CacheLoader getInstance() {
            return cacheLoader;
        }

        @Nonnull
        private byte[] byteBuffer = new byte[1000]; //reusable readable byte buffer
        private int currentSize = 0;

        @Override
        protected Void doInBackground(Object... params) {

            if (params == null || params.length != 4)
                throw new IllegalArgumentException("Failed to give reference to required objects");
            if (!(params[0] instanceof Callable &&
                    params[1] instanceof DataInputStream &&
                    params[2] instanceof Boolean &&
                    params[3] instanceof WeakReference))
                throw new IllegalArgumentException("Required objects not of expected type");

            @SuppressWarnings("unchecked")
            final Callable<Collection<Message>> networkFetcher = (Callable<Collection<Message>>) params[0];
            @Nullable DataInputStream dataInputStream = (DataInputStream) params[1];
            final boolean cacheInvalidated = (boolean) params[2];
            @SuppressWarnings("unchecked")
            final WeakReference<Cache> cacheWeakReference = (WeakReference<Cache>) params[3];

            /**
             * If cache stream is dead, try to open new one
             */
            if (isCacheStreamDead(dataInputStream) && !cacheInvalidated) {

                final Cache cache;
                //noinspection unchecked
                if ((cache = cacheWeakReference.get()) == null)
                    return null; //buffer destroyed

                final Optional<DataInputStream> streamOptional = getCacheStream(cache.cacheInjectorCallbacks.getCacheDirectory(), cache.fileName);
                if (streamOptional.isPresent())
                    dataInputStream = streamOptional.get();
                cache.streamFromCache = dataInputStream; // set this stream as cache stream
            }

            /**
             * The batch of items to return
             */
            final Collection<Message> itemsToReturn;
            final boolean fullLoad;
            /**
             * If cache stream is still dead or external input says cache is invalidated, fetch from network.
             * This should guarantee a cache stream next time loader is started.
             * Right now cache won't be utilized as we already fetched everything from network.
             * Items will be overwritten to cache file.
             */
            if (isCacheStreamDead(dataInputStream) || cacheInvalidated) {

                itemsToReturn = fetchFromNetwork(networkFetcher);
                fullLoad = true;

                final Cache cache;
                //noinspection unchecked
                if ((cache = cacheWeakReference.get()) == null)
                    return null; //buffer destroyed

                //display as is, set true for this turn irrespective of failure
                cache.loadingDone.set(true);
                //cache can not be invalidated as fetched from network
                cache.cacheInvalidator.cacheInvalidated.set(false); //not invalidated
                cache.cacheInvalidator.invalidatorOpen.set(false);  //freeze invalidator
                //store in cache
                storeInCache(cache.cacheInjectorCallbacks.getCacheDirectory(), cache.fileName, itemsToReturn);
                //no need to check for visibilityMap here

            } else {

                /**
                 * Cache stream was found. Continue loading from cache.
                 * EOF / Exception will signal end of loading.
                 */

                itemsToReturn = new ArrayList<>(BATCH_SIZE);
                fullLoad = false;

                for (int index = 0; index < BATCH_SIZE; index++) {

                    //get instance here to make sure not dead
                    final Cache cache;
                    //noinspection unchecked
                    if ((cache = cacheWeakReference.get()) == null)
                        return null; //buffer destroyed

                    if (readItemIntoList(dataInputStream)) {

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
                        if (cache.visibilityMap == null)
                            itemsToReturn.add(item);
                        else {
                            //add only if visible
                            final long itemId = cache.cacheInjectorCallbacks.getItemId(item);
                            if (cache.visibilityMap.containsKey(itemId) && cache.visibilityMap.get(itemId))
                                itemsToReturn.add(item);
                        }

                    } else {

                        //stream dead, mark loading done
                        cache.loadingDone.set(true);
                        break;
                    }
                }
            }

            final Cache cache;
            //noinspection unchecked
            if ((cache = cacheWeakReference.get()) == null)
                return null; //buffer destroyed

            synchronized (cache.cacheInjectorCallbacks) {

                if (cache.loadingDone.get()) {

                    if (itemsToReturn != null && !itemsToReturn.isEmpty())
                        cache.cacheInjectorCallbacks.injectElements(itemsToReturn, fullLoad); //make the insertion

                    cache.loadingDone(); //remove last item (loading)

                } else if (itemsToReturn != null && !itemsToReturn.isEmpty()) //insert before loading
                    cache.cacheInjectorCallbacks.injectElements(itemsToReturn, fullLoad); //make the insertion

                /**
                 * If loading has finished request a full injection of smart lists
                 * Else request partial injection
                 */
                cache.signalExternalInjection(cache.loadingDone.get());
            }

            return null;
        }

        @Override
        public Optional<DataInputStream> getCacheStream(File cacheDirectory, String fileName) {

            //sanity check
            if (cacheDirectory == null || !cacheDirectory.exists() || !cacheDirectory.isDirectory())
                return Optional.absent();

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
            } catch (IOException e) {
                e.printStackTrace();
                return Optional.absent();
            }

            //get unCompressor stream
            final GZIPInputStream unCompressor;
            try {
                unCompressor = new GZIPInputStream(new FileInputStream(randomAccessFile.getFD()));
            } catch (IOException e) {

                e.printStackTrace();
                try {
                    randomAccessFile.setLength(0); //reset whole file
                    randomAccessFile.close();
                } catch (IOException ignored) {
                }
                return Optional.absent();
            }

            //return cache stream
            return Optional.of(new DataInputStream(unCompressor));
        }

        @Override
        public boolean storeInCache(File cacheDirectory, String fileName, Collection<Message> items) {

            //sanity check
            if (cacheDirectory == null || !cacheDirectory.exists() || !cacheDirectory.isDirectory() || items == null || items.isEmpty())
                return false;

            final RandomAccessFile randomAccessFile;
            try {
                randomAccessFile = new RandomAccessFile(cacheDirectory + "/" + fileName, "rw");
                randomAccessFile.setLength(0); //reset the size
            } catch (IOException e) {
                e.printStackTrace();
                return false; //nothing to do here, bad shit happened
            }

            //get the out put stream
            final GZIPOutputStream compressor;
            try {
                compressor = new GZIPOutputStream(new FileOutputStream(randomAccessFile.getFD()));
            } catch (IOException e) {

                e.printStackTrace();
                try {
                    randomAccessFile.setLength(0); //reset whole file
                    randomAccessFile.close();
                } catch (IOException ignored) {
                }
                return false;
            }

            final DataOutputStream dataOutputStream = new DataOutputStream(compressor);
            //default
            int currentSize;
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
                    dataOutputStream.writeInt(currentSize);
                    dataOutputStream.write(byteBuffer, 0, currentSize);
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

            try {
                randomAccessFile.close();
            } catch (IOException ignored) {
            }
            return true;
        }

        @Override
        public boolean isCacheStreamDead(InputStream stream) {

            //basic check
            if (stream == null)
                return true;

            //strict check
            try {
                //noinspection ResultOfMethodCallIgnored just checking
                stream.available();
                return false; //all checks passed
            } catch (IOException e) {

                e.printStackTrace();
                Log.e("Ayush", e.getLocalizedMessage());

                try {
                    stream.close(); //just in case
                } catch (IOException ignored) {
                }
                return true; //stream seems to be dead
            }
        }

        @Override
        public Collection<Message> fetchFromNetwork(Callable<Collection<Message>> networkFetcher) {

            return MiscUtils.autoRetry(() -> {

                try {
                    return networkFetcher.call();
                } catch (Exception e) {

                    if (e instanceof IOException)
                        throw (IOException) e;
                    else
                        return null;
                }
            }, Optional.absent()).orNull();
        }

        @Override
        public boolean readItemIntoList(@Nonnull DataInputStream dataInputStream) {

            try {
                currentSize = dataInputStream.readInt();
            } catch (IOException e) {

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
                dataInputStream.readFully(byteBuffer, 0, currentSize);
            } catch (IOException e) {
                e.printStackTrace();
                return false; //exit loader, do not retry
            }

            return true; //read successful
        }
    }
}