package reach.project.utils.viewHelpers;

import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by dexter on 21/11/15.
 */
public class SingleItemViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener {

    protected final HandOverMessage<Integer> handOverMessage;

    protected int position;

    public SingleItemViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        super(itemView);
        this.handOverMessage = handOverMessage;
        this.itemView.setOnClickListener(this);
    }

    public void bindPosition(int position) {
        this.position = position;
    }

    @Override
    public void onClick(View v) {
        handOverMessage.handOverMessage(position);
    }
}
