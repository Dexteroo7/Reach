package reach.project.coreViews.fileManager.apps;

import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.List;

import reach.project.R;
import reach.project.apps.App;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;

/**
 * Created by dexter on 25/11/15.
 */
class RecentAdapter extends SimpleRecyclerAdapter<App, AppItemHolder> implements MoreQualifier {

    private final PackageManager packageManager;
    private final View.OnClickListener extraClick;

    public RecentAdapter(List<App> messageList,
                         HandOverMessage<App> handOverMessage,
                         int resourceId,
                         PackageManager packageManager,
                         View.OnClickListener extraClick) {

        super(messageList, handOverMessage, resourceId);
        this.packageManager = packageManager;
        this.extraClick = extraClick;
    }

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

    public void visibilityChanged(String packageName) {

        final List<App> recentApps = getMessageList();

        int position;
        for (position = 0; position < recentApps.size(); position++)
            if (recentApps.get(position).packageName.equals(packageName))
                break;

        //recent adapter might not contain everything, as is limited to 4
        if (position < getItemCount())
            notifyItemChanged(position);

        final RecyclerView.Adapter adapter;
        if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
            adapter.notifyItemChanged(position); //position will be same
    }

    @Override
    public int getItemCount() {

        int size = super.getItemCount();
        return size < 4 ? size : 4;
    }

    @Nullable
    private WeakReference<RecyclerView.Adapter> adapterWeakReference = null;

    @Override
    public AppItemHolder getViewHolder(View itemView) {
        return new AppItemHolder(itemView, this, extraClick);
    }

    @Override
    public long getItemId(App item) {
        return item.packageName.hashCode();
    }

    @Override
    public void onBindViewHolder(AppItemHolder holder, App item) {

        holder.packageName.set(item.packageName);
        holder.appName.setText(item.applicationName);
        try {
            holder.appIcon.setImageDrawable(packageManager.getApplicationIcon(item.packageName));
        } catch (PackageManager.NameNotFoundException ignored) {
            holder.appIcon.setImageDrawable(null);
        }

        //if contains and is true
        if (ApplicationFragment.isVisible(item.packageName)) {
            holder.toggleImage.setImageResource(R.drawable.icon_everyone);
            //holder.toggleText.setText("Everyone");
        } else {
            holder.toggleImage.setImageResource(R.drawable.icon_locked);
            //holder.toggleText.setText("Only Me");
        }
    }

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        this.adapterWeakReference = adapterWeakReference;
    }
}
