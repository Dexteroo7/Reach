package reach.project.coreViews.explore;

import android.os.AsyncTask;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import reach.project.utils.MiscUtils;

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
    private final List<T> storyBuffer = new ArrayList<>(100);

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
            (r, executor) -> {//ignored
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

        if (position > currentSize)
            return null;

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

        synchronized (storyBuffer) {
            storyBuffer.clear();
        }
        executorService.shutdownNow();
    }

    //runnable to fetch stories
    private static class FetchNextBatch<T> extends AsyncTask<Callable, Void, Optional<Exploration>> {

        @Override
        protected final Optional<Exploration> doInBackground(Callable... params) {

            Log.i("Ayush", "Fetching next batch of stories");

            if (params == null || params.length == 0)
                return Optional.absent();

            //cast to exact type
            final Callable<Collection<T>> exactType = params[0];
            final Collection<T> stories = MiscUtils.autoRetry(exactType::call, Optional.absent()).or(Collections.emptyList());

            if (stories == null || stories.isEmpty())
                return Optional.absent();

            final ExploreBuffer<T> buffer;
            //noinspection unchecked
            if (bufferWeakReference == null || (buffer = bufferWeakReference.get()) == null)
                return Optional.absent(); //buffer destroyed

            //handle done for today response
            for (T item : stories)
                if (buffer.exploration.isDoneForDay(item)) {

                    isDoneForToday.set(true);
                    Log.i("Ayush", "RECEIVED DONE FOR TODAY !");
                    buffer.executorService.shutdown(); //shut down this shit
                    break;
                }

            //we need to overwrite any prior indication so we remove it
            final int currentSize = buffer.storyBuffer.size();
            final T lastItem = buffer.storyBuffer.get(currentSize - 1);
            final int insertFrom;

            if (buffer.exploration.isDoneForDay(lastItem) || buffer.exploration.isLoading(lastItem)) {

                if (isDoneForToday.get()) {
                    insertFrom = currentSize - 1;
                    synchronized (buffer.storyBuffer) {
                        buffer.storyBuffer.remove(currentSize - 1);
                    }
                } else
                    insertFrom = currentSize - 1;
                Log.i("Ayush", "Removing loading response");
            } else
                insertFrom = currentSize;

            synchronized (buffer.storyBuffer) {
                buffer.storyBuffer.addAll(insertFrom, stories); //make the insertion
            }

            return Optional.of(buffer.exploration);
        }

        @Override
        protected final void onPostExecute(Optional<Exploration> optional) {

            super.onPostExecute(optional);
            if (optional.isPresent())
                optional.get().notifyDataAvailable();
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
