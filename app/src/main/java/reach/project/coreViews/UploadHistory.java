package reach.project.coreViews;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.LongSparseArray;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import reach.backend.entities.userApi.model.CompletedOperation;
import reach.backend.entities.userApi.model.CompletedOperationCollection;
import reach.project.R;
import reach.project.adapter.ReachQueueAdapter;
import reach.project.adapter.ReachUploadAdapter;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachDatabaseProvider;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.sql.ReachDatabaseHelper;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.utils.auxiliaryClasses.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

/**
 * Created by Dexter on 6/16/2015.
 */
public class UploadHistory extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private final List<CompletedOperation> completedOperations = new ArrayList<>();
    private final LongSparseArray<String> friendLongSparseArray = new LongSparseArray<>();

    private ReachQueueAdapter onGoingUploadsAdapter;
    private ReachUploadAdapter uploadAdapter;
    private ListView uploadList;
    private ProgressBar loading;
    private MergeAdapter uploadHistoryAdapter;
    private TextView emptyTV1,emptyTV2;

    private static WeakReference<UploadHistory> reference;
    public static UploadHistory newUploadInstance() {

        UploadHistory fragment;
        if(reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new UploadHistory());
        return fragment;
    }

    @Override
    public void onDestroyView() {

        getLoaderManager().destroyLoader(StaticData.UPLOAD_LOADER);
        if(onGoingUploadsAdapter != null &&
                onGoingUploadsAdapter.getCursor() != null &&
                !onGoingUploadsAdapter.getCursor().isClosed())
            onGoingUploadsAdapter.getCursor().close();
        onGoingUploadsAdapter = null;

        friendLongSparseArray.clear();
        completedOperations.clear();
        if(uploadAdapter != null)
            uploadAdapter.cleanUp();
        uploadList = null;
        super.onDestroyView();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        return new CursorLoader(getActivity(),
                ReachDatabaseProvider.CONTENT_URI,
                StaticData.DOWNLOADED_LIST,
                ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?",
                new String[]{1 + ""},
                ReachDatabaseHelper.COLUMN_ADDED + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        if(cursorLoader.getId() == StaticData.UPLOAD_LOADER && cursor != null && !cursor.isClosed()) {

            onGoingUploadsAdapter.swapCursor(cursor);
            final int count = cursor.getCount();
            if (count == 0 && uploadList != null) {
                uploadHistoryAdapter.setActive(emptyTV1, true);
                MiscUtils.setEmptyTextforListView(uploadList, "No one is streaming from you currently");
            }
            else
                uploadHistoryAdapter.setActive(emptyTV1, false);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if(cursorLoader.getId() == StaticData.UPLOAD_LOADER)
            onGoingUploadsAdapter.swapCursor(null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        final ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        if (actionBar!=null)
            actionBar.setTitle("Upload History");
        uploadList = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.listView));

        long myId = SharedPrefUtils.getServerId(getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS));
        if(myId == 0)
            return rootView;
        uploadAdapter = new ReachUploadAdapter(getActivity(),
                R.layout.upload_queue_item, completedOperations, friendLongSparseArray);
        onGoingUploadsAdapter = new ReachQueueAdapter(getActivity(), null, 0);
        uploadList.setSelector(android.R.color.transparent);

        uploadHistoryAdapter = new MergeAdapter();
        final TextView textView = new TextView(rootView.getContext());
        textView.setText("Ongoing");
        textView.setTextColor(getResources().getColor(R.color.reach_color));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        textView.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(10), 0, 0);
        uploadHistoryAdapter.addView(textView);

        emptyTV1 = new TextView(getActivity());
        emptyTV1.setText("No one is streaming currently");
        emptyTV1.setTextColor(getResources().getColor(R.color.darkgrey));
        emptyTV1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        emptyTV1.setPadding(MiscUtils.dpToPx(15),MiscUtils.dpToPx(15),0,MiscUtils.dpToPx(15));
        uploadHistoryAdapter.addView(emptyTV1,false);
        uploadHistoryAdapter.setActive(emptyTV1,false);

        uploadHistoryAdapter.addAdapter(onGoingUploadsAdapter);
        TextView textView2 = new TextView(rootView.getContext());
        textView2.setText("Completed");
        textView2.setTextColor(getResources().getColor(R.color.reach_color));
        textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
        textView2.setTypeface(textView2.getTypeface(), Typeface.BOLD);
        textView2.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(10), 0, 0);
        uploadHistoryAdapter.addView(textView2);

        loading = new ProgressBar(getActivity());
        loading.setIndeterminate(true);
        loading.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT));
        uploadHistoryAdapter.addView(loading);

        emptyTV2 = new TextView(getActivity());
        emptyTV2.setText("No uploads completed");
        emptyTV2.setTextColor(getResources().getColor(R.color.darkgrey));
        emptyTV2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        emptyTV2.setPadding(MiscUtils.dpToPx(15),MiscUtils.dpToPx(15),0,MiscUtils.dpToPx(15));
        uploadHistoryAdapter.addView(emptyTV2,false);
        uploadHistoryAdapter.setActive(emptyTV2,false);

        uploadHistoryAdapter.addAdapter(uploadAdapter);
        uploadList.setAdapter(uploadHistoryAdapter);

        getLoaderManager().initLoader(StaticData.UPLOAD_LOADER, null, this);
        new GetUploadHistory().execute(myId);
        return rootView;
    }

    private class GetUploadHistory extends AsyncTask<Long, Void, Void> {

        @Override
        protected Void doInBackground(final Long... params) {

            final CompletedOperationCollection dataToReturn = MiscUtils.autoRetry(new DoWork<CompletedOperationCollection>() {
                @Override
                public CompletedOperationCollection doWork() throws IOException {
                    return StaticData.userEndpoint.getCompletedOperations(params[0]).execute();
                }
            }, Optional.<Predicate<CompletedOperationCollection>>absent()).orNull();
            final List<CompletedOperation> list;
            if(dataToReturn == null || (list = dataToReturn.getItems()) == null || list.isEmpty()) return null;

            final Set<Long> ids = new HashSet<>();
            for(CompletedOperation completedOperation : list)
                ids.addAll(completedOperation.getReceiver());
            final String [] whereArgument = new String[ids.size()];
            int i = 0;
            for(Long id : ids)
                whereArgument[i++] = id+"";

            final int argCount = ids.size(); // number of IN arguments
            final StringBuilder inList = new StringBuilder(argCount*2);
            for(i=0;i<argCount;i++) {
                if(i > 0)
                    inList.append(",");
                inList.append("?");
            }
            final String whereClause = ReachFriendsHelper.COLUMN_ID + " IN ("+inList.toString()+")";
            if(getActivity() == null)
                return null;

            final Cursor cursor = getActivity().getContentResolver().query(
                    ReachFriendsProvider.CONTENT_URI,
                    new String[]{
                            ReachFriendsHelper.COLUMN_ID,
                            ReachFriendsHelper.COLUMN_USER_NAME
                    },
                    whereClause,
                    whereArgument, null);
            if(cursor != null) {
                friendLongSparseArray.clear();
                while (cursor.moveToNext())
                    friendLongSparseArray.append(cursor.getLong(0), cursor.getString(1));
                cursor.close();
            }
            completedOperations.clear();
            completedOperations.addAll(list);
            Collections.sort(completedOperations, new Comparator<CompletedOperation>() {
                @Override
                public int compare(CompletedOperation lhs, CompletedOperation rhs) {
                    return rhs.getTime().compareTo(lhs.getTime());
                }
            });

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            if(isCancelled() || getActivity() == null || getActivity().isFinishing()  || uploadList == null)
                return;
            uploadHistoryAdapter.setActive(loading,false);
            if (completedOperations == null || completedOperations.size()==0) {
                uploadHistoryAdapter.setActive(emptyTV2,true);
                MiscUtils.setEmptyTextforListView(uploadList, "No uploads done yet");
            }
            else {
                uploadHistoryAdapter.setActive(emptyTV2, false);
            }
            uploadAdapter.notifyDataSetChanged();
            super.onPostExecute(aVoid);
        }
    }
}