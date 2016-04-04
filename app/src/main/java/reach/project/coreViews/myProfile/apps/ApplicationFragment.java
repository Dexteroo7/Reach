package reach.project.coreViews.myProfile.apps;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.florent37.materialviewpager.MaterialViewPagerHelper;
import com.google.common.collect.Ordering;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.apps.App;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.coreViews.myProfile.MyProfileFragment;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 25/11/15.
 */
public class ApplicationFragment extends Fragment implements HandOverMessage<App> {

    private static final String TAG = "MyProfile/" + ApplicationFragment.class.getSimpleName();
    private static long userId = 0;
    private ProgressBar mLoadingProgress;

    public static ApplicationFragment getInstance(String header) {

        final Bundle args = new Bundle();
        args.putString("header", header);
        ApplicationFragment fragment = new ApplicationFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    private SharedPreferences preferences = null;
    @Nullable
    private ParentAdapter parentAdapter;

    //handle 2 at a time
    private final ExecutorService visibilityHandler = Executors.unconfigurableExecutorService(Executors.newFixedThreadPool(2));

    public void handOverMessage(@Nonnull App message) {

        try {
            if (preferences == null || parentAdapter == null)
                return;

            final String packageName = message.packageName;
            final boolean newVisibility = !parentAdapter.isVisible(packageName);

            //update in memory
            synchronized (parentAdapter.packageVisibility) {
                parentAdapter.packageVisibility.put(packageName, newVisibility);
            }
            //update in disk
            SharedPrefUtils.addPackageVisibility(preferences, packageName, newVisibility);
            //update on server
            new ToggleVisibility(this).executeOnExecutor(visibilityHandler, userId, packageName, newVisibility);

            //notify that visibility has changed
            parentAdapter.visibilityChanged(packageName);
//        Toast.makeText(getContext(), "Clicked on " + message.applicationName, Toast.LENGTH_SHORT).show();

        }
        catch (Exception e){
            Log.e(TAG, "Unidentified error in myprofile -> apps, Exception = " + e.toString() );
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_myprofile_app, container, false);
        final RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        final Activity activity = getActivity();

        parentAdapter = new ParentAdapter(this, activity);
        //update the package visibilities
        preferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        parentAdapter.packageVisibility.putAll(SharedPrefUtils.getPackageVisibilities(preferences));

        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(activity));
        mRecyclerView.setAdapter(parentAdapter);
        MaterialViewPagerHelper.registerRecyclerView(activity, mRecyclerView, null);
        userId = SharedPrefUtils.getServerId(preferences);
        mLoadingProgress = (ProgressBar) rootView.findViewById(R.id.loadingProgress);
        //mLoadingProgress.setVisibility(View.VISIBLE);

        List<App> appData = null;
        if((appData = (List<App>) ReachApplication.readCachedFile(getActivity(),StaticData.APP_DATA_CACHE_KEY))==null){
            mLoadingProgress.setVisibility(View.VISIBLE);
            new GetApplications(this).executeOnExecutor(visibilityHandler, activity);
        }
        else{
            Log.d(TAG, "onCreateView: AppsData fetched from cache");
            final List<App> recentApps = Ordering
                    .from(StaticData.byInstallDate)
                    .compound(StaticData.byName)
                    .greatestOf(appData, 20);
            if(parentAdapter!=null) {
                parentAdapter.updateAllApps(appData);
                parentAdapter.updateRecentApps(recentApps);
            }

        }

        //new GetApplications(this).executeOnExecutor(visibilityHandler, activity);

        return rootView;
    }

    private static final class GetApplications extends AsyncTask<Context, Void, Pair<List<App>, List<App>>> {

        private WeakReference<ApplicationFragment> applicationFragmentWeakReference;

        public GetApplications(ApplicationFragment applicationFragment) {
            this.applicationFragmentWeakReference = new WeakReference<>(applicationFragment);
        }

        @Override
        protected Pair<List<App>, List<App>> doInBackground(Context... params) {

            final SharedPreferences preferences = params[0].getSharedPreferences("Reach", Context.MODE_PRIVATE);
            final PackageManager packageManager = params[0].getPackageManager();

            final List<App> apps = MiscUtils.getApplications(packageManager, preferences);
            final List<App> recentApps = Ordering
                    .from(StaticData.byInstallDate)
                    .compound(StaticData.byName)
                    .greatestOf(apps, 20);
            Collections.sort(apps, StaticData.byName);
            return new Pair<>(apps, recentApps);
        }

        @Override
        protected void onPostExecute(Pair<List<App>, List<App>> pair) {

            super.onPostExecute(pair);

            MiscUtils.useFragment(applicationFragmentWeakReference, fragment -> {

                fragment.mLoadingProgress.setVisibility(View.GONE);
                if (fragment.parentAdapter != null) {
                    fragment.parentAdapter.updateAllApps(pair.first);
                    fragment.parentAdapter.updateRecentApps(pair.second);
                }
            });

        }
    }

    private static class ToggleVisibility extends AsyncTask<Object, Void, Boolean> {

        private WeakReference<ApplicationFragment> applicationFragmentWeakReference;

        public ToggleVisibility(ApplicationFragment applicationFragment) {
            this.applicationFragmentWeakReference = new WeakReference<>(applicationFragment);
        }

        /**
         * params[0] = oldVisibility
         * params[1] = songId
         * params[2] = userId
         */

        @Override
        protected Boolean doInBackground(Object... params) {

            if (params == null || params.length != 3)
                throw new IllegalArgumentException("Necessary arguments not given");

            final long serverId;
            final String packageName;
            final boolean visibility;

            if (params[0] instanceof Long && params[1] instanceof String && params[2] instanceof Boolean) {
                serverId = (long) params[0];
                packageName = (String) params[1];
                visibility = (boolean) params[2];
            } else
                throw new IllegalArgumentException("Arguments not of expected type");

            boolean failed = false;
            //TODO
//            try {
//                final MyString response = StaticData.APP_VISIBILITY_API.update(
//                        serverId, //serverId
//                        packageName, //packageName
//                        visibility).execute(); //if 0 (false) make it true and vice-versa
//                if (response == null || TextUtils.isEmpty(response.getString()) || response.getString().equals("false"))
//                    failed = true; //mark failed
//            } catch (IOException e) {
//                e.printStackTrace();
//                failed = true; //mark failed
//            }

            //reset if failed
            if (failed)
                MiscUtils.useFragment(applicationFragmentWeakReference, fragment -> {

                    //reset in memory
                    if (fragment.parentAdapter != null) {
                        synchronized (fragment.parentAdapter.packageVisibility) {
                            fragment.parentAdapter.packageVisibility.put(packageName, !visibility);
                        }
                    }
                    //reset in disk
                    SharedPrefUtils.addPackageVisibility(fragment.preferences, packageName, !visibility);
                });

            return failed;
        }

        @Override
        protected void onPostExecute(Boolean failed) {

            super.onPostExecute(failed);
            if (failed)
                MiscUtils.useContextAndFragment(applicationFragmentWeakReference, (context, fragment) -> {

                    //notify that visibility has changed
                    Log.i("Ayush", "Server update failed for app");
                    if (fragment.parentAdapter != null) {
                        fragment.parentAdapter.visibilityChanged(null);
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }
}
