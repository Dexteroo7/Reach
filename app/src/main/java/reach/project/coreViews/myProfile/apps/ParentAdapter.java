package reach.project.coreViews.myProfile.apps;

import android.content.pm.PackageManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.apps.App;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.ListHolder;
import reach.project.utils.viewHelpers.RecyclerViewMaterialAdapter;

/**
 * Created by dexter on 25/11/15.
 */
class ParentAdapter extends RecyclerViewMaterialAdapter<RecyclerView.ViewHolder> implements HandOverMessage<App>, RecentAdapter.VisibilityHook {

    private final HandOverMessage<App> handOverApp;

    public ParentAdapter(HandOverMessage<App> handOverApp) {

        this.handOverApp = handOverApp;
        setHasStableIds(true);
    }

    static final byte VIEW_TYPE_RECENT = 0;
    static final byte VIEW_TYPE_ALL = 1;

    public final Map<String, Boolean> packageVisibility = MiscUtils.getMap(100);

    ///////////All songs cursor
    private final List<App> allAppsList = new ArrayList<>(100);

    private int allAppCount = 0;

    public void updateAllApps(List<App> allApps) {

        Log.i("Ayush", "UPDATING ALL APPS");
        synchronized (allAppsList) {
            this.allAppsList.clear();
            this.allAppsList.addAll(allApps);
        }
        //no need to notify
    }

    public void destroy() {
        allAppsList.clear();
    }
    ///////////

    ///////////Recent music adapter
    private final RecentAdapter recentAdapter;

    {
        final List<App> defaultList = new ArrayList<>(1);
        defaultList.add(new App.Builder().build());
        recentAdapter = new RecentAdapter(defaultList, this, R.layout.app_mylibray_grid_item, this);
    }

    public void updateRecentApps(List<App> newRecent) {

        Log.i("Ayush", "UPDATING RECENT APPS");
        if (newRecent.isEmpty())
            return;
        recentAdapter.updateRecent(newRecent);
    }
    ///////////

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
//        Log.i("Ayush", "Toggle " + position + " " +
//                packageVisibility.get(packageName) + " " +
//                ((App) getItem(position)).packageName);

        //will pick the new visibility from the map
//        Log.i("Ayush", hasObservers() + " obs");

//        getActualAdapter.getActualAdapter().notifyDataSetChanged();
        notifyItemChanged(position); //adjust for header
        recentAdapter.visibilityChanged(packageName);
    }

    /**
     * Will either return App object OR flag for recent list
     *
     * @param position position to load
     * @return object
     */
    @Nonnull
    private Object getItem(int position) {

//        Log.i("Ayush", "PARENT GET ITEM CALL " + position);

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

    final Object[] reUsable = new Object[2];

    @Override
    public long getItemId(int position) {

//        Log.i("Ayush", "Checking item id parent " + position);

        final Object item = getItem(position);
        if (item instanceof App) {

            final App app = (App) item;
            final boolean visibility;
            if (packageVisibility.containsKey(app.packageName))
                visibility = packageVisibility.get(app.packageName);
            else
                visibility = true;

            reUsable[0] = app;
            reUsable[1] = visibility;

            return Arrays.hashCode(reUsable);
        } else
            return super.getItemId(position);
    }

    @Override
    public void handOverMessage(@Nonnull App message) {
        handOverApp.handOverMessage(message);
    }

    @Override
    public boolean isVisible(String packageName) {
        return packageVisibility.containsKey(packageName) && packageVisibility.get(packageName);
    }

    //////////////////////

    @Override
    public void newBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        Log.i("Ayush", "Re-binding parent " + position);

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
                appItemHolder.appIcon.setImageDrawable(null);
            }

            //if contains and is true
            if (packageVisibility.containsKey(appExactType.packageName) && packageVisibility.get(appExactType.packageName)) {
                appItemHolder.toggleButton.setImageResource(R.drawable.icon_locked);
                appItemHolder.toggleText.setText("Everyone");
            } else {
                appItemHolder.toggleButton.setImageResource(R.drawable.icon_locked);
                appItemHolder.toggleText.setText("Only Me");
            }

        } else {

            final ListHolder horizontalViewHolder = (ListHolder) holder;
            holder.itemView.setBackgroundResource(R.drawable.border_shadow3);
            horizontalViewHolder.headerText.setText("Recently Installed");
            horizontalViewHolder.listOfItems.setLayoutManager(
                    new CustomGridLayoutManager(holder.itemView.getContext(), 2));
            horizontalViewHolder.listOfItems.setAdapter(recentAdapter);
        }
    }

    @Override
    public RecyclerView.ViewHolder newCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case VIEW_TYPE_ALL: {

                return new AppItemHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.app_mylibray_list_item, parent, false), position -> {

                    final Object object = getItem(position);
                    if (object instanceof App)
                        handOverApp.handOverMessage((App) object);
                    else
                        throw new IllegalStateException("Position must correspond with an App");
                });
            }

            case VIEW_TYPE_RECENT: {
                return new ListHolder(parent, R.layout.list_with_more_button_padding);
            }

            default:
                return null;
        }
    }

    @Override
    public int newGetItemCount() {

        Log.i("Ayush", "PARENT GET ITEM COUNT");

        allAppCount = allAppsList.size();
        return allAppCount + 1; //adjust for recent
    }

    @Override
    public int newGetItemViewType(int position) {

        final Object item = getItem(position);
        if (item instanceof App)
            return VIEW_TYPE_ALL;
        else
            return VIEW_TYPE_RECENT;
    }

    @Override
    public RecyclerView.ViewHolder inflatePlaceHolder(View view) {
        return null;
    }
}