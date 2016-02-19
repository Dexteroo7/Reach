package reach.project.player;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 21/11/15.
 */
public class SingleItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final HandOverMessage<Integer> handOverMessage;

    public SingleItemViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        super(itemView);
        this.handOverMessage = handOverMessage;
        this.itemView.setOnClickListener(this);
    }

    @Override
    public final void onClick(View v) {

        itemView.setSelected(true);
        handOverMessage.handOverMessage(getAdapterPosition());
    }
}
