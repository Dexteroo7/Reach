package reach.project.coreViews.push;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import reach.project.R;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.ListHolder;

/**
 * Created by dexter on 04/12/15.
 */
public class ParentAdapter extends RecyclerView.Adapter<ListHolder> {

    private final RecyclerView.Adapter apps, music;

    public ParentAdapter(RecyclerView.Adapter apps, RecyclerView.Adapter music) {
        this.apps = apps;
        this.music = music;
    }

    public ListHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case 0: return new ListHolder(parent, R.layout.list_with_more_button_header);
            case 1: return new ListHolder(parent);
            default: return new ListHolder(parent);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(ListHolder holder, int position) {

        switch (position) {

            case 0: {

                holder.headerText.setText("Apps");
                holder.listOfItems.setLayoutManager(new CustomLinearLayoutManager(
                        holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
                holder.listOfItems.setAdapter(apps);
                break;
            }

            case 1: {

                holder.itemView.setBackgroundResource(R.drawable.border_shadow1);
                holder.headerText.setText("Songs");
                holder.listOfItems.setLayoutManager(new CustomLinearLayoutManager(
                        holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
                holder.listOfItems.setAdapter(music);
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
