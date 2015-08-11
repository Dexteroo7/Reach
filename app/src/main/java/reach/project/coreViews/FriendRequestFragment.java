package reach.project.coreViews;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import reach.backend.entities.userApi.model.ReceivedRequest;
import reach.project.R;
import reach.project.adapter.ReachFriendRequestAdapter;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.utils.MiscUtils;
import reach.project.utils.SuperInterface;
import reach.project.utils.auxiliaryClasses.DoWork;

public class FriendRequestFragment extends Fragment {

    private static final List<ReceivedRequest> receivedRequests = new ArrayList<>();
    private static WeakReference<FriendRequestFragment> reference = null;
    private static ReachFriendRequestAdapter adapter = null;
    private static ExecutorService friendsRefresher = null;
    private static long serverId = 0;

    public static FriendRequestFragment newInstance(long id) {

        FriendRequestFragment fragment;
        if (reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new FriendRequestFragment());

        serverId = id;
        return fragment;
    }

    public static void refresh() {

        if (friendsRefresher != null && adapter != null)
            new FetchRequests().executeOnExecutor(friendsRefresher, adapter);
    }

    private final AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            final ReachFriendRequestAdapter adapter = (ReachFriendRequestAdapter) parent.getAdapter();
            if (adapter.accepted.get(position)) {

                final ReceivedRequest receivedRequest = adapter.getItem(position);
                final long userId = receivedRequest.getId();

                //check validity
                final Cursor cursor = view.getContext().getContentResolver().query(
                        Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
                        new String[]{ReachFriendsHelper.COLUMN_ID},
                        ReachFriendsHelper.COLUMN_ID + " = ?",
                        new String[]{userId + ""}, null);

                if (cursor == null || !cursor.moveToFirst()) {

                    if (cursor != null)
                        cursor.close();

                    //fail
                    adapter.remove(receivedRequest);
                    Toast.makeText(view.getContext(), "404", Toast.LENGTH_SHORT).show();
                    return;
                }
                cursor.close();

                mListener.onOpenLibrary(receivedRequest.getId());
            }
        }
    };

    private SuperInterface mListener;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        final ListView listView = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.listView));
        final ReachFriendRequestAdapter adapter = new ReachFriendRequestAdapter(getActivity(), R.layout.notification_item, receivedRequests, serverId);

        listView.setPadding(0, MiscUtils.dpToPx(10), 0, 0);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(itemClickListener);

        friendsRefresher = Executors.unconfigurableExecutorService(new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>()));

        refresh();
        return rootView;
    }

    @Override
    public void onDestroyView() {

        if (friendsRefresher != null)
            friendsRefresher.shutdownNow();
        friendsRefresher = null;

        if (adapter != null)
            adapter.clear();
        adapter = null;
        super.onDestroyView();
    }

    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);
        try {
            mListener = (SuperInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SuperInterface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private static final class FetchRequests extends AsyncTask<ArrayAdapter, Void, ArrayAdapter> {

        @Override
        protected ArrayAdapter doInBackground(ArrayAdapter... params) {

            receivedRequests.clear();

            final Optional<List<ReceivedRequest>> optional = MiscUtils.autoRetry(new DoWork<List<ReceivedRequest>>() {
                @Override
                public List<ReceivedRequest> doWork() throws IOException {

                    final List<ReceivedRequest> receivedRequests =
                            StaticData.userEndpoint.getReceivedRequests(serverId).execute().getItems();
                    Collections.reverse(receivedRequests);
                    return receivedRequests;
                }
            }, Optional.<Predicate<List<ReceivedRequest>>>absent());

            if (optional.isPresent())
                receivedRequests.addAll(optional.get());

            return params[0];
        }

        @Override
        protected void onPostExecute(ArrayAdapter adapter) {

            super.onPostExecute(adapter);

            if (adapter != null)
                adapter.notifyDataSetChanged();
        }
    }
}
