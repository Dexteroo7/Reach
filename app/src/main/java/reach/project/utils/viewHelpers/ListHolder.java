package reach.project.utils.viewHelpers;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.utils.ReachCursorAdapter;

/**
 * Created by dexter on 18/11/15.
 */
public final class ListHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    public final TextView headerText;
    public final RecyclerView listOfItems;

    public ListHolder(ViewGroup parent) {

        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_with_more_button, parent, false));
        headerText = (TextView) itemView.findViewById(R.id.headerText);
        listOfItems = (RecyclerView) itemView.findViewById(R.id.listOfItems);
        itemView.findViewById(R.id.moreButton).setOnClickListener(this);
    }

    @Nullable
    private AlertDialog dialog = null;

    @Override
    public void onClick(View view) {

        if (dialog != null) {
            dialog.show();
            return;
        }

        final RecyclerView.Adapter adapter = listOfItems.getAdapter();

        if (!(adapter instanceof MoreQualifier))
            throw new IllegalArgumentException("More button qualifier failed");

        final String title = (String) headerText.getText();
        final Context context = view.getContext();
        final RecyclerView.Adapter newAdapter;

        if (adapter instanceof SimpleRecyclerAdapter) {

            final SimpleRecyclerAdapter<Object, SingleItemViewHolder> reference = (SimpleRecyclerAdapter) adapter;
            newAdapter = new SimpleRecyclerAdapter<Object, SingleItemViewHolder>(
                    reference.getMessageList(),
                    reference.getHandOverMessage(),
                    reference.getResourceId()) {

                @Override
                public SingleItemViewHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
                    return reference.getViewHolder(itemView, handOverMessage);
                }

                @Override
                public void onBindViewHolder(SingleItemViewHolder holder, Object item) {
                    reference.onBindViewHolder(holder, item);
                }
            };

        } else if (adapter instanceof ReachCursorAdapter) {

            final ReachCursorAdapter<SingleItemViewHolder> reference = (ReachCursorAdapter) adapter;
            newAdapter = new ReachCursorAdapter<SingleItemViewHolder>(
                    reference.getHandOverMessage(),
                    reference.getResourceId()) {

                @Override
                public SingleItemViewHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
                    return reference.getViewHolder(itemView, handOverMessage);
                }

                @Override
                public void onBindViewHolder(SingleItemViewHolder holder, Cursor item) {
                    reference.onBindViewHolder(holder, item);
                }

                @Override
                public int getItemId(@Nonnull Cursor cursor) {
                    return reference.getItemId(cursor);
                }
            };
        } else
            throw new IllegalArgumentException("More button invalid adapter type");

        final RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new CustomLinearLayoutManager(context));
        recyclerView.setAdapter(newAdapter);

        (dialog = new AlertDialog.Builder(context)
                .setView(recyclerView)
                .setCancelable(true)
                .setTitle(title).create()).show();

        ((MoreQualifier) adapter).passNewAdapter(new WeakReference<>(newAdapter));
    }
}