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
import reach.project.coreViews.myProfile.EmptyRecyclerView;
import reach.project.music.MySongsHelper;
import reach.project.music.MySongsProvider;
import reach.project.music.ReachDatabase;
import reach.project.music.SongCursorHelper;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
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
        final Context context = rootView.getContext();
        final TextView emptyViewText = (TextView) rootView.findViewById(R.id.empty_textView);
        emptyViewText.setText("Dawg");

        mRecyclerView = (EmptyRecyclerView) rootView.findViewById(R.id.recyclerView);
        mRecyclerView.setEmptyView(rootView.findViewById(R.id.empty_imageView));
        parentAdapter = new ParentAdapter(this, this);
        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(context));
        mRecyclerView.setAdapter(parentAdapter);

        final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        userId = SharedPrefUtils.getServerId(preferences);

        getLoaderManager().initLoader(StaticData.MY_LIBRARY_LOADER, null, this);

        return rootView;
    }


    @Override
    public void onDestroyView() {

        super.onDestroyView();
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
            } else if (count == SongHelper.MUSIC_DATA_LIST.length) {

            }
            //To play the songs downloaded from reach
            else if (count == SongHelper.MUSIC_DATA_LIST.length) {

                final MusicData musicData = SongHelper.getMusicData(cursor);
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
                    SongProvider.CONTENT_URI,
                    SongCursorHelper.SONG_HELPER.getProjection(),
                    SongHelper.COLUMN_STATUS + " = ? and (" + //show only finished
                            SongHelper.COLUMN_OPERATION_KIND + " = ? or " + //show only finished downloads
                            SongHelper.COLUMN_OPERATION_KIND + " = ?)",  //and own songs
                    new String[]{
                            ReachDatabase.Status.FINISHED.getString(),
                            ReachDatabase.OperationKind.DOWNLOAD_OP.getString(),
                            ReachDatabase.OperationKind.OWN.getString()},
                    SongHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE");
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed() || parentAdapter == null)
            return;

        final int count = data.getCount();
        if (loader.getId() == StaticData.MY_LIBRARY_LOADER) {

            Log.i("Ayush", "MyLibrary file manager " + count);
            StaticData.librarySongsCount = count;
            parentAdapter.setNewMyLibraryCursor(data);
            if (count != parentAdapter.getItemCount() - 1) //update only if count has changed
                parentAdapter.updateRecentMusic(getRecentMyLibrary());

        } else if (loader.getId() == StaticData.DOWNLOAD_LOADER) {

//            Log.i("Ayush", "Downloaded file manager " + count);
            StaticData.downloadedSongsCount = count;
        }
        mRecyclerView.checkIfEmpty(parentAdapter.getItemCount());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (parentAdapter == null)
            return;

        if (loader.getId() == StaticData.MY_LIBRARY_LOADER)
            parentAdapter.setNewMyLibraryCursor(null);
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
