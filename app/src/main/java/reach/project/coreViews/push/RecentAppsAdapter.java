package reach.project.coreViews.push;

import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

import reach.project.apps.App;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;

/**
 * Created by dexter on 25/11/15.
 */
class RecentAppsAdapter extends SimpleRecyclerAdapter<App, PushItemHolder> implements MoreQualifier {

    public RecentAppsAdapter(List<App> messageList, HandOverMessage<App> handOverMessage, int resourceId) {
        super(messageList, handOverMessage, resourceId);
        setHasStableIds(true);
    }

    @Nullable
    private WeakReference<RecyclerView.Adapter> adapterWeakReference = null;

    /**
     * MUST CALL FROM UI THREAD
     *
     * @param newMessages the new collection to display
     */
    public synchronized void updateRecent(List<App> newMessages) {

        //in case of apps, every thing is pre sorted
        getMessageList().clear();
        getMessageList().addAll(newMessages);

        notifyDataSetChanged();

        final RecyclerView.Adapter adapter;
        if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
            adapter.notifyDataSetChanged();
    }

    public synchronized void selectionChanged(String packageName) {

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
        if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
            adapter.notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {

//        Log.i("Ayush", "Checking item id recent " + position);
        return getItem(position).hashCode();
    }

    @Override
    public PushItemHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        return new PushItemHolder(itemView, handOverMessage);
    }

    public final Map<String, Boolean> selected = MiscUtils.getMap(100);

    @Override
    public void onBindViewHolder(PushItemHolder holder, App item) {

//        Log.i("Ayush", "Re-binding recent " + item.applicationName);

        final PackageManager packageManager = holder.text.getContext().getPackageManager();

        holder.text.setText(item.applicationName);
        try {

            holder.image.setImageDrawable(packageManager.getApplicationIcon(item.packageName));

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            holder.image.setImageDrawable(null);
        }

        final boolean isSelected;
        if (selected.containsKey(item.packageName))
            isSelected = selected.get(item.packageName);
        else
            isSelected = false;

        holder.checkBox.setChecked(isSelected);
        holder.mask.setVisibility(isSelected ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {

        final int size = super.getItemCount();
        return size > 6 ? 6 : size;
    }

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        this.adapterWeakReference = adapterWeakReference;
    }
}
