package reach.project.coreViews.fileManager.music.fragments;

import android.app.Activity;
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

import com.github.florent37.materialviewpager.MaterialViewPagerHelper;
import com.github.florent37.materialviewpager.adapter.RecyclerViewMaterialAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.coreViews.fileManager.music.adapters.ParentAdapter;
import reach.project.music.MySongsHelper;
import reach.project.music.MySongsProvider;
import reach.project.music.Song;
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

    private static WeakReference<MyLibraryFragment> reference = null;
    private static long userId = 0;

    public static MyLibraryFragment getInstance(String header) {

        final Bundle args;
        MyLibraryFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new MyLibraryFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing MyLibraryFragment object :)");
            args = fragment.getArguments();
        }
        args.putString("header", header);
        return fragment;
    }

    private ParentAdapter parentAdapter = new ParentAdapter(this, this);

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_simple_recycler, container, false);
        final RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        final Activity activity = getActivity();

        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(activity));
        mRecyclerView.setAdapter(new RecyclerViewMaterialAdapter(parentAdapter));
        MaterialViewPagerHelper.registerRecyclerView(activity, mRecyclerView, null);

        getLoaderManager().initLoader(StaticData.DOWNLOAD_LOADER, null, this);
        getLoaderManager().initLoader(StaticData.MY_LIBRARY_LOADER, null, this);

        final SharedPreferences preferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        userId = SharedPrefUtils.getServerId(preferences);
        return rootView;
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
        getLoaderManager().destroyLoader(StaticData.DOWNLOAD_LOADER);
        getLoaderManager().destroyLoader(StaticData.MY_LIBRARY_LOADER);
        parentAdapter.destroy();
    }

    @Override
    public void handOverMessage(@Nonnull Object message) {

        if (message instanceof Cursor) {

            final Cursor cursor = (Cursor) message;
            final int count = cursor.getColumnCount();
            if (count == MySongsHelper.DISK_LIST.length) {

                final MusicData musicData = MySongsHelper.getMusicData(cursor, userId);
                MiscUtils.playSong(musicData, getContext());
            } else if (count == ReachDatabaseHelper.ADAPTER_LIST.length) {

                final MusicData musicData = ReachDatabaseHelper.getMusicData(cursor);
                MiscUtils.playSong(musicData, getContext());
            } else
                throw new IllegalArgumentException("Unknown column count found");

        } else if (message instanceof Song) {

            //TODO

        } else
            throw new IllegalArgumentException("Unknown type handed over");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.MY_LIBRARY_LOADER)
            return new CursorLoader(getActivity(),
                    MySongsProvider.CONTENT_URI,
                    MySongsHelper.DISK_LIST,
                    null, null, null); //show all songs !
        else if (id == StaticData.DOWNLOAD_LOADER)
            return new CursorLoader(getActivity(),
                    ReachDatabaseProvider.CONTENT_URI,
                    ReachDatabaseHelper.ADAPTER_LIST,
                    ReachDatabaseHelper.COLUMN_STATUS + " = ? and " + //show only finished
                            ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
                    new String[]{ReachDatabase.FINISHED + "", "0"}, null);

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed())
            return;

        if (loader.getId() == StaticData.MY_LIBRARY_LOADER) {

            parentAdapter.setNewMyLibraryCursor(data);
            parentAdapter.updateRecentMusic(getRecentMyLibrary());

        } else if (loader.getId() == StaticData.DOWNLOAD_LOADER) {

            parentAdapter.setNewDownLoadCursor(data);
            parentAdapter.updateRecentMusic(getRecentDownloaded());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (loader.getId() == StaticData.MY_LIBRARY_LOADER)
            parentAdapter.setNewMyLibraryCursor(null);
        else if (loader.getId() == StaticData.DOWNLOAD_LOADER)
            parentAdapter.setNewDownLoadCursor(null);
    }

    @NonNull
    private List<Song> getRecentDownloaded() {

        final Cursor cursor = getContext().getContentResolver().query(ReachDatabaseProvider.CONTENT_URI,
                ReachDatabaseHelper.ADAPTER_LIST,
                ReachDatabaseHelper.COLUMN_STATUS + " = ? and " + //show only finished
                        ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?", //show only downloads
                new String[]{ReachDatabase.FINISHED + "", "0"},
                ReachDatabaseHelper.COLUMN_DATE_ADDED + " DESC LIMIT 20"); //top 20

        if (cursor == null || cursor.getCount() == 0)
            return Collections.emptyList();

        final List<Song> latestDownloaded = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {

            final Song.Builder songBuilder = new Song.Builder();
            songBuilder.actualName(cursor.getString(16));
            songBuilder.displayName(cursor.getString(5));
            songBuilder.album(cursor.getString(15));
            songBuilder.artist(cursor.getString(6));
            songBuilder.path(cursor.getString(4));

            songBuilder.songId(cursor.getLong(14));
            songBuilder.duration(cursor.getLong(8));
            songBuilder.size(cursor.getLong(1));
            songBuilder.dateAdded(cursor.getLong(17));

            latestDownloaded.add(songBuilder.build());
        }

        return latestDownloaded;
    }

    @NonNull
    private List<Song> getRecentMyLibrary() {

        final Cursor cursor = getContext().getContentResolver().query(MySongsProvider.CONTENT_URI,
                MySongsHelper.DISK_LIST,
                null, null, MySongsHelper.COLUMN_DATE_ADDED + " DESC LIMIT 20");

        if (cursor == null || cursor.getCount() == 0)
            return Collections.emptyList();

        final List<Song> latestDownloaded = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {

            final Song.Builder songBuilder = new Song.Builder();
            songBuilder.actualName(cursor.getString(9));
            songBuilder.displayName(cursor.getString(3));
            songBuilder.album(cursor.getString(6));
            songBuilder.artist(cursor.getString(4));
            songBuilder.path(cursor.getString(2));

            songBuilder.songId(cursor.getLong(0));
            songBuilder.duration(cursor.getLong(5));
            songBuilder.size(cursor.getLong(1));
            songBuilder.dateAdded(cursor.getLong(9));
            latestDownloaded.add(songBuilder.build());
        }

        return latestDownloaded;
    }
}