package reach.project.coreViews.myProfile.apps;

import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.google.common.collect.Ordering;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

import reach.project.R;
import reach.project.apps.App;
import reach.project.core.StaticData;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;

/**
 * Created by dexter on 25/11/15.
 */
class RecentAdapter extends SimpleRecyclerAdapter<App, AppItemHolder> implements MoreQualifier {

    private final VisibilityHook visibilityHook;

    public RecentAdapter(List<App> messageList, HandOverMessage<App> handOverMessage, int resourceId, VisibilityHook visibilityHook) {
        super(messageList, handOverMessage, resourceId);
        this.visibilityHook = visibilityHook;
        setHasStableIds(true);
    }

    @Nullable
    private WeakReference<RecyclerView.Adapter> moreAdapter = null;

    /**
     * MUST CALL FROM UI THREAD
     *
     * @param newMessages the new collection to display
     */
    public synchronized void updateRecent(List<App> newMessages) {

        final List<App> recentApps = getMessageList();
        recentApps.removeAll(newMessages);

        final List<App> newSortedList;
        recentApps.addAll(newMessages);
        newSortedList = Ordering.from(StaticData.primaryApps).compound(StaticData.secondaryApps).greatestOf(recentApps, 20);
        recentApps.clear();
        recentApps.addAll(newSortedList);
        notifyDataSetChanged();
        final RecyclerView.Adapter adapter;
        if (moreAdapter != null && (adapter = moreAdapter.get()) != null)
            adapter.notifyDataSetChanged();
    }

    public synchronized void visibilityChanged(String packageName) {

        final List<App> recentApps = getMessageList();

        int position = -1;
        for (int index = 0; index < recentApps.size(); index++)
            if (recentApps.get(index).packageName.equals(packageName)) {
                position = index;
                break;
            }

        Log.i("Ayush", hasObservers() + " obs");

        //will pick the new visibility from the map
        if (position > -1)
            notifyItemChanged(position);

        final RecyclerView.Adapter adapter;
        if (moreAdapter != null && (adapter = moreAdapter.get()) != null)
            adapter.notifyDataSetChanged();
    }

    final Object[] reUsable = new Object[2];

    @Override
    public long getItemId(int position) {

//        Log.i("Ayush", "Checking item id recent " + position);

        final App app = getItem(position);
        final boolean visibility = visibilityHook.isVisible(app.packageName);

        reUsable[0] = app;
        reUsable[1] = visibility;

        return Arrays.hashCode(reUsable);
    }

    @Override
    public AppItemHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        return new AppItemHolder(itemView, handOverMessage);
    }

    @Override
    public void onBindViewHolder(AppItemHolder holder, App item) {

//        Log.i("Ayush", "Re-binding recent " + item.applicationName);

        final PackageManager packageManager = holder.appName.getContext().getPackageManager();

        holder.appName.setText(item.applicationName);
        try {

            holder.appIcon.setImageDrawable(packageManager.getApplicationIcon(item.packageName));

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            holder.appIcon.setImageDrawable(null);
        }

        //if contains and is true
        if (visibilityHook.isVisible(item.packageName)) {
            holder.toggleButton.setImageResource(R.drawable.ic_pending_lock);
            holder.toggleButton2.setVisibility(View.GONE);
            holder.toggleText.setText("Everyone");
        }
        else {
            holder.toggleButton.setImageResource(R.drawable.icon_locked);
            holder.toggleButton2.setVisibility(View.VISIBLE);
            holder.toggleText.setText("Only Me");
        }
    }

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        this.moreAdapter = adapterWeakReference;
    }

    public interface VisibilityHook {

        boolean isVisible(String packageName);
    }

    @Override
    public int getItemCount() {
        int size = super.getItemCount();
        return size < 4 ? size : 4;
    }
}
