package reach.project.coreViews.fileManager.apps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.apps.App;
import reach.project.utils.ThreadLocalRandom;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreListHolder;

/**
 * Created by dexter on 25/11/15.
 */
public class ParentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements HandOverMessage<Integer> {

    private static final byte VIEW_TYPE_RECENT = 0;
    private static final byte VIEW_TYPE_ALL = 1;

    private final long recentHolderId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
    private final List<App> allAppsList = new ArrayList<>(100);

    private final View.OnClickListener extraClick;
    private final HandOverMessage<App> handOverApp;
    private final PackageManager packageManager;
    private final RecentAdapter recentAdapter;

    public ParentAdapter(HandOverMessage<App> handOverApp,
                         Context context,
                         View.OnClickListener extraClick) {

        this.packageManager = context.getPackageManager();
        this.extraClick = extraClick;
        this.handOverApp = handOverApp;

        this.recentAdapter = new RecentAdapter(
                new ArrayList<>(20),
                handOverApp,
                R.layout.app_grid_item,
                packageManager,
                extraClick);

        setHasStableIds(true);
    }

    public void updateAllApps(List<App> allApps) {

        synchronized (allAppsList) {

            this.allAppsList.clear();
            this.allAppsList.addAll(allApps);
        }
        notifyDataSetChanged();
    }

    public void updateRecentApps(List<App> newRecent) {
//        if (newRecent.isEmpty())
//            return;
        recentAdapter.updateRecent(newRecent);
    }

    public void visibilityChanged(String packageName) {

        int position;
        for (position = 0; position < allAppsList.size(); position++)
            if (allAppsList.get(position).packageName.equals(packageName))
                break;

        position++;//adjust for recent
        notifyItemChanged(position);
        recentAdapter.visibilityChanged(packageName);
    }

    //////////////////////////////

    @Override
    public void handOverMessage(@Nonnull Integer position) {

        final Object object = getItem(position);
        if (object instanceof App)
            ParentAdapter.this.handOverApp.handOverMessage((App) object);
        else
            throw new IllegalStateException("Position must correspond with an App");
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case VIEW_TYPE_ALL: {

                final View itemView =
                        LayoutInflater.from(parent.getContext()).inflate(R.layout.app_list_item, parent, false);
                return new AppItemHolder(
                        itemView,
                        this::handOverMessage,
                        extraClick);
            }

            case VIEW_TYPE_RECENT: {

                final MoreListHolder moreListHolder = new MoreListHolder(parent);
                moreListHolder.headerText.setText("Recently Installed");
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

            appItemHolder.packageName.set(appExactType.packageName); //set the packageName
            appItemHolder.appName.setText(appExactType.applicationName);
            try {
                appItemHolder.appIcon.setImageDrawable(packageManager.getApplicationIcon(appExactType.packageName));
            } catch (PackageManager.NameNotFoundException ignored) {
                appItemHolder.appIcon.setImageDrawable(null);
            }

            if (ApplicationFragment.isVisible(appExactType.packageName)) {
                appItemHolder.toggleImage.setImageResource(R.drawable.icon_everyone);
                //appItemHolder.toggleText.setText("Everyone");
            } else {
                appItemHolder.toggleImage.setImageResource(R.drawable.icon_locked);
                //appItemHolder.toggleText.setText("Only Me");
            }

        } else {

            holder.itemView.setBackgroundResource(R.drawable.border_shadow1);
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

            if (position < allAppsList.size())
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

        return allAppsList.size() + 1; //adjust for recent
    }
}