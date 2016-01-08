package reach.project.utils.viewHelpers;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;


abstract public class ViewStatePagerAdapter extends PagerAdapter {
    // Holds how many views are saved
    private static final String TAG = "ViewStatePagerAdapter";
    private static final boolean DEBUG = true;

    private static final String STATE_LENGTH = "stateLength";

    private final LayoutInflater inflater;

    private ArrayList<SparseArray<Parcelable>> savedState = new ArrayList<>();
    private SparseArray<View> views = new SparseArray<>();

    private int state = -1; // start with not possible position

    protected ViewStatePagerAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    @Override public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);

        // Only refresh when primary changes
        if (state != position) {
            state = position;
            onViewFocused((View) object, position);
        }
    }

    /**
     * Called when view becomes the primary view.
     * Note: Override to handle when view is showed.
     *
     * @param view     View showed
     * @param position int position of view
     */
    protected void onViewFocused(View view, int position) {

    }


    @Override final public Object instantiateItem(ViewGroup container, int position) {
        View view = newView(inflater, container, position);

        // Keep track of view
        views.put(position, view);

        if (position < savedState.size()) {
            SparseArray<Parcelable> state = savedState.get(position);
            if (state != null) {
                view.restoreHierarchyState(state);
            }
        }

        container.addView(view, 0);

        return view;
    }


    @Override public void destroyItem(ViewGroup container, int position, Object object) {
        final View view = (View) object;

        // Fill the array
        while (savedState.size() <= position) {
            savedState.add(null);
        }

        // Save state
        SparseArray<Parcelable> viewState = new SparseArray<>();
        view.saveHierarchyState(viewState);
        savedState.set(position, viewState);

        views.put(position, null);

        container.removeView(view);
    }

    abstract protected View newView(LayoutInflater inflater, ViewGroup container, int position);

    @Override public final boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override public Parcelable saveState() {
        Bundle state = null;

        final int stateCount = savedState.size();

        if (DEBUG) Log.d(TAG, "saveState() view sparse array count -> " + stateCount);
        if (stateCount > 0) {
            state = new Bundle();

            SparseArray<Parcelable>[] sparseArrays = new SparseArray[stateCount];
            savedState.toArray(sparseArrays);


            for (int i = 0; i < sparseArrays.length; i++) {
                final SparseArray<Parcelable> array = sparseArrays[i];
                state.putSparseParcelableArray(String.valueOf(i), array);

                if (DEBUG) Log.d(TAG, "saveState() -> " + (array == null ? 0 : array.size()));

            }

            state.putInt(STATE_LENGTH, stateCount);
        }

        for (int i = 0; i < views.size(); i++) {
            if (state == null) {
                state = new Bundle();
            }

            int key = views.keyAt(i);
            View view = views.valueAt(key);
            if (view != null) {
                SparseArray<Parcelable> viewState = new SparseArray<>();
                view.saveHierarchyState(viewState);
                state.putSparseParcelableArray(String.valueOf(key), viewState);
                if (stateCount <= key) {
                    state.putInt(STATE_LENGTH, key);
                }
            }
        }

        return state;
    }

    @Override public void restoreState(Parcelable state, ClassLoader loader) {
        super.restoreState(state, loader);

        if (DEBUG) Log.d(TAG, "restoreState()");

        if (state != null) {
            Bundle bundle = (Bundle) state;
            int length = bundle.getInt(STATE_LENGTH);
            if (DEBUG) Log.d(TAG, "restoreState() length -> " + length);

            for (int i = 0; i < length; i++) {
                SparseArray<Parcelable> viewState = bundle.getSparseParcelableArray(String.valueOf(i));
                savedState.add(viewState);
            }
        }
    }
}