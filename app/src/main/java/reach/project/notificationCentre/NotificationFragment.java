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
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import reach.backend.notifications.notificationApi.model.NotificationBase;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.friends.ReachFriendsHelper;
import reach.project.friends.ReachFriendsProvider;
import reach.project.notificationCentre.notifications.BecameFriends;
import reach.project.notificationCentre.notifications.Like;
import reach.project.notificationCentre.notifications.NotificationBaseLocal;
import reach.project.notificationCentre.notifications.Push;
import reach.project.notificationCentre.notifications.PushAccepted;
import reach.project.notificationCentre.notifications.Types;
import reach.project.utils.MiscUtils;
import reach.project.utils.auxiliaryClasses.SuperInterface;

public class NotificationFragment extends Fragment {

    private SuperInterface mListener;

    public static final List<NotificationBaseLocal> notifications = new ArrayList<>();
    private static WeakReference<NotificationFragment> reference = null;
    private ExecutorService notificationRefresher = null;
    private ListView listView = null;
    private static long serverId = 0;

    public static NotificationFragment newInstance(long id) {

        NotificationFragment fragment;
        if (reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new NotificationFragment());

        serverId = id;
        return fragment;
    }

    public static WeakReference<NotificationFragment> getReference() {
        return reference;
    }

    public void refresh() {

        if (notificationRefresher != null && listView != null)
            new NotificationSync().executeOnExecutor(notificationRefresher);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_notification, container, false);

        listView = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.listView));
        listView.setPadding(0, MiscUtils.dpToPx(10), 0, 0);
        listView.setAdapter(new ReachNotificationAdapter(getActivity(), R.layout.notification_item, notifications, serverId));
        listView.setOnItemClickListener(itemClickListener);

        notificationRefresher = Executors.unconfigurableExecutorService(new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(),
                (r, executor) -> {/**ignored**/}));

        reference = new WeakReference<>(this);

        refresh();
        return rootView;
    }

    @Override
    public void onDestroyView() {

        if (notificationRefresher != null)
            notificationRefresher.shutdownNow();

        notificationRefresher = null;
        listView = null;
        super.onDestroyView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (SuperInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement SuperInterface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

            final ReachNotificationAdapter adapter = (ReachNotificationAdapter) parent.getAdapter();
            final NotificationBaseLocal notificationBaseLocal = adapter.getItem(position);
            final Types type = notificationBaseLocal.getTypes();
            final long hostID = notificationBaseLocal.getHostId();

            switch (type) {

                case DEFAULT:
                    throw new IllegalArgumentException("Default notification in list !");
                case PUSH:
                    if (ReachNotificationAdapter.accepted.get(notificationBaseLocal.getNotificationId())) {
                        mListener.anchorFooter();
                        ReachNotificationAdapter.accepted.delete(notificationBaseLocal.getNotificationId());
                    } else return; //TODO fix this hack
                    break;
                case LIKE:
                    mListener.anchorFooter();
                    break;
                case BECAME_FRIENDS:
                    //check validity
                    final Cursor cursor = view.getContext().getContentResolver().query(
                            Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + hostID),
                            new String[]{ReachFriendsHelper.COLUMN_ID},
                            ReachFriendsHelper.COLUMN_ID + " = ?",
                            new String[]{hostID + ""}, null);

                    if (cursor == null || !cursor.moveToFirst()) {

                        MiscUtils.autoRetryAsync(() -> StaticData.notificationApi.removeNotification(notificationBaseLocal.getNotificationId(), serverId).execute(),
                                Optional.absent());

                        if (cursor != null)
                            cursor.close();
                        //fail
                        adapter.remove(notificationBaseLocal);
                        Toast.makeText(view.getContext(), "404", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    cursor.close();
                    mListener.onOpenLibrary(hostID);
                    break;
                case PUSH_ACCEPTED:
                    mListener.anchorFooter();
                    break;
            }

            mListener.closeDrawers();
            adapter.remove(notificationBaseLocal);
            MiscUtils.autoRetryAsync(() -> StaticData.notificationApi.removeNotification(notificationBaseLocal.getNotificationId(), serverId).execute(), Optional.absent());
        }
    };

    private static final class NotificationSync extends AsyncTask<Void, Void, List<NotificationBase>> {

        @Override
        protected List<NotificationBase> doInBackground(Void... params) {

            if (serverId == 0)
                return null;

            return MiscUtils.autoRetry(() -> StaticData.notificationApi.getNotifications(serverId, (int) NotificationBaseLocal.GET_UN_READ).execute().getItems(),
                    Optional.absent()).orNull();
        }

        @Override
        protected void onPostExecute(List<NotificationBase> notificationBaseList) {

            super.onPostExecute(notificationBaseList);

            MiscUtils.useFragment(reference, fragment -> {

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
                    notifications.clear();

                    for (NotificationBase base : notificationBaseList) {

                        if (base.getTypes().equals(Types.BECAME_FRIENDS.name())) {

                            final BecameFriends becameFriends = new BecameFriends();
                            becameFriends.portData(base);
                            notifications.add(becameFriends);

                        } else if (base.getTypes().equals(Types.LIKE.name())) {

                            final Like like = new Like();
                            like.portData(base);

                            like.setSongName((String) base.get("songName"));
                            notifications.add(like);

                        } else if (base.getTypes().equals(Types.PUSH.name())) {

                            final Push push = new Push();
                            push.portData(base);

                            push.setPushContainer((String) base.get("pushContainer"));
                            push.setFirstSongName((String) base.get("firstSongName"));
                            push.setCustomMessage((String) base.get("customMessage"));
                            push.setSize(Integer.parseInt(base.get("size").toString()));
                            notifications.add(push);

                        } else if (base.getTypes().equals(Types.PUSH_ACCEPTED.name())) {

                            final PushAccepted pushAccepted = new PushAccepted();
                            pushAccepted.portData(base);

                            pushAccepted.setFirstSongName((String) base.get("firstSongName"));
                            pushAccepted.setSize(Integer.parseInt(base.get("size").toString()));
                            notifications.add(pushAccepted);

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
