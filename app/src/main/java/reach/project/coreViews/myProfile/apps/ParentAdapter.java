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

import reach.project.R;
import reach.project.apps.App;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.AbstractListHolder;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.RecyclerViewMaterialAdapter;

/**
 * Created by dexter on 25/11/15.
 */
final class ParentAdapter extends RecyclerViewMaterialAdapter<RecyclerView.ViewHolder> implements RecentAdapter.VisibilityHook {

    private static final byte VIEW_TYPE_RECENT = 0;
    private static final byte VIEW_TYPE_ALL = 1;
    static final Map<String, Boolean> packageVisibility = MiscUtils.getMap(100);

    private final HandOverMessage<App> handOverApp;
    private final RecentAdapter recentAdapter;

    public ParentAdapter(HandOverMessage<App> handOverApp) {

        this.handOverApp = handOverApp;
        setHasStableIds(true);
        recentAdapter = new RecentAdapter(new ArrayList<>(20), handOverApp, R.layout.app_mylibray_grid_item, this);
    }

    ///////////Data set ops
    private final List<App> allAppsList = new ArrayList<>(100);

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

        notifyItemChanged(position); //adjust for header
        recentAdapter.visibilityChanged(packageName);
    }

    private final Object[] reUsable = new Object[2];

    @Override
    public boolean isVisible(String packageName) {
        return packageVisibility.containsKey(packageName) && packageVisibility.get(packageName);
    }

    //////////////////////

    @Override
    protected void newBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        Log.i("Ayush", "Re-binding parent " + position);
        if (position > 0) {

            final App appExactType = allAppsList.get(position - 1); //re-adjust for recent
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
            if (isVisible(appExactType.packageName)) {
                appItemHolder.toggleButton.setImageResource(R.drawable.icon_locked);
                appItemHolder.toggleText.setText("Everyone");
            } else {
                appItemHolder.toggleButton.setImageResource(R.drawable.icon_locked);
                appItemHolder.toggleText.setText("Only Me");
            }
        } else {

            //assume its recent
            final AbstractListHolder horizontalViewHolder = (AbstractListHolder) holder;
            holder.itemView.setBackgroundResource(R.drawable.border_shadow3);
            horizontalViewHolder.headerText.setText("Recently Installed");
            horizontalViewHolder.listOfItems.setLayoutManager(
                    new CustomGridLayoutManager(holder.itemView.getContext(), 2));
            horizontalViewHolder.listOfItems.setAdapter(recentAdapter);
        }
    }

    @Override
    protected RecyclerView.ViewHolder newCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case VIEW_TYPE_ALL: {

                return new AppItemHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.app_mylibray_list_item, parent, false),
                        position -> handOverApp.handOverMessage(allAppsList.get(position - 1))); //re-adjust for recent
            }

            case VIEW_TYPE_RECENT: {
                return new AbstractListHolder(parent,
                        R.layout.list_with_more_button_padding, //Main resource id
                        R.id.headerText, //id for header text
                        R.id.listOfItems, //id for list (recycler view)
                        R.id.moreButton); //id of more button
            }

            default:
                return null;
        }
    }

    @Override
    protected int newGetItemCount() {
        return allAppsList.size() + 1; //adjust for recent
    }

    @Override
    protected int newGetItemViewType(int position) {

        if (position > 0)
            return VIEW_TYPE_ALL;
        else
            return VIEW_TYPE_RECENT;
    }

    @Override
    protected RecyclerView.ViewHolder inflatePlaceHolder(View view) {
        return new RecyclerView.ViewHolder(view) {
        };
    }

    @Override
    protected long newGetItemId(int position) {

//        Log.i("Ayush", "Checking item id parent " + position);

        if (position > 0) {

            final App app = allAppsList.get(position - 1); //re-adjust for recent
            final boolean visibility = isVisible(app.packageName);

            reUsable[0] = app;
            reUsable[1] = visibility;

            return Arrays.hashCode(reUsable);
        } else
            return super.getItemId(position);
    }
}