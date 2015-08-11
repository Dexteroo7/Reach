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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import reach.backend.notifications.notificationApi.model.NotificationBase;
import reach.project.R;
import reach.project.adapter.ReachNotificationAdapter;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.notifications.BecameFriends;
import reach.project.database.notifications.Like;
import reach.project.database.notifications.NotificationBaseLocal;
import reach.project.database.notifications.Push;
import reach.project.database.notifications.PushAccepted;
import reach.project.database.notifications.Types;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.utils.MiscUtils;
import reach.project.utils.SuperInterface;
import reach.project.utils.auxiliaryClasses.DoWork;

public class NotificationFragment extends Fragment {

    private SuperInterface mListener;

    private static final List<NotificationBaseLocal> notifications = new ArrayList<>();
    private static WeakReference<NotificationFragment> reference = null;
    private  ReachNotificationAdapter adapter = null;
    private  ExecutorService notificationRefresher = null;
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

        if (notificationRefresher != null && adapter != null)
            new NotificationSync().executeOnExecutor(notificationRefresher, adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        final ListView listView = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.listView));
        adapter = new ReachNotificationAdapter(getActivity(), R.layout.notification_item, notifications, serverId);

        //listView.setBackgroundColor(getResources().getColor(R.color.default_grey));
        listView.setPadding(0, MiscUtils.dpToPx(10), 0, 0);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(itemClickListener);

        notificationRefresher = Executors.unconfigurableExecutorService(new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>()));

        refresh();
        return rootView;
    }

    @Override
    public void onDestroyView() {

        if (notificationRefresher != null)
            notificationRefresher.shutdownNow();
        notificationRefresher = null;

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
                case LIKE:
                    mListener.anchorFooter(true);
                    break;
                case BECAME_FRIENDS:
                    //check validity
                    final Cursor cursor = view.getContext().getContentResolver().query(
                            Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + hostID),
                            new String[]{ReachFriendsHelper.COLUMN_ID},
                            ReachFriendsHelper.COLUMN_ID + " = ?",
                            new String[]{hostID + ""}, null);

                    //check validity
                    if (cursor == null || !cursor.moveToFirst()) {

                        MiscUtils.autoRetryAsync(new DoWork<Void>() {
                            @Override
                            public Void doWork() throws IOException {
                                return StaticData.notificationApi.removeNotification(notificationBaseLocal.getNotificationId(), serverId).execute();
                            }
                        }, Optional.<Predicate<Void>>absent());

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
                    mListener.anchorFooter(true);
                    break;
            }
        }
    };

    private static final class NotificationSync extends AsyncTask<ArrayAdapter, Void, ArrayAdapter> {

        @Override
        protected ArrayAdapter doInBackground(ArrayAdapter... params) {

            notifications.clear();

            final Optional<List<NotificationBase>> list = MiscUtils.autoRetry(
                    new DoWork<List<NotificationBase>>() {
                        @Override
                        public List<NotificationBase> doWork() throws IOException {
                            if (serverId == 0)
                                return null;
                            return StaticData.notificationApi.getNotifications(serverId, (int) NotificationBaseLocal.GET_UN_READ).execute().getItems();
                        }
                    }, Optional.<Predicate<List<NotificationBase>>>absent());

            if (!list.isPresent())
                return params[0];

            /**
             * Clear all Notifications and add latest ones
             */
            for (NotificationBase base : list.get()) {

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
                    push.setSize(Integer.parseInt(base.get("size").toString()));
                    notifications.add(push);

                } else if (base.getTypes().equals(Types.PUSH_ACCEPTED.name())) {

                    final PushAccepted accepted = new PushAccepted();
                    accepted.portData(base);

                    accepted.setFirstSongName((String) base.get("firstSongName"));
                    accepted.setSize(Integer.parseInt(base.get("size").toString()));
                    notifications.add(accepted);

                } else
                    throw new IllegalArgumentException("Wrong notification type received " + base.getTypes());
            }

            return params[0];
        }

        @Override
        protected void onPostExecute(final ArrayAdapter adapter) {

            super.onPostExecute(adapter);
            if (adapter != null)
                adapter.notifyDataSetChanged();
        }
    }
}
