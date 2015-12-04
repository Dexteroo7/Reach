package reach.project.coreViews.explore;

import android.os.AsyncTask;
import android.util.Log;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by dexter on 15/10/15.
 */
class ExploreBuffer<T> implements Closeable {

    private static final AtomicBoolean isDoneForToday = new AtomicBoolean(false);
    private static WeakReference<ExploreBuffer> bufferWeakReference;

    public static <T> ExploreBuffer<T> getInstance(Exploration<T> exploration) {

        final ExploreBuffer<T> buffer = new ExploreBuffer<>(exploration);
        bufferWeakReference = new WeakReference<>(buffer);
        //do some stuff
        return buffer;
    }

    ///////////////

    //can not call private constructor !
    private ExploreBuffer() {
        this.exploration = null;
    }

    private ExploreBuffer(Exploration<T> exploration) {
        this.exploration = exploration;
        storyBuffer.add(exploration.getLoadingResponse());
    }

    //holds a buffer of network objects
    private final CopyOnWriteArrayList<T> storyBuffer = new CopyOnWriteArrayList<>(new ArrayList<>(100));

    //interface for stuff
    private final Exploration<T> exploration;

    //custom thread pool
    private final ThreadFactory factory = new ThreadFactoryBuilder() //specify the name
            .setNameFormat("explorer_thread")
            .setPriority(Thread.MAX_PRIORITY)
            .setDaemon(false)
            .setUncaughtExceptionHandler((thread, ex) -> {
                //TODO track and ?restart
                ex.getLocalizedMessage();
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

    /**
     * Map index to buffer position.
     *
     * @param position the index of view pager to get item for
     * @return the respective item
     */
    public T getViewItem(int position) {

        final int currentSize = storyBuffer.size();

        //if reaching end of story and are not done yet
        if (position > currentSize - 3 && !isDoneForToday.get()) {

//            final T lastItem = storyBuffer.get(currentSize - 1);
//            if (!exploration.isDoneForDay(lastItem) && !exploration.isLoading(lastItem)) {
//
//                //if indication is absent
//                Log.i("Ayush", "Adding a loading response");
//                storyBuffer.add(exploration.getLoadingResponse());
//                exploration.notifyDataAvailable();
//            }

            new FetchNextBatch<>().executeOnExecutor(executorService, exploration.fetchNextBatch());
        }

        return storyBuffer.get(position);
    }

    /**
     * Get the current size of story buffer
     *
     * @return size in int
     */
    public int currentBufferSize() {
        return storyBuffer.size();
    }

    @Override
    public void close() {

        storyBuffer.clear();
        executorService.shutdownNow();
    }

    //runnable to fetch stories
    private static class FetchNextBatch<T> extends AsyncTask<Callable, Void, Collection<T>> {

        @Override
        protected final Collection<T> doInBackground(Callable... params) {

            Log.i("Ayush", "Fetching next batch of stories");

            if (params == null || params.length == 0)
                return null;

            try {
                return (Collection<T>) params[0].call();
            } catch (Exception e) {
                e.printStackTrace();
                //TODO track
            }

            return null;
        }

        @Override
        protected final void onPostExecute(Collection<T> batch) {

            super.onPostExecute(batch);

            if (batch == null || batch.isEmpty())
                return;

            final ExploreBuffer<T> buffer;
            //noinspection unchecked
            if (bufferWeakReference == null || (buffer = bufferWeakReference.get()) == null)
                return; //buffer destroyed

            //handle done for today response
            for (T item : batch)
                if (buffer.exploration.isDoneForDay(item)) {

                    isDoneForToday.set(true);
                    Log.i("Ayush", "RECEIVED DONE FOR TODAY !");
                    buffer.executorService.shutdownNow(); //shut down this shit
                    break;
                }

            //we need to overwrite any prior indication so we remove it
            final int currentSize = buffer.storyBuffer.size();
            final T lastItem = buffer.storyBuffer.get(currentSize - 1);
            final int insertFrom;
            if (buffer.exploration.isDoneForDay(lastItem) || buffer.exploration.isLoading(lastItem)) {
//                buffer.storyBuffer.remove(currentSize - 1);
                if (isDoneForToday.get()) {
                    insertFrom = currentSize - 1;
                    buffer.storyBuffer.remove(currentSize-1);
                }
                else
                    insertFrom = currentSize - 1;
                Log.i("Ayush", "Removing loading response");
            } else
                insertFrom = currentSize;

            buffer.storyBuffer.addAll(insertFrom, batch); //make the insertion
            buffer.exploration.notifyDataAvailable();
        }
    }

    interface Exploration<T> {

        //notify new batch added
        void notifyDataAvailable();

        //fetch next batch
        Callable<Collection<T>> fetchNextBatch();

        //verify done for today
        boolean isDoneForDay(T object);

        //verify loading
        boolean isLoading(T object);

        //get loading response
        T getLoadingResponse();
    }
}
