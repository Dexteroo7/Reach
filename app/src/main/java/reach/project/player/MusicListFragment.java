package reach.project.player;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
public class MusicListFragment extends Fragment implements HandOverMessage,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static long userId = 0;
    public static final String CURRENT_SONG_ID_KEY = "current_id";

    public static MusicListFragment getInstance(String header,long currently_playing_song_id) {

        final Bundle args = new Bundle();
        args.putString("header", header);
        args.putLong(CURRENT_SONG_ID_KEY, currently_playing_song_id);
        MusicListFragment fragment = new MusicListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    private MusicListAdapter musicListAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_music_list, container, false);
        final EmptyRecyclerView mRecyclerView = (EmptyRecyclerView) rootView.findViewById(R.id.recyclerView);
        final Context context = mRecyclerView.getContext();

        final long current_playing_song_id = getArguments().getLong(CURRENT_SONG_ID_KEY);
        //TODO: Check where in the adapter there is current song and scroll to that position
        musicListAdapter = new MusicListAdapter(this, this, context,current_playing_song_id);
        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(context));
        mRecyclerView.setAdapter(musicListAdapter);
        mRecyclerView.setEmptyView(rootView.findViewById(R.id.empty_imageView));

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
        if (musicListAdapter != null)
            musicListAdapter.close();
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

        if (data == null || data.isClosed() || musicListAdapter == null)
            return;

        final int count = data.getCount();
        if (loader.getId() == StaticData.MY_LIBRARY_LOADER) {

//            Log.i("Ayush", "MyLibrary file manager " + count);

            musicListAdapter.setNewMyLibraryCursor(data);


        } else if (loader.getId() == StaticData.DOWNLOAD_LOADER) {

//            Log.i("Ayush", "Downloaded file manager " + count);

            musicListAdapter.setNewDownLoadCursor(data);

        }
    }

    private int findItemInCursor(long id, Cursor c, int song_id_index){

        /*while(c.){

        }*/

        return 0;

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (musicListAdapter == null)
            return;

        if (loader.getId() == StaticData.MY_LIBRARY_LOADER)
            musicListAdapter.setNewMyLibraryCursor(null);
        else if (loader.getId() == StaticData.DOWNLOAD_LOADER)
            musicListAdapter.setNewDownLoadCursor(null);
    }

}
