package reach.project.coreViews;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import reach.backend.entities.userApi.model.ReceivedRequest;
import reach.project.R;
import reach.project.adapter.ReachFriendRequestAdapter;
import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;
import reach.project.utils.SuperInterface;
import reach.project.utils.auxiliaryClasses.DoWork;

public class FriendRequestFragment extends Fragment {

    private static long serverId;
    private static AtomicBoolean refreshing = new AtomicBoolean(false);
    private static final List<ReceivedRequest> receivedRequests = new ArrayList<>();
    private static WeakReference<FriendRequestFragment> reference = null;

    public static FriendRequestFragment newInstance(long serverId) {

        FriendRequestFragment.serverId = serverId;
        FriendRequestFragment fragment;
        if (reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new FriendRequestFragment());
        return fragment;
    }

    private final AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            final ReachFriendRequestAdapter adapter = (ReachFriendRequestAdapter) parent.getAdapter();
            if (adapter.accepted.get(position)) {
                final ReceivedRequest receivedRequest = adapter.getItem(position);
                mListener.onOpenLibrary(receivedRequest.getId());
            }
        }
    };

    private SuperInterface mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        final ListView listView = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.listView));
        final ReachFriendRequestAdapter adapter = new ReachFriendRequestAdapter(getActivity(), R.layout.notification_item, receivedRequests, serverId);

        listView.setPadding(0, MiscUtils.dpToPx(10), 0, 0);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(itemClickListener);

        if (!refreshing.get()) {
            refreshing.set(true);
            new FetchRequests().executeOnExecutor(StaticData.threadPool, adapter);
        }
        return rootView;
    }

    @Override
    public void onDestroyView() {
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

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            refreshing.set(false);
        }
    }
}
