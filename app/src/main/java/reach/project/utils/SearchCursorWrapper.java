package reach.project.utils;

import android.database.Cursor;
import android.database.CursorWrapper;

/**
 * Created by gauravsobti on 29/03/16.
 */
public class SearchCursorWrapper extends CursorWrapper {

    private String filter;
    private int column;
    private int[] index;
    private int count = 0;
    private int pos = 0;

    public SearchCursorWrapper(Cursor cursor, String filter, int column) {
        super(cursor);
        this.filter = filter.toLowerCase();
        this.column = column;
        if (this.filter != "") {
            this.count = super.getCount();
            this.index = new int[this.count];
            for (int i = 0; i < this.count; i++) {
                super.moveToPosition(i);
                //TODO: Delete this if there is an anomaly
                this.pos = i;
                if (this.getString(this.column).toLowerCase().contains(this.filter))
                    this.index[this.pos++] = i;
            }
            this.count = this.pos;
            this.pos = 0;
            super.moveToFirst();
        } else {
            this.count = super.getCount();
            this.index = new int[this.count];
            for (int i = 0; i < this.count; i++) {
                this.index[i] = i;
            }
        }
    }

    @Override
    public boolean move(int offset) {
        return this.moveToPosition(this.pos + offset);
    }

    @Override
    public boolean moveToNext() {
        return this.moveToPosition(this.pos + 1);
    }

    @Override
    public boolean moveToPrevious() {
        return this.moveToPosition(this.pos - 1);
    }

    @Override
    public boolean moveToFirst() {
        return this.moveToPosition(0);
    }

    @Override
    public boolean moveToLast() {
        return this.moveToPosition(this.count - 1);
    }

    @Override
    public int getCount() {
        return this.count;
    }

    @Override
    public int getPosition() {
        return this.pos;
    }

    @Override
    public boolean moveToPosition(int position) {
        if (position >= this.count || position < 0)
            return false;
        return super.moveToPosition(this.index[position]);
    }
}
