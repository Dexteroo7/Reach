package reach.project.utils.viewHelpers;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.wire.Message;

import java.util.List;

/**
 * Created by dexter on 21/11/15.
 */
public abstract class MoreButtonAdapter<T extends Message, F extends SingleItemViewHolder> extends RecyclerView.Adapter<F> implements HandOverMessage<Integer> {

    private final List<T> messageList;
    private final HandOverMessage<T> handOverMessage;
    private final int resourceId;

    public MoreButtonAdapter(List<T> messageList, HandOverMessage<T> handOverMessage, int resourceId) {
        this.messageList = messageList;
        this.handOverMessage = handOverMessage;
        this.resourceId = resourceId;
        setHasStableIds(true);
    }

    @Override
    public F onCreateViewHolder(ViewGroup parent, int viewType) {
        return getViewHolder(LayoutInflater.from(parent.getContext()).inflate(resourceId, parent, false), this);
    }

    @Override
    public long getItemId(int position) {
        return messageList.get(position).hashCode();
    }

    public abstract F getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage);

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public void handOverMessage(@NonNull Integer position) {
        handOverMessage.handOverMessage(messageList.get(position));
    }

    public T getItem(int position) {
        return messageList.get(position);
    }
}