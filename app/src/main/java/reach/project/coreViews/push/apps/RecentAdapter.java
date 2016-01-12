package reach.project.coreViews.push.apps;

import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.List;

import reach.project.apps.App;
import reach.project.core.ReachActivity;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;

/**
 * Created by dexter on 25/11/15.
 */
class RecentAdapter extends SimpleRecyclerAdapter<App, AppItemHolder> implements MoreQualifier, Closeable {

    private final PackageManager packageManager;

    public RecentAdapter(List<App> messageList,
                         HandOverMessage<App> handOverMessage,
                         int resourceId,
                         PackageManager packageManager) {

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

        getMessageList().clear();

        synchronized (getMessageList()) {
            getMessageList().addAll(newMessages);
        }

        notifyDataSetChanged();
        final RecyclerView.Adapter adapter;
        if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
            adapter.notifyDataSetChanged();
    }

    @Override
    public AppItemHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        return new AppItemHolder(itemView, handOverMessage);
    }

    @Override
    public long getItemId(App item) {
        return item.packageName.hashCode();
    }

    @Override
    public void onBindViewHolder(AppItemHolder holder, App item) {

        holder.appName.setText(item.applicationName);
        holder.checkBox.setSelected(ReachActivity.SELECTED_APPS.contains(item));
        try {
            holder.appIcon.setImageDrawable(packageManager.getApplicationIcon(item.packageName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        this.adapterWeakReference = adapterWeakReference;
    }

    @Override
    public void close() {
        getMessageList().clear();
    }
}
