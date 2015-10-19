package reach.project.explore.internal;

/**
 * Created by dexter on 16/10/15.
 */

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal model of a page
 *
 * @param <T> the datatype of the indicator.
 */
public final class PageModel<T> {

    private T mIndicator;
    private final ViewGroup mParentView;
    private final List<View> mChildren;

    public PageModel(final ViewGroup parent, final T indicator) {

        mParentView = parent;
        mIndicator = indicator;
        final int size = parent.getChildCount();
        mChildren = new ArrayList<>(size);

        for (int i = 0; i < size; i++)
            mChildren.add(parent.getChildAt(i));
    }

    /**
     * @return {@code true} if the model has child views.
     */
    public boolean hasChildren() {
        return mChildren != null && mChildren.size() != 0;
    }


    private void emptyChildren() {
        if (hasChildren()) {
            mChildren.clear();
        }
    }

    public List<View> getChildren() {
        return mChildren;
    }

    public void removeAllChildren() {
        mParentView.removeAllViews();
        emptyChildren();
    }

    public void addChild(final View child) {
        addViewToParent(child);
        mChildren.add(child);
    }

    public void removeViewFromParent(final View view) {
        mParentView.removeView(view);
    }

    public void addViewToParent(final View view) {
        mParentView.addView(view);
    }

    public ViewGroup getParentView() {
        return mParentView;
    }

    public T getIndicator() {
        return mIndicator;
    }

    public void setIndicator(final T indicator) {
        mIndicator = indicator;
    }
}
