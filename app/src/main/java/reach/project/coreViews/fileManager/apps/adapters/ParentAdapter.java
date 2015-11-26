package reach.project.coreViews.fileManager.apps.adapters;

import android.content.pm.PackageManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.apps.App;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.ListHolder;

/**
 * Created by dexter on 25/11/15.
 */
public class ParentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener, HandOverMessage<App> {

    private final HandOverMessage<App> handOverApp;

    public ParentAdapter(HandOverMessage<App> handOverApp) {
        this.handOverApp = handOverApp;
    }

    public static final byte VIEW_TYPE_RECENT = 0;
    public static final byte VIEW_TYPE_ALL = 1;

    ///////////All songs cursor
    private final List<App> allAppsList = new ArrayList<>(100);
    private int allAppCount = 0;
    private int latestTotalCount = 0;

    public void updateAllAppCount(List<App> allApps) {

        synchronized (allAppsList) {
            this.allAppsList.clear();
            this.allAppsList.addAll(allApps);
        }
        notifyDataSetChanged();
    }

    public void destroy() {

        allAppsList.clear();
        notifyItemRangeRemoved(0, latestTotalCount);
    }
    ///////////

    ///////////Recent music adapter
    private final RecentAdapter recentAdapter;

    {
        final List<App> defaultList = new ArrayList<>(1);
        defaultList.add(new App.Builder().build());
        recentAdapter = new RecentAdapter(defaultList, this, R.layout.app_list_item);
    }

    public void updateRecentApps(List<App> newRecent) {
        if (newRecent.isEmpty())
            return;
        recentAdapter.updateRecent(newRecent);
    }
    ///////////

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case VIEW_TYPE_ALL: {

                return new AppItemHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.app_list_item, parent, false), position -> {

                    position++; //adjust for recent
                    final Object object= getItem(position);
                    if (object instanceof App)
                        handOverApp.handOverMessage((App) object);
                    else
                        throw new IllegalStateException("Position must correspond with an App");
                });
            }

            case VIEW_TYPE_RECENT: {
                return new ListHolder(parent);
            }

            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final Object friend = getItem(position);
        if (friend instanceof App) {

            final App appExactType = (App) friend;
            final AppItemHolder appItemHolder = (AppItemHolder) holder;
            final PackageManager packageManager = appItemHolder.appName.getContext().getPackageManager();

            appItemHolder.bindPosition(position);
            appItemHolder.appName.setText(appExactType.applicationName);
            try {
                appItemHolder.appIcon.setImageDrawable(packageManager.getApplicationIcon(appExactType.packageName));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            //use
        } else {

            final ListHolder horizontalViewHolder = (ListHolder) holder;
            horizontalViewHolder.headerText.setText("Recently Added");
            horizontalViewHolder.moreButton.setOnClickListener(this);
            horizontalViewHolder.listOfItems.setLayoutManager(
                    new CustomLinearLayoutManager(holder.itemView.getContext(),
                            LinearLayoutManager.HORIZONTAL, false));
            horizontalViewHolder.listOfItems.setAdapter(recentAdapter);
        }
    }

    /**
     * Will either return App object OR flag for recent list
     *
     * @param position position to load
     * @return object
     */
    @Nonnull
    private Object getItem(int position) {

        if (position == 0)
            return false; //recent

        else {

            position--; //account for recent shit

            if (position < allAppCount)
                return allAppsList.get(position);
            else
                throw new IllegalStateException("App list has been invalidated");
        }
    }

    @Override
    public int getItemViewType(int position) {

        final Object item = getItem(position);
        if (item instanceof App)
            return VIEW_TYPE_ALL;
        else
            return VIEW_TYPE_RECENT;
    }

    @Override
    public int getItemCount() {

        allAppCount = allAppsList.size();
        return latestTotalCount = allAppCount + 1; //adjust for recent
    }

    @Override
    public void handOverMessage(@Nonnull App message) {
        handOverApp.handOverMessage(message);
    }

    @Override
    public void onClick(View v) {
        //TODO MORE BUTTON HANDLE
    }
}
