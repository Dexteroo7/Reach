package reach.project.utils.viewHelpers;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import reach.project.R;

/**
 * Created by dexter on 18/11/15.
 */
public final class ListHolder extends RecyclerView.ViewHolder {

    public final TextView headerText;
    public final TextView moreButton;
    public final RecyclerView listOfItems;

    public ListHolder(ViewGroup parent) {

        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_with_more_button, parent, false));
        headerText = (TextView) itemView.findViewById(R.id.headerText);
        moreButton = (TextView) itemView.findViewById(R.id.moreButton);
        listOfItems = (RecyclerView) itemView.findViewById(R.id.listOfItems);
    }
}