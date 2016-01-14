package reach.project.coreViews.explore;

import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

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
import java.util.concurrent.atomic.AtomicBoolean;

import reach.project.utils.MiscUtils;

/**
 * Created by dexter on 15/10/15.
 */
class ExploreBuffer<T> implements Closeable {

    private static final String EXPLORE_CACHE_FILE = "EXPLORE_CACHE_FILE";

    @Nullable
    private static WeakReference<ExploreBuffer> bufferWeakReference;

    public static <T> ExploreBuffer<T> getInstance(ExplorationCallbacks<T> explorationCallbacks,
                                                   File cacheDirectory) {

        final ExploreBuffer<T> buffer = new ExploreBuffer<>(explorationCallbacks, cacheDirectory);
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
                          File cacheDirectory) {

        this.explorationCallbacks = explorationCallbacks;
        this.cacheDirectory = cacheDirectory;
        //initialize with data from local cache
        new FetchFromCache<>().executeOnExecutor(storyFetchingService, cacheDirectory);
    }

    private final File cacheDirectory;

    //flag for done for today
    private final AtomicBoolean isDoneForToday = new AtomicBoolean(false);

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
        if (position > currentSize - 3 && !isDoneForToday.get())
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

    //is loader done ?
    public boolean isDoneForToday() {
        return isDoneForToday.get();
    }

    @Override
    public void close() {

        storyCachingService.submit(new SaveExplore<>(storyBuffer, cacheDirectory));
        storyFetchingService.shutdownNow();
        storyCachingService.shutdown(); //wait for cache to happen
    }

    //utility to cache last few stories before going away
    private static final class SaveExplore<T> implements Runnable {

        private final List<T> fullList;
        private final List<T> storiesToSave;
        private final File cacheDirectory;

        private SaveExplore(List<T> fullList, File cacheDirectory) {

            final int size = fullList.size();

            this.fullList = fullList;
            if (size > 10)
                this.storiesToSave = fullList.subList(size - 10, size);
            else
                this.storiesToSave = fullList;
            this.cacheDirectory = cacheDirectory;
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
            } finally {
                MiscUtils.closeQuietly(fullList, storiesToSave);
            }

            final byte[] toCache = MiscUtils.useReference(bufferWeakReference, buffer -> {
                return buffer.explorationCallbacks.transform(storiesToSave);
            }).or(new byte[0]);

            if (toCache == null || toCache.length == 0) {

                MiscUtils.closeQuietly(fullList, storiesToSave);
                return; //nothing to cache, weird
            }

            //cache the byte array and close
            try {
                cacheAccess.write(toCache);
                cacheAccess.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                MiscUtils.closeQuietly(fullList, storiesToSave);
            }
        }
    }

    //utility to fetch stories from cache
    private static class FetchFromCache<T> extends AsyncTask<File, Void, Collection<T>> {

        @Override
        protected Collection<T> doInBackground(File... params) {

            //read explore cache
            final byte[] byteArray;
            try {
                final RandomAccessFile cacheAccess = new RandomAccessFile(params[0] + "/" + EXPLORE_CACHE_FILE, "rwd");
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
            //noinspection unchecked
            return MiscUtils.useReference(bufferWeakReference, exploreBuffer -> {
                return exploreBuffer.explorationCallbacks.transform(byteArray);
            }).or(Collections.emptyList());
        }

        @Override
        protected void onPostExecute(Collection<T> stories) {

            super.onPostExecute(stories);

            if (stories == null || stories.isEmpty())
                return;

            final ExploreBuffer<T> buffer;
            //noinspection unchecked
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
            //noinspection unchecked
            if (bufferWeakReference == null || (buffer = bufferWeakReference.get()) == null)
                return; //buffer destroyed

            if (stories == null || stories.isEmpty()) {

                buffer.isDoneForToday.set(true);
                Log.i("Ayush", "RECEIVED DONE FOR TODAY !");
                buffer.storyFetchingService.shutdown(); //shut down this shit
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

        /**
         * @param data the byte[] to transform from
         * @return collection of explore stories, take care to remove loading / done for today etc...
         */
        Collection<T> transform(byte[] data);

        /**
         * @param data the collection to transform into byte[]
         * @return byte[] explore stories, take care to remove loading / done for today etc...
         */
        byte[] transform(List<T> data);
    }
}
