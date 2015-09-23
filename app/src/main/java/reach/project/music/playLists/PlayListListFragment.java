package reach.project.music.playLists;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;
import reach.project.utils.auxiliaryClasses.SuperInterface;
import reach.project.utils.viewHelpers.kmshack.newsstand.ScrollTabHolderFragment;

public class PlayListListFragment extends ScrollTabHolderFragment implements LoaderManager.LoaderCallbacks<Cursor> , AbsListView.OnScrollListener {

    private ListView playListGrid;
    private View rootView;
    private long userId = 0;

    private SuperInterface mListener;
    private ReachPlayListsAdapter reachPlayListsAdapter = null;
    private int mPosition;

    private final AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

            final Cursor cursor = (Cursor)reachPlayListsAdapter.getItem(position);

            if (null != mListener) {
                mListener.startMusicListFragment(
                        userId,
                        "",
                        "",
                        cursor.getString(1),
                        3);
            }
        }
    };

    private static WeakReference<PlayListListFragment> reference = null;

    public static PlayListListFragment newInstance(long id, int pos) {

        final Bundle args;
        PlayListListFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new PlayListListFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing play list fragment object :)");
            args = fragment.getArguments();
        }

        args.putLong("id", id);
        args.putInt("position", pos);
        return fragment;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return new CursorLoader(getActivity(),
                ReachPlayListProvider.CONTENT_URI,
                new String[]{
                        ReachPlayListHelper.COLUMN_SIZE,
                        ReachPlayListHelper.COLUMN_PLAY_LIST_NAME,
                        ReachPlayListHelper.COLUMN_ID
                },
                ReachPlayListHelper.COLUMN_USER_ID + " = ?",
                new String[]{userId + ""},
                ReachPlayListHelper.COLUMN_PLAY_LIST_NAME + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        if (cursorLoader.getId() == StaticData.PLAY_LIST_LOADER && cursor != null && !cursor.isClosed()) {

            reachPlayListsAdapter.swapCursor(cursor);
//            final Future<?> isMusicFetching = ContactsListFragment.isMusicFetching.get(getArguments().getLong("id"));
//            if((isMusicFetching == null || isMusicFetching.isCancelled() || isMusicFetching.isDone()) && cursor.getCount() == 0 && playListGrid!=null)
//                MiscUtils.setEmptyTextforGridView(playListGrid, "No playlists found");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (cursorLoader.getId() == StaticData.PLAY_LIST_LOADER)
            reachPlayListsAdapter.swapCursor(null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_album, container, false);
        mPosition = getArguments().getInt("position");
        playListGrid = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.albumGrid));
        View placeHolderView = inflater.inflate(R.layout.view_header_placeholder, playListGrid, false);
        playListGrid.addHeaderView(placeHolderView);
        if (reachPlayListsAdapter == null)
            reachPlayListsAdapter = new ReachPlayListsAdapter(getActivity(), R.layout.musiclist_playlist_item, null, 0);
        playListGrid.setOnScrollListener(this);
        playListGrid.setAdapter(reachPlayListsAdapter);
        playListGrid.setOnItemClickListener(listener);
        userId = getArguments().getLong("id");

        getLoaderManager().initLoader(StaticData.PLAY_LIST_LOADER, null, this);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        getLoaderManager().destroyLoader(StaticData.PLAY_LIST_LOADER);
        if (reachPlayListsAdapter != null && reachPlayListsAdapter.getCursor() != null && !reachPlayListsAdapter.getCursor().isClosed())
            reachPlayListsAdapter.getCursor().close();
        reachPlayListsAdapter = null;

        playListGrid.setOnItemClickListener(null);
        rootView = null;
        playListGrid = null;
        super.onDestroyView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (SuperInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void adjustScroll(int scrollHeight) {
        if (scrollHeight == 0 && playListGrid.getFirstVisiblePosition() >= 1) {
            return;
        }

        playListGrid.setSelectionFromTop(1, scrollHeight);

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (mScrollTabHolder != null)
            mScrollTabHolder.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount, mPosition);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // nothing
    }
}