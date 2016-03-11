package reach.project.coreViews.fileManager.music.myLibrary;

import android.content.Context;
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
import reach.project.music.ReachDatabase;
import reach.project.music.Song;
import reach.project.music.SongCursorHelper;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 25/11/15.
 */
public class MyLibraryFragment extends Fragment implements HandOverMessage,
        LoaderManager.LoaderCallbacks<Cursor> {

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
            final Song musicData = SongCursorHelper.SONG_HELPER.parse(cursor);
            //musicData.setProcessed(musicData.size);
            MiscUtils.playSong(musicData, getContext());

            // To play songs of the user (not the downloaded ones)
            /*if (count == MySongsHelper.DISK_LIST.length) {*//*

                final Song musicData = SongCursorHelper.SONG_HELPER.parse(cursor);
                MiscUtils.playSong(musicData, getContext());
            }
            //To play the songs downloaded from reach
            else if (count == SongHelper.MUSIC_DATA_LIST.length) {

                final MusicData musicData = SongHelper.getMusicData(cursor);
                MiscUtils.playSong(musicData, getContext());
            } else
                throw new IllegalArgumentException("Unknown column count found");*/
            // Music Data is used for recent list songs
        } else if (message instanceof Song) {

            final Song musicData = (Song) message;
            //musicData.setProcessed(musicData.size);
            MiscUtils.playSong(musicData, getContext());
        } else
            throw new IllegalArgumentException("Unknown type handed over");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.MY_LIBRARY_LOADER)
            return new CursorLoader(getActivity(),
                    SongProvider.CONTENT_URI,
                    SongCursorHelper.SONG_HELPER.getProjection(),
                    "(" + SongHelper.COLUMN_OPERATION_KIND + " = ? and " + SongHelper.COLUMN_STATUS + " = ?) or " +
                            SongHelper.COLUMN_OPERATION_KIND + " = ?",
                    new String[]{
                            ReachDatabase.OperationKind.DOWNLOAD_OP.getString(),
                            ReachDatabase.Status.FINISHED.getString(),
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
            //if(StaticData.librarySongsCount < data.getCount()){
                //MyProfileActivity.countChanged = true;
                StaticData.librarySongsCount = count;
            //}

            if (count != parentAdapter.getItemCount() - 1) //update only if count has changed
                parentAdapter.updateRecentMusic(getRecentMyLibrary());
            parentAdapter.setNewMyLibraryCursor(data);
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
    private List<Song> getRecentMyLibrary() {

        /*final Cursor cursor = new CursorLoader(getActivity(),
                SongProvider.CONTENT_URI,
                SongCursorHelper.SONG_HELPER.getProjection(),
                "(" + SongHelper.COLUMN_OPERATION_KIND + " = ? and " + SongHelper.COLUMN_STATUS + " = ?) or " +
                        SongHelper.COLUMN_OPERATION_KIND + " = ?",
                new String[]{
                        ReachDatabase.OperationKind.DOWNLOAD_OP.getString(),
                        ReachDatabase.Status.FINISHED.getString(),
                        ReachDatabase.OperationKind.OWN.getString()},
                SongHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE");*/

        final Cursor cursor = getContext().getContentResolver().query(
                SongProvider.CONTENT_URI,
                SongCursorHelper.SONG_HELPER.getProjection(),
                "(" + SongHelper.COLUMN_OPERATION_KIND + " = ? and " + SongHelper.COLUMN_STATUS + " = ?) or " +
                        SongHelper.COLUMN_OPERATION_KIND + " = ?",
                new String[]{
                        ReachDatabase.OperationKind.DOWNLOAD_OP.getString(),
                        ReachDatabase.Status.FINISHED.getString(),
                        ReachDatabase.OperationKind.OWN.getString()}, //all songs
                SongHelper.COLUMN_DATE_ADDED + " DESC, " +
                        SongHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC LIMIT 20");

        /*final Cursor cursor = getContext().getContentResolver().query(MySongsProvider.CONTENT_URI,
                SongCuHelper.DISK_LIST,
                null, null, //all songs
                MySongsHelper.COLUMN_DATE_ADDED + " DESC, " +
                        MySongsHelper.COLUMN_DISPLAY_NAME + " COLLATE NOCASE ASC LIMIT 20");
*/
        if (cursor == null)
            return Collections.emptyList();

        /*final List<MusicData> latestMyLibrary = new ArrayList<>(cursor.getCount());*/
        final List<Song> latestMyLibrary = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            /*latestMyLibrary.add(MySongsHelper.getMusicData(cursor, userId));*/
            latestMyLibrary.add(SongCursorHelper.SONG_HELPER.parse(cursor));
        }

        cursor.close();

        return latestMyLibrary;
    }
}