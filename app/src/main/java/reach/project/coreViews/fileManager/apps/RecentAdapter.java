package reach.project.coreViews.fileManager.apps;

import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.google.common.collect.Ordering;

import java.lang.ref.WeakReference;
import java.util.List;

import reach.project.apps.App;
import reach.project.core.StaticData;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;

/**
 * Created by dexter on 25/11/15.
 */
class RecentAdapter extends SimpleRecyclerAdapter<App, AppItemHolder> implements MoreQualifier {

    private final PackageManager packageManager;

    public RecentAdapter(List<App> messageList,
                         HandOverMessage<App> handOverMessage,
                         PackageManager packageManager,
                         int resourceId) {

        super(messageList, handOverMessage, resourceId);
        this.packageManager = packageManager;
    }

    @Override
    public int getItemCount() {
        int size = super.getItemCount();
        return size < 4 ? size : 4;
    }

    @Nullable
    private WeakReference<RecyclerView.Adapter> adapterWeakReference = null;

    /**
     * MUST CALL FROM UI THREAD
     *
     * @param newMessages the new collection to display
     */
    public void updateRecent(List<App> newMessages) {

        getMessageList().removeAll(newMessages);

        final List<App> newSortedList;
        synchronized (getMessageList()) {

            getMessageList().addAll(newMessages);
            newSortedList = Ordering.from(StaticData.byInstallDate).compound(StaticData.byName).greatestOf(getMessageList(), 20);
            getMessageList().clear();
            getMessageList().addAll(newSortedList);
        }

        notifyDataSetChanged();
        final RecyclerView.Adapter adapter;
        if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null) {


            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public AppItemHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        Log.i("Ayush", "Creating ViewHolder " + getClass().getName());

        return new AppItemHolder(itemView, handOverMessage);
    }

    @Override
    public long getItemId(App item) {
        return item.packageName.hashCode();
    }

    @Override
    public void onBindViewHolder(AppItemHolder holder, App item) {

        Log.i("Ayush", "Binding ViewHolder " + getClass().getName());

        holder.appName.setText(item.applicationName);
        try {
            holder.appIcon.setImageDrawable(packageManager.getApplicationIcon(item.packageName));
        } catch (PackageManager.NameNotFoundException ignored) {
            holder.appIcon.setImageDrawable(null);
        }
    }

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        this.adapterWeakReference = adapterWeakReference;
    }
}
