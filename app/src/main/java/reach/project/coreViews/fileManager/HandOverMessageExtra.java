package reach.project.coreViews.fileManager;

import javax.annotation.Nonnull;

import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by Gaurav Sobti on 09/03/16.
 */
public interface HandOverMessageExtra<T> extends HandOverMessage<Integer> {

    @Override
    void handOverMessage(@Nonnull Integer position);

    T getExtra(@Nonnull Integer position);

    void putExtra(int position, T item);
}