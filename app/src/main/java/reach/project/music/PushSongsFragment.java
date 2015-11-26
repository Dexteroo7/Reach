package reach.project.music;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.merge.MergeAdapter;
import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashSet;

import reach.backend.entities.messaging.Messaging;
import reach.backend.entities.messaging.model.MyString;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.StringCompress;
import reach.project.utils.auxiliaryClasses.SuperInterface;

public class PushSongsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SearchView.OnQueryTextListener, SearchView.OnCloseListener, PushSongAdapter.IsItemSelected {

    private MergeAdapter combinedAdapter = null;
    private PushSongAdapter myLibraryAdapter = null;
    private PushSongAdapter downloadedAdapter = null;
    private TextView emptyDownload = null, emptyMyLibrary = null;

    private String selectionDownloader, selectionMyLibrary, mCurFilter;
    private String[] selectionArgumentsDownloader;
    private String[] selectionArgumentsMyLibrary;

    private ListView pushLibraryList;
    private SearchView searchView;
    private Toolbar toolbar;

    private SuperInterface mListener;
    private long serverId;

    private static WeakReference<PushSongsFragment> reference = null;

    public static PushSongsFragment newInstance() {

        PushSongsFragment fragment;
        if (reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new PushSongsFragment());
        return fragment;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {

        if (id == StaticData.PUSH_MY_LIBRARY_LOADER) {

            return new CursorLoader(getActivity(),
                    MySongsProvider.CONTENT_URI,
                    myLibraryAdapter.getProjectionMyLibrary(),
                    selectionMyLibrary,
                    selectionArgumentsMyLibrary,
                    MySongsHelper.COLUMN_DISPLAY_NAME + " ASC");
        } else if (id == StaticData.PUSH_DOWNLOADED_LOADER) {

            return new CursorLoader(getActivity(),
                    ReachDatabaseProvider.CONTENT_URI,
                    downloadedAdapter.getProjectionDownloaded(),
                    selectionDownloader,
                    selectionArgumentsDownloader,
                    ReachDatabaseHelper.COLUMN_DISPLAY_NAME + " ASC");
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        if (cursorLoader.getId() == StaticData.PUSH_MY_LIBRARY_LOADER && cursor != null && !cursor.isClosed()) {

            myLibraryAdapter.swapCursor(cursor);
            final int count = cursor.getCount();
            if (count == 0 && pushLibraryList != null)
                combinedAdapter.setActive(emptyMyLibrary, true);
            else
                combinedAdapter.setActive(emptyMyLibrary, false);
        } else if (cursorLoader.getId() == StaticData.PUSH_DOWNLOADED_LOADER && cursor != null && !cursor.isClosed()) {

            downloadedAdapter.swapCursor(cursor);
            final int count = cursor.getCount();
            if (count == 0 && pushLibraryList != null)
                combinedAdapter.setActive(emptyDownload, true);
            else
                combinedAdapter.setActive(emptyDownload, false);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

        if (cursorLoader.getId() == StaticData.PUSH_MY_LIBRARY_LOADER)
            myLibraryAdapter.swapCursor(null);
        else if (cursorLoader.getId() == StaticData.PUSH_DOWNLOADED_LOADER)
            downloadedAdapter.swapCursor(null);
    }

    @Override
    public void onDestroyView() {

        LocalUtils.selectedList.clear();
        LocalUtils.booleanArray.clear();

        toolbar.setSubtitle("");

        selectionMyLibrary = null;
        selectionDownloader = null;
        selectionArgumentsMyLibrary = null;
        selectionArgumentsDownloader = null;

        getLoaderManager().destroyLoader(StaticData.PUSH_MY_LIBRARY_LOADER);
        if (myLibraryAdapter != null && myLibraryAdapter.getCursor() != null && !myLibraryAdapter.getCursor().isClosed())
            myLibraryAdapter.getCursor().close();

        getLoaderManager().destroyLoader(StaticData.PUSH_DOWNLOADED_LOADER);
        if (downloadedAdapter != null && downloadedAdapter.getCursor() != null && !downloadedAdapter.getCursor().isClosed())
            downloadedAdapter.getCursor().close();

        myLibraryAdapter = null;
        downloadedAdapter = null;

        pushLibraryList = null;
        if (searchView != null) {

            searchView.setOnQueryTextListener(null);
            searchView.setOnCloseListener(null);
            searchView.setQuery(null, false);
            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }

        searchView = null;
        toolbar = null;

        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final Activity activity = getActivity();
        serverId = SharedPrefUtils.getServerId(activity.getSharedPreferences("Reach", Context.MODE_PRIVATE));

        final View rootView = inflater.inflate(R.layout.fragment_privacy, container, false);
        pushLibraryList = (ListView) rootView.findViewById(R.id.privacyList);
        pushLibraryList.setOnItemClickListener(LocalUtils.listener);

        toolbar = ((Toolbar) rootView.findViewById(R.id.privacyToolbar));
        toolbar.setTitle("Share Music");
        toolbar.setSubtitle("Select unto 5 Songs");
        toolbar.inflateMenu(R.menu.push_songs_menu);

        final View doneButton = toolbar.findViewById(R.id.done_button);

        if (serverId == StaticData.devika)
            doneButton.setOnLongClickListener(v -> {
                new LocalUtils.PushSongsDevikaSpecial().executeOnExecutor(StaticData.temporaryFix);
                return false;
            });

        doneButton.setOnClickListener(v -> {

            new LocalUtils.PushSongsDevikaSpecial().executeOnExecutor(StaticData.temporaryFix);

//            if (LocalUtils.selectedList.size() == 0)
//                Toast.makeText(activity, "Please select some songs first", Toast.LENGTH_SHORT).show();
//            else
//                mListener.onPushNext(LocalUtils.selectedList);
        });

        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbar.setNavigationOnClickListener(v -> activity.onBackPressed());
        searchView = (SearchView) toolbar.getMenu().findItem(R.id.search_button).getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);

        selectionMyLibrary = MySongsHelper.COLUMN_USER_ID + " = ? and " +
                MySongsHelper.COLUMN_VISIBILITY + " = ?";
        selectionArgumentsMyLibrary = new String[]{serverId + "", "1"};

        selectionDownloader = ReachDatabaseHelper.COLUMN_RECEIVER_ID + " = ? and " +
                ReachDatabaseHelper.COLUMN_VISIBILITY + " = ? and " +
                ReachDatabaseHelper.COLUMN_STATUS + " = ?";
        selectionArgumentsDownloader = new String[]{serverId + "", "1", ReachDatabase.FINISHED + ""};

        loadAdapter();
        return rootView;
    }

    @Override
    public boolean onClose() {

//        searchView.setQuery(null, true);
//        searchView.clearFocus();
//
//        selectionMyLibrary = MySongsHelper.COLUMN_USER_ID + " = ? and " +
//                MySongsHelper.COLUMN_VISIBILITY + " = ?";
//        selectionArgumentsMyLibrary = new String[]{serverId + "", 1 + ""};
//        getLoaderManager().restartLoader(StaticData.PUSH_MY_LIBRARY_LOADER, null, this);
//
//        selectionDownloader = MySongsHelper.COLUMN_USER_ID + " = ? and " +
//                MySongsHelper.COLUMN_VISIBILITY + " = ? and " +
//                ReachDatabaseHelper.COLUMN_STATUS + " = ?";
//        selectionArgumentsDownloader = new String[]{serverId + "", "1", ReachDatabase.FINISHED + ""};
//        getLoaderManager().restartLoader(StaticData.PUSH_DOWNLOADED_LOADER, null, this);
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

            selectionMyLibrary = MySongsHelper.COLUMN_USER_ID + " = ? and " +
                    MySongsHelper.COLUMN_VISIBILITY + " = ?";
            selectionArgumentsMyLibrary = new String[]{serverId + "", "1"};

            selectionDownloader = ReachDatabaseHelper.COLUMN_RECEIVER_ID + " = ? and " +
                    ReachDatabaseHelper.COLUMN_VISIBILITY + " = ? and " +
                    ReachDatabaseHelper.COLUMN_STATUS + " = ?";
            selectionArgumentsDownloader = new String[]{serverId + "", "1", ReachDatabase.FINISHED + ""};
        } else {

            selectionMyLibrary = MySongsHelper.COLUMN_USER_ID + " = ? and " +
                    MySongsHelper.COLUMN_VISIBILITY + " = ? and " +
                    MySongsHelper.COLUMN_DISPLAY_NAME + " LIKE ?";
            selectionArgumentsMyLibrary = new String[]{serverId + "", "1", "%" + mCurFilter + "%"};

            selectionDownloader = ReachDatabaseHelper.COLUMN_RECEIVER_ID + " = ? and " +
                    ReachDatabaseHelper.COLUMN_VISIBILITY + " = ? and " +
                    ReachDatabaseHelper.COLUMN_STATUS + " = ? and " +
                    ReachDatabaseHelper.COLUMN_DISPLAY_NAME + " LIKE ?";
            selectionArgumentsDownloader = new String[]{serverId + "", "1", ReachDatabase.FINISHED + "", "%" + mCurFilter + "%"};
        }

        getLoaderManager().restartLoader(StaticData.PUSH_MY_LIBRARY_LOADER, null, this);
        getLoaderManager().restartLoader(StaticData.PUSH_DOWNLOADED_LOADER, null, this);
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
                    + " must implement OnNextListener");
        }
    }

    private void loadAdapter() {

        /**
         * Set up adapter for Music player
         */
        if (reference == null)
            return;

        combinedAdapter = new MergeAdapter();

        final Context context = getContext();

        combinedAdapter.addView(LocalUtils.getDownloadedTextView(context));
        combinedAdapter.addView(emptyDownload = LocalUtils.getEmptyDownload(context), false);
        combinedAdapter.addAdapter(downloadedAdapter = new PushSongAdapter(context, R.layout.pushlibrary_item, null, 0, PushSongsFragment.this));

        combinedAdapter.addView(LocalUtils.getMyLibraryTextView(context));
        combinedAdapter.addView(emptyMyLibrary = LocalUtils.getEmptyLibrary(context), false);
        combinedAdapter.addAdapter(myLibraryAdapter = new PushSongAdapter(context, R.layout.pushlibrary_item, null, 0, PushSongsFragment.this));

        pushLibraryList.setAdapter(combinedAdapter);

        getLoaderManager().initLoader(StaticData.PUSH_MY_LIBRARY_LOADER, null, this);
        getLoaderManager().initLoader(StaticData.PUSH_DOWNLOADED_LOADER, null, this);
    }

    @Override
    public boolean isSelected(int hashCode) {
        return LocalUtils.booleanArray.get(hashCode, false);
    }

    private enum LocalUtils {
        ;

        public static TextView getDownloadedTextView(Context context) {

            if (context == null)
                return null;
            final TextView textView = new TextView(context);
            textView.setText("Downloaded");
            textView.setTextColor(ContextCompat.getColor(context, R.color.darkgrey));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
            textView.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(10), 0, 0);
            return textView;
        }

        public static TextView getMyLibraryTextView(Context context) {

            if (context == null)
                return null;
            final TextView textView = new TextView(context);
            textView.setText("My Songs");
            textView.setTextColor(ContextCompat.getColor(context, R.color.darkgrey));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
            textView.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(10), 0, 0);
            return textView;
        }

        public static TextView getEmptyDownload(Context context) {

            if (context == null)
                return null;
            final TextView emptyTV1 = new TextView(context);
            emptyTV1.setText("No downloaded songs");
            emptyTV1.setTextColor(ContextCompat.getColor(context, R.color.darkgrey));
            emptyTV1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            emptyTV1.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(10), 0, 0);
            return emptyTV1;
        }

        public static TextView getEmptyLibrary(Context context) {

            if (context == null)
                return null;
            final TextView emptyTV2 = new TextView(context);
            emptyTV2.setText("No Music on your phone");
            emptyTV2.setTextColor(ContextCompat.getColor(context, R.color.darkgrey));
            emptyTV2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            emptyTV2.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(10), 0, 0);
            return emptyTV2;
        }

        public static final HashSet<TransferSong> selectedList = new HashSet<>();
        public static final SparseBooleanArray booleanArray = new SparseBooleanArray();

        public static final AdapterView.OnItemClickListener listener = (parent, view, position, id) -> {

            final MergeAdapter adapter = (MergeAdapter) parent.getAdapter();

            final ImageView toggle = (ImageView) view.findViewById(R.id.listToggle);
            final Cursor songCursor = (Cursor) adapter.getItem(position);

            //create transfer song object
            final TransferSong transferSong = new TransferSong(
                    songCursor.getLong(7), //size of song
                    songCursor.getLong(1), //songId
                    songCursor.getLong(5), //duration
                    songCursor.getString(2), //displayName
                    songCursor.getString(3), //actualName
                    songCursor.getString(4), //artistName
                    songCursor.getString(6), //album
                    songCursor.getString(8), //genre
                    songCursor.getBlob(9)); //albumArtData
            final int hashCode = transferSong.hashCode();

            if (!booleanArray.get(hashCode, false)) {

                if (selectedList.size() < 5) {

                    booleanArray.append(hashCode, true);
                    selectedList.add(transferSong);
                    toggle.setBackgroundResource(R.drawable.circular_background_dark);
                    toggle.setImageResource(R.drawable.check_white);
                    final int pad = MiscUtils.dpToPx(5);
                    toggle.setPadding(pad, pad, pad, pad);
                } else
                    Toast.makeText(view.getContext(), "Maximum 5 Songs allowed", Toast.LENGTH_SHORT).show();
            } else {

                booleanArray.put(hashCode, false);
                selectedList.remove(transferSong);
                toggle.setBackgroundResource(0);
                toggle.setImageResource(R.drawable.add_grey);
                toggle.setPadding(0, 0, 0, 0);
            }
        };

        public static final class PushSongsDevikaSpecial extends AsyncTask<Void, Integer, Boolean> {

            int offset = 0;

            @Override
            protected Boolean doInBackground(Void... voids) {

                final String pushData = new Gson().toJson(selectedList, new TypeToken<HashSet<TransferSong>>() {
                }.getType());
                final String firstSongsName = selectedList.iterator().next().getDisplayName();
                final int size = selectedList.size();

                final SharedPreferences preferences = MiscUtils.useContextFromFragment(reference, context -> {
                    return context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
                }).orNull();

                final PushContainer pushContainer = new PushContainer(
                        0,                      //receiverID
                        StaticData.devika,      //senderID
                        pushData,               //songData
                        SharedPrefUtils.getUserName(preferences),      //userName
                        "Devika",               //receiverName
                        (short) size,           //songCount
                        SharedPrefUtils.getImageId(preferences),       //imageID
                        firstSongsName,         //firstSongName
                        1 + "");                //networkType

                final String toSend;
                try {
                    toSend = Base64.encodeToString(StringCompress.compress(new Gson().toJson(pushContainer, PushContainer.class)), Base64.DEFAULT);
                } catch (IllegalStateException | JsonSyntaxException | IOException e) {
                    e.printStackTrace();
                    return null;
                }

                Messaging.MessagingEndpoint.DevikaPushToAll devikaPushToAll;
                while (offset != -1) {

                    publishProgress(offset);

                    //create new request
                    devikaPushToAll = MiscUtils.autoRetry(() -> StaticData.messagingEndpoint.devikaPushToAll(
                            offset, //offset
                            1000, //number of users to push to each turn

                            toSend, //the container
                            firstSongsName, //first song name
                            "Party on!", //custom message
                            size), Optional.absent()).orNull();

                    if (devikaPushToAll == null) {
                        Log.i("Ayush", "Error creating request");
                        return false;
                    }

                    final MyString myString = MiscUtils.autoRetry(devikaPushToAll::execute, Optional.of(input -> input == null || TextUtils.isEmpty(input.getString()))).orNull();

                    if (myString == null || TextUtils.isEmpty(myString.getString())) {
                        Log.i("Ayush", "Got a null response during push");
                        return false;
                    }

                    offset = Integer.parseInt(myString.getString());

                    //update the offset
                    if (offset == 0) {
                        Log.i("Ayush", "Offset can not be zero");
                        return false; //shiz
                    }
                }

                return true;
            }

            @Override
            protected void onPostExecute(Boolean myBoolean) {

                super.onPostExecute(myBoolean);
                if (myBoolean == null || !myBoolean)
                    MiscUtils.useContextFromFragment(reference, context -> {
                        Toast.makeText(context, "Network error while sharing songs. Please try again", Toast.LENGTH_SHORT).show();
                    });
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                Toast.makeText(reference.get().getContext(), "Pushed to " + values[0] + " users", Toast.LENGTH_LONG).show();
            }
        }
    }
}
