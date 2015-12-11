package reach.project.utils.viewHelpers;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by dexter on 10/12/15.
 */
public abstract class RecyclerViewMaterialAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T> {

    //the constants value of the header view
    private final int TYPE_PLACEHOLDER = Integer.MIN_VALUE;

    //the size taken by the header
    private final int mPlaceholderSize = 1;

    @Override
    public final int getItemViewType(int position) {

        if (position < mPlaceholderSize)
            return TYPE_PLACEHOLDER;
        else
            return newGetItemViewType(position - mPlaceholderSize); //call getItemViewType on the adapter, less mPlaceholderSize
    }

    //dispatch getItemCount to the actual adapter, add mPlaceholderSize
    @Override
    public final int getItemCount() {
        return newGetItemCount() + mPlaceholderSize;
    }

    //add the header on first position, else display the true adapter's cells
    @Override
    public final T onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case TYPE_PLACEHOLDER:
                return inflatePlaceHolder(LayoutInflater.from(parent.getContext())
                        .inflate(com.github.florent37.materialviewpager.R.layout.material_view_pager_placeholder, parent, false));
            default:
                return newCreateViewHolder(parent, viewType);
        }
    }

    //dispatch onBindViewHolder on the actual mAdapter
    @Override
    public final void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        switch (getItemViewType(position)) {
            case TYPE_PLACEHOLDER:
                break;
            default:
                newBindViewHolder(holder, position - mPlaceholderSize);
                break;
        }
    }

    @Override
    public final long getItemId(int position) {

        if (position < mPlaceholderSize)
            return super.getItemId(position);

        return newGetItemId(position - mPlaceholderSize);
    }

    protected abstract void newBindViewHolder(RecyclerView.ViewHolder holder, int position);

    protected abstract T newCreateViewHolder(ViewGroup parent, int viewType);

    protected abstract int newGetItemCount();

    protected abstract int newGetItemViewType(int position);

    protected abstract long newGetItemId(int position);

    protected abstract T inflatePlaceHolder(View view);
}