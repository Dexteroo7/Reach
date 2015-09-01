package reach.project.music.songs;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.HashSet;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.SuperInterface;

public class PushSongsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    private ListView pushLibraryList;
    private PushSongAdapter pushSongAdapter = null;
    private TextView songsCount;
    private SearchView searchView;
    private ActionBar actionBar;

    private SuperInterface mListener;
    private String mCurFilter, selection;
    private String[] selectionArguments;
    private long serverId;
    private final HashSet<TransferSong> selectedList = new HashSet<>();

    private final AdapterView.OnItemClickListener listener = (parent, view, position, id) -> {

        final ImageView toggle = (ImageView) view.findViewById(R.id.listToggle);
        final Cursor songCursor = (Cursor) pushSongAdapter.getItem(position);
        final TransferSong transferSong = new TransferSong(
                songCursor.getLong(7), //size of song
                songCursor.getLong(1), //songId
                songCursor.getLong(5), //duration
                songCursor.getString(2), //displayName
                songCursor.getString(3), //actualName
                songCursor.getString(4)); //artistName
        final int hashCode = transferSong.hashCode();

        if (!pushSongAdapter.getCheck(hashCode)) {

            if (selectedList.size() < 5) {

                pushSongAdapter.setCheck(hashCode, true);
                selectedList.add(transferSong);
                toggle.setBackgroundResource(R.drawable.circular_background_dark);
                toggle.setImageResource(R.drawable.check_white);
                final int pad = MiscUtils.dpToPx(5);
                toggle.setPadding(pad, pad, pad, pad);
            } else
                Toast.makeText(getActivity(), "Maximum 5 Songs allowed", Toast.LENGTH_SHORT).show();
        } else {

            pushSongAdapter.setCheck(transferSong.hashCode(), false);
            selectedList.remove(transferSong);
            toggle.setBackgroundResource(0);
            toggle.setImageResource(R.drawable.add_grey);
            toggle.setPadding(0, 0, 0, 0);
        }
    };

    private static WeakReference<PushSongsFragment> reference = null;

    public static PushSongsFragment newInstance() {

        PushSongsFragment fragment;
        if (reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new PushSongsFragment());
        return fragment;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return new CursorLoader(getActivity(),
                ReachSongProvider.CONTENT_URI,
                pushSongAdapter.getProjection(),
                selection,
                selectionArguments,
                ReachSongHelper.COLUMN_DISPLAY_NAME + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        if (cursorLoader.getId() == StaticData.SONGS_LOADER && cursor != null && !cursor.isClosed()) {

            final int count = cursor.getCount();
            pushSongAdapter.swapCursor(cursor);
            songsCount.setText(count + " Songs");
            if (count == 0 && pushLibraryList != null)
                MiscUtils.setEmptyTextForListView(pushLibraryList, "No songs found");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (cursorLoader.getId() == StaticData.SONGS_LOADER)
            pushSongAdapter.swapCursor(null);
    }

    @Override
    public void onDestroyView() {

        if (actionBar != null)
            actionBar.setSubtitle("");

        selectedList.clear();
        selectionArguments = null;

        pushSongAdapter.cleanUp();
        getLoaderManager().destroyLoader(StaticData.SONGS_LOADER);
        if (pushSongAdapter != null && pushSongAdapter.getCursor() != null && !pushSongAdapter.getCursor().isClosed())
            pushSongAdapter.getCursor().close();

        pushSongAdapter = null;
        pushLibraryList = null;
        if (searchView != null) {
            searchView.setOnQueryTextListener(null);
            searchView.setOnCloseListener(null);
            searchView.setQuery(null, false);
            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }

        searchView = null;
        songsCount = null;
        actionBar = null;
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_privacy, container, false);
        pushLibraryList = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.privacyList));
        songsCount = (TextView) rootView.findViewById(R.id.songsCount);
        actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if (actionBar != null) {
            actionBar.setTitle("Share Music");
            actionBar.setSubtitle("Select upto 5 Songs");
        }
        serverId = SharedPrefUtils.getServerId(getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS));
        if (pushSongAdapter == null)
            pushSongAdapter = new PushSongAdapter(getActivity(), R.layout.pushlibrary_item, null, 0);
        selection = ReachSongHelper.COLUMN_USER_ID + " = ? and " + ReachSongHelper.COLUMN_VISIBILITY + " = ?";
        selectionArguments = new String[]{serverId + "", 1 + ""};

        pushLibraryList.setAdapter(pushSongAdapter);
        pushLibraryList.setOnItemClickListener(listener);
        getLoaderManager().initLoader(StaticData.SONGS_LOADER, null, this);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.push_songs_menu, menu);
        searchView = (SearchView) menu.findItem(R.id.search_button).getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {

            case R.id.done_button: {
                if (selectedList.size() == 0)
                    Toast.makeText(getActivity(), "Please select some songs first", Toast.LENGTH_SHORT).show();
                else
                    mListener.onPushNext(selectedList);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onClose() {

        searchView.setQuery(null, true);
        selection = ReachSongHelper.COLUMN_USER_ID + " = ? and " + ReachSongHelper.COLUMN_VISIBILITY + " = ?";
        selectionArguments = new String[]{serverId + "", 1 + ""};
        getLoaderManager().restartLoader(StaticData.SONGS_LOADER, null, this);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {

        if (searchView == null)
            return false;
        // Called when the action bar search text has changed.  Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        final String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
        // Don't do anything if the filter hasn't actually changed.
        // Prevents restarting the loader when restoring state.
        if (mCurFilter == null && newFilter == null) {
            return true;
        }
        if (mCurFilter != null && mCurFilter.equals(newFilter)) {
            return true;
        }
        mCurFilter = newFilter;

        if (TextUtils.isEmpty(newText)) {
            selection = ReachSongHelper.COLUMN_USER_ID + " = ? and " + ReachSongHelper.COLUMN_VISIBILITY + " = ?";
            selectionArguments = new String[]{serverId + "", 1 + ""};
        } else {
            selection = ReachSongHelper.COLUMN_USER_ID + " = ? and " +
                    ReachSongHelper.COLUMN_VISIBILITY + " = ? and " +
                    ReachSongHelper.COLUMN_DISPLAY_NAME + " LIKE ?";
            selectionArguments = new String[]{serverId + "", 1 + "", "%" + mCurFilter + "%"};
        }
        getLoaderManager().restartLoader(StaticData.SONGS_LOADER, null, this);
        return true;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setHasOptionsMenu(true);
        try {
            mListener = (SuperInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnNextListener");
        }
    }
}
