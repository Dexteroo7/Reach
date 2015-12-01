package reach.project.utils.viewHelpers.moreButton;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Created by dexter on 30/11/15.
 */
public abstract class MoreDialog<T, E extends SingleItemViewHolder> {

    private final List<T> items = new ArrayList<>();
    private final HandOverMessage<T> handOverMessage;
    private final int resourceId;

    public abstract E mGetViewHolder(View itemView, HandOverMessage<Integer> handOverMessage);

    public abstract void mOnBindViewHolder(E holder, T item);

    public MoreDialog(HandOverMessage<T> handOverMessage, int resourceId) {
        this.handOverMessage = handOverMessage;
        this.resourceId = resourceId;
    }

    public void updateItems(Iterator<T> newItems) {

        items.clear();
        while (newItems.hasNext())
            items.add(newItems.next());
    }

    public AlertDialog show(Context context, String title) {

        final SimpleRecyclerAdapter<T, E> simpleRecyclerAdapter = new SimpleRecyclerAdapter<T, E>(items, handOverMessage, resourceId) {
            @Override
            public E getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
                return mGetViewHolder(itemView, handOverMessage);
            }

            @Override
            public void onBindViewHolder(E holder, T item) {
                mOnBindViewHolder(holder, item);
            }
        };

        final RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new CustomLinearLayoutManager(context));
        recyclerView.setAdapter(simpleRecyclerAdapter);

        return new AlertDialog.Builder(context)
                .setView(recyclerView)
                .setCancelable(true)
                .setTitle(title).create();
    }
}
