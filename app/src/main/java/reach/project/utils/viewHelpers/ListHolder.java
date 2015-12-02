package reach.project.utils.viewHelpers;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import reach.project.R;

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

    @Override
    public void onClick(View view) {

        final String title = (String) headerText.getText();
        final Context context = view.getContext();
        final RecyclerView.Adapter adapter = listOfItems.getAdapter();

        if (adapter instanceof MoreQualifier && adapter instanceof SimpleRecyclerAdapter) {

            final SimpleRecyclerAdapter<Object, SingleItemViewHolder> reference = (SimpleRecyclerAdapter) adapter;
            final SimpleRecyclerAdapter<Object, SingleItemViewHolder> simpleRecyclerAdapter = new SimpleRecyclerAdapter<Object, SingleItemViewHolder>(
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

            final RecyclerView recyclerView = new RecyclerView(context);
            recyclerView.setLayoutManager(new CustomGridLayoutManager(context, 2));
            recyclerView.setAdapter(simpleRecyclerAdapter);

            new AlertDialog.Builder(context)
                    .setView(recyclerView)
                    .setCancelable(true)
                    .setTitle(title).show();

            ((MoreQualifier) adapter).passNewAdapter(new WeakReference<>(simpleRecyclerAdapter));

        }
//        else
//            throw new IllegalArgumentException("More button qualifier failed");
    }
}