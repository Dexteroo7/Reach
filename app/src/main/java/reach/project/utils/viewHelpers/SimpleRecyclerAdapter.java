package reach.project.utils.viewHelpers;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by dexter on 21/11/15.
 */
public abstract class SimpleRecyclerAdapter<T, F extends SingleItemViewHolder> extends RecyclerView.Adapter<F> implements HandOverMessage<Integer> {

    private final List<T> messageList;
    private final HandOverMessage<T> handOverMessage;
    private final int resourceId;

    public SimpleRecyclerAdapter(List<T> messageList, HandOverMessage<T> handOverMessage, int resourceId) {
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
    public void onBindViewHolder(F holder, int position) {
        onBindViewHolder(holder, messageList.get(position));
    }

    @Override
    public long getItemId(int position) {
        return getItemId(messageList.get(position));
    }

    public abstract F getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage);
    public abstract long getItemId(T item);

    public abstract void onBindViewHolder(F holder, T item);

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

    public List<T> getMessageList() {
        return messageList;
    }

    public HandOverMessage<T> getHandOverMessage() {
        return handOverMessage;
    }

    public int getResourceId() {
        return resourceId;
    }
}