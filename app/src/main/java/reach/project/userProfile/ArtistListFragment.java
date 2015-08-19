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

import reach.project.R;
import reach.project.adapter.ReachArtistsAdapter;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachArtistProvider;
import reach.project.database.sql.ReachArtistHelper;
import reach.project.utils.MiscUtils;
import reach.project.utils.auxiliaryClasses.SuperInterface;
import reach.project.utils.auxiliaryClasses.ReachArtist;

public class ArtistListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private GridView artistGrid;
    private View rootView;

    private SuperInterface mListener;
    private ReachArtistsAdapter reachArtistsAdapter = null;
    private final AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            final ReachArtist reachArtist =
                    ReachArtistHelper.cursorToProcess((Cursor) reachArtistsAdapter.getItem(position));
            mListener.startMusicListFragment(
                    reachArtist.getUserID(),
                    "",
                    reachArtist.getArtistName(),
                    "",
                    2);
        }
    };

    private static WeakReference<ArtistListFragment> reference;
    public static ArtistListFragment newInstance(long id) {

        final Bundle args;
        ArtistListFragment fragment;
        if(reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new ArtistListFragment());
            fragment.setArguments(args = new Bundle());
        }
        else {
            Log.i("Ayush", "Reusing artist list fragment object :)");
            args = fragment.getArguments();
        }

        args.putLong("id", id);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_album, container, false);
        artistGrid = MiscUtils.addLoadingToGridView((GridView) rootView.findViewById(R.id.albumGrid));
        if(reachArtistsAdapter == null)
            reachArtistsAdapter = new ReachArtistsAdapter(getActivity(), R.layout.musiclist_artist, null, 0);
        artistGrid.setOnItemClickListener(listener);
        artistGrid.setAdapter(reachArtistsAdapter);

        getLoaderManager().initLoader(StaticData.ARTIST_LOADER, null, this);
        return rootView;
    }

    @Override
    public void onDestroyView() {

        getLoaderManager().destroyLoader(StaticData.ARTIST_LOADER);

        if(reachArtistsAdapter != null && reachArtistsAdapter.getCursor() != null && !reachArtistsAdapter.getCursor().isClosed())
            reachArtistsAdapter.getCursor().close();

        artistGrid.setOnItemClickListener(null);

        artistGrid = null;
        reachArtistsAdapter = null;
        reachArtistsAdapter = null;
        rootView = null;

        super.onDestroyView();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return new CursorLoader(getActivity(),
                ReachArtistProvider.CONTENT_URI,
                ReachArtistHelper.projection,
                ReachArtistHelper.COLUMN_USER_ID + " = ?",
                new String[]{getArguments().getLong("id") + ""},
                ReachArtistHelper.COLUMN_ARTIST + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        if(cursorLoader.getId() == StaticData.ARTIST_LOADER && cursor != null && !cursor.isClosed()) {

            reachArtistsAdapter.swapCursor(cursor);
//            final Future<?> isMusicFetching = ContactsListFragment.isMusicFetching.get(getArguments().getLong("id"));
//            if((isMusicFetching == null || isMusicFetching.isCancelled() || isMusicFetching.isDone()) && cursor.getCount() == 0 && artistGrid!=null)
//                MiscUtils.setEmptyTextforGridView(artistGrid, "No artists found");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if(cursorLoader.getId() == StaticData.ARTIST_LOADER)
            reachArtistsAdapter.swapCursor(null);
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
