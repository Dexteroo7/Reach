package reach.project.coreViews.fileManager.music.myLibrary;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.music.ReachDatabase;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.music.MySongsHelper;
import reach.project.music.MySongsProvider;
import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 25/11/15.
 */
public class MyLibraryFragment extends Fragment implements HandOverMessage,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static long userId = 0;

    public static MyLibraryFragment getInstance(String header) {

        final Bundle args = new Bundle();
        args.putString("header", header);
        MyLibraryFragment fragment = new MyLibraryFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    private ParentAdapter parentAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_mylibrary, container, false);
        final RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        final Context context = mRecyclerView.getContext();

        parentAdapter = new ParentAdapter(this, this);
        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(context));
        mRecyclerView.setAdapter(parentAdapter);

        final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        userId = SharedPrefUtils.getServerId(preferences);

        getLoaderManager().initLoader(StaticData.DOWNLOAD_LOADER, null, this);
        getLoaderManager().initLoader(StaticData.MY_LIBRARY_LOADER, null, this);

        return rootView;
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
        getLoaderManager().destroyLoader(StaticData.DOWNLOAD_LOADER);
        getLoaderManager().destroyLoader(StaticData.MY_LIBRARY_LOADER);
        if (parentAdapter != null)
            parentAdapter.close();
    }

    @Override
    public void handOverMessage(@Nonnull Object message) {

        if (message instanceof Cursor) {

            final Cursor cursor = (Cursor) message;
            final int count = cursor.getColumnCount();
            if (count == MySongsHelper.DISK_LIST.length) {

                final MusicData musicData = MySongsHelper.getMusicData(cursor, userId);
                MiscUtils.playSong(musicData, getContext());
            } else if (count == SongHelper.MUSIC_DATA_LIST.length) {

                final MusicData musicData = SongHelper.getMusicData(cursor);
                MiscUtils.playSong(musicData, getContext());
            } else
                throw new IllegalArgumentException("Unknown column count found");

        } else if (message instanceof MusicData) {
            MiscUtils.playSong((MusicData) message, getContext());
        } else
            throw new IllegalArgumentException("Unknown type handed over");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.MY_LIBRARY_LOADER)
            return new CursorLoader(getActivity(),
                    MySongsProvider.CONTENT_URI,
                    MySongsHelper.DISK_LIST,
                    null, null,
                    MySongsHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE"); //show all songs !
        else if (id == StaticData.DOWNLOAD_LOADER)
            return new CursorLoader(getActivity(),
                    SongProvider.CONTENT_URI,
                    SongHelper.MUSIC_DATA_LIST,
                    SongHelper.COLUMN_STATUS + " = ? and " + //show only finished
                            SongHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
                    new String[]{ReachDatabase.FINISHED + "", "0"},
                    SongHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE");

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed() || parentAdapter == null)
            return;

        final int count = data.getCount();
        if (loader.getId() == StaticData.MY_LIBRARY_LOADER) {

//            Log.i("Ayush", "MyLibrary file manager " + count);

            parentAdapter.setNewMyLibraryCursor(data);
            if (count != parentAdapter.myLibraryCount) //update only if count has changed
                parentAdapter.updateRecentMusic(getRecentMyLibrary());


        } else if (loader.getId() == StaticData.DOWNLOAD_LOADER) {

//            Log.i("Ayush", "Downloaded file manager " + count);

            parentAdapter.setNewDownLoadCursor(data);
            if (count != parentAdapter.downloadedCount) //update only if count has changed
                parentAdapter.updateRecentMusic(getRecentDownloaded());

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (parentAdapter == null)
            return;

        if (loader.getId() == StaticData.MY_LIBRARY_LOADER)
            parentAdapter.setNewMyLibraryCursor(null);
        else if (loader.getId() == StaticData.DOWNLOAD_LOADER)
            parentAdapter.setNewDownLoadCursor(null);
    }

    @NonNull
    private List<MusicData> getRecentDownloaded() {

        final Cursor cursor = getContext().getContentResolver().query(SongProvider.CONTENT_URI,
                SongHelper.MUSIC_DATA_LIST,
                SongHelper.COLUMN_STATUS + " = ? and " + //show only finished
                        SongHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
                new String[]{ReachDatabase.FINISHED + "", "0"},
                SongHelper.COLUMN_DATE_ADDED + " DESC, " +
                        SongHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC LIMIT 20"); //top 20

        if (cursor == null)
            return Collections.emptyList();

        final List<MusicData> latestDownloaded = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext())
            latestDownloaded.add(SongHelper.getMusicData(cursor));

        cursor.close();

        return latestDownloaded;
    }

    @NonNull
    private List<MusicData> getRecentMyLibrary() {

        final Cursor cursor = getContext().getContentResolver().query(MySongsProvider.CONTENT_URI,
                MySongsHelper.DISK_LIST,
                null, null, //all songs
                MySongsHelper.COLUMN_DATE_ADDED + " DESC, " +
                        MySongsHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC LIMIT 20");

        if (cursor == null)
            return Collections.emptyList();

        final List<MusicData> latestMyLibrary = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext())
            latestMyLibrary.add(MySongsHelper.getMusicData(cursor, userId));

        cursor.close();

        return latestMyLibrary;
    }
}
