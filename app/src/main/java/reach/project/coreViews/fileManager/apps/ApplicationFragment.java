package reach.project.coreViews.fileManager.apps;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.appspot.able_door_616.contentStateApi.ContentStateApi;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.collect.Ordering;
import com.google.common.hash.Hashing;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.apps.App;
import reach.project.apps.AppCursorHelper;
import reach.project.core.StaticData;
import reach.project.utils.CloudEndPointsUtils;
import reach.project.utils.ContentType;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.ancillaryClasses.UseActivityWithResult;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 25/11/15.
 */
public class ApplicationFragment extends Fragment implements HandOverMessage<App> {

    private static final String TAG = ApplicationFragment.class.getSimpleName();
    static final Map<String, Boolean> PACKAGE_VISIBILITY = MiscUtils.getMap(100);

    public static ApplicationFragment getInstance(String header) {
        return new ApplicationFragment();
    }

    static boolean isVisible(String packageName) {

        final Boolean visibility = PACKAGE_VISIBILITY.get(packageName);
        return visibility != null && visibility;
    }

    private final ExecutorService visibilityToggler = Executors.newSingleThreadExecutor();

    @Nullable
    private ParentAdapter parentAdapter = null;
    @Nullable
    private ProgressBar loadingProgress = null;
    private long serverId = 0;

    private final View.OnClickListener EXTRA_CLICK = view -> {

        //noinspection unchecked
        final String packageName = ((AtomicReference<String>) view.getTag()).get();
        if (TextUtils.isEmpty(packageName))
            return;

        final Context context = view.getContext();
        final PopupMenu popupMenu = new PopupMenu(context, view);

        popupMenu.inflate(R.menu.app_manager_menu);
        popupMenu.setOnMenuItemClickListener(item -> {

            switch (item.getItemId()) {
                case R.id.hide:
                    //hide
                    visibilityToggler.submit(new ToggleVisibility(
                            packageName,
                            serverId,
                            new WeakReference<>(this)));
                    return true;
                case R.id.uninstall:
                    //uninstall
                    final Uri packageURI = Uri.parse("package:" + packageName);
                    final Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                    context.startActivity(uninstallIntent);
                    //TODO update apps list, when SQL made
                    return true;
                default:
                    return false;
            }
        });

        final MenuItem hideItem = popupMenu.getMenu().findItem(R.id.hide);
        hideItem.setChecked(isVisible(packageName));
        popupMenu.show();
    };

    public void handOverMessage(@Nonnull App message) {
        MiscUtils.openApp(getContext(), message.packageName);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        final Context context = getContext();
        final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);

