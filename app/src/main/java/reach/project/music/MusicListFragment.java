package reach.project.music;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import java.lang.ref.WeakReference;
import java.util.Map;

import reach.backend.music.musicVisibilityApi.model.JsonMap;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.friends.ReachFriendsHelper;
import reach.project.friends.ReachFriendsProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.SuperInterface;
import reach.project.utils.viewHelpers.kmshack.newsstand.ScrollTabHolderFragment;

public class MusicListFragment extends ScrollTabHolderFragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SearchView.OnQueryTextListener, SearchView.OnCloseListener, AbsListView.OnScrollListener {

    private SuperInterface mListener;
    private ReachMusicAdapter reachMusicAdapter = null;
    private String mCurFilter;
    private String whereClause;
    private String[] whereArgs;

    private long userId;
    private byte type;
    private ListView musicList;
    private SearchView searchView;
    private int mPosition;

    private final AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if (mListener != null && position != 0) {

                final Cursor cursor = (Cursor) reachMusicAdapter.getItem(position - 1);
                final long senderId = cursor.getLong(2);

                final Cursor senderCursor = getActivity().getContentResolver().query(
                        Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + senderId),
                        new String[]{ReachFriendsHelper.COLUMN_USER_NAME,
                                ReachFriendsHelper.COLUMN_STATUS,
                                ReachFriendsHelper.COLUMN_NETWORK_TYPE},
                        ReachFriendsHelper.COLUMN_ID + " = ?",
                        new String[]{senderId + ""}, null);

                if (senderCursor == null)
                    return;
                if (!senderCursor.moveToFirst()) {
                    senderCursor.close();
                    return;
                }
                if (searchView != null)
                    ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE);
                if (SharedPrefUtils.getReachQueueSeen(sharedPreferences)) {
                    Snackbar.make(parent, cursor.getString(3) + " added to your Queue", Snackbar.LENGTH_LONG)
                            .setAction("VIEW", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mListener.anchorFooter();
                                }
                            })
                            .show();
                } else {
                    SharedPrefUtils.setReachQueueSeen(sharedPreferences);
                    mListener.anchorFooter();
                }

                mListener.addSongToQueue(
                        cursor.getLong(1), //songId
                        cursor.getLong(2), //senderId
                        cursor.getLong(8), //size
                        cursor.getString(3), //displayName
                        cursor.getString(4), //actualName
                        false, //multiple
                        senderCursor.getString(0), //userName
                        senderCursor.getShort(1) + "", //onlineStatus
                        senderCursor.getShort(2) + "", //networkType
                        cursor.getString(5), //artistName
                        cursor.getLong(7), //duration
                        cursor.getString(6), //album
                        cursor.getString(10)); //genre
                senderCursor.close();
            }
        }
    };

    private static WeakReference<MusicListFragment> fullListReference;
    private static WeakReference<MusicListFragment> recentListReference;

    public static MusicListFragment newFullListInstance(long id, int pos) {

        final Bundle args;
        MusicListFragment fragment;
        if (fullListReference == null || (fragment = fullListReference.get()) == null) {

            fullListReference = new WeakReference<>(fragment = new MusicListFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing Music list fragment object :)");
            args = fragment.getArguments();
        }

        args.putLong("id", id);
        args.putByte("type", StaticData.FULL_LIST_LOADER);
        args.putInt("position", pos);
        return fragment;
    }

    public static MusicListFragment newRecentListInstance(long id, int pos) {

        final Bundle args;
        MusicListFragment fragment;
        if (recentListReference == null || (fragment = recentListReference.get()) == null) {

            recentListReference = new WeakReference<>(fragment = new MusicListFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing Music list fragment object :)");
            args = fragment.getArguments();
        }

        args.putLong("id", id);
        args.putByte("type", StaticData.RECENT_LIST_LOADER);
        args.putInt("position", pos);
        return fragment;
    }

