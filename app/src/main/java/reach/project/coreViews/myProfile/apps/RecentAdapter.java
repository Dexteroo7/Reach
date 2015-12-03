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

import reach.project.apps.App;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;

/**
 * Created by dexter on 25/11/15.
 */
class RecentAdapter extends SimpleRecyclerAdapter<App, AppItemHolder> implements MoreQualifier {

    private final VisibilityHook visibilityHook;

    private final Ordering<App> primary = new Ordering<App>() {
        @Override
        public int compare(@Nullable App left, @Nullable App right) {

            final Long a = left == null || left.installDate == null ? 0 : left.installDate;
            final Long b = right == null || right.installDate == null ? 0 : right.installDate;

            return a.compareTo(b);
        }
    };

    private final Ordering<App> secondary = new Ordering<App>() {
        @Override
        public int compare(@Nullable App left, @Nullable App right) {

            final String a = left == null || left.applicationName == null ? "" : left.applicationName;
            final String b = right == null || right.applicationName == null ? "" : right.applicationName;

            return a.compareTo(b);
        }
    };

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
        newSortedList = Ordering.from(primary).compound(secondary).greatestOf(recentApps, 20);
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
        if (visibilityHook.isVisible(item.packageName))
            holder.appIcon.setAlpha(1.0f);
        else
            holder.appIcon.setAlpha(0.5f);
    }

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        this.moreAdapter = adapterWeakReference;
    }

    public interface VisibilityHook {

        boolean isVisible(String packageName);
    }
}
