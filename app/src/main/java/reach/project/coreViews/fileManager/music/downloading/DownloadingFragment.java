package reach.project.coreViews.fileManager.music.downloading;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 25/11/15.
 */
public class DownloadingFragment extends Fragment implements HandOverMessage<Cursor>,
        LoaderManager.LoaderCallbacks<Cursor> {

    @Nullable
    private static WeakReference<DownloadingFragment> reference = null;
//    private static long userId = 0;

    public static DownloadingFragment getInstance(String header) {

        final Bundle args;
        DownloadingFragment fragment;
        if (reference == null || (fragment = reference.get()) == null || MiscUtils.isFragmentDead(fragment)) {
            reference = new WeakReference<>(fragment = new DownloadingFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing DownloadingFragment object :)");
            args = fragment.getArguments();
        }
        args.putString("header", header);
        return fragment;
    }

    private final DownloadingAdapter downloadingAdapter = new DownloadingAdapter(this, R.layout.downloading_card);

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_simple_recycler, container, false);
        final RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        final Activity activity = getActivity();

        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(activity));
        mRecyclerView.setAdapter(downloadingAdapter);

//        final SharedPreferences preferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
//        userId = SharedPrefUtils.getServerId(preferences);

        getLoaderManager().initLoader(StaticData.DOWNLOADING_LOADER, null, this);

        return rootView;
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
        getLoaderManager().destroyLoader(StaticData.DOWNLOADING_LOADER);
        downloadingAdapter.close();
    }

    @Override
    public void handOverMessage(@Nonnull Cursor cursor) {
        MiscUtils.playSong(ReachDatabaseHelper.getMusicData(cursor), getContext());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.DOWNLOADING_LOADER)
            return new CursorLoader(getActivity(),
                    ReachDatabaseProvider.CONTENT_URI,
                    ReachDatabaseHelper.MUSIC_DATA_LIST,
                    ReachDatabaseHelper.COLUMN_STATUS + " != ? and " + //show only non finished
                            ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
                    new String[]{ReachDatabase.FINISHED + "", "0"},
                    ReachDatabaseHelper.COLUMN_DATE_ADDED + " DESC");

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed())
            return;

        if (loader.getId() == StaticData.DOWNLOADING_LOADER) {

//            Log.i("Ayush", "Setting new cursor " + data.getCount());
            downloadingAdapter.setCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (loader.getId() == StaticData.DOWNLOADING_LOADER) {

//            Log.i("Ayush", "Invalidating downloading cursor");
            downloadingAdapter.setCursor(null);
        }
    }
}
