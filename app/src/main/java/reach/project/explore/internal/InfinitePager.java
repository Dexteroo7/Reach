package reach.project.explore.internal;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Created by dexter on 16/10/15.
 */
public class InfinitePager<T> extends ViewPager {

    private static final String TAG = "InfiniteViewPager";

    private int mCurrPosition = Constants.PAGE_POSITION_CENTER;
    private OnInfinitePageChangeListener<T> mListener;

    public InfinitePager(Context context) {
        this(context, null);
    }

    public InfinitePager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public Parcelable onSaveInstanceState() {

        final InfinitePagerAdapter<T> adapter = getAdapter();
        if (adapter == null) {
            Log.d(Constants.LOG_TAG, " onSaveInstanceState adapter == null");
            return super.onSaveInstanceState();
        }

        final Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.SUPER_STATE, super.onSaveInstanceState());
        bundle.putString(Constants.ADAPTER_STATE, adapter.getStringRepresentation(adapter.getCurrentIndicator()));

        return bundle;
    }

    @Override
    public void onRestoreInstanceState(final Parcelable state) {
        
        final InfinitePagerAdapter<T> adapter = getAdapter();
        if (adapter == null) {

            if (Constants.DEBUG) {
                Log.w(Constants.LOG_TAG, "onRestoreInstanceState adapter == null");
            }
            super.onRestoreInstanceState(state);
            return;
        }
        if (state instanceof Bundle) {

            final Bundle bundle = (Bundle) state;
            final String representation = bundle.getString(Constants.ADAPTER_STATE);
            final T c = adapter.convertToIndicator(representation);
            adapter.setCurrentIndicator(c);
            super.onRestoreInstanceState(bundle.getParcelable(Constants.SUPER_STATE));
            return;
        }
        super.onRestoreInstanceState(state);
    }

//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent event) {
//        // Never allow swiping to switch between pages
//        return mListener.canSwipe(event) && super.onInterceptTouchEvent(event);
//    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        // Never allow swiping to switch between pages
//        return mListener.canSwipe(event) && super.onTouchEvent(event);
//    }

    private void initInfiniteViewPager() {

        setCurrentItem(Constants.PAGE_POSITION_CENTER);

        addOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageScrolled(int i, float positionOffset, int positionOffsetPixels) {

                if (mListener != null && getAdapter() != null) {
                    final InfinitePagerAdapter<T> adapter = getAdapter();
                    mListener.onPageScrolled(adapter.getCurrentIndicator(), positionOffset, positionOffsetPixels);
                }
            }

            @Override
            public void onPageSelected(int position) {

                mCurrPosition = position;
                if (Constants.DEBUG)
                    Log.d(TAG, "on page " + position);

                if (mListener != null && getAdapter() != null) {
                    final InfinitePagerAdapter<T> adapter = getAdapter();
                    mListener.onPageSelected(adapter.getCurrentIndicator());
                }
            }

            @Override
            public void onPageScrollStateChanged(final int state) {

                final InfinitePagerAdapter<T> adapter = getAdapter();
                if (adapter == null)
                    return;

                if (state == ViewPager.SCROLL_STATE_IDLE) {

                    if (mCurrPosition == Constants.PAGE_POSITION_LEFT) {

                        adapter.movePageContents(Constants.PAGE_POSITION_CENTER, Constants.PAGE_POSITION_RIGHT);
                        adapter.movePageContents(Constants.PAGE_POSITION_LEFT, Constants.PAGE_POSITION_CENTER);
                        adapter.setCurrentIndicator(adapter.getPreviousIndicator());
                        adapter.fillPage(Constants.PAGE_POSITION_LEFT);
                    } else if (mCurrPosition == Constants.PAGE_POSITION_RIGHT) {

                        adapter.movePageContents(Constants.PAGE_POSITION_CENTER, Constants.PAGE_POSITION_LEFT);
                        adapter.movePageContents(Constants.PAGE_POSITION_RIGHT, Constants.PAGE_POSITION_CENTER);
                        adapter.setCurrentIndicator(adapter.getNextIndicator());
                        adapter.fillPage(Constants.PAGE_POSITION_RIGHT);
                    }
                    setCurrentItem(Constants.PAGE_POSITION_CENTER, false);
                }

                if (mListener != null)
                    mListener.onPageScrollStateChanged(state);
            }
        });
    }

    @Override
    public final void setCurrentItem(final int item) {

        if (item != Constants.PAGE_POSITION_CENTER)
            throw new RuntimeException("Cannot change page index unless its 1.");

        super.setCurrentItem(item);
    }

    /**
     * Set the current {@code indicator}.
     * @param indicator the new indicator to set.
     */
    public final void setCurrentIndicator(final T indicator) {

        final PagerAdapter adapter = getAdapter();
        if (adapter == null)
            return;

        final InfinitePagerAdapter<T> infinitePagerAdapter = getAdapter();
        final Object currentIndicator = infinitePagerAdapter.getCurrentIndicator();
        if (currentIndicator.getClass() != indicator.getClass())
            return;

        infinitePagerAdapter.reset();
        infinitePagerAdapter.setCurrentIndicator(indicator);
        for (int i = 0; i < Constants.PAGE_COUNT; i++)
            infinitePagerAdapter.fillPage(i);
    }

    @Override
    public final void setOffscreenPageLimit(final int limit) {

        if (limit != getOffscreenPageLimit())
            throw new RuntimeException("OffscreenPageLimit cannot be changed.");

        super.setOffscreenPageLimit(limit);
    }

    @Override
    public void setAdapter(final PagerAdapter adapter) {

        if (adapter instanceof InfinitePagerAdapter) {
            super.setAdapter(adapter);
            initInfiniteViewPager();
        } else
            throw new IllegalArgumentException("Adapter should be an instance of InfinitePagerAdapter.");
    }

    @Override
    public InfinitePagerAdapter<T> getAdapter() {

        final PagerAdapter adapter = super.getAdapter();
        if (adapter instanceof InfinitePagerAdapter)
            return (InfinitePagerAdapter<T>) adapter;

        throw new RuntimeException("Adapter must be of type InfinitePagerAdapter");
    }

    public void setOnInfinitePageChangeListener(OnInfinitePageChangeListener<T> listener) {
        mListener = listener;
    }

    /**
     * Callback interface for responding to changing state of the selected indicator.
     */
    public interface OnInfinitePageChangeListener<T> {

        /**
         * This method will be invoked when the current page is scrolled, either as part
         * of a programmatically initiated smooth scroll or a user initiated touch scroll.
         *
         * @param indicator Indicator of the first page currently being displayed.
         * @param positionOffset Value from [0, 1) indicating the offset from the page at position.
         * @param positionOffsetPixels Value in pixels indicating the offset from position.
         */
        void onPageScrolled(T indicator, float positionOffset, int positionOffsetPixels);

        /**
         * This method will be invoked when a new page has been selected.
         * @param indicator the indicator of this page.
         */
        void onPageSelected(T indicator);

        /**
         * Called when the scroll state changes. Useful for discovering when the user
         * begins dragging, when the pager is automatically settling to the current page,
         * or when it is fully stopped/idle.
         *
         * @param state The new scroll state.
         * @see ViewPager#SCROLL_STATE_IDLE
         * @see ViewPager#SCROLL_STATE_DRAGGING
         * @see ViewPager#SCROLL_STATE_SETTLING
         */
        void onPageScrollStateChanged(final int state);
    }
}