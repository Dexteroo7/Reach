package reach.project.coreViews.yourProfile.apps;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.appspot.able_door_616.userApi.UserApi;
import com.appspot.able_door_616.userApi.model.SimpleApp;
import com.github.florent37.materialviewpager.MaterialViewPagerHelper;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Optional;
import com.squareup.wire.Message;
import com.squareup.wire.Wire;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import reach.project.R;
import reach.project.apps.App;
import reach.project.core.StaticData;
import reach.project.coreViews.yourProfile.blobCache.Cache;
import reach.project.coreViews.yourProfile.blobCache.CacheAdapterInterface;
import reach.project.coreViews.yourProfile.blobCache.CacheInjectorCallbacks;
import reach.project.coreViews.yourProfile.blobCache.CacheType;
import reach.project.utils.CloudEndPointsUtils;
import reach.project.utils.CloudStorageUtils;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;

/**
 * Created by dexter on 15/11/15.
 */
public class YourProfileAppFragment extends Fragment implements CacheInjectorCallbacks<Message>, CacheAdapterInterface<Message, App> {

    @Nullable
    private static WeakReference<YourProfileAppFragment> reference = null;
    private static long hostId = 0;
    private ProgressBar mLoadingProgress;
    private String TAG;
    //private ProgressBar mLoadingView;

    public static YourProfileAppFragment newInstance(long hostId) {

        final Bundle args;
        YourProfileAppFragment fragment;
        reference = new WeakReference<>(fragment = new YourProfileAppFragment());
        fragment.setArguments(args = new Bundle());
        args.putLong("hostId", hostId);
        return fragment;
    }

    private final List<Message> appData = new ArrayList<>(100);
    private final ExecutorService appUpdaterService = MiscUtils.getRejectionExecutor();

    @Nullable
    private Cache fullListCache = null, smartListCache = null, recentAppCache = null;
    private int lastPosition = 0;

    @Override
    public void onDestroyView() {

        super.onDestroyView();
        hostId = 0;

        MiscUtils.closeQuietly(fullListCache, smartListCache, recentAppCache);
        fullListCache = smartListCache = recentAppCache = null;
        appData.clear();
    }

    @Nullable
    private ParentAdapter parentAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        hostId = getArguments().getLong("hostId", 0L);

        fullListCache = new Cache(this, CacheType.APPLICATIONS_FULL_LIST, hostId) {
            @Override
            protected Callable<List<? extends Message>> fetchFromNetwork() {

                return () -> CloudStorageUtils.fetchApps(hostId, new WeakReference<>(getContext()));
            }

            @Override
            protected Message getItem(byte[] source, int offset, int count) throws IOException {
                return new Wire(App.class).parseFrom(source, offset, count, App.class);
            }
        };

        recentAppCache = new Cache(this, CacheType.APPLICATIONS_RECENT_LIST, hostId) {
            @Override
            protected Callable<List<? extends Message>> fetchFromNetwork() {
                return getRecent;
            }

            @Override
            protected Message getItem(byte[] source, int offset, int count) throws IOException {
                return new Wire(RecentApps.class).parseFrom(source, offset, count, RecentApps.class);
            }
        };

        smartListCache = new Cache(this, CacheType.APPLICATIONS_SMART_LIST, hostId) {
            @Override
            protected Callable<List<? extends Message>> fetchFromNetwork() {
                return getSmart;
            }

            @Override
            protected Message getItem(byte[] source, int offset, int count) throws IOException {
                return new Wire(SmartApps.class).parseFrom(source, offset, count, SmartApps.class);
            }
        };

