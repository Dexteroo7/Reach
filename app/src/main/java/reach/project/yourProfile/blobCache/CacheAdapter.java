package reach.project.yourProfile.blobCache;

import android.widget.BaseAdapter;

/**
 * Created by dexter on 05/11/15.
 */
public abstract class CacheAdapter<T> extends BaseAdapter {

    private final CacheAdapterInterface<T> cacheAdapterInterface;

    protected CacheAdapter(CacheAdapterInterface<T> cacheAdapterInterface) {
        this.cacheAdapterInterface = cacheAdapterInterface;
    }

    @Override
    public int getCount() {
        return cacheAdapterInterface.getCount();
    }

    @Override
    public T getItem(int position) {
        return cacheAdapterInterface.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return cacheAdapterInterface.getItemId(cacheAdapterInterface.getItem(position));
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public interface CacheAdapterInterface<T> {

        int getCount();
        long getItemId(T item);
        T getItem(int position);
    }
}
