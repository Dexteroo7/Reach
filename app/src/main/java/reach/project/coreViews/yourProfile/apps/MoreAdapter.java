package reach.project.coreViews.yourProfile.apps;

import android.content.pm.PackageManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.List;

import reach.project.apps.App;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;

/**
 * Created by dexter on 18/11/15.
 */
class MoreAdapter extends SimpleRecyclerAdapter<App, AppItemHolder> implements MoreQualifier {

    public MoreAdapter(List<App> messageList, HandOverMessage<App> handOverMessage, int resourceId) {
        super(messageList, handOverMessage, resourceId);
    }

    @Override
    public AppItemHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        return new AppItemHolder(itemView, handOverMessage);
    }

    @Override
    public void onBindViewHolder(AppItemHolder holder, App item) {

        holder.appName.setText(item.applicationName);
        final PackageManager packageManager = holder.appName.getContext().getPackageManager();

        try {
            holder.appIcon.setImageDrawable(packageManager.getApplicationIcon(item.packageName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        //ignored as an "update" will not happen over here
    }
}