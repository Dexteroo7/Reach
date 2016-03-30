package reach.project.utils;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.util.Log;

/**
 * Created by gauravsobti on 29/03/16.
 */
public class SearchCursorWrapper extends CursorWrapper {

    private static final String TAG = SearchCursorWrapper.class.getSimpleName();
    private String filter;
    private int column;
    private int[] filterMap;
    private int mPos = -1;
    private int mCount = 0;

    public SearchCursorWrapper(Cursor cursor, String filter, int column) {
        super(cursor);
        this.filter = filter.toLowerCase();
        Log.d(TAG, "filter:"+this.filter);
        this.column = column;
        int count = super.getCount();

        if (this.filter != "") {
            this.filterMap = new int[count];
            int filteredCount = 0;
            for (int i=0;i<count;i++) {
                super.moveToPosition(i);
                if (this.getString(this.column).toLowerCase().contains(this.filter)){
                    this.filterMap[filteredCount] = i;
                    filteredCount++;
                }
            }
            this.mCount = filteredCount;
        } else {
            this.filterMap = new int[count];
            this.mCount = count;
            for (int i=0;i<count;i++) {
                this.filterMap[i] = i;
            }
        }

    }

    public int getCount() { return this.mCount; }

    @Override
    public boolean moveToPosition(int position) {
        Log.d(TAG,"moveToPosition:"+position);
        // Make sure position isn't past the end of the cursor
        final int count = getCount();
        if (position >= count) {
            mPos = count;
            return false;
        }
        // Make sure position isn't before the beginning of the cursor
        if (position < 0) {
            mPos = -1;
            return false;
        }
        final int realPosition = filterMap[position];
        // When moving to an empty position, just pretend we did it
        boolean moved = realPosition == -1 ? true : super.moveToPosition(realPosition);
        if (moved) {
            mPos = position;
        } else {
            mPos = -1;
        }
        Log.d(TAG,"end moveToPosition:"+position);
        return moved;
    }
    @Override
    public final boolean move(int offset) {
        return moveToPosition(mPos + offset);
    }
    @Override
    public final boolean moveToFirst() {
        return moveToPosition(0);
    }
    @Override
    public final boolean moveToLast() {
        return moveToPosition(getCount() - 1);
    }
    @Override
    public final boolean moveToNext() {
        return moveToPosition(mPos + 1);
    }
    @Override
    public final boolean moveToPrevious() {
        return moveToPosition(mPos - 1);
    }
    @Override
    public final boolean isFirst() {
        return mPos == 0 && getCount() != 0;
    }
    @Override
    public final boolean isLast() {
        int cnt = getCount();
        return mPos == (cnt - 1) && cnt != 0;
    }
    @Override
    public final boolean isBeforeFirst() {
        if (getCount() == 0) {
            return true;
        }
        return mPos == -1;
    }
    @Override
    public final boolean isAfterLast() {
        if (getCount() == 0) {
            return true;
        }
        return mPos == getCount();
    }
    @Override
    public int getPosition() {
        return mPos;
    }
}
