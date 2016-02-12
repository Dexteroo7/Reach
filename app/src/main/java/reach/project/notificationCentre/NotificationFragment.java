package reach.project.notificationCentre;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
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

import reach.backend.notifications.notificationApi.model.NotificationBase;
import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.coreViews.yourProfile.YourProfileActivity;
import reach.project.notificationCentre.notifications.BecameFriends;
import reach.project.notificationCentre.notifications.Like;
import reach.project.notificationCentre.notifications.NotificationBaseLocal;
import reach.project.notificationCentre.notifications.Push;
import reach.project.notificationCentre.notifications.PushAccepted;
import reach.project.notificationCentre.notifications.Types;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

public class NotificationFragment extends Fragment {


    private static long serverId = 0;
    private static NotificationAdapter notificationAdapter = null;

    public static NotificationFragment newInstance() {
        return new NotificationFragment();
    }

    //    public void refresh() {
//
//        if (listView != null)
//            new NotificationSync().executeOnExecutor(notificationRefresher);
//    }
    public static void clearNotifications() {
        if (serverId == 0)
            return;

        MiscUtils.autoRetryAsync(() -> StaticData.NOTIFICATION_API.markAllRead(serverId).execute(),
                Optional.absent());

        if (notificationAdapter == null)
            return;
        notificationAdapter.clear();
        if (listView == null)
            return;

        MiscUtils.setEmptyTextForListView(listView, "No notifications");
    }

    private final List<NotificationBaseLocal> notifications = new ArrayList<>();
    private final ExecutorService notificationRefresher = MiscUtils.getRejectionExecutor();

    @Nullable
    private static ListView listView = null;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_notification, container, false);

        listView = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.listView));
        listView.setPadding(0, MiscUtils.dpToPx(10), 0, 0); //ignore cant be null
        serverId = SharedPrefUtils.getServerId(getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE));
        notificationAdapter = new NotificationAdapter(getActivity(), R.layout.notification_item, notifications, serverId);
        listView.setAdapter(notificationAdapter);
        listView.setOnItemClickListener(itemClickListener);

        return rootView;
    }

    @Override
    public void onResume() {

        super.onResume();
        new NotificationSync(this).executeOnExecutor(notificationRefresher);
    }

    private AdapterView.OnItemClickListener itemClickListener = (parent, view, position, id) -> {

        final NotificationAdapter adapter = (NotificationAdapter) parent.getAdapter();
        final NotificationBaseLocal notificationBaseLocal = adapter.getItem(position);
        final Types type = notificationBaseLocal.getTypes();
        final long hostID = notificationBaseLocal.getHostId();

        switch (type) {

            case DEFAULT:
                throw new IllegalArgumentException("Default notification in list !");
            case PUSH:
                if (NotificationAdapter.accepted.get(notificationBaseLocal.getNotificationId())) {
                    NotificationAdapter.accepted.delete(notificationBaseLocal.getNotificationId());
                } else return; //TODO fix this hack
                break;
            case LIKE:
                YourProfileActivity.openProfile(hostID, getActivity());
                break;
            case BECAME_FRIENDS:
                //check validity
                final Cursor cursor = view.getContext().getContentResolver().query(
                        Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + hostID),
                        new String[]{ReachFriendsHelper.COLUMN_ID},
                        ReachFriendsHelper.COLUMN_ID + " = ?",
                        new String[]{hostID + ""}, null);

                if (cursor == null || !cursor.moveToFirst()) {
                    MiscUtils.autoRetryAsync(() -> StaticData.NOTIFICATION_API.removeNotification(notificationBaseLocal.getNotificationId(), serverId).execute(),
                            Optional.absent());

                    if (cursor != null)
                        cursor.close();
                    //fail
                    adapter.remove(notificationBaseLocal);
                    Toast.makeText(view.getContext(), "404", Toast.LENGTH_SHORT).show();
                    return;
                }
                cursor.close();
                YourProfileActivity.openProfile(hostID, getActivity());
                break;
            case PUSH_ACCEPTED:
                final Intent foreGround = new Intent(getContext(), ReachActivity.class);
                foreGround.setAction(ReachActivity.OPEN_MANAGER_SONGS_DOWNLOADING);
                foreGround.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(foreGround);
                break;
        }

        adapter.remove(notificationBaseLocal);
        MiscUtils.autoRetryAsync(() -> StaticData.NOTIFICATION_API.removeNotification(notificationBaseLocal.getNotificationId(), serverId).execute(), Optional.absent());
    };

    private static final class NotificationSync extends AsyncTask<Void, Void, List<NotificationBase>> {

        private WeakReference<NotificationFragment> notificationFragmentWeakReference;

        public NotificationSync(NotificationFragment notificationFragment) {
            this.notificationFragmentWeakReference = new WeakReference<NotificationFragment>(notificationFragment);
        }

        @Override
        protected List<NotificationBase> doInBackground(Void... params) {

            if (serverId == 0)
                return null;

            return MiscUtils.autoRetry(() -> StaticData.NOTIFICATION_API.getNotifications(serverId, (int) NotificationBaseLocal.GET_UN_READ).execute().getItems(),
                    Optional.absent()).orNull();
        }

        @Override
        protected void onPostExecute(List<NotificationBase> notificationBaseList) {

            super.onPostExecute(notificationBaseList);

            MiscUtils.useFragment(notificationFragmentWeakReference, fragment -> {

                final ListAdapter temp;
                if (fragment.listView == null || (temp = fragment.listView.getAdapter()) == null)
                    return null;

                final ArrayAdapter adapter = (ArrayAdapter) temp;

                if (notificationBaseList == null || notificationBaseList.isEmpty()) {

                    MiscUtils.setEmptyTextForListView(fragment.listView, "No notifications");
                    adapter.clear();
                } else {

                    /**
                     * Clear all Notifications and add latest ones
                     */
                    fragment.notifications.clear();

                    for (NotificationBase base : notificationBaseList) {

                        if (base.getTypes().equals(Types.BECAME_FRIENDS.name())) {

                            final BecameFriends becameFriends = new BecameFriends();
                            becameFriends.portData(base);
                            fragment.notifications.add(becameFriends);

                        } else if (base.getTypes().equals(Types.LIKE.name())) {

                            final Like like = new Like();
                            like.portData(base);

                            like.setSongName((String) base.get("songName"));
                            fragment.notifications.add(like);

                        } else if (base.getTypes().equals(Types.PUSH.name())) {

                            final Push push = new Push();
                            push.portData(base);

                            push.setPushContainer((String) base.get("pushContainer"));
                            push.setFirstSongName((String) base.get("firstSongName"));
                            push.setCustomMessage((String) base.get("customMessage"));
                            push.setSize(Integer.parseInt(base.get("size").toString()));
                            fragment.notifications.add(push);

                        } else if (base.getTypes().equals(Types.PUSH_ACCEPTED.name())) {

                            final PushAccepted pushAccepted = new PushAccepted();
                            pushAccepted.portData(base);

                            pushAccepted.setFirstSongName((String) base.get("firstSongName"));
                            pushAccepted.setSize(Integer.parseInt(base.get("size").toString()));
                            fragment.notifications.add(pushAccepted);

                        } else
                            throw new IllegalArgumentException("Wrong notification type received " + base.getTypes());
                    }
                    adapter.notifyDataSetChanged();
                }

                return null;
            });
        }
    }
}
