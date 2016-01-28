package reach.project.coreViews.friends;

import javax.annotation.Nonnull;

import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 19/01/16.
 */
public interface HandOverMessageExtra<T> extends HandOverMessage<Integer> {

    @Override
    void handOverMessage(@Nonnull Integer position);

    T getExtra(@Nonnull Integer position);
}
