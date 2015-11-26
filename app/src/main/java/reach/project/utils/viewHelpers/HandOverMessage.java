package reach.project.utils.viewHelpers;

import javax.annotation.Nonnull;

/**
 * Created by dexter on 21/11/15.
 */
public interface HandOverMessage<E> {

    void handOverMessage(@Nonnull E message);
}
