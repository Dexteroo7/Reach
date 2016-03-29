package reach.project.utils.viewHelpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.utils.MiscUtils;
import reach.project.utils.ReachCursorAdapter;
import reach.project.utils.SharedPrefUtils;

/**
 * Created by dexter on 16/12/15.
 */
public class MoreListHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    public final TextView headerText;
    public final RecyclerView listOfItems;
    private SharedPreferences preferences;


    public MoreListHolder(ViewGroup parent,
                          int itemViewResourceId,
                          int headerTextResourceId,
                          int listOfItemsResourceId,
                          int moreButtonId) {

        super(LayoutInflater.from(parent.getContext()).inflate(itemViewResourceId, parent, false));
        this.headerText = (TextView) itemView.findViewById(headerTextResourceId);
        this.listOfItems = (RecyclerView) itemView.findViewById(listOfItemsResourceId);
        itemView.findViewById(moreButtonId).setOnClickListener(this);
        itemView.setPadding(MiscUtils.dpToPx(10),MiscUtils.dpToPx(32), MiscUtils.dpToPx(10),0 );
        itemView.setBackgroundResource(R.drawable.border_shadow1);
    }

    public MoreListHolder(ViewGroup parent) {

        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_with_more_button, parent, false));
        this.headerText = (TextView) itemView.findViewById(R.id.headerText);
        this.listOfItems = (RecyclerView) itemView.findViewById(R.id.listOfItems);
        this.listOfItems.setNestedScrollingEnabled(false);
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

        if(preferences == null) {
            preferences = SharedPrefUtils.getPreferences(view.getContext());
        }

        ((ReachApplication) view.getContext().getApplicationContext()).getTracker().send(new HitBuilders.EventBuilder()
                .setCategory("More button clicked")
                .setAction("Username = " + SharedPrefUtils.getUserName(preferences))
                .setAction("User id = " + SharedPrefUtils.getServerId(preferences))
                .setValue(1)
                .build());

        final RecyclerView.Adapter adapter = listOfItems.getAdapter();

        if (!(adapter instanceof MoreQualifier))
            throw new IllegalArgumentException("More button qualifier failed");

        CharSequence charSequence = headerText.getText();
        if (!(charSequence instanceof String))
            return;

        final String title = (String) charSequence;
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
                public long getItemId(Object item) {
                    return reference.getItemId(item);
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
                    reference.getResourceId(),
                    reference.getCursor()) {

                @Override
                public SingleItemViewHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
                    return reference.getViewHolder(itemView, handOverMessage);
                }

                @Override
                public void onBindViewHolder(SingleItemViewHolder holder, Cursor item) {
                    reference.onBindViewHolder(holder, item);
                }

                @Override
                public long getItemId(@Nonnull Cursor cursor) {
                    return reference.getItemId(cursor);
                }
            };
        } else
            throw new IllegalArgumentException("More button invalid adapter type");

        final RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setPadding(0, MiscUtils.dpToPx(10), 0, 0);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 2));
        recyclerView.setAdapter(newAdapter);

        (dialog = new AlertDialog.Builder(context)
                .setView(recyclerView)
                .setCancelable(true)
                .setTitle(title).create()).show();

        ((MoreQualifier) adapter).passNewAdapter(new WeakReference<>(newAdapter));
    }
}