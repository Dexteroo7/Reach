package reach.project.coreViews.yourProfile.blobCache;

import com.squareup.wire.Message;

import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 18/11/15.
 */
public interface CacheAdapterInterface<T extends Message, E> extends HandOverMessage<E> {

    int getItemCount();

    T getItem(int position);
}