package reach.project.reachProcess.auxiliaryClasses;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;
/**
 * Created by Dexter on 08-06-2015.
 * @param <T> the call-back interface to parent
 */
public abstract class ReachTask<T> implements Runnable {

    //kill switch for parent
    protected final AtomicBoolean kill = new AtomicBoolean(true);
    //call-back interface
    protected final T handlerInterface;

    public ReachTask(T handlerInterface) {
        this.handlerInterface = handlerInterface;
    }
    public void close() {
        kill.set(true);
    }
    @Override
    public void run() {

        //clean up before use
        Log.i("Downloader", "Initialize sanitize");
        sanitize();
        //now execute the work
        Log.i("Downloader", "Perform work");
        performTask();
        //clean up when done
        Log.i("Downloader", "Post sanitize");
        sanitize();
        Log.i("Downloader", "TASK COMPLETED");
    }
    protected abstract void sanitize();
    protected abstract void performTask();
}
