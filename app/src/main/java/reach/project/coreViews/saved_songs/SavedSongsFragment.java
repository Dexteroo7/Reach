package reach.project.coreViews.saved_songs;


import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import javax.annotation.Nonnull;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;
import reach.project.utils.ancillaryClasses.SuperInterface;
import reach.project.utils.viewHelpers.HandOverMessage;


public class SavedSongsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, HandOverMessage<Object>{


    private static final int SAVED_SONGS_LOADER = 54321;
    private RecyclerView mSavedSongsList;
    private SavedSongsAdapter mListAdapter;
    public static final String TAG = SavedSongsFragment.class.getSimpleName();
    private SuperInterface mListener;
    private View emptyView;
    private TextView emptyTextView;
    private SearchView searchView;
    final Bundle bundle = new Bundle();
    public static final String YES = "YES";
    public static final String NO = "NO";

    public SavedSongsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_saved_data, container, false);
        final Toolbar mToolbar = (Toolbar) rootView.findViewById(R.id.myReachToolbar);
        mToolbar.setTitle("Saved Songs");
        mToolbar.inflateMenu(R.menu.pager_menu);
        mToolbar.setOnMenuItemClickListener(mListener != null ? mListener.getMenuClickListener() : null);
        SearchManager searchManager =
                (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchViewMenuItem = mToolbar.getMenu().findItem(R.id.search);
        searchView = (SearchView) searchViewMenuItem.getActionView();
        searchView.setQueryHint("Search");
        mSavedSongsList = (RecyclerView)rootView.findViewById(R.id.saved_songs_recyclerview);
        mListAdapter = new SavedSongsAdapter(this);
        mSavedSongsList.setLayoutManager(new LinearLayoutManager(getContext()));
        mSavedSongsList.setAdapter(mListAdapter);
        emptyView = rootView.findViewById(R.id.empty_imageView);
        emptyTextView = (TextView) rootView.findViewById(R.id.empty_textView);
        emptyTextView.setText("No Saved Songs");
        getLoaderManager().initLoader(SAVED_SONGS_LOADER,null,this);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                if(newText == null){
                    return true;
                }

                final String constraint = MiscUtils.getFilterLikeString(newText);
                bundle.putString(StaticData.FILTER_STRING_KEY,constraint );
                getLoaderManager().restartLoader(SAVED_SONGS_LOADER, bundle, SavedSongsFragment.this);
                return true;
            }
        });

        MenuItemCompat.setOnActionExpandListener(searchViewMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return true;
            }
        });


        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(args == null) {
            return new CursorLoader(getActivity(),
                    SavedSongsContract.SavedSongsEntry.CONTENT_URI,
                    SavedSongsContract.SavedSongsEntry.projection,
                    SavedSongsContract.SavedSongsEntry.TYPE + " = ? ",
                    new String[]{"1"},
                    SavedSongsContract.SavedSongsEntry.DATE_ADDED + " DESC "
            );
        }
        else{
            final String filterString = args.getString(StaticData.FILTER_STRING_KEY);
            return new CursorLoader(getActivity(),
                    SavedSongsContract.SavedSongsEntry.CONTENT_URI,
                    SavedSongsContract.SavedSongsEntry.projection,
                    SavedSongsContract.SavedSongsEntry.TYPE + " = ? AND " + SavedSongsContract.SavedSongsEntry.DISPLAY_NAME + " LIKE ?",
                    new String[]{"1",filterString},
                    SavedSongsContract.SavedSongsEntry.DATE_ADDED + " DESC "
            );
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(TAG, "onLoadFinished: Saved Songs");

        if(data == null || data.isClosed()){
            Toast.makeText(getActivity().getApplicationContext(), "Error!", Toast.LENGTH_SHORT).show();
            showErrorView();
            return;
        }

        if(data.getCount()==0){
            //Toast.makeText(getActivity().getApplicationContext(), "No Saved Songs", Toast.LENGTH_SHORT).show();
            showEmptyView();
        }
        else {
            hideEmptyView();
        }

        Log.d(TAG, "onLoadFinished: Saved Songs Count = " + data.getCount());

        //data.moveToFirst();
        //Toast.makeText(getActivity(), "Data = " + data.getString(1), Toast.LENGTH_LONG).show();
        mListAdapter.setNewMySavesSongsCursor(data);

    }

    private void hideEmptyView() {
        if(emptyView == null || mSavedSongsList == null)
            return;
        emptyView.setVisibility(View.GONE);
        mSavedSongsList.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mListAdapter.setNewMySavesSongsCursor(null);
    }

    private void showErrorView() {
    }

    private void showEmptyView() {
        Log.d(TAG, "showEmptyView: Inside show emptyView");
        if(emptyView == null || mSavedSongsList == null) {
            Log.d(TAG, "showEmptyView: view is null");
            return;
        }
        Log.d(TAG, "showing EmptyView");
        emptyView.setVisibility(View.VISIBLE);
        mSavedSongsList.setVisibility(View.GONE);
    }



    @Override
    public void handOverMessage(@Nonnull Object message) {
        if(message == null) {
            Log.e(TAG, "handOverMessage: Got null from adapter");
            return;
        }
        final Pair data = (Pair)message;
        final Cursor cursorData = (Cursor) data.first;
        final int action = (int) data.second;

        final String yTid = cursorData.getString(0);


        if( action == 0 ){
            if(yTid == null || TextUtils.isEmpty(yTid)){
                throw new IllegalArgumentException("Ytid is null in SavedSongsFragment");
            }
            mListener.showYTVideo(yTid);
        }
        else if( action == 1 ){
            if(yTid == null || TextUtils.isEmpty(yTid)){
                throw new IllegalArgumentException("Ytid is null in SavedSongsFragment");
            }

            DialogInterface.OnClickListener positiveClickListener = new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(yTid == null || TextUtils.isEmpty(yTid))
                        return;

                    new DeleteSongInDatabaseTask(getActivity(),yTid,"Saved").execute();
                }
            };


            AlertDialog removeSavedDataDialog = MiscUtils.getAlertDialogBox(getActivity(),
                    "Are you sure you want to remove this song?",
                    positiveClickListener,
                    null,
                    YES,
                    NO
            );

            removeSavedDataDialog.show();



        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(mListAdapter!=null){
            mListAdapter.destroyCursorData();
        }
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        try {
            mListener = (SuperInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement SplashInterface");
        }
    }

    @Override
    public void onDetach() {

        super.onDetach();
        Log.d("Ayush", "ExploreFragment - onDetach");
        mListener = null;
    }

}
