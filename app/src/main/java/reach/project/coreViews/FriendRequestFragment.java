package reach.project.coreViews;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import reach.backend.entities.userApi.model.ReceivedRequest;
import reach.project.R;
import reach.project.adapter.ReachFriendRequestAdapter;
import reach.project.core.StaticData;
import reach.project.utils.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.SuperInterface;

public class FriendRequestFragment extends Fragment {

    private static WeakReference<FriendRequestFragment> reference = null;
    private SuperInterface mListener;
    private ListView listView;
    public static List<Boolean> opened,accepted;
    private Activity activity;

    public static FriendRequestFragment newInstance() {

        FriendRequestFragment fragment;
        if(reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new FriendRequestFragment());
        return fragment;
    }
    public FriendRequestFragment() {
        // Required empty public constructor
    }

    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (accepted.get(position)) {
                ReceivedRequest receivedRequest = ((ReachFriendRequestAdapter) parent.getAdapter()).getItem(position);
                mListener.onOpenLibrary(receivedRequest.getId());
            }
        }
    };

    private final class FetchRequests extends AsyncTask<Long, Void, List<ReceivedRequest>> {

        @Override
        protected List<ReceivedRequest> doInBackground(Long... params) {
            return MiscUtils.autoRetry(new DoWork<List<ReceivedRequest>>() {
                @Override
                protected List<ReceivedRequest> doWork() throws IOException {
                    long myId = SharedPrefUtils.getServerId(activity.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS));
                    if(myId == 0)
                        return null;
                    List<ReceivedRequest> receivedRequests = StaticData.userEndpoint.getReceivedRequests(myId).execute().getItems();
                    Collections.reverse(receivedRequests);
                    return receivedRequests;
                }
            }, Optional.<Predicate<List<ReceivedRequest>>>absent()).orNull();
        }

        @Override
        protected void onPostExecute(List<ReceivedRequest> receivedRequests) {
            super.onPostExecute(receivedRequests);
            if(isCancelled() || isRemoving() || activity == null || activity.isFinishing() )
                return;

            if (receivedRequests != null && receivedRequests.size()>0) {
                ReachFriendRequestAdapter reachFriendRequestAdapter = new ReachFriendRequestAdapter(getActivity(), R.layout.notification_item, receivedRequests);
                opened = new ArrayList<>(Collections.nCopies(receivedRequests.size(), true));
                accepted = new ArrayList<>(Collections.nCopies(receivedRequests.size(), false));
                listView.setAdapter(reachFriendRequestAdapter);
                listView.setOnItemClickListener(itemClickListener);
            }
            else {
                MiscUtils.setEmptyTextforListView(listView,"No friends requests for you!");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        listView = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.listView));
        new FetchRequests().executeOnExecutor(StaticData.threadPool);
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
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
}
