//package reach.project.coreViews.myProfile.apps;
//
//import android.app.Activity;
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.content.pm.PackageManager;
//import android.graphics.Color;
//import android.support.annotation.NonNull;
//import android.support.annotation.Nullable;
//import android.support.v7.widget.RecyclerView;
//import android.util.Log;
//import android.view.View;
//
//import java.io.Closeable;
//import java.lang.ref.WeakReference;
//import java.util.List;
//
//import reach.project.R;
//import reach.project.apps.App;
//import reach.project.utils.SharedPrefUtils;
//import reach.project.utils.viewHelpers.HandOverMessage;
//import reach.project.utils.viewHelpers.MoreQualifier;
//import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;
//import reach.project.utils.viewHelpers.tourguide.Overlay;
//import reach.project.utils.viewHelpers.tourguide.ToolTip;
//import reach.project.utils.viewHelpers.tourguide.TourGuide;
//
///**
// * Created by dexter on 25/11/15.
// */
//class RecentAdapter extends SimpleRecyclerAdapter<App, AppItemHolder> implements MoreQualifier, Closeable {
//
//    private final VisibilityHook visibilityHook;
//    private final PackageManager packageManager;
//    private TourGuide tourGuide = null;
//    private boolean shouldShowCoach1;
//    private SharedPreferences sharedPreferences;
//
//    public RecentAdapter(List<App> messageList,
//                         HandOverMessage<App> handOverMessage,
//                         int resourceId,
//                         VisibilityHook visibilityHook,
//                         Context context) {
//
//        super(messageList, handOverMessage, resourceId);
//
//        this.visibilityHook = visibilityHook;
//        this.packageManager = context.getPackageManager();
//        this.sharedPreferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
//        shouldShowCoach1 = !SharedPrefUtils.getMyProfileCoach1Seen(sharedPreferences);
//    }
//
//    @Override
//    public void handOverMessage(@NonNull Integer position) {
//        if (tourGuide != null) {
//            Log.d("Ashish", "tourGuide.cleanUp");
//            tourGuide.cleanUp();
//        }
//        super.handOverMessage(position);
//    }
//
//    @Nullable
//    private WeakReference<RecyclerView.Adapter> adapterWeakReference = null;
//
//    /**
//     * MUST CALL FROM UI THREAD
//     *
//     * @param newMessages the new collection to display
//     */
//    public void updateRecent(List<App> newMessages) {
//
//        getMessageList().clear();
//
//        synchronized (getMessageList()) {
//            getMessageList().addAll(newMessages);
//        }
//
//        //shouldShowCoach1 = true;
//        notifyDataSetChanged();
//        final RecyclerView.Adapter adapter;
//        if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
//            adapter.notifyDataSetChanged();
//    }
//
//    public synchronized void visibilityChanged(String packageName) {
//
//        final List<App> recentApps = getMessageList();
//
//        int position = -1;
//        for (int index = 0; index < recentApps.size(); index++)
//            if (recentApps.get(index).packageName.equals(packageName)) {
//                position = index;
//                break;
//            }
//
//        //recent adapter might not contain everything, as is limited to 4
//        if (position < getItemCount())
//            notifyItemChanged(position);
//
//        final RecyclerView.Adapter adapter;
//        if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
//            adapter.notifyItemChanged(position); //position will be same
//    }
//
//    @Override
//    public long getItemId(int position) {
//
////        Log.i("Ayush", "Checking item id recent " + position);
//        return getItem(position).packageName.hashCode();
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
////        Log.i("Ayush", "Re-binding recent " + item.applicationName);
//        if (shouldShowCoach1 && holder.getAdapterPosition() == 0) {
//            Log.d("Ashish", "tourGuide.playOn");
//            final ToolTip toolTip = new ToolTip()
//                    .setTextColor(Color.WHITE)
//                    .setTitle("Manage privacy")
//                    .setShadow(false)
//                    .setDescription("Tap to hide/unhide from friends. By default, your personal files are hidden!");
//            final Overlay overlay = new Overlay()
//                    .setBackgroundColor(Color.parseColor("#BF000000"))
//                    .setStyle(Overlay.Style.Rectangle);
//            tourGuide = TourGuide.init((Activity) holder.itemView.getContext()).with(TourGuide.Technique.Click)
//                    .setToolTip(toolTip)
//                    .setOverlay(overlay)
//                    .playOn(holder.itemView);
//            SharedPrefUtils.setMyProfileCoach1Seen(sharedPreferences);
//            shouldShowCoach1 = false;
//        }
//
//        holder.appName.setText(item.applicationName);
//        try {
//            holder.appIcon.setImageDrawable(packageManager.getApplicationIcon(item.packageName));
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//            holder.appIcon.setImageDrawable(null);
//        }
//
//        //if contains and is true
//        if (visibilityHook.isVisible(item.packageName)) {
//            holder.toggleButton.setImageResource(R.drawable.icon_everyone);
//            holder.toggleButton2.setVisibility(View.GONE);
//            holder.toggleText.setText("Everyone");
//        }
//        else {
//            holder.toggleButton.setImageResource(R.drawable.icon_locked);
//            holder.toggleButton2.setVisibility(View.VISIBLE);
//            holder.toggleText.setText("Only Me");
//        }
//    }
//
//    @Override
//    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
//        this.adapterWeakReference = adapterWeakReference;
//    }
//
//    @Override
//    public void close() {
//        getMessageList().clear();
//    }
//
//    public interface VisibilityHook {
//
//        boolean isVisible(String packageName);
//    }
//
//    @Override
//    public int getItemCount() {
//        int size = super.getItemCount();
//        return size < 4 ? size : 4;
//    }
//}