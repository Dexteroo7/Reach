package reach.project.coreViews.friends;

import android.database.Cursor;

import javax.annotation.Nonnull;

import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 19/01/16.
 */
public interface HandOverWithContext extends HandOverMessage<Integer> {

    @Override
    void handOverMessage(@Nonnull Integer position);

    Cursor getCursor(@Nonnull Integer position);
}
