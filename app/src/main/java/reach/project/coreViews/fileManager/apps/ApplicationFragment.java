package reach.project.coreViews.fileManager.apps;

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

    @Nullable
    private static WeakReference<ApplicationFragment> reference = null;
    private static long userId = 0;

    public static ApplicationFragment getInstance(String header) {

        final Bundle args;
        ApplicationFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new ApplicationFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing ApplicationFragment object :)");
            args = fragment.getArguments();
        }
        args.putString("header", header);
        return fragment;
    }

    private final ParentAdapter parentAdapter = new ParentAdapter(this);
    private final ExecutorService applicationsFetcher = Executors.newSingleThreadExecutor();

    public void handOverMessage(@Nonnull App message) {
        MiscUtils.openApp(getContext(), message.packageName);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_simple_recycler, container, false);
        final RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        final Activity activity = getActivity();

        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(activity));
        mRecyclerView.setAdapter(parentAdapter);
        final SharedPreferences preferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        userId = SharedPrefUtils.getServerId(preferences);

        new GetApplications().executeOnExecutor(applicationsFetcher, activity);

        return rootView;
    }

    private static final class GetApplications extends AsyncTask<Context, Void, Pair<List<App>, List<App>>> {

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

            MiscUtils.useFragment(reference, fragment -> {

                fragment.parentAdapter.updateAllAppCount(pair.first);
                fragment.parentAdapter.updateRecentApps(pair.second);
            });
        }
    }
}