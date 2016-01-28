package reach.project.coreViews.explore;

import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import reach.project.utils.MiscUtils;

/**
 * Created by dexter on 15/10/15.
 */
class ExploreBuffer<T> implements Closeable {

    private static final String EXPLORE_CACHE_FILE = "EXPLORE_CACHE_FILE";

    @Nullable
    private static WeakReference<ExploreBuffer> bufferWeakReference;

    public static <T> ExploreBuffer<T> getInstance(ExplorationCallbacks<T> explorationCallbacks,
                                                   File cacheDirectory,
                                                   Function<byte[], Collection<T>> bytesToObject,
                                                   Function<List<T>, byte[]> objectToBytes) {

        final ExploreBuffer<T> buffer = new ExploreBuffer<>(
                explorationCallbacks,
                cacheDirectory,
                bytesToObject,
                objectToBytes);
        bufferWeakReference = new WeakReference<>(buffer);
        //do some stuff
        return buffer;
    }

    ///////////////

    @SuppressWarnings("unused")
    private ExploreBuffer() throws IllegalAccessException {

        this.explorationCallbacks = null;
        this.cacheDirectory = null;
        throw new IllegalAccessException("Fuck off");
    }

    private ExploreBuffer(ExplorationCallbacks<T> explorationCallbacks,
                          File cacheDirectory,
                          Function<byte[], Collection<T>> bytesToObject,
                          Function<List<T>, byte[]> objectToBytes) {

        this.explorationCallbacks = explorationCallbacks;
        this.cacheDirectory = cacheDirectory;
        this.objectToBytes = objectToBytes;
        //initialize with data from local cache
        new FetchFromCache<>().executeOnExecutor(storyFetchingService, cacheDirectory, bytesToObject);
    }

    //transformer functions
    private final Function<List<T>, byte[]> objectToBytes;

    //the cache directory
    private final File cacheDirectory;

    //holds a buffer of network objects
    private final List<T> storyBuffer = new ArrayList<>(100);

    //interface for stuff
    private final ExplorationCallbacks<T> explorationCallbacks;

    //custom thread pool
    private final ThreadFactory factory = new ThreadFactoryBuilder() //specify the name
            .setNameFormat("explorer_thread")
            .setPriority(Thread.MAX_PRIORITY)
            .setDaemon(false)
            .build();

    //an executor for getting stories from server
    private final ExecutorService storyFetchingService = MiscUtils.getRejectionExecutor(factory);

    //an executor for caching stories
    private final ExecutorService storyCachingService = MiscUtils.getRejectionExecutor(factory);

    /**
     * Map index to buffer position.
     *
     * @param position the index of view pager to get item for
     * @return the respective item
     */
    public T getViewItem(int position) {

        final int currentSize = storyBuffer.size();

        //if reaching end of story and are not done yet
        if (position == currentSize - 1)
            new FetchNextBatch<>().executeOnExecutor(storyFetchingService, explorationCallbacks.fetchNextBatch());

        return storyBuffer.get(position);
    }

    /**
     * Get the current size of story buffer
     *
     * @return size in int
     */
    public int currentBufferSize() {

        final int currentSize = storyBuffer.size();
        if (currentSize == 0)
            new FetchNextBatch<>().executeOnExecutor(storyFetchingService, explorationCallbacks.fetchNextBatch());
        return currentSize;
    }

    @Override
    public void close() {

        storyCachingService.submit(new SaveExplore<>(storyBuffer, cacheDirectory, objectToBytes));
        storyFetchingService.shutdownNow();
    }

    //utility to cache last few stories before going away
    private static final class SaveExplore<T> implements Runnable {

        private final List<T> storiesToSave;
        private final File cacheDirectory;
        private final Function<List<T>, byte[]> transformer;