        serverId = SharedPrefUtils.getServerId(preferences);
        parentAdapter = new ParentAdapter(this, context, EXTRA_CLICK);
        new GetApplications(this).executeOnExecutor(MiscUtils.getRejectionExecutor());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_myprofile_app, container, false);
        final RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        loadingProgress = (ProgressBar) rootView.findViewById(R.id.loadingProgress);
        loadingProgress.setVisibility(View.VISIBLE);

        rootView.setPadding(MiscUtils.dpToPx(10), MiscUtils.dpToPx(2), MiscUtils.dpToPx(10), 0);
        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(mRecyclerView.getContext()));
        mRecyclerView.setAdapter(parentAdapter);

        return rootView;
    }

    private static final class GetApplications extends AsyncTask<Void, Void, Pair<List<App>, List<App>>> {

        private WeakReference<ApplicationFragment> reference;

        public GetApplications(ApplicationFragment applicationFragment) {
            this.reference = new WeakReference<>(applicationFragment);
        }

        @Override
        protected final Pair<List<App>, List<App>> doInBackground(Void... params) {

            final List<App> appList = MiscUtils.useContextFromFragment(reference, (UseActivityWithResult<Activity, List<App>>) context -> {

                final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
                final PackageManager packageManager = context.getPackageManager();
                PACKAGE_VISIBILITY.putAll(SharedPrefUtils.getPackageVisibilities(preferences));

                final List<ApplicationInfo> applicationInfoList = MiscUtils.getInstalledApps(packageManager);
                if (applicationInfoList == null || applicationInfoList.isEmpty())
                    return Collections.emptyList();

                return AppCursorHelper.getApps(
                        applicationInfoList,
                        packageManager,
                        PACKAGE_VISIBILITY);
            }).or(Collections.emptyList());

            if (appList == null || appList.isEmpty())
                return new Pair<>(Collections.emptyList(), Collections.emptyList());

            //sort by name
            Collections.sort(appList, StaticData.byName);

            final List<App> recentApps = Ordering
                    .from(StaticData.byInstallDate)
                    .compound(StaticData.byName)
                    .greatestOf(appList, 20);

            return new Pair<>(appList, recentApps);
        }

        @Override
        protected void onPostExecute(Pair<List<App>, List<App>> pair) {

            super.onPostExecute(pair);

            StaticData.appsCount = pair.first.size();

            MiscUtils.useFragment(reference, fragment -> {

                if (fragment.loadingProgress != null)
                    fragment.loadingProgress.setVisibility(View.GONE);
                if (fragment.parentAdapter != null) {
                    fragment.parentAdapter.updateAllApps(pair.first);
                    fragment.parentAdapter.updateRecentApps(pair.second);
                }
            });
        }
    }

    private static final class ToggleVisibility implements Runnable {

        private final String packageName;
        private final long serverId;
        private final WeakReference<ApplicationFragment> reference;

        public ToggleVisibility(String packageName,
                                long serverId,
                                WeakReference<ApplicationFragment> reference) {

            this.packageName = packageName;
            this.serverId = serverId;
            this.reference = reference;
        }

        @Override
        public void run() {

            final boolean newVisibility = !isVisible(packageName);
            final HttpTransport transport = new NetHttpTransport();
            final JsonFactory factory = new JacksonFactory();

            final GoogleAccountCredential credential = MiscUtils.useContextFromFragment(reference, context -> {

                final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
                synchronized (PACKAGE_VISIBILITY) {
                    //update in memory
                    PACKAGE_VISIBILITY.put(packageName, newVisibility);
                    //update in disk, over-write is faster
                    SharedPrefUtils.overWritePackageVisibility(preferences, PACKAGE_VISIBILITY);
                }

                return GoogleAccountCredential
                        .usingAudience(context, StaticData.SCOPE)
                        .setSelectedAccountName(SharedPrefUtils.getEmailId(preferences));
            }).orNull();

            //update the adapter
            MiscUtils.useFragment(reference, fragment -> {
                if (fragment.parentAdapter != null)
                    fragment.parentAdapter.visibilityChanged(packageName);
            });

            final ContentStateApi contentStateApi = CloudEndPointsUtils.updateBuilder(new ContentStateApi.Builder(transport, factory, credential))
                    .setRootUrl("https://1-dot-client-module-dot-able-door-616.appspot.com/_ah/api/").build();

            boolean failed = false;
            try {
                contentStateApi.update(
                        serverId,
                        ContentType.APP.name(),
                        MiscUtils.calculateAppHash(packageName, Hashing.sipHash24()),
                        ContentType.State.VISIBLE.name(),
                        newVisibility).execute();

            } catch (IOException e) {
                e.printStackTrace();
                failed = true; //mark failed
            }

            if (failed) {

                //reset the visibility
                MiscUtils.useContextFromFragment(reference, context -> {

                    final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
                    synchronized (PACKAGE_VISIBILITY) {
                        //update in memory
                        PACKAGE_VISIBILITY.put(packageName, !newVisibility);
                        //update in disk, over-write is faster
                        SharedPrefUtils.overWritePackageVisibility(preferences, PACKAGE_VISIBILITY);
                    }
                });

                //update the adapter
                MiscUtils.runOnUiThreadFragment(reference, (context, fragment) -> {
                    Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
                    if (fragment.parentAdapter != null)
                        fragment.parentAdapter.visibilityChanged(packageName);
                });

                Log.i("Ayush", "Server update failed for app");
            }
        }
    }
}