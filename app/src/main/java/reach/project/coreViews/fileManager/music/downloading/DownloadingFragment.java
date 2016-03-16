package reach.project.coreViews.fileManager.music.downloading;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.music.ReachDatabase;
import reach.project.music.Song;
import reach.project.music.SongCursorHelper;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.coreViews.myProfile.EmptyRecyclerView;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 25/11/15.
 */
public class DownloadingFragment extends Fragment implements HandOverMessage<Cursor>, LoaderManager.LoaderCallbacks<Cursor> {

    private static final String NO_DOWNLOADS_TEXT = "No current\ndownloads!";
    private EmptyRecyclerView mRecyclerView;

    public static DownloadingFragment getInstance(String header) {

        final Bundle args = new Bundle();
        args.putString("header", header);
        DownloadingFragment fragment = new DownloadingFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    private DownloadingAdapter downloadingAdapter = null;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_mylibrary, container, false);
         mRecyclerView = (EmptyRecyclerView) rootView.findViewById(R.id.recyclerView);
        final Context context = mRecyclerView.getContext();
        downloadingAdapter = new DownloadingAdapter(this, R.layout.downloading_card);
        final TextView emptyViewText = (TextView) rootView.findViewById(R.id.empty_textView);
        emptyViewText.setText(NO_DOWNLOADS_TEXT);
        mRecyclerView.setEmptyView(rootView.findViewById(R.id.empty_imageView));

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

        final Song song = SongCursorHelper.DOWNLOADING_TO_SONG_HELPER.parse(cursor);
        MiscUtils.playSong(song, getContext());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.DOWNLOADING_LOADER)
            return new CursorLoader(getActivity(),
                    SongProvider.CONTENT_URI,
                    SongCursorHelper.DOWNLOADING_HELPER.getProjection(),
                    SongHelper.COLUMN_STATUS + " != ? and " + //show only non finished
                            SongHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
                    new String[]{ReachDatabase.Status.FINISHED.getString(),
                            ReachDatabase.OperationKind.DOWNLOAD_OP.getString()},
                    SongHelper.COLUMN_DATE_ADDED + " DESC");

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed() || downloadingAdapter == null)
            return;

        if (loader.getId() == StaticData.DOWNLOADING_LOADER) {

//            Log.i("Ayush", "Setting new cursor " + data.getCount());
            downloadingAdapter.setCursor(data);
        }
        mRecyclerView.checkIfEmpty(downloadingAdapter.getItemCount());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (loader.getId() == StaticData.DOWNLOADING_LOADER && downloadingAdapter != null) {

//            Log.i("Ayush", "Invalidating downloading cursor");
            downloadingAdapter.setCursor(null);
        }
    }
}
