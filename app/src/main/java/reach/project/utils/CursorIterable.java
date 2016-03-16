package reach.project.utils;

import android.database.Cursor;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

/**
 * Created by dexter on 02/03/16.
 */
public class CursorIterable implements Iterable<Cursor>, Closeable {

    private final Cursor mCursor;
    private final int startingPosition;

    private boolean mIsClosed = false;

    public CursorIterable(Cursor mCursor) {
        this.mCursor = mCursor;
        startingPosition = mCursor.getPosition();
    }

    @Override
    public void close() throws IOException {

        if (mIsClosed)
            throw new IllegalStateException("closing when already closed");
        mIsClosed = true;
        mCursor.close();
    }

    @Override
    public Iterator<Cursor> iterator() {

        if (mIsClosed || mCursor == null || mCursor.isClosed())
            throw new IllegalStateException("calling iterator() when the cursor is closed");

        if (mCursor.getPosition() != startingPosition && !mCursor.moveToPosition(startingPosition))
            throw new IllegalStateException("could not move to starting position " + startingPosition);

        return new CursorIterator(mCursor);
    }
}
