package reach.project.coreViews.fileManager.apps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.apps.App;
import reach.project.coreViews.fileManager.HandOverMessageExtra;
import reach.project.utils.MiscUtils;
import reach.project.utils.ThreadLocalRandom;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreListHolder;

/**
 * Created by dexter on 25/11/15.
 */
public class ParentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements RecentAdapter.VisibilityHook, Closeable {

    private static final byte VIEW_TYPE_RECENT = 0;
    private static final byte VIEW_TYPE_ALL = 1;
    public final Map<String, Boolean> packageVisibility = MiscUtils.getMap(100);

    private final long recentHolderId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
    private final HandOverMessage<App> handOverApp;
    private final PackageManager packageManager;
    private final RecentAdapter recentAdapter;

    private final HandOverMessageExtra<App> handOverMessageExtra = new HandOverMessageExtra<App>() {

        @Override
        public void handOverMessage(@Nonnull Integer position) {

            final Object object = getItem(position);
            if (object instanceof App)
                ParentAdapter.this.handOverApp.handOverMessage((App) object);
            else
                throw new IllegalStateException("Position must correspond with an App");
        }

        @Override
        public App getExtra(@Nonnull Integer position) {

            final Object object = getItem(position);
            if (object instanceof App)
                return (App) object;
            else
                throw new IllegalStateException("Position must correspond with an App");
        }

        @Override
        public void putExtra(int position, App item) {

        }

        @Override
        public void handOverAppVisibilityMessage(int position, boolean visibility, String packageName) {
            ParentAdapter.this.handOverAppVisibilityToggle.handOverMessage(position,visibility,packageName);
        }

        @Override
        public void handOverSongVisibilityMessage(int position, Object message) {

        }
    };

    private final HandOverVisibilityToggle handOverAppVisibilityToggle;

    public static interface HandOverVisibilityToggle{

        public void handOverMessage(int position, boolean visibility, String packageName);

    }

    public ParentAdapter(HandOverMessage<App> handOverApp, Context context,HandOverVisibilityToggle handOverAppVisibilityToggle) {

        this.packageManager = context.getPackageManager();
        this.handOverApp = handOverApp;
        this.handOverAppVisibilityToggle = handOverAppVisibilityToggle;

        this.recentAdapter = new RecentAdapter(new ArrayList<>(20), handOverApp, packageManager, R.layout.app_grid_item, this);
        setHasStableIds(true);
    }

    public synchronized void visibilityChanged(String packageName) {

        if (TextUtils.isEmpty(packageName)) {
            notifyDataSetChanged();
            recentAdapter.notifyDataSetChanged();
        }

        int position = -1;
        for (int index = 0; index < allAppsList.size(); index++)
            if (allAppsList.get(index).packageName.equals(packageName)) {

                position = index;
                break;
            }

        position++;//adjust for recent
        position++;//adjust for material header

        notifyItemChanged(position); //adjust for header
        recentAdapter.visibilityChanged(packageName);
    }

    ///////////Data set ops
    private final List<App> allAppsList = new ArrayList<>(100);

    private int allAppCount = 0;

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
                        .inflate(R.layout.app_list_item, parent, false), handOverMessageExtra);
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
            appItemHolder.menuData.setPosition(holder.getAdapterPosition());
            appItemHolder.appName.setText(appExactType.applicationName);
            try {
                appItemHolder.appIcon.setImageDrawable(packageManager.getApplicationIcon(appExactType.packageName));
            } catch (PackageManager.NameNotFoundException ignored) {
                appItemHolder.appIcon.setImageDrawable(null);
            }

            //if contains and is true
            if (isVisible(appExactType.packageName)) {
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
        return allAppCount + 1; //adjust for recent
    }

    @Override
    public boolean isVisible(String packageName) {
        return packageVisibility.containsKey(packageName) && packageVisibility.get(packageName);
    }
}
