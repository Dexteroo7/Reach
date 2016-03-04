package reach.project.utils.viewHelpers;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by adminReach on 16/02/16.
 */
public class EmptyTextViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {


    public final ImageView mEmptyImageView;

    public EmptyTextViewHolder(ViewGroup parent,
                           int itemViewResourceId,
                           int emptyImageViewResourceId) {

        super(LayoutInflater.from(parent.getContext()).inflate(itemViewResourceId, parent, false));
        this.mEmptyImageView = (ImageView) itemView.findViewById(emptyImageViewResourceId);
        itemView.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        // Provide action on emptyview as required

    }
}
