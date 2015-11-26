package reach.project.coreViews.fileManager;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.google.common.base.Optional;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import reach.backend.entities.userApi.model.CompletedOperation;
import reach.backend.entities.userApi.model.CompletedOperationCollection;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.friends.ReachFriendsHelper;
import reach.project.friends.ReachFriendsProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

/**
 * Created by Dexter on 6/16/2015.
 */
public class UploadHistory extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final List<CompletedOperation> completedOperations = new ArrayList<>();
    private static final LongSparseArray<String> friendLongSparseArray = new LongSparseArray<>();

    private ReachUploadAdapter uploadAdapter;
    private ReachQueueAdapter onGoingUploadsAdapter;
    private ListView uploadList;
    private ProgressBar loading;
    private MergeAdapter uploadHistoryAdapter;
    private TextView emptyTV1, emptyTV2;

    private static WeakReference<UploadHistory> reference;

    public static UploadHistory newUploadInstance() {

        UploadHistory fragment;
        if (reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new UploadHistory());
        return fragment;
    }

    @Override
    public void onDestroyView() {

        getLoaderManager().destroyLoader(StaticData.UPLOAD_LOADER);
        if (onGoingUploadsAdapter != null &&
                onGoingUploadsAdapter.getCursor() != null &&
                !onGoingUploadsAdapter.getCursor().isClosed())
            onGoingUploadsAdapter.getCursor().close();
        onGoingUploadsAdapter = null;

        uploadAdapter = null;
        friendLongSparseArray.clear();
        completedOperations.clear();
        uploadList = null;
        super.onDestroyView();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        return new CursorLoader(getActivity(),
                ReachDatabaseProvider.CONTENT_URI,
                ReachDatabaseHelper.ADAPTER_LIST,
                ReachDatabaseHelper.COLUMN_OPERATION_KIND + " = ?",
                new String[]{"1"},
                ReachDatabaseHelper.COLUMN_DATE_ADDED + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        if (cursorLoader.getId() == StaticData.UPLOAD_LOADER && cursor != null && !cursor.isClosed()) {

            onGoingUploadsAdapter.swapCursor(cursor);
            final int count = cursor.getCount();
            if (count == 0 && uploadList != null)
                uploadHistoryAdapter.setActive(emptyTV1, true);
            else
                uploadHistoryAdapter.setActive(emptyTV1, false);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (cursorLoader.getId() == StaticData.UPLOAD_LOADER)
            onGoingUploadsAdapter.swapCursor(null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_list, container, false);

        Toolbar mToolbar = (Toolbar) rootView.findViewById(R.id.listToolbar);
        mToolbar.setTitle("Upload History");
        mToolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());

        uploadList = (ListView) rootView.findViewById(R.id.listView);

        long myId = SharedPrefUtils.getServerId(getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE));
        if (myId == 0)
            return rootView;
        onGoingUploadsAdapter = new ReachQueueAdapter(getActivity(), null, 0);
        uploadList.setSelector(android.R.color.transparent);

        uploadHistoryAdapter = new MergeAdapter();
        final TextView textView = new TextView(rootView.getContext());
        textView.setText("Ongoing");
        textView.setTextColor(ContextCompat.getColor(getContext(), R.color.reach_color));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        textView.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(10), 0, 0);
        uploadHistoryAdapter.addView(textView);

        emptyTV1 = new TextView(getActivity());
        emptyTV1.setText("No one is streaming currently");
        emptyTV1.setTextColor(ContextCompat.getColor(getContext(), R.color.darkgrey));
        emptyTV1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        emptyTV1.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(15), 0, MiscUtils.dpToPx(15));
        uploadHistoryAdapter.addView(emptyTV1, false);
        uploadHistoryAdapter.setActive(emptyTV1, false);

        uploadHistoryAdapter.addAdapter(onGoingUploadsAdapter);
        TextView textView2 = new TextView(rootView.getContext());
        textView2.setText("Completed");
        textView2.setTextColor(ContextCompat.getColor(getContext(), R.color.reach_color));
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
        emptyTV2.setTextColor(ContextCompat.getColor(getContext(), R.color.darkgrey));
        emptyTV2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        emptyTV2.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(15), 0, MiscUtils.dpToPx(15));
        uploadHistoryAdapter.addView(emptyTV2, false);
        uploadHistoryAdapter.setActive(emptyTV2, false);

        uploadHistoryAdapter.addAdapter(uploadAdapter = new ReachUploadAdapter(getActivity(),
                R.layout.upload_queue_item, completedOperations, friendLongSparseArray));
        uploadList.setAdapter(uploadHistoryAdapter);

        getLoaderManager().initLoader(StaticData.UPLOAD_LOADER, null, this);
        new GetUploadHistory().execute(myId);
        return rootView;
    }

    private static class GetUploadHistory extends AsyncTask<Long, Void, List<CompletedOperation>> {

        @Override
        protected List<CompletedOperation> doInBackground(final Long... params) {

            final CompletedOperationCollection dataToReturn = MiscUtils.autoRetry(() ->
                    StaticData.userEndpoint.getCompletedOperations(params[0]).execute(), Optional.absent()).orNull();
            final List<CompletedOperation> list;
            if (dataToReturn == null || (list = dataToReturn.getItems()) == null || list.isEmpty())
                return null;

            final Set<Long> ids = new HashSet<>(list.size());
            for (CompletedOperation completedOperation : list)
                ids.addAll(completedOperation.getReceiver());
            final String[] whereArgument = new String[ids.size()];
            int i = 0;
            for (Long id : ids)
                whereArgument[i++] = id + "";

            final int argCount = ids.size(); // number of IN arguments
            final StringBuilder inList = new StringBuilder(argCount * 2);
            for (i = 0; i < argCount; i++) {
                if (i > 0)
                    inList.append(",");
                inList.append("?");
            }

            final String whereClause = ReachFriendsHelper.COLUMN_ID + " IN (" + inList.toString() + ")";
            final Cursor cursor = MiscUtils.useContextFromFragment(reference, context -> {

                return context.getContentResolver().query(
                        ReachFriendsProvider.CONTENT_URI,
                        new String[]{
                                ReachFriendsHelper.COLUMN_ID,
                                ReachFriendsHelper.COLUMN_USER_NAME
                        },
                        whereClause,
                        whereArgument, null);
            }).orNull();

            if (cursor == null)
                return null;

            if (!cursor.moveToFirst()) {
                cursor.close();
                return null;
            }

            friendLongSparseArray.clear();
            while (cursor.moveToNext())
                friendLongSparseArray.append(cursor.getLong(0), cursor.getString(1));
            cursor.close();
            Collections.sort(list, (lhs, rhs) -> rhs.getTime().compareTo(lhs.getTime()));
            return list;
        }

        @Override
        protected void onPostExecute(List<CompletedOperation> operations) {

            super.onPostExecute(operations);

            MiscUtils.useFragment(reference, fragment -> {

                fragment.uploadHistoryAdapter.setActive(fragment.loading, false);

                if (operations == null || operations.isEmpty()) {

                    fragment.uploadHistoryAdapter.setActive(fragment.emptyTV2, true);
                    fragment.uploadAdapter.clear();
                } else {

                    completedOperations.clear();
                    completedOperations.addAll(operations);
                    fragment.uploadHistoryAdapter.setActive(fragment.emptyTV2, false);
                    fragment.uploadAdapter.notifyDataSetChanged();
                }
                return null;
            });
        }
    }
}