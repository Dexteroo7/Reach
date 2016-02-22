package reach.project.coreViews.myProfile;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by adminReach on 15/02/16.
 */
public class EmptyRecyclerView extends RecyclerView {
    private View emptyView;
    public EmptyRecyclerView(Context context) {
        super(context);
    }

    public EmptyRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EmptyRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    public void checkIfEmpty(int count ) {

        if (emptyView != null && getAdapter() != null) {
            final boolean emptyViewVisible = count == 0;
            Log.d("EmptyRecyclerView","emptyViewVisible = "+ emptyViewVisible);
            emptyView.setVisibility(emptyViewVisible ? VISIBLE : GONE);
            setVisibility(emptyViewVisible ? GONE : VISIBLE);
        }
    }

    @Override
    public void setAdapter(Adapter adapter) {

        super.setAdapter(adapter);


        //checkIfEmpty();
    }


    public void setEmptyView(View emptyView) {
        this.emptyView = emptyView;
        //checkIfEmpty();
    }
}