package reach.project.yourprofile.blobCache;

import com.squareup.wire.Message;

import java.io.File;
import java.util.Collection;

/**
 * Created by dexter on 07/11/15.
 */
public interface CacheInjectorCallbacks<T extends Message> {

    //get cacheDirectory
    File getCacheDirectory();

    //get ItemId for item
    long getItemId(T item);

    /**
     * hook for returning cache loading result, THIS IS NOT UI THREAD
     *
     * @param elements  the elements to inject
     * @param overWrite mark true if overwrite is needed
     */
    void injectElements(Collection<T> elements, boolean overWrite, boolean removeLoading);
}