        final View rootView = inflater.inflate(R.layout.fragment_myprofile_app, container, false);
        //rootView.setPadding(MiscUtils.dpToPx(10),MiscUtils.dpToPx(32), MiscUtils.dpToPx(10),0 );
        final RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        final Activity activity = getActivity();
        // TODO: Comment out when implementing loading view. currently there is no callback to detect that the app data has been loaded
        /*mLoadingProgress = (ProgressBar) rootView.findViewById(R.id.loadingProgress);
        FrameLayout.LayoutParams progressBarMarginParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,FrameLayout.LayoutParams.WRAP_CONTENT);
        progressBarMarginParams.gravity = Gravity.CENTER;
        progressBarMarginParams.setMargins(0,MiscUtils.dpToPx(240),0,0);
        mLoadingProgress.setLayoutParams(progressBarMarginParams);
        mLoadingProgress.setVisibility(View.VISIBLE);*/
        parentAdapter = new ParentAdapter<>(this);
        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(activity));
        mRecyclerView.setAdapter(parentAdapter);
        MaterialViewPagerHelper.registerRecyclerView(activity, mRecyclerView, null);

        //update music
        appUpdaterService.submit(appUpdater);
        return rootView;
    }

    @Override
    public int getItemCount() {

        final int size = appData.size();
        if (size == 0) {
            if (recentAppCache != null)
                recentAppCache.loadMoreElements(true);
            if (fullListCache != null)
                fullListCache.loadMoreElements(false);
        }

        return size;
    }

    @Override
    public Message getItem(int position) {

        if (position > lastPosition)
            lastPosition = position;

        final int currentSize = appData.size();

        //if reaching end of story and are not done yet
        if (position > currentSize - 5 && fullListCache != null) {
            //request a partial load
            fullListCache.loadMoreElements(false);
        }

        return appData.get(position);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "OnPause called static reference is null now");
        reference = null;
        super.onPause();

    }


    @Override
    public void onResume() {
        Log.d(TAG, "OnResume called");
        if(reference == null){
            Log.d(TAG, "OnResume called static reference is being initialized with this");
            reference = new WeakReference<>(this);
        }
        super.onResume();

    }

    @Override
    public void handOverMessage(@NonNull App item) {
        MiscUtils.openAppInPlayStore(getActivity(), item.packageName, hostId, "YOUR_PROFILE");
    }

    @Override
    public File getCacheDirectory() {
        return getContext().getCacheDir();
    }

    @Override
    public boolean verifyItemVisibility(Message item) {
        return true;
    }

    @Override
    public void injectElements(List<Message> elements, boolean overWrite, boolean loadingDone) {

        if (elements == null || elements.isEmpty())
            return;



        final Message typeCheckerInstance = elements.get(0);
        final Class typeChecker;
        if (typeCheckerInstance instanceof App)
            typeChecker = App.class;
        else if (typeCheckerInstance instanceof RecentApps)
            typeChecker = RecentApps.class;
        else if (typeCheckerInstance instanceof SmartApps)
            typeChecker = SmartApps.class;
        else
            return;

        Log.i("Ayush", "Inserting " + elements.size() + " new items " + typeChecker.getName());

        ///////////

        if (overWrite)
            intelligentOverwrite(elements, typeChecker);
        if (!elements.isEmpty())
            painter(elements, typeChecker);

        //notify
        Log.i("Ayush", "Reloading list " + appData.size());
        if (parentAdapter != null)
            parentAdapter.notifyDataSetChanged();
        //TODO: Uncomment when needed
        //mLoadingView.setVisibility(View.GONE);


        /**
         * If loading has finished request a full injection of smart lists
         * Else request partial injection
         */
        if (typeChecker == App.class && smartListCache != null)
            smartListCache.loadMoreElements(true);
    }

    private void intelligentOverwrite(List<? extends Message> elements, Class typeChecker) {

        //nothing to overwrite
        if (appData.isEmpty())
            return;

        final Iterator<? extends Message> messageIterator = elements.iterator();
        int updatedSize = appData.size();
        int index;

        for (index = 0; index < updatedSize; index++) {

            //ignore if element is not of same class type
            if (!appData.get(index).getClass().equals(typeChecker))
                continue;

            //get the next message to overwrite if present
            if (messageIterator.hasNext()) {

                //we have a message to overwrite, do it
                synchronized (appData) {
                    appData.set(index, messageIterator.next());
                }
                messageIterator.remove(); //must remove

            } else {

                synchronized (appData) {
                    appData.remove(index);//remove as this item is no longer valid
                }
                updatedSize--; //update iteration size
            }
        }
    }

    private void painter(List<? extends Message> elements, Class typeChecker) {

        if (appData.isEmpty())
            synchronized (appData) {
                appData.addAll(elements);
            }

        else if (typeChecker == RecentApps.class)

            synchronized (appData) {
                appData.addAll(0, elements);
            }

        else if (typeChecker == SmartApps.class) {

            final int size = appData.size();
            if (lastPosition > size)
                lastPosition = size;
            synchronized (appData) {
                appData.addAll(lastPosition, elements);
            }

        } else if (typeChecker == App.class)

            synchronized (appData) {
                appData.addAll(elements);
            }
    }

    private static final Callable<List<? extends Message>> getRecent = () -> {

        final UserApi userApi = MiscUtils.useContextFromFragment(reference, activity -> {

            final SharedPreferences preferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
            final HttpTransport transport = new NetHttpTransport();
            final JsonFactory factory = new JacksonFactory();
            final GoogleAccountCredential credential = GoogleAccountCredential
                    .usingAudience(activity, StaticData.SCOPE)
                    .setSelectedAccountName(SharedPrefUtils.getEmailId(preferences));
            Log.d("CodeVerification", credential.getSelectedAccountName());

            return CloudEndPointsUtils.updateBuilder(new UserApi.Builder(transport, factory, credential))
                    .setRootUrl("https://1-dot-client-module-dot-able-door-616.appspot.com/_ah/api/").build();
        }).orNull();

        final List<SimpleApp> simpleApps;
        if (userApi == null)
            simpleApps = Collections.emptyList();
        else
            simpleApps = MiscUtils.autoRetry(() -> userApi.fetchRecentApps(hostId).execute().getItems(), Optional.absent()).or(Collections.emptyList());

        final List<App> toReturn = new ArrayList<>(simpleApps.size());
        for (SimpleApp simpleApp : simpleApps) {

            final App.Builder appBuilder = new App.Builder();

            appBuilder.applicationName(simpleApp.getApplicationName());
            appBuilder.visible(simpleApp.getVisible());
            appBuilder.installDate(simpleApp.getInstallDate());
            appBuilder.description(simpleApp.getDescription());
            appBuilder.launchIntentFound(simpleApp.getLaunchIntentFound());
            appBuilder.packageName(simpleApp.getPackageName());
            appBuilder.processName(simpleApp.getProcessName());
            toReturn.add(appBuilder.build());
        }

        final RecentApps.Builder recentBuilder = new RecentApps.Builder();
        recentBuilder.title("Recently Added");
        recentBuilder.appList(toReturn);

        return Arrays.asList(recentBuilder.build());
    };

    private static final Callable<List<? extends Message>> getSmart = Collections::emptyList;

    private static final Runnable appUpdater = () -> {

        final String localHash = MiscUtils.useContextFromFragment(reference, context -> {
            return SharedPrefUtils.getCloudStorageFileHash(
                    context.getSharedPreferences("Reach", Context.MODE_PRIVATE),
                    MiscUtils.getAppStorageKey(hostId));
        }).or("");

        final InputStream keyStream = MiscUtils.useContextFromFragment(reference, context -> {
            try {
                return context.getAssets().open("key.p12");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }).orNull();

        if (TextUtils.isEmpty(localHash) || keyStream == null)
            return; //dead

        if (CloudStorageUtils.isNewAppAvailable(hostId, keyStream, localHash)) {

            //new music available, invalidate everything
            MiscUtils.useFragment(reference, fragment -> {

                if (fragment.fullListCache != null) {
                    fragment.fullListCache.invalidateCache();
                    fragment.fullListCache.loadMoreElements(true);
                }
                if (fragment.smartListCache != null) {
                    fragment.smartListCache.invalidateCache();
                    fragment.smartListCache.loadMoreElements(true);
                }
                if (fragment.recentAppCache != null) {
                    fragment.recentAppCache.invalidateCache();
                    fragment.recentAppCache.loadMoreElements(true);
                }
            });
        }
    };
}