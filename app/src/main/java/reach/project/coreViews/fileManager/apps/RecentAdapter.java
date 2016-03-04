package reach.project.coreViews.fileManager.apps;

import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.List;

import javax.annotation.Nonnull;

import reach.project.apps.App;
import reach.project.coreViews.friends.HandOverMessageExtra;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;

/**
 * Created by dexter on 25/11/15.
 */
class RecentAdapter extends SimpleRecyclerAdapter<App, AppItemHolder> implements MoreQualifier, Closeable {

    private final PackageManager packageManager;
    private final VisibilityHook visibilityHook;


    public RecentAdapter(List<App> messageList,
                         HandOverMessage<App> handOverMessage,
                         PackageManager packageManager,
                         int resourceId, VisibilityHook visibilityHook) {

        super(messageList, handOverMessage, resourceId);
        this.visibilityHook = visibilityHook;
        this.packageManager = packageManager;
    }

    private final HandOverMessageExtra<App> handOverMessageExtra = new HandOverMessageExtra<App>() {
        @Override
        public void handOverMessage(@Nonnull Integer position) {
            RecentAdapter.this.handOverMessage(position);
        }

        @Override
        public App getExtra(@Nonnull Integer position) {

            final App app = getItem(position);
            if (app != null)
                return app;
            else
                throw new IllegalStateException("App has been corrupted");
        }
    };

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
        return new AppItemHolder(itemView, handOverMessageExtra);
    }

    @Override
    public long getItemId(App item) {
        return item.packageName.hashCode();
    }

    @Override
    public void onBindViewHolder(AppItemHolder holder, App item) {

        holder.position = holder.getAdapterPosition();
        holder.appName.setText(item.applicationName);
        try {
            holder.appIcon.setImageDrawable(packageManager.getApplicationIcon(item.packageName));
        } catch (PackageManager.NameNotFoundException ignored) {
            holder.appIcon.setImageDrawable(null);
        }

        /*//if contains and is true
        if (visibilityHook.isVisible(item.packageName)) {
            holder.toggleButton.setImageResource(R.drawable.icon_everyone);
            holder.toggleText.setText("Everyone");
        }
        else {
            holder.toggleButton.setImageResource(R.drawable.icon_locked);
            holder.toggleText.setText("Only Me");
        }*/
    }

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        this.adapterWeakReference = adapterWeakReference;
    }

    @Override
    public void close() {
        getMessageList().clear();
    }

    public interface VisibilityHook {

        boolean isVisible(String packageName);
    }
}
