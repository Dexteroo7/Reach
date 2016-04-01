package reach.project.coreViews.fileManager.myfiles_search;

/**
 * Created by gauravsobti on 01/04/16.
 */
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import javax.annotation.Nonnull;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.HandOverMessageExtra;
import reach.project.utils.CloudEndPointsUtils;
import reach.project.utils.ContentType;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;
import android.content.pm.PackageManager;
import android.widget.Toast;

import com.appspot.able_door_616.contentStateApi.ContentStateApi;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.hash.Hashing;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import reach.project.apps.App;

class AppsSearchAdapter extends SimpleRecyclerAdapter<App, AppItemHolder> implements MoreQualifier, Closeable {

    private final PackageManager packageManager;
    public static final Map<String, Boolean> packageVisibility = MiscUtils.getMap(100);
    private final SharedPreferences preferences;
    private final ExecutorService visibilityHandler = Executors.unconfigurableExecutorService(Executors.newFixedThreadPool(2));
    Context context;

    public AppsSearchAdapter(List<App> messageList,
                         HandOverMessage<App> handOverMessage,
                         PackageManager packageManager,
                         int resourceId,
                             Context context
                         ) {

        super(messageList, handOverMessage, resourceId);
        this.packageManager = packageManager;
        this.context = context;
        this.preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
    }

    private final HandOverMessageExtra<App> handOverMessageExtra = new HandOverMessageExtra<App>() {
        @Override
        public void handOverMessage(@Nonnull Integer position) {
            AppsSearchAdapter.this.handOverMessage(position);
        }

        @Override
        public App getExtra(@Nonnull Integer position) {

            final App app = getItem(position);
            if (app != null)
                return app;
            else
                throw new IllegalStateException("App has been corrupted");
        }

        @Override
        public void putExtra(int position, App item) {

        }

        @Override
        public void handOverAppVisibilityMessage(String packageName) {
            //visibilityHook.handOverAppVisibility(packageName);
            final boolean newVisibility = !isVisible(packageName);
            //update in memory
            synchronized (packageVisibility) {
                packageVisibility.put(packageName, newVisibility);
            }
            //update in disk
            SharedPrefUtils.addPackageVisibility(preferences, packageName, newVisibility);
            //update on server
            new ToggleVisibility(context, preferences, packageName, newVisibility).executeOnExecutor(visibilityHandler);
            //notify that visibility has changed
            visibilityChanged(packageName);
        }

        @Override
        public void handOverSongVisibilityMessage(int position, Object message) {

        }
    };


    private static class ToggleVisibility extends AsyncTask<Object, Void, Boolean> {

        private final Context context;
        private final SharedPreferences preferences;
        private final String packageName;
        private final boolean visible;

        public ToggleVisibility(Context context, SharedPreferences preferences, String packageName, boolean visible) {

            this.context = context;
            this.preferences = preferences;
            this.packageName = packageName;
            this.visible = visible;
        }

        @Override
        protected Boolean doInBackground(Object... params) {

            boolean failed = false;

            final HttpTransport transport = new NetHttpTransport();
            final JsonFactory factory = new JacksonFactory();
            final GoogleAccountCredential credential = GoogleAccountCredential
                    .usingAudience(context, StaticData.SCOPE)
                    .setSelectedAccountName(SharedPrefUtils.getEmailId(preferences));

            final ContentStateApi contentStateApi = CloudEndPointsUtils.updateBuilder(new ContentStateApi.Builder(transport, factory, credential))
                    .setRootUrl("https://1-dot-client-module-dot-able-door-616.appspot.com/_ah/api/").build();

            try {
                contentStateApi.update(SharedPrefUtils.getServerId(preferences), ContentType.APP.name(), MiscUtils.calculateAppHash(packageName, Hashing.sipHash24()), ContentType.State.VISIBLE.name(), visible).execute();
            } catch (IOException e) {
                e.printStackTrace();
                failed = true; //mark failed
            }
            return failed;
        }

        @Override
        protected void onPostExecute(Boolean failed) {
            super.onPostExecute(failed);
            if (failed) {
                //reset in memory
                synchronized (packageVisibility) {
                    packageVisibility.put(packageName, !visible);
                }
                //reset in disk
                SharedPrefUtils.addPackageVisibility(preferences, packageName, !visible);
                //notify that visibility has changed
                Log.i("Ayush", "Server update failed for app");
                //visibilityChanged(null);
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
            }
        }
    }



    @Override
    public int getItemCount() {
        int size = super.getItemCount();
        return size;
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

        holder.menuData.setPosition(holder.getAdapterPosition());
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

    public synchronized void visibilityChanged(String packageName) {
        final List<App> recentApps = getMessageList();

        int position = -1;
        for (int index = 0; index < recentApps.size(); index++)
            if (recentApps.get(index).packageName.equals(packageName)) {
                position = index;
                break;
            }

        //recent adapter might not contain everything, as is limited to 4
        //if (position < getItemCount())
            notifyItemChanged(position);

        final RecyclerView.Adapter adapter;
        //if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
            //adapter.notifyItemChanged(position); //position will be same
    }

    @Override
    public void close() {
        getMessageList().clear();
    }

    public boolean isVisible(String packageName) {
        return packageVisibility.containsKey(packageName) ? packageVisibility.get(packageName) : true;
        //return packageVisibility.containsKey(packageName) && packageVisibility.get(packageName);
    }


    public interface VisibilityHook {

        boolean isVisible(String packageName);

        void handOverAppVisibility(String packageName);
    }


   }
