package reach.project.utils;

import android.util.Log;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by dexter on 07/09/15.
 */

/**
 * A basic cache which loads data in background and returns <Status, Data>
 * Status : LOADING, ERROR, FOUND
 * Data : the data requested
 *
 * @param <T> the Data to cache
 * @param <E> the parameter which can be used to uniquely fetch that data (network url for eg)
 */

public class AsyncCache<E, T> {

    //fetches data in background
    private final ExecutorService executor;

    {
        //Test maximum number of threads
        final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
        final int CORE_POOL_SIZE = CPU_COUNT + 1;
        final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
        final int KEEP_ALIVE = 1;
        /**
         * An {@link Executor} that can be used to execute tasks in parallel.
         */
        executor = Executors.unconfigurableExecutorService(new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), (r, executor) -> {
            Log.i("Ayush", "Cache rejected my runnable :(");
        }));
//        executor = Executors.newSingleThreadExecutor(); //for testing
    }

    //caches found data
    final ConcurrentHashMap<E, T> mainCache = new ConcurrentHashMap<>();
    //caches Java futures
    final ConcurrentHashMap<E, Future> futureCache = new ConcurrentHashMap<>();

    //function to load T (data) using E (key)
    final Function<E, Optional<T>> loadData;
    //callback to get key for data
    final KeyCallback<E, T> keyCallback;
    //empty values to cache (prevent useless re-trials)
    final EmptyDataCallback<T> emptyDataCallback;

    public AsyncCache(Function<E, Optional<T>> loadData, KeyCallback<E, T> keyCallback, EmptyDataCallback<T> emptyDataCallback) {
        this.loadData = loadData;
        this.keyCallback = keyCallback;
        this.emptyDataCallback = emptyDataCallback;
    }

    /**
     * The get call. Uses key to fetch the data in background and uses Result Callback to
     * pass that data to caller, if hes alive (using weakReferences)
     *
     * @param key    the key for the data
     * @param doWork result callback
     * @return data bundled with status
     */
    public final DataReturn getData(E key, ResultCallback<T> doWork) {

        final T fromMainCache = mainCache.get(key);

        if (fromMainCache != null) {

            //data found in main cache, ez life
            return new DataReturn(Status.FOUND, fromMainCache);
        } else {

            final Future fromLoader = futureCache.get(key);
            if (fromLoader == null) {

                //data not found and not loading, lets load it
                futureCache.put(key, executor.submit(new DataLoader(key, doWork)));
                return new DataReturn(Status.LOADING, null);
            } else {

                /**
                 * Data not found, but probably loading.
                 * We are confident that "execution exception" will not be thrown
                 */
                if (fromLoader.isCancelled()) {
                    //restart load ?
                    return new DataReturn(Status.ERROR, null);
                } else
                    return new DataReturn(Status.LOADING, null);
            }
        }
    }

    public final void submitDataExplicitly(T data) {

        final E key = keyCallback.getKey(data);
        mainCache.put(key, data);

        //remove any running thread for same data
        final Future future = futureCache.get(key);
        if (future != null)
            future.cancel(true); //the thread will handle removal
    }

    public interface KeyCallback<E, T> {
        E getKey(T data);
    }

    public interface EmptyDataCallback<T> {
        T getEmptyData();
    }

    public interface ResultCallback<T> {
        void result(Optional<T> data);
    }

    public enum Status {
        LOADING,
        ERROR,
        FOUND
    }

    /**
     * Packaging for data retrieval from cache.
     * Will also tell the status.
     * ALWAYS CHECK STATUS !
     */
    public final class DataReturn {

        private final Status status;
        private final T data;

        private DataReturn(Status status, T data) {
            this.status = status;
            this.data = data;
        }

        public Status getStatus() {
            return status;
        }

        public Optional<T> getData() {

            if (status == Status.LOADING || status == Status.ERROR)
                return Optional.absent();
            if (status == Status.FOUND)
                return Optional.of(data);
            else
                throw new IllegalStateException("AsyncCache illegal status exception, status can not be " + status);
        }
    }

    /**
     * Loads data in background.
     * Holds a weakReference to resultCallback to pass result
     */
    private final class DataLoader implements Runnable {

        final E key;
        final WeakReference<ResultCallback<T>> resultReference;

        private DataLoader(E key, ResultCallback<T> result) {
            this.key = key;
            this.resultReference = new WeakReference<>(result);
        }

        @Override
        public void run() {

            final Optional<T> result;
            try {
                result = loadData.apply(key);
            } catch (Exception e) {
                //if anything goes wrong, we MUST remove from future cache
                e.printStackTrace();
                futureCache.remove(key);
                mainCache.putIfAbsent(key, emptyDataCallback.getEmptyData()); //we do not want useless retrials
                return;
            }

            //remove from future cache
            futureCache.remove(key);

            if (result != null && result.isPresent()) {

                //save in main cache
                mainCache.put(key, result.get());
                final ResultCallback<T> resultCallback = resultReference.get();
                if (resultCallback != null)
                    resultCallback.result(result);
                else
                    Log.i("Ayush", "Result reference expired");

            } else
                mainCache.putIfAbsent(key, emptyDataCallback.getEmptyData()); //we do not want useless retrials
        }
    }
}