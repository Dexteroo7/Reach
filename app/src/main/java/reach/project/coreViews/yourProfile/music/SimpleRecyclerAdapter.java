package reach.project.coreViews.yourProfile.music;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import reach.project.music.Song;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Created by dexter on 21/11/15.
 */
public abstract class SimpleRecyclerAdapter<T, F extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<F> implements HandOverMessage<Pair<Integer,Integer>> {

    private final List<T> messageList;
    private final HandOverMessage<T> handOverMessage;
    private final int resourceId;
    private final ParentAdapter.HandOverPerformActionTask performActionTask;

    public SimpleRecyclerAdapter(List<T> messageList, HandOverMessage<T> handOverMessage, int resourceId, ParentAdapter.HandOverPerformActionTask performActionTask) {
        this.messageList = messageList;
        this.handOverMessage = handOverMessage;
        this.resourceId = resourceId;
        this.performActionTask = performActionTask;
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

    public abstract F getViewHolder(View itemView, HandOverMessage<Pair<Integer,Integer>> handOverMessage);
    public abstract long getItemId(T item);

    public abstract void onBindViewHolder(F holder, T item);

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public void handOverMessage(@NonNull Pair<Integer,Integer> positionWithAction ) {
        if(positionWithAction.second == 2){
            performActionTask.performAction(1,(Song) messageList.get(positionWithAction.first));
        }
        else {
            handOverMessage.handOverMessage(messageList.get(positionWithAction.first));
        }
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

    public ParentAdapter.HandOverPerformActionTask getPerformActionTask() {
        return performActionTask;
    }

    public int getResourceId() {
        return resourceId;
    }
}