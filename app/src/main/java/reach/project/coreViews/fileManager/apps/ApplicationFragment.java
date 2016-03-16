package reach.project.coreViews.fileManager.apps;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.common.collect.Ordering;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.apps.App;
import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 25/11/15.
 */
public class ApplicationFragment extends Fragment implements HandOverMessage<App> {

    private static final String TAG = ApplicationFragment.class.getSimpleName();
    ProgressBar loadingProgress;

    public static ApplicationFragment getInstance(String header) {

        final Bundle args = new Bundle();
        args.putString("header", header);
        final ApplicationFragment fragment = new ApplicationFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Nullable
    ParentAdapter parentAdapter = null;
    @Nullable
    SharedPreferences preferences = null;

    private final ExecutorService applicationsFetcher = Executors.newSingleThreadExecutor();

    public void handOverMessage(@Nonnull App message) {
        MiscUtils.openApp(getContext(), message.packageName);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_myprofile_app, container, false);
        final RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        final Context context = mRecyclerView.getContext();
        loadingProgress = (ProgressBar) rootView.findViewById(R.id.loadingProgress);

        preferences =context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        parentAdapter = new ParentAdapter(this, context);
        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(context));
        mRecyclerView.setAdapter(parentAdapter);
        parentAdapter.packageVisibility.putAll(SharedPrefUtils.getPackageVisibilities(preferences));

        loadingProgress.setVisibility(View.VISIBLE);
        new GetApplications(this).executeOnExecutor(applicationsFetcher, context);

        return rootView;
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
        if (parentAdapter != null)
            parentAdapter.close();
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
            //if(StaticData.appsCount < pair.first.size()){
              //  MyProfileActivity.countChanged = true;
                StaticData.appsCount = pair.first.size();
            //}

            ApplicationFragment context = applicationFragmentWeakReference.get();
            if(context!=null){
                context.loadingProgress.setVisibility(View.GONE);
            }

            MiscUtils.useFragment(applicationFragmentWeakReference, fragment -> {
                if (fragment.parentAdapter != null) {
                    fragment.parentAdapter.updateAllAppCount(pair.first);
                    fragment.parentAdapter.updateRecentApps(pair.second);
                }


            });
        }
    }
}