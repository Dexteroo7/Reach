package reach.project.coreViews.saved_songs;


import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.utils.viewHelpers.HandOverMessage;


public class SavedSongsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, HandOverMessage<Cursor>{


    private static final int SAVED_SONGS_LOADER = 54321;
    private RecyclerView mSavedSongsList;
    private SavedSongsAdapter mListAdapter;
    public static final String TAG = SavedSongsFragment.class.getSimpleName();

    public SavedSongsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_saved_data, container, false);
        mSavedSongsList = (RecyclerView)v.findViewById(R.id.saved_songs_recyclerview);
        mListAdapter = new SavedSongsAdapter(this);
        mSavedSongsList.setAdapter(mListAdapter);

        getLoaderManager().initLoader(SAVED_SONGS_LOADER,null,this);

        return v;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                SavedSongsContract.SavedSongsEntry.CONTENT_URI,
                SavedSongsContract.SavedSongsEntry.projection,
                null,
                null,
                SavedSongsContract.SavedSongsEntry.DATE_ADDED
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if(data == null || data.isClosed()){
            Toast.makeText(getActivity(), "Error!", Toast.LENGTH_SHORT).show();
            showErrorView();
            return;
        }

        if(data.getCount()==0){
            Toast.makeText(getActivity(), "No Saved Songs", Toast.LENGTH_SHORT).show();
            showEmptyView();
        }

        //data.moveToFirst();
        //Toast.makeText(getActivity(), "Data = " + data.getString(1), Toast.LENGTH_LONG).show();
        mListAdapter.setNewMySavesSongsCursor(data);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mListAdapter.setNewMySavesSongsCursor(null);
    }

    private void showErrorView() {
    }

    private void showEmptyView() {
    }

    @Override
    public void handOverMessage(@Nonnull Cursor message) {
        if(message == null) {
            Log.e(TAG, "handOverMessage: Got null from adapter");
            return;
        }



    }
}