//    public static MusicListFragment newTypeInstance(long id, String albumName, String artistName, String playListName, int type) {
//
//        final Bundle args;
//        MusicListFragment fragment;
//        if (typeReference == null || (fragment = typeReference.get()) == null) {
//            typeReference = new WeakReference<>(fragment = new MusicListFragment());
//            fragment.setArguments(args = new Bundle());
//        } else {
//            Log.i("Ayush", "Reusing Music list fragment object :)");
//            args = fragment.getArguments();
//        }
//
//        args.putLong("id", id);
//        args.putString("albumName", albumName);
//        args.putString("artistName", artistName);
//        args.putString("playListName", playListName);
//        args.putInt("type", type);
//        //0 = songs, 1 = albums, 2 = artists, 3 = playlists
//        return fragment;
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        type = getArguments().getByte("type", (byte) 0);

        final View rootView = inflater.inflate(R.layout.fragment_simple_list, container, false);
        mPosition = getArguments().getInt("position");
        musicList = MiscUtils.addLoadingToMusicListView((ListView) rootView.findViewById(R.id.listView));


        View placeHolderView = inflater.inflate(R.layout.view_header_placeholder, musicList, false);
        musicList.addHeaderView(placeHolderView);
        musicList.setOnScrollListener(this);
        userId = getArguments().getLong("id");

        if (searchView != null) {

            searchView.setOnQueryTextListener(this);
            searchView.setOnCloseListener(this);
        }

        whereClause = MySongsHelper.COLUMN_USER_ID + " = ? and " +
                MySongsHelper.COLUMN_VISIBILITY + " = ? ";
        whereArgs = new String[]{userId + "", "1"};

//        switch (type) {
//
//            case StaticData.FULL_LIST_LOADER: {
//
//
//                break;
//            }
//            case StaticData.RECENT_LIST_LOADER: {
//
//                whereClause = MySongsHelper.COLUMN_USER_ID + " = ? and " +
//                        MySongsHelper.COLUMN_VISIBILITY + " = ? and " +
//                        MySongsHelper.COLUMN_ALBUM + " = ? ";
//                whereArgs = new String[]{userId + "", 1 + "", getArguments().getString("albumName")};
//                break;
//            }

