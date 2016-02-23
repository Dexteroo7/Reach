package reach.project.coreViews.fileManager.music.downloading;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.music.ReachDatabase;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 25/11/15.
 */
public class DownloadingFragment extends Fragment implements HandOverMessage<Cursor>, LoaderManager.LoaderCallbacks<Cursor> {

    public static DownloadingFragment getInstance(String header) {

        final Bundle args = new Bundle();
        args.putString("header", header);
        DownloadingFragment fragment = new DownloadingFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    private DownloadingAdapter downloadingAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_simple_recycler, container, false);
        final RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        final Context context = mRecyclerView.getContext();
        downloadingAdapter = new DownloadingAdapter(this, R.layout.downloading_card);

        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(context));
        mRecyclerView.setAdapter(downloadingAdapter);

        getLoaderManager().initLoader(StaticData.DOWNLOADING_LOADER, null, this);
        return rootView;
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
        getLoaderManager().destroyLoader(StaticData.DOWNLOADING_LOADER);
        if (downloadingAdapter != null)
            downloadingAdapter.close();
        downloadingAdapter = null;
    }

    @Override
    public void handOverMessage(@Nonnull Cursor cursor) {
        MiscUtils.playSong(SongHelper.getMusicData(cursor), getContext());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.DOWNLOADING_LOADER)
            return new CursorLoader(getActivity(),
                    SongProvider.CONTENT_URI,
                    SongHelper.MUSIC_DATA_LIST,
                    SongHelper.COLUMN_STATUS + " != ? and " + //show only non finished
                            SongHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
                    new String[]{ReachDatabase.FINISHED + "", "0"},
                    SongHelper.COLUMN_DATE_ADDED + " DESC");

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed())
            return;

        if (loader.getId() == StaticData.DOWNLOADING_LOADER && downloadingAdapter != null) {

//            Log.i("Ayush", "Setting new cursor " + data.getCount());
            downloadingAdapter.setCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (loader.getId() == StaticData.DOWNLOADING_LOADER && downloadingAdapter != null) {

//            Log.i("Ayush", "Invalidating downloading cursor");
            downloadingAdapter.setCursor(null);
        }
    }
}
