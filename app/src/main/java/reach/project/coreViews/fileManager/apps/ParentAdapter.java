package reach.project.coreViews.fileManager.apps;

import android.content.pm.PackageManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.apps.App;
import reach.project.utils.viewHelpers.MoreListHolder;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 25/11/15.
 */
class ParentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final HandOverMessage<App> handOverApp;
    private final RecentAdapter recentAdapter;
    private final long recentHolderId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);

    private final HandOverMessage<Integer> handOverMessage = new HandOverMessage<Integer>() {
        @Override
        public void handOverMessage(@Nonnull Integer position) {

            final Object object = getItem(position);
            if (object instanceof App)
                handOverApp.handOverMessage((App) object);
            else
                throw new IllegalStateException("Position must correspond with an App");
        }
    };

    private final PackageManager packageManager;

    public ParentAdapter(HandOverMessage<App> handOverApp, PackageManager packageManager) {

        this.packageManager = packageManager;
        this.handOverApp = handOverApp;
        this.recentAdapter = new RecentAdapter(new ArrayList<>(20), handOverApp, packageManager, R.layout.app_grid_item);
        setHasStableIds(true);
    }

    private static final byte VIEW_TYPE_RECENT = 0;
    private static final byte VIEW_TYPE_ALL = 1;

    ///////////Data set ops
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

    public void updateRecentApps(List<App> newRecent) {
        if (newRecent.isEmpty())
            return;
        recentAdapter.updateRecent(newRecent);
    }

    public void destroy() {

        allAppsList.clear();
        notifyItemRangeRemoved(0, latestTotalCount);
    }
    ///////////


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Log.i("Ayush", "Creating ViewHolder " + getClass().getName());

        switch (viewType) {

            case VIEW_TYPE_ALL: {

                return new AppItemHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.app_list_item, parent, false), handOverMessage);
            }

            case VIEW_TYPE_RECENT: {
                return new MoreListHolder(parent);
            }

            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        Log.i("Ayush", "Binding ViewHolder " + getClass().getName());

        final Object friend = getItem(position);
        if (friend instanceof App) {

            final App appExactType = (App) friend;
            final AppItemHolder appItemHolder = (AppItemHolder) holder;

            appItemHolder.bindPosition(position);
            appItemHolder.appName.setText(appExactType.applicationName);
            try {
                appItemHolder.appIcon.setImageDrawable(packageManager.getApplicationIcon(appExactType.packageName));
            } catch (PackageManager.NameNotFoundException ignored) {
                appItemHolder.appIcon.setImageDrawable(null);
            }

            //use
        } else {

            final MoreListHolder horizontalViewHolder = (MoreListHolder) holder;
            holder.itemView.setBackgroundResource(R.drawable.border_shadow1);
            horizontalViewHolder.headerText.setText("Recently Installed");
            horizontalViewHolder.listOfItems.setLayoutManager(new CustomGridLayoutManager(holder.itemView.getContext(), 2));
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
    public long getItemId(int position) {

        final Object item = getItem(position);
        if (item instanceof App)
            return ((App) item).packageName.hashCode();

        return recentHolderId;
    }

    @Override
    public int getItemCount() {

        allAppCount = allAppsList.size();
        return latestTotalCount = allAppCount + 1; //adjust for recent
    }
}
