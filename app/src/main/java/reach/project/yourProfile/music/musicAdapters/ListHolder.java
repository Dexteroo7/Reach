package reach.project.yourProfile.music.musicAdapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import reach.project.R;

/**
 * Created by dexter on 18/11/15.
 */
class ListHolder extends RecyclerView.ViewHolder {

    public final TextView headerText;
    public final TextView moreButton;
    public final RecyclerView listOfItems;

    protected ListHolder(View itemView) {

        super(itemView);
        headerText = (TextView) itemView.findViewById(R.id.headerText);
        moreButton = (TextView) itemView.findViewById(R.id.moreButton);
        listOfItems = (RecyclerView) itemView.findViewById(R.id.listOfItems);
    }
}