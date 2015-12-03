package reach.project.coreViews.yourProfile.apps;

import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.wire.Message;

import reach.project.R;
import reach.project.apps.App;
import reach.project.coreViews.yourProfile.blobCache.CacheAdapterInterface;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.ListHolder;


/**
 * Created by dexter on 13/11/15.
 */
class AppAdapter<T extends Message> extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements View.OnClickListener, HandOverMessage<App> {

    private static final byte APP_ITEM_TYPE = 1;
    private static final byte RECENT_LIST_TYPE = 2;
    private static final byte SMART_LIST_TYPE = 3;

    private final CacheAdapterInterface<T, App> cacheAdapterInterface;

    public AppAdapter(CacheAdapterInterface<T, App> cacheAdapterInterface) {
        this.cacheAdapterInterface = cacheAdapterInterface;
        setHasStableIds(true);
    }

    @Override
    public int getItemViewType(int position) {

        final Message message = cacheAdapterInterface.getItem(position);
        if (message instanceof App)
            return APP_ITEM_TYPE;
//        else if (message instanceof AppL)
//            return RECENT_LIST_TYPE;
//        else if (message instanceof SmartApp)
//            return SMART_LIST_TYPE;
        else
            throw new IllegalArgumentException("Unknown message found in list");
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case APP_ITEM_TYPE:
                return new AppItemHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.app_list_item, parent, false), position -> {

                            final T message = cacheAdapterInterface.getItem(position);
                            if (message instanceof App)
                                cacheAdapterInterface.handOverMessage((App) message);
                            else
                                throw new IllegalArgumentException("App item holder passed on an illegal value type");
                        });
            case RECENT_LIST_TYPE:
                return new ListHolder(parent);
            case SMART_LIST_TYPE:
                return new ListHolder(parent);
            default:
                throw new IllegalArgumentException("Unknown view type found");
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final Message message = cacheAdapterInterface.getItem(position);
        if (message instanceof App && holder instanceof AppItemHolder) {

            final App app = (App) message;
            final AppItemHolder appAppItemHolder = (AppItemHolder) holder;

            appAppItemHolder.bindPosition(position);
            appAppItemHolder.appName.setText(app.applicationName);

            final PackageManager packageManager = appAppItemHolder.appName.getContext().getPackageManager();

            try {
                appAppItemHolder.appIcon.setImageDrawable(packageManager.getApplicationIcon(app.packageName));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

        }

//        else if (message instanceof RecentApp && holder instanceof ListHolder) {
//
//            final RecentApp recentApp = (RecentApp) message;
//            final ListHolder listHolder = (ListHolder) holder;
//            listHolder.headerText.setText(recentApp.title);
//            listHolder.listOfItems.setLayoutManager(new CustomGridLayoutManager(holder.itemView.getContext(), 2));
//
//            Log.i("Ayush", "Found recent items with size " + recentApp.appList.size() + " ");
//            if (recentApp.appList.size() < 4)
//                listHolder.listOfItems.setAdapter(new ListAdapterWithMore(recentApp.appList, this, R.layout.app_list_item));
//            else
//                listHolder.listOfItems.setAdapter(new ListAdapterWithMore(recentApp.appList.subList(0, 4), this, R.layout.app_list_item));
//
//        } else if (message instanceof SmartApp && holder instanceof ListHolder) {
//
//            final SmartApp smartApp = (SmartApp) message;
//            final ListHolder listHolder = (ListHolder) holder;
//            listHolder.headerText.setText(smartApp.title);
//            listHolder.listOfItems.setLayoutManager(new CustomLinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
//            if (smartApp.appList.size() < 4)
//                listHolder.listOfItems.setAdapter(new ListAdapterWithMore(smartApp.appList, this, R.layout.app_list_item));
//            else
//                listHolder.listOfItems.setAdapter(new ListAdapterWithMore(smartApp.appList.subList(0, 4), this, R.layout.app_list_item));
//        }
    }

    @Override
    public void onClick(View v) {

        Object object = v.getTag();
        if (!(object instanceof Integer))
            return;
        int pos = (int) object;

        final Message message = cacheAdapterInterface.getItem(pos);
//        if (message instanceof RecentApp) {
//            RecentApp recentApp = (RecentApp) message;
//            //open MoreMusicFragment
//        } else if (message instanceof SmartApp) {
//            SmartApp smartApp = (SmartApp) message;
//        }
    }

    @Override
    public long getItemId(int position) {
        return cacheAdapterInterface.getItem(position).hashCode();
    }

    @Override
    public int getItemCount() {
//        Log.i("Ayush", "Returning new size " + cacheAdapterInterface.getItemCount());
        return cacheAdapterInterface.getItemCount();
    }

//    @Override
//    public void handOverMessage(App app) {
//        cacheAdapterInterface.handOverMessage(app);
//    }
//
//    @Override
//    public void handOverMessage(int position) {
//
//        final T item = cacheAdapterInterface.getItem(position);
//        if (item instanceof App)
//            cacheAdapterInterface.handOverMessage((App) item);
//        else
//            throw new IllegalArgumentException("Expecting App, found something else");
//    }

    @Override
    public void handOverMessage(@NonNull App app) {
        cacheAdapterInterface.handOverMessage(app);
    }
}
