package reach.project.explore;

import android.os.AsyncTask;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by dexter on 15/10/15.
 */
public class ExploreBuffer<T> implements Closeable {

    private static WeakReference<ExploreBuffer> bufferWeakReference;

    public static <T> ExploreBuffer<T> getInstance(int bufferLength, Exploration<T> exploration) {

        final ExploreBuffer<T> buffer = new ExploreBuffer<>(bufferLength, exploration);
        bufferWeakReference = new WeakReference<>(buffer);
        //do some stuff
        return buffer;
    }

    ///////////////

    private ExploreBuffer(int bufferLength, Exploration<T> exploration) {

        this.bufferLength = bufferLength;
        this.exploration = exploration;
    }

    //holds a buffer of network objects
    private final ConcurrentLinkedQueue<T> storyBuffer = new ConcurrentLinkedQueue<>();

    //buffer for view
    private final Set<T> viewBuffer = Collections.newSetFromMap(new LinkedHashMap<T, Boolean>() {
        protected boolean removeEldestEntry(Map.Entry<T, Boolean> eldest) {
            return size() > bufferLength;
        }
    });

    //interface for stuff
    private final Exploration<T> exploration;

    //fixed view buffer length
    private final int bufferLength;

    //custom thread pool
    private final ThreadFactory factory = new ThreadFactoryBuilder() //specify the name
            .setNameFormat("explorer_thread")
            .setPriority(Thread.MAX_PRIORITY)
            .setDaemon(false)
            .setUncaughtExceptionHandler((thread, ex) -> {
                //TODO track and restart
                //ex.getLocalizedMessage()
//                new FetchNextBatch<>().executeOnExecutor(executorService);
            })
            .build();

    //an executor for getting stories from server
    private final ExecutorService executorService = new ThreadPoolExecutor(
            1, //only 1 thread
            1, //only 1 thread
            0L, TimeUnit.MILLISECONDS, //no waiting
            new SynchronousQueue<>(false), //only 1 thread
            factory,
            (r, executor) -> {
                //TODO track and ignore
            });

    public Optional<T> getViewItem(int position) {

        if (position > viewBuffer.size())
            return Optional.absent(); //indicate loading / done for today

        final Iterator<T> itemIterator = viewBuffer.iterator();
        int currentPosition = 0;
        while (itemIterator.hasNext() && ++currentPosition < position)
            itemIterator.next();

        if (currentPosition == position)
            return Optional.of(itemIterator.next());

        return Optional.absent(); //should not happen
    }

    /**
     * Takes out an item from the buffer, returning absent if not found (show loading)
     * Also fires refill network calls if remaining buffer is empty
     *
     * @return optional over T
     */
    private Optional<T> takeNextItem() {

        final T toReturn = storyBuffer.poll();

        //if no more items found, fetch next list
        if (toReturn == null) {
            new FetchNextBatch<>().executeOnExecutor(executorService);
            return Optional.absent();
        }

        //if this was the last time, fetch next list
        if (storyBuffer.peek() == null)
            new FetchNextBatch<>().executeOnExecutor(executorService);

        return Optional.of(toReturn);
    }

    @Override
    public void close() throws IOException {

        viewBuffer.clear();
        storyBuffer.clear();
        executorService.shutdownNow();
    }

    //runnable to fetch stories
    private static class FetchNextBatch<T> extends AsyncTask<Void, Void, Collection<T>> {

        @Override
        protected Collection<T> doInBackground(Void... params) {

            final ExploreBuffer<T> buffer;
            //noinspection unchecked
            if (bufferWeakReference == null || (buffer = bufferWeakReference.get()) == null)
                return null;

            try {
                return buffer.exploration.fetchNextBatch().call();
            } catch (Exception e) {
                e.printStackTrace();
                //TODO track
            }

            return null;
        }

        @Override
        protected void onPostExecute(Collection<T> batch) {

            super.onPostExecute(batch);

            final ExploreBuffer<T> buffer;
            //noinspection unchecked
            if (batch == null || batch.size() == 0 || bufferWeakReference == null || (buffer = bufferWeakReference.get()) == null)
                return;

            buffer.storyBuffer.addAll(batch); //make the insertion
            buffer.exploration.notifyDataAvailable();
        }
    }

    public interface Exploration<T> {

        //get tracker
        void notifyDataAvailable();

        Callable<Collection<T>> fetchNextBatch();
    }
}
