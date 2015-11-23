package reach.project.yourProfile;

import com.squareup.wire.Message;

import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 18/11/15.
 */
public interface CacheAdapterInterface<T extends Message, E> extends HandOverMessage<E> {

    int getItemCount();

    long getItemId(T item);

    T getItem(int position);
}