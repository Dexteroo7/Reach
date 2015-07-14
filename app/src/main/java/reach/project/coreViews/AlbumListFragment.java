package reach.project.coreViews;

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

import reach.project.R;
import reach.project.adapter.ReachAlbumsAdapter;
import reach.project.core.StaticData;
import reach.project.database.ReachAlbum;
import reach.project.database.contentProvider.ReachAlbumProvider;
import reach.project.database.sql.ReachAlbumHelper;
import reach.project.utils.MiscUtils;
import reach.project.utils.SuperInterface;

public class AlbumListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private GridView albumGrid;
    private View rootView;
    private ReachAlbumsAdapter reachAlbumsAdapter = null;

    private SuperInterface mListener;
    private final AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            final ReachAlbum reachAlbum =
                    ReachAlbumHelper.cursorToProcess((Cursor) reachAlbumsAdapter.getItem(position));
            mListener.startMusicListFragment(
                    reachAlbum.getUserId(),
                    reachAlbum.getAlbumName(),
                    "",
                    "",
                    1);
        }
    };

    private static WeakReference<AlbumListFragment> reference = null;
    public static AlbumListFragment newInstance(long id) {

        final Bundle args;
        AlbumListFragment fragment;
        if(reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new AlbumListFragment());
            fragment.setArguments(args = new Bundle());
        }
        else {
            Log.i("Ayush", "Reusing album list fragment object :)");
            args = fragment.getArguments();
        }
        args.putLong("id", id);
        return fragment;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_album, container, false);
        albumGrid = MiscUtils.addLoadingToGridView((GridView) rootView.findViewById(R.id.albumGrid));
        if(reachAlbumsAdapter == null)
            reachAlbumsAdapter = new ReachAlbumsAdapter(getActivity(), R.layout.musiclist_album, null, 0);
        albumGrid.setOnItemClickListener(listener);
        albumGrid.setAdapter(reachAlbumsAdapter);
        getLoaderManager().initLoader(StaticData.ALBUM_LOADER, null, this);
        return rootView;
    }

    @Override
    public void onDestroyView() {

        getLoaderManager().destroyLoader(StaticData.ALBUM_LOADER);
        if(reachAlbumsAdapter != null && reachAlbumsAdapter.getCursor() != null && !reachAlbumsAdapter.getCursor().isClosed())
            reachAlbumsAdapter.getCursor().close();

        albumGrid.setOnItemClickListener(null);
        reachAlbumsAdapter = null;
        rootView = null;
        albumGrid = null;
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

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return new CursorLoader(getActivity(),
                ReachAlbumProvider.CONTENT_URI,
                ReachAlbumHelper.projection,
                ReachAlbumHelper.COLUMN_USER_ID + " = ?",
                new String[]{getArguments().getLong("id") + ""},
                ReachAlbumHelper.COLUMN_ALBUM + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        if(cursorLoader.getId() == StaticData.ALBUM_LOADER && cursor != null && !cursor.isClosed()) {
            reachAlbumsAdapter.swapCursor(cursor);
            final Future<?> isMusicFetching = ContactsListFragment.isMusicFetching.get(getArguments().getLong("id"));
            if((isMusicFetching == null || isMusicFetching.isCancelled() || isMusicFetching.isDone()) && cursor.getCount() == 0 && albumGrid!=null)
                MiscUtils.setEmptyTextforGridView(albumGrid, "No albums found");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if(cursorLoader.getId() == StaticData.ALBUM_LOADER)
            reachAlbumsAdapter.swapCursor(null);
    }
}
