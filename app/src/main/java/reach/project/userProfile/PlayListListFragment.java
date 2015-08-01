package reach.project.userProfile;
        
import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import java.lang.ref.WeakReference;
import java.util.concurrent.Future;

import reach.backend.entities.userApi.model.ReachPlayList;
import reach.project.R;
import reach.project.adapter.ReachPlayListsAdapter;
import reach.project.core.StaticData;
import reach.project.coreViews.ContactsListFragment;
import reach.project.database.contentProvider.ReachPlayListProvider;
import reach.project.database.sql.ReachPlayListHelper;
import reach.project.utils.MiscUtils;
import reach.project.utils.SuperInterface;

public class PlayListListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private GridView playListGrid;
    private View rootView;

    private SuperInterface mListener;
    private ReachPlayListsAdapter reachPlayListsAdapter = null;
    private final AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

            if (null != mListener) {

                final ReachPlayList reachPlayListDatabase =
                        ReachPlayListHelper.cursorToProcess((Cursor) reachPlayListsAdapter.getItem(position));
                mListener.startMusicListFragment(reachPlayListDatabase.getUserId(),
                        "",
                        "",
                        reachPlayListDatabase.getPlaylistName(),
                        3);
            }
        }
    };

    private static WeakReference<PlayListListFragment> reference = null;
    public static PlayListListFragment newInstance(long id) {

        final Bundle args;
        PlayListListFragment fragment;
        if(reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new PlayListListFragment());
            fragment.setArguments(args = new Bundle());
        }
        else {
            Log.i("Ayush", "Reusing play list fragment object :)");
            args = fragment.getArguments();
        }

        args.putLong("id", id);
        return fragment;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return new CursorLoader(getActivity(),
                ReachPlayListProvider.CONTENT_URI,
                ReachPlayListHelper.projection,
                ReachPlayListHelper.COLUMN_USER_ID + " = ?",
                new String[]{getArguments().getLong("id") + ""},
                ReachPlayListHelper.COLUMN_PLAY_LIST_NAME + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        if(cursorLoader.getId() == StaticData.PLAY_LIST_LOADER && cursor != null && !cursor.isClosed()) {

            reachPlayListsAdapter.swapCursor(cursor);
            final Future<?> isMusicFetching = ContactsListFragment.isMusicFetching.get(getArguments().getLong("id"));
            if((isMusicFetching == null || isMusicFetching.isCancelled() || isMusicFetching.isDone()) && cursor.getCount() == 0 && playListGrid!=null)
                MiscUtils.setEmptyTextforGridView(playListGrid, "No playlists found");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if(cursorLoader.getId() == StaticData.PLAY_LIST_LOADER)
            reachPlayListsAdapter.swapCursor(null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                                           Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_album, container, false);
        playListGrid = MiscUtils.addLoadingToGridView((GridView) rootView.findViewById(R.id.albumGrid));
        if(reachPlayListsAdapter == null)
            reachPlayListsAdapter = new ReachPlayListsAdapter(getActivity(), R.layout.musiclist_playlist_item, null, 0);
        playListGrid.setAdapter(reachPlayListsAdapter);
        playListGrid.setOnItemClickListener(listener);

        getLoaderManager().initLoader(StaticData.PLAY_LIST_LOADER, null, this);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        getLoaderManager().destroyLoader(StaticData.PLAY_LIST_LOADER);
        if(reachPlayListsAdapter != null && reachPlayListsAdapter.getCursor() != null && !reachPlayListsAdapter.getCursor().isClosed())
                reachPlayListsAdapter.getCursor().close();
        reachPlayListsAdapter = null;

        playListGrid.setOnItemClickListener(null);
        rootView = null;
        playListGrid = null;
        super.onDestroyView();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
                mListener = (SuperInterface) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString()
                                + " must implement OnFragmentInteractionListener");
            }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}