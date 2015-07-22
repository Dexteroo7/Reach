package reach.project.utils;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by dexter on 20/07/15.
 */
public abstract class ListLoader implements Runnable, LoaderManager.LoaderCallbacks<Cursor> {

    private final int id;
    private final WeakReference<Activity> activityReference;
    private final AtomicBoolean isRunning = new AtomicBoolean(false),
            loaded = new AtomicBoolean(false);

    public ListLoader(Activity activity, int id) {
        this.activityReference = new WeakReference<>(activity);
        this.id = id;
    }

    public void restartLoader() {

        final Activity activity = activityReference.get();
        if (isDead(activity))
            return;
        activity.getLoaderManager().restartLoader(id, null, this);
    }

    public void startLoader() {

        final Activity activity = activityReference.get();
        if (isDead(activity))
            return;
        activity.getLoaderManager().initLoader(id, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        if (i == id)
            return onCreateLoader();
        else
            return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        if (cursorLoader.getId() != id || cursor == null || cursor.isClosed())
            return;

        final int count = cursor.getCount();
        if (count == 0) {

            //if loading show loader
            if (isRunning.get())
                showLoader.run();
            else //else show empty view
                showEmptyView.run();
            loaded.set(false);
        } else {
            //show the list
            onLoaded(cursor, count);
            loaded.set(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (cursorLoader.getId() == id)
            onLoaderReset();
    }

    @Override
    public void run() {

        //if activity dead don't proceed
        final Activity activity = activityReference.get();
        if (isRunning.get() || isDead(activity))
            return;

        //show loaded view if list not yet loaded
        activity.runOnUiThread(showLoader);

        isRunning.set(true);
        try {
            //load list
            doWork();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //show empty if list not yet loaded
            try {
                Thread.sleep(2000L);
                activity.runOnUiThread(showEmptyView);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                isRunning.set(false);
            }
        }
    }

    private final Runnable showLoader = new Runnable() {
        @Override
        public synchronized void run() {
            if (!loaded.get())
                showLoader();
        }
    };

    private final Runnable showEmptyView = new Runnable() {
        @Override
        public synchronized void run() {
            if (!loaded.get())
                showEmptyView();
        }
    };

    private boolean isDead(Activity activity) {
        return activity == null || activity.isFinishing();
    }

    public abstract void showEmptyView();

    public abstract void showLoader();

    public abstract void onLoaderReset();

    public abstract Loader<Cursor> onCreateLoader();

    public abstract void onLoaded(Cursor data, int count);

    public abstract void doWork();
}