//            case 2: {
//
//                whereClause = MySongsHelper.COLUMN_USER_ID + " = ? and " +
//                        MySongsHelper.COLUMN_VISIBILITY + " = ? and " +
//                        MySongsHelper.COLUMN_ARTIST + " = ? ";
//                whereArgs = new String[]{userId + "", 1 + "", getArguments().getString("artistName")};
//                break;
//            }
//            case 3: {
//
//                final Cursor cursor = getActivity().getContentResolver().query(
//                        ReachPlayListProvider.CONTENT_URI,
//                        new String[]{ReachPlayListHelper.COLUMN_ARRAY_OF_SONG_IDS},
//                        ReachPlayListHelper.COLUMN_USER_ID + " = ? and " +
//                                ReachPlayListHelper.COLUMN_PLAY_LIST_NAME + " = ?",
//                        new String[]{
//                                userId + "",
//                                getArguments().getString("playListName")},
//                        null);
//
//                final String[] ids;
//                if (cursor == null ||
//                        !cursor.moveToFirst() ||
//                        (ids = ReachPlayListHelper.toStringArray(cursor.getBlob(0))) == null ||
//                        ids.length == 0) {
//
//                    if (cursor != null)
//                        cursor.close();
//                    MiscUtils.setEmptyTextForListView(musicList, "No visible songs found");
//                    return rootView;
//                }
//                cursor.close();
//
//                Log.i("Ayush", "Looking for playLists in " + Arrays.toString(ids));
//                final int argCount = ids.length; // number of IN arguments
//                final StringBuilder inList = new StringBuilder(argCount * 2);
//                for (int i = 0; i < argCount; i++) {
//                    if (i > 0)
//                        inList.append(",");
//                    inList.append("?");
//                }
//                whereClause = MySongsHelper.COLUMN_SONG_ID + " IN (" + inList.toString() + ") and " +
//                        MySongsHelper.COLUMN_VISIBILITY + " = 1 and " +
//                        MySongsHelper.COLUMN_USER_ID + " = " + userId;
//                whereArgs = ids;
//                break;
//            }
//        }

        reachMusicAdapter = new ReachMusicAdapter(getActivity(), R.layout.musiclist_item, null, 0, ReachMusicAdapter.LIST);
        musicList.setAdapter(reachMusicAdapter);
        musicList.setOnItemClickListener(listener);

        getLoaderManager().initLoader(type, null, this);
        return rootView;
    }

    @Override
    public void onDestroyView() {

        getLoaderManager().destroyLoader(type);
        if (reachMusicAdapter != null && reachMusicAdapter.getCursor() != null && !reachMusicAdapter.getCursor().isClosed())
            reachMusicAdapter.getCursor().close();
        reachMusicAdapter = null;

        if (searchView != null) {

            searchView.setOnQueryTextListener(null);
            searchView.setOnCloseListener(null);
            searchView.setQuery(null, false);
            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }

        //searchView = null;
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
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {

        if (id == StaticData.FULL_LIST_LOADER && type == StaticData.FULL_LIST_LOADER) {

            return new CursorLoader(getActivity(),
                    MySongsProvider.CONTENT_URI,
                    reachMusicAdapter.getProjectionMyLibrary(),
                    whereClause,
                    whereArgs,
                    MySongsHelper.COLUMN_DISPLAY_NAME + " ASC");
        } else if (id == StaticData.RECENT_LIST_LOADER && type == StaticData.RECENT_LIST_LOADER) {

            return new CursorLoader(getActivity(),
                    MySongsProvider.CONTENT_URI,
                    reachMusicAdapter.getProjectionMyLibrary(),
                    whereClause,
                    whereArgs,
                    MySongsHelper.COLUMN_DATE_ADDED + " DESC LIMIT 20");
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        if ((cursorLoader.getId() == StaticData.FULL_LIST_LOADER ||
                cursorLoader.getId() == StaticData.RECENT_LIST_LOADER)
                && cursor != null && !cursor.isClosed()) {

            reachMusicAdapter.swapCursor(cursor);
//            int count = cursor.getCount();
//            final Future<?> isMusicFetching = ContactsListFragment.isMusicFetching.get(userId);
//            if((isMusicFetching == null || isMusicFetching.isCancelled() || isMusicFetching.isDone()) && count==0 && musicList!=null)
//                MiscUtils.setEmptyTextForListView(musicList,"No visible songs found");
//            if (getArguments().getInt("type") != 0) {
//
//                final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
//                if (actionBar != null)
//                    actionBar.setSubtitle(count + " Songs");
//            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (cursorLoader.getId() == StaticData.FULL_LIST_LOADER ||
                cursorLoader.getId() == StaticData.RECENT_LIST_LOADER)
            reachMusicAdapter.swapCursor(null);
    }

    @Override
    public boolean onClose() {

//        searchView.setQuery(null, true);
//        whereClause = MySongsHelper.COLUMN_USER_ID + " = ? and " +
//                MySongsHelper.COLUMN_VISIBILITY + " = ? ";
//        whereArgs = new String[]{userId + "", "1"};
//
//        getLoaderManager().restartLoader(type, null, this);
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

        final String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
        if (mCurFilter == null && newFilter == null) {
            return true;
        }
        if (mCurFilter != null && mCurFilter.equals(newFilter)) {
            return true;
        }
        mCurFilter = newFilter;
        final String filter = "%" + mCurFilter + "%";

        if (TextUtils.isEmpty(newText)) {

            whereClause = MySongsHelper.COLUMN_USER_ID + " = ? and " +
                    MySongsHelper.COLUMN_VISIBILITY + " = ? ";
            whereArgs = new String[]{userId + "", "1"};
        } else {

            whereClause =
                    MySongsHelper.COLUMN_USER_ID + " = ? and " +
                            MySongsHelper.COLUMN_VISIBILITY + " = ? and (" +
                            MySongsHelper.COLUMN_ACTUAL_NAME + " LIKE ? or " +
                            MySongsHelper.COLUMN_ARTIST + " LIKE ? or " +
                            MySongsHelper.COLUMN_ALBUM + " LIKE ? or " +
                            MySongsHelper.COLUMN_DISPLAY_NAME + " LIKE ?)";
            whereArgs = new String[]{userId + "", "1", filter, filter, filter, filter};
        }

        try {
            getLoaderManager().restartLoader(type, null, this);
        } catch (IllegalStateException ignored) {
        }

        return true;
    }

    @Override
    public void adjustScroll(int scrollHeight) {

        if (scrollHeight == 0 && musicList.getFirstVisiblePosition() >= 1) {
            return;
        }
        musicList.setSelectionFromTop(1, scrollHeight);

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

    public void setSearchView(SearchView sView) {

        if (isAdded())
            onClose();

        if (sView == null && searchView != null) {
            //invalidate old
            searchView.setOnQueryTextListener(null);
            searchView.setOnCloseListener(null);
            searchView = null;
        } else if (sView != null) {
            //set new
            searchView = sView;
            searchView.setOnQueryTextListener(this);
            searchView.setOnCloseListener(this);
        }
    }

//    private static final class GetMusic implements Runnable {
//
//        private final long hostId;
//
//        private GetMusic(long hostId) {
//            this.hostId = hostId;
//        }
//
//        @Override
//        public void run() {
//
//            //fetch Music
//            final Boolean aBoolean = MiscUtils.useContextFromFragment(reference, (Context context) -> CloudStorageUtils.getMusicData(hostId, context)).orNull();
//
//            //do we check for visibility ?
//            final boolean exit = aBoolean == null || !aBoolean; //reverse because false means exit
//            if (exit) {
//                Log.i("Ayush", "do not check for visibility");
//                return;
//            }
//
//            Log.i("Ayush", "Fetching visibility data");
//            //fetch visibility data
//            final MusicData visibility = MiscUtils.autoRetry(() -> StaticData.musicVisibility.get(hostId).execute(), Optional.absent()).orNull();
//            final JsonMap visibilityMap;
//            if (visibility == null || (visibilityMap = visibility.getVisibility()) == null || visibilityMap.isEmpty()) {
//                Log.i("Ayush", "no visibility data found");
//                return; //no visibility data found
//            }
//
//            //handle visibility data
//            MiscUtils.useContextFromFragment(reference, (Context context) -> handleVisibility(context, visibilityMap, hostId));
//        }
//    }

    private static synchronized void handleVisibility(Context context,
                                                      JsonMap visibilityMap,
                                                      long serverId) {

        final MySongsHelper mySongsHelper = new MySongsHelper(context);
        final SQLiteDatabase sqlDB = mySongsHelper.getWritableDatabase();
        sqlDB.beginTransaction();

        try {

            for (Map.Entry<String, Object> objectEntry : visibilityMap.entrySet()) {

                if (objectEntry == null) {
                    //TODO track
                    Log.i("Ayush", "objectEntry was null");
                    continue;
                }

                final String key = objectEntry.getKey();
                final Object value = objectEntry.getValue();

                if (TextUtils.isEmpty(key) || !TextUtils.isDigitsOnly(key) || value == null || !(value instanceof Boolean)) {
                    //TODO track
                    Log.i("Ayush", "Found shit data inside visibilityMap " + key + " " + value);
                    continue;
                }

                final ContentValues values = new ContentValues();
                values.put(MySongsHelper.COLUMN_VISIBILITY, (short) ((Boolean) value ? 1 : 0));

                sqlDB.update(MySongsHelper.SONG_TABLE,
                        values,
                        MySongsHelper.COLUMN_USER_ID + " = ? and " + MySongsHelper.COLUMN_SONG_ID + " = ?", new String[]{serverId + "", key});
            }

            visibilityMap.clear();
            sqlDB.setTransactionSuccessful();

        } finally {

            sqlDB.endTransaction();
            mySongsHelper.close();
        }
        context.getContentResolver().notifyChange(MySongsProvider.CONTENT_URI, null);
    }
}
