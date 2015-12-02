package reach.project.coreViews.myProfile.apps;

import android.content.pm.PackageManager;
import android.view.View;

import com.google.common.collect.Ordering;

import java.util.Comparator;
import java.util.List;

import reach.project.apps.App;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;

/**
 * Created by dexter on 25/11/15.
 */
class RecentAdapter extends SimpleRecyclerAdapter<App, AppItemHolder> {

    private final List<App> recentApps;

    public RecentAdapter(List<App> messageList, HandOverMessage<App> handOverMessage, int resourceId) {
        super(messageList, handOverMessage, resourceId);
        this.recentApps = messageList;
    }

    /**
     * MUST CALL FROM UI THREAD
     *
     * @param newMessages the new collection to display
     */
    public void updateRecent(List<App> newMessages) {

        recentApps.removeAll(newMessages);

        final List<App> newSortedList;
        synchronized (recentApps) {

            recentApps.addAll(newMessages);
            newSortedList = Ordering.from(new Comparator<App>() {
                @Override
                public int compare(App lhs, App rhs) {

                    final Long a = lhs.installDate == null ? 0 : lhs.installDate;
                    final Long b = rhs.installDate == null ? 0 : rhs.installDate;

                    return a.compareTo(b);
                }
            }).greatestOf(recentApps, 20);
            recentApps.clear();
            recentApps.addAll(newSortedList);
        }

        notifyDataSetChanged();
    }

    @Override
    public AppItemHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        return new AppItemHolder(itemView, handOverMessage);
    }

    @Override
    public void onBindViewHolder(AppItemHolder holder, App item) {

        final PackageManager packageManager = holder.appName.getContext().getPackageManager();

        holder.appName.setText(item.applicationName);
        try {
            holder.appIcon.setImageDrawable(packageManager.getApplicationIcon(item.packageName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
