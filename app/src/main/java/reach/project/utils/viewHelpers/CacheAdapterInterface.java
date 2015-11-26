package reach.project.utils.viewHelpers;

import com.squareup.wire.Message;

/**
 * Created by dexter on 18/11/15.
 */
public interface CacheAdapterInterface<T extends Message, E> extends HandOverMessage<E> {

    int getItemCount();

    long getItemId(T item);

    T getItem(int position);
}