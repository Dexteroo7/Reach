package reach.project.coreViews.push;

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
        return new ListHolder(parent);
    }

    @Override
    public void onBindViewHolder(ListHolder holder, int position) {

        switch (position) {

            case 0: {

                holder.itemView.setBackgroundResource(R.drawable.border_shadow3);
                holder.headerText.setText("Recently Installed Apps");
                holder.listOfItems.setLayoutManager(new CustomLinearLayoutManager(holder.itemView.getContext()));
                holder.listOfItems.setAdapter(apps);
                break;
            }

            case 1: {

                holder.itemView.setBackgroundResource(R.drawable.border_shadow3);
                holder.headerText.setText("Recent Music");
                holder.listOfItems.setLayoutManager(new CustomLinearLayoutManager(holder.itemView.getContext()));
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
