package reach.project.coreViews;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import reach.backend.music.musicVisibilityApi.model.MyString;
import reach.project.R;
import reach.project.adapter.ReachMusicAdapter;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachSongProvider;
import reach.project.database.sql.ReachSongHelper;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.SuperInterface;

public class PrivacyFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    private ReachMusicAdapter reachMusicAdapter;
    private TextView songsCount;
    private SearchView searchView;
    private ListView privacyList;
    private View rootView;
    private ActionBar actionBar;

    private SuperInterface mListener;
    private String mCurFilter, selection;
    private String [] selectionArguments;
    private long serverId;

    private final AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            privacyList.setEnabled(false);
            final Cursor reachSongCursor = (Cursor)reachMusicAdapter.getItem(position);
            /**
             * params[0] = oldVisibility
             * params[1] = songId
             * params[2] = userId
             */
            new ToggleVisibility().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    (long) reachSongCursor.getShort(9),
                    reachSongCursor.getLong(1),
                    reachSongCursor.getLong(2));
        }
    };

    private static WeakReference<PrivacyFragment> reference = null;
    public static PrivacyFragment newInstance(boolean first) {

        final Bundle args;
        PrivacyFragment fragment;
        if(reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new PrivacyFragment());
            fragment.setArguments(args = new Bundle());
        }
        else {
            Log.i("Ayush", "Reusing album list fragment object :)");
            args = fragment.getArguments();
        }
        args.putBoolean("first", first);
        return fragment;
    }

    public PrivacyFragment() {
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        if(getActivity() == null) return null;
        return new CursorLoader(getActivity(),
                ReachSongProvider.CONTENT_URI,
                StaticData.DISK_COMPLETE_NO_PATH,
                selection,
                selectionArguments,
                ReachSongHelper.COLUMN_DISPLAY_NAME + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        if(cursorLoader.getId() == StaticData.SONGS_LOADER && cursor != null && !cursor.isClosed()) {

            int count = cursor.getCount();
            reachMusicAdapter.swapCursor(cursor);
            songsCount.setText(count + " Songs");
            if (count==0 && privacyList!=null)
            MiscUtils.setEmptyTextForListView(privacyList, "No songs found");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if(cursorLoader.getId() == StaticData.SONGS_LOADER)
            reachMusicAdapter.swapCursor(null);
    }

    @Override
    public void onDestroyView() {

        if(actionBar != null)
            actionBar.setSubtitle("");
        getLoaderManager().destroyLoader(StaticData.SONGS_LOADER);
        if(reachMusicAdapter != null && reachMusicAdapter.getCursor() != null && !reachMusicAdapter.getCursor().isClosed())
            reachMusicAdapter.getCursor().close();

        reachMusicAdapter = null;
        songsCount = null;
        privacyList = null;
        if(searchView != null) {
            searchView.setOnQueryTextListener(null);
            searchView.setOnCloseListener(null);
            searchView.setQuery(null, false);
            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }

        searchView = null;
        rootView = null;
        actionBar = null;
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_privacy, container, false);
        privacyList = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.privacyList));
        songsCount = (TextView) rootView.findViewById(R.id.songsCount);
        actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();

        if(actionBar != null) {
            actionBar.setTitle("Hide Songs");
            actionBar.setSubtitle("Click to Hide/Unhide Songs");
        }

        if (getArguments().getBoolean("first"))
            new InfoDialog().show(getChildFragmentManager(),"info_dialog");
        serverId = SharedPrefUtils.getServerId(getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS));
        reachMusicAdapter = new ReachMusicAdapter(getActivity(), R.layout.privacylist_item, null, 0, ReachMusicAdapter.LIST);
        selection = ReachSongHelper.COLUMN_USER_ID + " = ? ";
        selectionArguments = new String[]{serverId+""};
        getLoaderManager().initLoader(StaticData.SONGS_LOADER, null, this);

        privacyList.setAdapter(reachMusicAdapter);
        privacyList.setOnItemClickListener(listener);
        return rootView;
    }

    private class ToggleVisibility extends AsyncTask<Long, Void, Boolean> {

        private synchronized void updateDatabase (ContentValues contentValues, long songId, long userId) {

            if(getActivity() == null || contentValues == null || songId == 0 || userId == 0)
                return;
            Log.i("Ayush", "Toggle Visibility " + getActivity().getContentResolver().update(
                    ReachSongProvider.CONTENT_URI,
                    contentValues,
                    ReachSongHelper.COLUMN_SONG_ID + " = ? and " + ReachSongHelper.COLUMN_USER_ID + " = ?",
                    new String[]{songId + "", userId + ""}));
        }

        /**
         * params[0] = oldVisibility
         * params[1] = songId
         * params[2] = userId
         */

        @Override
        protected Boolean doInBackground(Long... params) {

            final ContentValues values = new ContentValues();
            if (params[0] == 0)
                values.put(ReachSongHelper.COLUMN_VISIBILITY, 1);
            else
                values.put(ReachSongHelper.COLUMN_VISIBILITY, 0);

            updateDatabase(values, params[1], params[2]);
            publishProgress(); //re-enable listView

            boolean failed = false;

            try {
                final MyString response = StaticData.musicVisibility.update(
                        params[2], //serverId
                        params[1], //songId
                        params[0] == 0).execute(); //if 0 (false) make it true and vice-versa
                if(response == null || TextUtils.isEmpty(response.getString()) || response.getString().equals("false"))
                    failed = true; //mark failed
            } catch (IOException e) {
                e.printStackTrace();
                failed = true; //mark failed
            }

            if (failed) {
                //reset if failed
                values.put(ReachSongHelper.COLUMN_VISIBILITY, params[0]);
                updateDatabase(values, params[1], params[2]);
            }

            return failed;
        }

        @Override
        protected void onPostExecute(Boolean failed) {
            super.onPostExecute(failed);

            if(isCancelled() || getActivity() == null || getActivity().isFinishing())
                return;

            if(failed)
                Toast.makeText(getActivity(), "Network error", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            privacyList.setEnabled(true);
        }
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        menu.clear();
        if (getArguments()!=null&&getArguments().getBoolean("first"))
            inflater.inflate(R.menu.privacy_menu, menu);
        else
            inflater.inflate(R.menu.search_menu, menu);

        searchView = (SearchView) menu.findItem(R.id.search_button).getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch(id){

            case R.id.done_button: {
                mListener.onPrivacyDone();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public boolean onClose() {

        searchView.setQuery(null, true);
        selection = ReachSongHelper.COLUMN_USER_ID + " = ? ";
        selectionArguments = new String[]{serverId+""};
        //TODO test if restart is needed here
        getLoaderManager().restartLoader(StaticData.SONGS_LOADER, null, this);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {

        if(searchView == null)
            return false;

        // Called when the action bar search text has changed.  Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        final String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
        // Don't do anything if the filter hasn't actually changed.
        // Prevents restarting the loader when restoring state.
        if (mCurFilter == null && newFilter == null) {
            return true;
        } if (mCurFilter != null && mCurFilter.equals(newFilter)) {
            return true;
        }
        mCurFilter = newFilter;

        if(TextUtils.isEmpty(newText)) {
            selection = ReachSongHelper.COLUMN_USER_ID + " = ? ";
            selectionArguments = new String[]{serverId+""};
        } else {
            selection = ReachSongHelper.COLUMN_USER_ID + " = ? and " + ReachSongHelper.COLUMN_DISPLAY_NAME + " LIKE ?";
            selectionArguments = new String[]{serverId + "", "%" + mCurFilter + "%"};
        }
        Log.i("Ayush", "Selection " + selection + " SelectionArguments " + Arrays.toString(selectionArguments));
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
                    + " must implement OnDoneClickListener");
        }
    }

    public static class InfoDialog extends DialogFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View v = inflater.inflate(R.layout.import_dialog, container, false);
            ImageView image = (ImageView) v.findViewById(R.id.image);
            image.setPadding(0,0,0,0);
            image.setBackgroundResource(0);
            Picasso.with(v.getContext()).load(R.drawable.hide_dialog).into(image);
            TextView text1 = (TextView) v.findViewById(R.id.text1);
            text1.setText("Tap to hide your songs. By default your personal audio files are hidden");
            TextView done = (TextView) v.findViewById(R.id.done);
            done.setText("Okay, I got it!");
            done.setVisibility(View.VISIBLE);
            done.setOnClickListener(v1 -> dismiss());
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            return v;
        }
    }
}