        private SaveExplore(List<T> fullList,
                            File cacheDirectory,
                            Function<List<T>, byte[]> transformer) {

            final int size = fullList.size();

            if (size > 10)
                this.storiesToSave = fullList.subList(size - 10, size);
            else
                this.storiesToSave = fullList;

            this.cacheDirectory = cacheDirectory;
            this.transformer = transformer;
        }

        @Override
        public void run() {

            final RandomAccessFile cacheAccess;
            try {
                cacheAccess = new RandomAccessFile(cacheDirectory + "/" + EXPLORE_CACHE_FILE, "rwd");
                cacheAccess.setLength(0); //reset the cache
            } catch (IOException e) {
                e.printStackTrace();
                return; //failed :(
            }

            final byte[] toCache = transformer.apply(storiesToSave);

            if (toCache == null || toCache.length == 0)
                return; //nothing to cache, weird

            //cache the byte array and close
            try {
                cacheAccess.write(toCache);
                cacheAccess.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //utility to fetch stories from cache
    private static class FetchFromCache<T> extends AsyncTask<Object, Void, Collection<T>> {

        @Override
        protected Collection<T> doInBackground(Object... params) {

            if (!(params.length == 2 &&
                    params[0] instanceof File &&
                    params[1] instanceof Function))
                throw new IllegalArgumentException("Expected arguments not found");

            final File cacheDirectory = (File) params[0];
            final Function<byte[], List<T>> transformer = (Function<byte[], List<T>>) params[1];

            //read explore cache
            final byte[] byteArray;
            try {
                final RandomAccessFile cacheAccess = new RandomAccessFile(cacheDirectory + "/" + EXPLORE_CACHE_FILE, "rwd");
                final int length = (int) cacheAccess.length();
                if (length == 0)
                    return Collections.emptyList();
                byteArray = new byte[length];
                cacheAccess.readFully(byteArray);
            } catch (IOException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }

            //parse the byte array and return
            return transformer.apply(byteArray);
        }

        @Override
        protected void onPostExecute(Collection<T> stories) {

            super.onPostExecute(stories);

            if (stories == null || stories.isEmpty())
                return;

            final ExploreBuffer<T> buffer;
            if (bufferWeakReference == null || (buffer = bufferWeakReference.get()) == null)
                return; //buffer destroyed

            //when loading from cache we always prepend the data
            synchronized (buffer.storyBuffer) {
                buffer.storyBuffer.addAll(0, stories); //make the insertion
            }

            Log.i("Ayush", "Explore cache inserted, trying to notify");
            buffer.explorationCallbacks.loadedFromCache(stories.size());
        }
    }

    //utility to fetch stories
    private static class FetchNextBatch<T> extends AsyncTask<Callable, Void, Collection<T>> {

        @Override
        protected final Collection<T> doInBackground(Callable... params) {

            Log.i("Ayush", "Fetching next batch of stories");

            if (params == null || params.length == 0)
                return Collections.emptyList();

            //cast to exact type
            final Callable<Collection<T>> exactType = params[0];
            return MiscUtils.autoRetry(exactType::call, Optional.absent()).or(Collections.emptyList());
        }

        @Override
        protected final void onPostExecute(Collection<T> stories) {

            super.onPostExecute(stories);

            final ExploreBuffer<T> buffer;
            if (bufferWeakReference == null || (buffer = bufferWeakReference.get()) == null)
                return; //buffer destroyed

            if (stories == null || stories.isEmpty()) {

                Log.i("Ayush", "RECEIVED DONE FOR TODAY !");
                return;
            }

            //append from last
            synchronized (buffer.storyBuffer) {
                buffer.storyBuffer.addAll(stories); //make the insertion
            }

            Log.i("Ayush", "Stories inserted and trying to notify");

            buffer.explorationCallbacks.notifyDataAvailable();
        }
    }

    interface ExplorationCallbacks<T> {

        //notify new batch added
        void notifyDataAvailable();

        //fetch next batch
        Callable<Collection<T>> fetchNextBatch();

        //first load from cache
        void loadedFromCache(int count);
    }
}
