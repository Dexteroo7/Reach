package reach.project.music.songs;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.merge.MergeAdapter;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.ref.WeakReference;

import reach.backend.music.musicVisibilityApi.model.MyString;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.uploadDownload.ReachDatabase;
import reach.project.uploadDownload.ReachDatabaseHelper;
import reach.project.uploadDownload.ReachDatabaseProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.SuperInterface;

public class PrivacyFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    private MergeAdapter combinedAdapter = null;
    private ReachMusicAdapter myLibraryAdapter = null;
    private ReachMusicAdapter downloadedAdapter = null;
    private TextView emptyDownload = null, emptyMyLibrary = null;

    private String selectionDownloader, selectionMyLibrary, mCurFilter;
    private String[] selectionArgumentsDownloader;
    private String[] selectionArgumentsMyLibrary;

    private SearchView searchView;
    private ListView privacyList;
    private View rootView;
    private Toolbar toolbar;

    private SuperInterface mListener;

    private long serverId;

    private static WeakReference<PrivacyFragment> reference = null;

    public static PrivacyFragment newInstance(boolean first) {

        final Bundle args;
        PrivacyFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new PrivacyFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing album list fragment object :)");
            args = fragment.getArguments();
        }
        args.putBoolean("first", first);
        return fragment;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {

        if (id == StaticData.PRIVACY_MY_LIBRARY_LOADER) {

            return new CursorLoader(getActivity(),
                    ReachSongProvider.CONTENT_URI,
                    myLibraryAdapter.getProjectionMyLibrary(),
                    selectionMyLibrary,
                    selectionArgumentsMyLibrary,
                    ReachSongHelper.COLUMN_DISPLAY_NAME + " ASC");
        } else if (id == StaticData.PRIVACY_DOWNLOADED_LOADER) {

            return new CursorLoader(getActivity(),
                    ReachDatabaseProvider.CONTENT_URI,
                    myLibraryAdapter.getProjectionDownloaded(),
                    selectionDownloader,
                    selectionArgumentsDownloader,
                    ReachDatabaseHelper.COLUMN_DISPLAY_NAME + " ASC");
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        if (cursorLoader.getId() == StaticData.PRIVACY_MY_LIBRARY_LOADER && cursor != null && !cursor.isClosed()) {

            myLibraryAdapter.swapCursor(cursor);
            if (cursor.getCount() == 0 && privacyList != null)
                combinedAdapter.setActive(emptyMyLibrary, true);
            else
                combinedAdapter.setActive(emptyMyLibrary, false);
        } else if (cursorLoader.getId() == StaticData.PRIVACY_DOWNLOADED_LOADER && cursor != null && !cursor.isClosed()) {

            downloadedAdapter.swapCursor(cursor);
            if (cursor.getCount() == 0 && privacyList != null)
                combinedAdapter.setActive(emptyDownload, true);
            else
                combinedAdapter.setActive(emptyDownload, false);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

        if (cursorLoader.getId() == StaticData.PRIVACY_MY_LIBRARY_LOADER)
            myLibraryAdapter.swapCursor(null);
        else if (cursorLoader.getId() == StaticData.PRIVACY_DOWNLOADED_LOADER)
            downloadedAdapter.swapCursor(null);
    }

    @Override
    public void onDestroyView() {

        toolbar.setSubtitle("");

        selectionMyLibrary = null;
        selectionDownloader = null;
        selectionArgumentsMyLibrary = null;
        selectionArgumentsDownloader = null;

        getLoaderManager().destroyLoader(StaticData.PRIVACY_MY_LIBRARY_LOADER);
        if (myLibraryAdapter != null && myLibraryAdapter.getCursor() != null && !myLibraryAdapter.getCursor().isClosed())
            myLibraryAdapter.getCursor().close();

        getLoaderManager().destroyLoader(StaticData.PRIVACY_DOWNLOADED_LOADER);
        if (downloadedAdapter != null && downloadedAdapter.getCursor() != null && !downloadedAdapter.getCursor().isClosed())
            downloadedAdapter.getCursor().close();

        myLibraryAdapter = null;
        downloadedAdapter = null;
        privacyList = null;

        if (searchView != null) {
            searchView.setOnQueryTextListener(null);
            searchView.setOnCloseListener(null);
            searchView.setQuery(null, false);
            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }

        searchView = null;
        rootView = null;
        toolbar = null;
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final Activity activity = getActivity();
        serverId = SharedPrefUtils.getServerId(activity.getSharedPreferences("Reach", Context.MODE_PRIVATE));

        rootView = inflater.inflate(R.layout.fragment_privacy, container, false);
        privacyList = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.privacyList));
        privacyList.setOnItemClickListener(LocalUtils.listener);

        toolbar = (Toolbar) rootView.findViewById(R.id.privacyToolbar);
        toolbar.setTitle("Hide Songs");
        toolbar.setSubtitle("Click to Hide/Unhide Songs");

        boolean first = false;

        if (getArguments()!=null)
            first = getArguments().getBoolean("first");

        if (first) {
            toolbar.inflateMenu(R.menu.privacy_menu);
            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.done_button) {
                    mListener.onPrivacyDone();
                    return true;
                }
                return false;
            });
        } else {

            toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            toolbar.setNavigationOnClickListener(v -> activity.onBackPressed());
            toolbar.inflateMenu(R.menu.search_menu);
        }

        searchView = (SearchView) toolbar.getMenu().findItem(R.id.search_button).getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);

        if (first)
            new LocalUtils.InfoDialog().show(getChildFragmentManager(), "info_dialog");

        selectionMyLibrary = ReachSongHelper.COLUMN_USER_ID + " = ?";
        selectionArgumentsMyLibrary = new String[]{serverId + ""};

        selectionDownloader = ReachDatabaseHelper.COLUMN_RECEIVER_ID + " = ? and " +
                ReachDatabaseHelper.COLUMN_STATUS + " = ?";
        selectionArgumentsDownloader = new String[]{serverId + "", ReachDatabase.FINISHED + ""};

        loadAdapter(first, activity);
        return rootView;
    }

    @Override
    public boolean onClose() {

//        searchView.setQuery(null, true);
//        selection = ReachSongHelper.COLUMN_USER_ID + " = ? ";
//        selectionArguments = new String[]{serverId + ""};
//        getLoaderManager().restartLoader(StaticData.SONGS_LOADER, null, this);
        if (searchView != null) {
            searchView.setQuery(null, true);
            searchView.clearFocus();
        }
        onQueryTextChange(null);
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

            selectionMyLibrary = ReachSongHelper.COLUMN_USER_ID + " = ?";
            selectionArgumentsMyLibrary = new String[]{serverId + ""};

            selectionDownloader = ReachDatabaseHelper.COLUMN_RECEIVER_ID + " = ? and " +
                    ReachDatabaseHelper.COLUMN_STATUS + " = ?";
            selectionArgumentsDownloader = new String[]{serverId + "", ReachDatabase.FINISHED + ""};
        } else {

            selectionMyLibrary = ReachSongHelper.COLUMN_USER_ID + " = ? and " +
                    ReachSongHelper.COLUMN_DISPLAY_NAME + " LIKE ?";
            selectionArgumentsMyLibrary = new String[]{serverId + "", "%" + mCurFilter + "%"};

            selectionDownloader = ReachDatabaseHelper.COLUMN_RECEIVER_ID + " = ? and " +
                    ReachDatabaseHelper.COLUMN_STATUS + " = ? and " +
                    ReachDatabaseHelper.COLUMN_DISPLAY_NAME + " LIKE ?";
            selectionArgumentsDownloader = new String[]{serverId + "", ReachDatabase.FINISHED + "", "%" + mCurFilter + "%"};
        }

        getLoaderManager().restartLoader(StaticData.PRIVACY_MY_LIBRARY_LOADER, null, this);
        getLoaderManager().restartLoader(StaticData.PRIVACY_DOWNLOADED_LOADER, null, this);
        Log.i("Downloader", "SEARCH SUBMITTED !");
        return true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (SuperInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnDoneClickListener");
        }
    }

    private void loadAdapter(boolean first, Context context) {

        /**
         * Set up adapter for Music player
         */
        combinedAdapter = new MergeAdapter();

        if (context == null)
            return;

        if (!first) {
            combinedAdapter.addView(LocalUtils.getDownloadedTextView(context));
            combinedAdapter.addView(emptyDownload = LocalUtils.getEmptyDownload(context), false);
            combinedAdapter.addAdapter(downloadedAdapter = new ReachMusicAdapter(context, R.layout.privacylist_item, null, 0, ReachMusicAdapter.LIST));
        }

        combinedAdapter.addView(LocalUtils.getMyLibraryTextView(context));
        combinedAdapter.addView(emptyMyLibrary = LocalUtils.getEmptyLibrary(context), false);
        combinedAdapter.addAdapter(myLibraryAdapter = new ReachMusicAdapter(context, R.layout.privacylist_item, null, 0, ReachMusicAdapter.LIST));

        privacyList.setAdapter(combinedAdapter);
        if (!first)
            getLoaderManager().initLoader(StaticData.PRIVACY_DOWNLOADED_LOADER, null, this);
        getLoaderManager().initLoader(StaticData.PRIVACY_MY_LIBRARY_LOADER, null, this);
    }


    private enum LocalUtils {
        ;

        public static TextView getDownloadedTextView(Context context) {

            final TextView textView = new TextView(context);
            textView.setText("Downloaded");
            textView.setTextColor(ContextCompat.getColor(context, R.color.darkgrey));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
            textView.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(10), 0, 0);
            return textView;
        }

        public static TextView getMyLibraryTextView(Context context) {

            final TextView textView = new TextView(context);
            textView.setText("My Songs");
            textView.setTextColor(ContextCompat.getColor(context, R.color.darkgrey));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
            textView.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(10), 0, 0);
            return textView;
        }

        public static TextView getEmptyDownload(Context context) {

            final TextView emptyTV1 = new TextView(context);
            emptyTV1.setText("No downloaded songs");
            emptyTV1.setTextColor(ContextCompat.getColor(context, R.color.darkgrey));
            emptyTV1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            emptyTV1.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(10), 0, 0);
            return emptyTV1;
        }

        public static TextView getEmptyLibrary(Context context) {

            final TextView emptyTV2 = new TextView(context);
            emptyTV2.setText("No Music on your phone");
            emptyTV2.setTextColor(ContextCompat.getColor(context, R.color.darkgrey));
            emptyTV2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            emptyTV2.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(10), 0, 0);
            return emptyTV2;
        }

        public static class ToggleVisibility extends AsyncTask<Long, Void, Boolean> {

            /**
             * params[0] = oldVisibility
             * params[1] = songId
             * params[2] = userId
             */

            @Override
            protected Boolean doInBackground(Long... params) {

                boolean failed = false;
                try {
                    final MyString response = StaticData.musicVisibility.update(
                            params[2], //serverId
                            params[1], //songId
                            params[0] == 0).execute(); //if 0 (false) make it true and vice-versa
                    if (response == null || TextUtils.isEmpty(response.getString()) || response.getString().equals("false"))
                        failed = true; //mark failed
                } catch (IOException e) {
                    e.printStackTrace();
                    failed = true; //mark failed
                }

                if (failed) {
                    //reset if failed
                    final ContentValues values = new ContentValues(1);
                    values.put(ReachSongHelper.COLUMN_VISIBILITY, params[0]);
                    MiscUtils.useContextFromFragment(reference, context -> {
                        updateDatabase(values, params[1], params[2], context);
                    });
                }

                return failed;
            }

            @Override
            protected void onPostExecute(Boolean failed) {

                super.onPostExecute(failed);
                if (failed)
                    MiscUtils.useContextFromFragment(reference, context -> {
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
                    });
            }
        }

        public static synchronized void updateDatabase(ContentValues contentValues, long songId, long userId, Context context) {

            if (context == null || contentValues == null || songId == 0 || userId == 0)
                return;

            final ContentResolver resolver = context.getContentResolver();

            int updated = resolver.update(
                    ReachSongProvider.CONTENT_URI,
                    contentValues,
                    ReachSongHelper.COLUMN_SONG_ID + " = ? and " + ReachSongHelper.COLUMN_USER_ID + " = ?",
                    new String[]{songId + "", userId + ""});

            if (updated == 0)
                updated = resolver.update(
                        ReachDatabaseProvider.CONTENT_URI,
                        contentValues,
                        ReachDatabaseHelper.COLUMN_UNIQUE_ID + " = ? and " + ReachDatabaseHelper.COLUMN_RECEIVER_ID + " = ?",
                        new String[]{songId + "", userId + ""});

            Log.i("Ayush", "Toggle Visibility " + updated);
        }

        public static final AdapterView.OnItemClickListener listener = (parent, view, position, id) -> {

            final Cursor reachSongCursor = (Cursor) parent.getAdapter().getItem(position);
            /**
             * params[0] = oldVisibility
             * params[1] = songId
             * params[2] = userId
             */
            final short oldVisibility = reachSongCursor.getShort(9);
            final long songId = reachSongCursor.getLong(1);
            final long userId = reachSongCursor.getLong(2);

            final ContentValues values = new ContentValues();
            if (oldVisibility == 0)
                values.put(ReachSongHelper.COLUMN_VISIBILITY, 1);
            else
                values.put(ReachSongHelper.COLUMN_VISIBILITY, 0);

            updateDatabase(values, songId, userId, view.getContext()
            );
            new ToggleVisibility().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    (long) reachSongCursor.getShort(9),
                    reachSongCursor.getLong(1),
                    reachSongCursor.getLong(2));
        };

        public static class InfoDialog extends DialogFragment {

            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

                final View v = inflater.inflate(R.layout.import_dialog, container, false);
                final ImageView image = (ImageView) v.findViewById(R.id.image);
                image.setPadding(0, 0, 0, 0);
                image.setBackgroundResource(0);
                Picasso.with(v.getContext()).load(R.drawable.hide_dialog).into(image);
                final TextView text1 = (TextView) v.findViewById(R.id.text1);
                text1.setText("Tap to hide your songs. By default your personal audio files are hidden");
                final TextView done = (TextView) v.findViewById(R.id.done);

                done.setText("Okay, I got it!");
                done.setVisibility(View.VISIBLE);
                done.setOnClickListener(v1 -> dismiss());
                getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                return v;
            }
        }
    }
}
