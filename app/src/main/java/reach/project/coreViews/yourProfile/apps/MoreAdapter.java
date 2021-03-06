//package reach.project.coreViews.yourProfile.apps;
//
//import android.content.pm.PackageManager;
//import android.net.Uri;
//import android.support.v7.widget.RecyclerView;
//import android.view.View;
//
//import java.lang.ref.WeakReference;
//import java.util.List;
//
//import reach.project.apps.App;
//import reach.project.utils.viewHelpers.HandOverMessage;
//import reach.project.utils.viewHelpers.MoreQualifier;
//import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;
//
///**
// * Created by dexter on 18/11/15.
// */
//class MoreAdapter extends SimpleRecyclerAdapter<App, AppItemHolder> implements MoreQualifier {
//
//    public MoreAdapter(List<App> messageList, HandOverMessage<App> handOverMessage, int resourceId) {
//        super(messageList, handOverMessage, resourceId);
//    }
//
//    @Override
//    public int getItemCount() {
//        int size = super.getItemCount();
//        return size < 4 ? size : 4;
//    }
//
//    @Override
//    public AppItemHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
//        return new AppItemHolder(itemView, handOverMessage);
//    }
//
//    @Override
//    public long getItemId(App item) {
//        return item.packageName.hashCode();
//    }
//
//    @Override
//    public void onBindViewHolder(AppItemHolder holder, App item) {
//
//        holder.appName.setText(item.applicationName);
//        holder.extraButton.setVisibility(View.INVISIBLE);
//        final PackageManager packageManager = holder.appName.getContext().getPackageManager();
//
//        try {
//            holder.appIcon.setImageDrawable(packageManager.getApplicationIcon(item.packageName));
//        } catch (PackageManager.NameNotFoundException ignored) {
//            holder.appIcon.setImageURI(Uri.parse("http://52.74.53.245:8080/getImage/appLogo?packageName=" + item.packageName));
//        }
//    }
//
//    @Override
//    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
//        //ignored as an "update" will not happen over here
//    }
//}