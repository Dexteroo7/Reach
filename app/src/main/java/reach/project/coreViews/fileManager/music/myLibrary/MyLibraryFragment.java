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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.coreViews.myProfile.EmptyRecyclerView;
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

    private EmptyRecyclerView mRecyclerView;

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

        final View rootView = inflater.inflate(R.layout.fragment_filemanager_music_mylibrary, container, false);
        mRecyclerView = (EmptyRecyclerView) rootView.findViewById(R.id.recyclerView);
        final Context context = mRecyclerView.getContext();
        final TextView emptyViewText = (TextView) rootView.findViewById(R.id.empty_textView);
        emptyViewText.setText(StaticData.NO_SONGS_TEXT);
        mRecyclerView.setEmptyView(rootView.findViewById(R.id.empty_imageView));
        parentAdapter = new ParentAdapter(this, this, context);
        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(context));
        mRecyclerView.setAdapter(parentAdapter);
        //mRecyclerView.setEmptyView();

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

    // The song is being played here
    @Override
    public void handOverMessage(@Nonnull Object message) {
        // Cursor is used for full list songs
        if (message instanceof Cursor) {

            final Cursor cursor = (Cursor) message;
            final int count = cursor.getColumnCount();

            // To play songs of the user (not the downloaded ones)
            if (count == MySongsHelper.DISK_LIST.length) {

                final MusicData musicData = MySongsHelper.getMusicData(cursor, userId);
                MiscUtils.playSong(musicData, getContext());

            }
            //To play the songs downloaded from reach
            else if (count == ReachDatabaseHelper.MUSIC_DATA_LIST.length) {

                final MusicData musicData = ReachDatabaseHelper.getMusicData(cursor);
                MiscUtils.playSong(musicData, getContext());
            } else
                throw new IllegalArgumentException("Unknown column count found");
        // Music Data is used for recent list songs
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
                    ReachDatabaseProvider.CONTENT_URI,
                    ReachDatabaseHelper.MUSIC_DATA_LIST,
                    ReachDatabaseHelper.COLUMN_STATUS + " = ? and " + //show only finished
                            ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
                    new String[]{ReachDatabase.FINISHED + "", "0"},
                    ReachDatabaseHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE");

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
        mRecyclerView.checkIfEmpty(parentAdapter.getItemCount());
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

        final Cursor cursor = getContext().getContentResolver().query(ReachDatabaseProvider.CONTENT_URI,
                ReachDatabaseHelper.MUSIC_DATA_LIST,
                ReachDatabaseHelper.COLUMN_STATUS + " = ? and " + //show only finished
                        ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
                new String[]{ReachDatabase.FINISHED + "", "0"},
                ReachDatabaseHelper.COLUMN_DATE_ADDED + " DESC, " +
                        ReachDatabaseHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC LIMIT 20"); //top 20

        if (cursor == null)
            return Collections.emptyList();

        final List<MusicData> latestDownloaded = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext())
            latestDownloaded.add(ReachDatabaseHelper.getMusicData(cursor));

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
