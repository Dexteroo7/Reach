package reach.project.utils;

import android.content.EntityIterator;
import android.database.Cursor;

import java.io.Closeable;
import java.util.Iterator;

public final class CursorIterator implements Iterator<Cursor>, Closeable {

    private final Cursor mCursor;
    private boolean mIsClosed;

    /**
     * Constructor that makes initializes the cursor such that the iterator points to the
     * first Entity, if there are any.
     *
     * @param cursor the cursor that contains the rows that make up the entities
     */
    public CursorIterator(Cursor cursor) {

        mIsClosed = false;
        mCursor = cursor;
        mCursor.moveToFirst();
    }

    /**
     * Returns whether there are more elements to iterate, i.e. whether the
     * iterator is positioned in front of an element.
     *
     * @return {@code true} if there are more elements, {@code false} otherwise.
     * @see EntityIterator#next()
     */
    public final boolean hasNext() {
        if (mIsClosed) {
            throw new IllegalStateException("calling hasNext() when the iterator is closed");
        }
        return !mCursor.isAfterLast();
    }

    /**
     * Returns the next object in the iteration, i.e. returns the element in
     * front of the iterator and advances the iterator by one position.
     *
     * @return the next object.
     * @throws java.util.NoSuchElementException if there are no more elements.
     * @see EntityIterator#hasNext()
     */
    public Cursor next() {
        if (mIsClosed) {
            throw new IllegalStateException("calling next() when the iterator is closed");
        }
        if (!hasNext()) {
            throw new IllegalStateException("you may only call next() if hasNext() is true");
        }
        return mCursor;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove not supported by EntityIterators");
    }

    public final void reset() {
        if (mIsClosed) {
            throw new IllegalStateException("calling reset() when the iterator is closed");
        }
        mCursor.moveToFirst();
    }

    /**
     * Indicates that this iterator is no longer needed and that any associated resources
     * may be released (such as a SQLite cursor).
     */
    @Override
    public final void close() {
        if (mIsClosed) {
            throw new IllegalStateException("closing when already closed");
        }
        mIsClosed = true;
        mCursor.close();
    }
}