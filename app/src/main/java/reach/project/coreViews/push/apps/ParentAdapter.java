package reach.project.coreViews.push.apps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.apps.App;
import reach.project.core.ReachActivity;
import reach.project.utils.ThreadLocalRandom;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreListHolder;

/**
 * Created by dexter on 25/11/15.
 */
class ParentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Closeable {

    private final HandOverMessage<App> handOverApp;
    private final RecentAdapter recentAdapter;
    private final PackageManager packageManager;
    private final long recentHolderId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);

    public ParentAdapter(HandOverMessage<App> handOverApp,
                         Context context) {

        this.handOverApp = handOverApp;
        this.recentAdapter = new RecentAdapter(new ArrayList<>(20), handOverApp, R.layout.push_app_grid_item, context.getPackageManager());
        this.packageManager = context.getPackageManager();
        setHasStableIds(true);
    }

    private static final byte VIEW_TYPE_RECENT = 0;
    private static final byte VIEW_TYPE_ALL = 1;

    ///////////Data set ops
    private final List<App> allAppsList = new ArrayList<>(100);
    private int allAppCount = 0;

    private final HandOverMessage<Integer> handOverMessage = new HandOverMessage<Integer>() {
        @Override
        public void handOverMessage(@Nonnull Integer position) {
            position++; //adjust for recent
            final Object object = getItem(position);
            if (object instanceof App)
                handOverApp.handOverMessage((App) object);
            else
                throw new IllegalStateException("Position must correspond with an App");
        }
    };

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

    @Override
    public void close() {

        allAppsList.clear();
        recentAdapter.close();
    }
    ///////////


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case VIEW_TYPE_ALL: {

                return new AppItemHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.push_app_list_item, parent, false), handOverMessage);
            }

            case VIEW_TYPE_RECENT: {

                final MoreListHolder moreListHolder = new MoreListHolder(parent);
                moreListHolder.headerText.setText("Recently Installed");
                if (moreListHolder.listOfItems.getLayoutManager() == null)
                    moreListHolder.listOfItems.setLayoutManager(new CustomGridLayoutManager(moreListHolder.listOfItems.getContext(), 2));
                moreListHolder.listOfItems.setAdapter(recentAdapter);
                return moreListHolder;
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

            appItemHolder.checkBox.setSelected(ReachActivity.SELECTED_APPS.contains(appExactType));
            appItemHolder.appName.setText(appExactType.applicationName);
            try {
                appItemHolder.appIcon.setImageDrawable(packageManager.getApplicationIcon(appExactType.packageName));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            //use
        } else
            holder.itemView.setBackgroundResource(R.drawable.border_shadow1);
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
            return allAppsList.get(position).packageName.hashCode();
        else
            return recentHolderId;
    }

    @Override
    public int getItemCount() {

        allAppCount = allAppsList.size();
        return allAppCount + 1; //adjust for recent
    }
}
