package reach.project.notificationCentre;

import android.content.Context;
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
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.common.base.Optional;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import reach.backend.entities.userApi.model.ReceivedRequest;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.coreViews.yourProfile.YourProfileActivity;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

public class FriendRequestFragment extends Fragment {

    public static final List<ReceivedRequest> receivedRequests = new ArrayList<>();
    private static long serverId = 0;

    private ExecutorService friendsRefresher = null;
    private ListView listView = null;

    public static FriendRequestFragment newInstance() {
        return new FriendRequestFragment();
    }

    public void refresh() {

        if (friendsRefresher != null && listView != null)
            new FetchRequests(this).executeOnExecutor(friendsRefresher);
    }

    private final AdapterView.OnItemClickListener itemClickListener = (parent, view, position, id) -> {

        final FriendRequestAdapter adapter = (FriendRequestAdapter) parent.getAdapter();
        final ReceivedRequest receivedRequest = adapter.getItem(position);
        final long userId = receivedRequest.getId();
        if (FriendRequestAdapter.accepted.get(userId, false)) {

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
            YourProfileActivity.openProfile(userId, getActivity());
            //mListener.onOpenLibrary(userId);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_notification, container, false);

        listView = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.listView));
        listView.setPadding(0, MiscUtils.dpToPx(10), 0, 0);
        serverId = SharedPrefUtils.getServerId(getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE));
        listView.setAdapter(new FriendRequestAdapter(getActivity(), R.layout.notification_item, receivedRequests, serverId));
        listView.setOnItemClickListener(itemClickListener);

        friendsRefresher = MiscUtils.getRejectionExecutor();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onDestroyView() {

        if (friendsRefresher != null)
            friendsRefresher.shutdownNow();
        friendsRefresher = null;

        listView = null;

        super.onDestroyView();
    }

    private static final class FetchRequests extends AsyncTask<Void, Void, List<ReceivedRequest>> {

        private WeakReference<FriendRequestFragment> friendRequestFragmentWeakReference;

        public FetchRequests(FriendRequestFragment friendRequestFragment) {
            this.friendRequestFragmentWeakReference = new WeakReference<>(friendRequestFragment);
        }

        @Override
        protected List<ReceivedRequest> doInBackground(Void... params) {

            if (serverId == 0)
                return null;

            return MiscUtils.autoRetry(() -> StaticData.USER_API.getReceivedRequests(serverId).execute().getItems(), Optional.absent()).orNull();
        }

        @Override
        protected void onPostExecute(List<ReceivedRequest> receivedRequestList) {

            super.onPostExecute(receivedRequestList);

            MiscUtils.useFragment(friendRequestFragmentWeakReference, fragment -> {

                final ListAdapter temp;
                if (fragment.listView == null || (temp = fragment.listView.getAdapter()) == null)
                    return null;

                final ArrayAdapter adapter = (ArrayAdapter) temp;

                if (receivedRequestList == null || receivedRequestList.isEmpty()) {

                    MiscUtils.setEmptyTextForListView(fragment.listView, "No friend requests");
                    adapter.clear();
                } else {

                    receivedRequests.clear();
                    receivedRequests.addAll(receivedRequestList);
                    adapter.notifyDataSetChanged();
                }

                return null;
            });
        }
    }
}
