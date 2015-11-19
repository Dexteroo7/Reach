package reach.project.friends.friendsAdapters;

/**
 * View holder dumps its click listener and defers to internal FriendsViewHolder
 * Created by dexter on 18/11/15.
 */

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import reach.project.R;

public final class HorizontalViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    public TextView mTextView2, mMoreBtn2;
    public RecyclerView mRecyclerView2;
    private int pos;

    public HorizontalViewHolder(View itemView) {
        super(itemView);
        mTextView2 = (TextView) itemView.findViewById(R.id.textView2);
        mMoreBtn2 = (TextView) itemView.findViewById(R.id.moreBtn2);
        mRecyclerView2 = (RecyclerView) itemView.findViewById(R.id.recyclerView2);
        mMoreBtn2.setOnClickListener(this);
    }

    public void bindTest(int position) {
        this.pos = position;
    }

    @Override
    public void onClick(View v) {

    }
}