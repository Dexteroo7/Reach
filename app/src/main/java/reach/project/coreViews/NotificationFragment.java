package reach.project.coreViews;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import reach.backend.notifications.notificationApi.model.NotificationBase;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.database.notifications.BecameFriends;
import reach.project.database.notifications.Like;
import reach.project.database.notifications.Push;
import reach.project.database.notifications.PushAccepted;
import reach.project.database.notifications.Types;
import reach.project.utils.MiscUtils;
import reach.project.utils.SuperInterface;
import reach.project.utils.auxiliaryClasses.DoWork;

public class NotificationFragment extends Fragment {

    private SuperInterface mListener;

//    private ReachNotificationAdapter adapter;

    private static final List<reach.project.database.notifications.NotificationBase> notifications = new ArrayList<>();
    private static final AtomicBoolean refreshing = new AtomicBoolean(false);
    private static WeakReference<NotificationFragment> reference = null;
    private static long serverId = 0;

    public static NotificationFragment newInstance(long serverId) {

        NotificationFragment.serverId = serverId;
        NotificationFragment fragment;
        if (reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new NotificationFragment());

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        final ListView listView = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.listView));
        listView.setPadding(0, MiscUtils.dpToPx(10), 0, 0);
        listView.setBackgroundColor(getResources().getColor(R.color.grey));
//        adapter = new ReachNotificationAdapter(mActivity, R.layout.notification_item, null, 0, getActivity().getApplication(),
//                SharedPrefUtils.getServerId(container.getContext().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)));
//        listView.setAdapter(adapter);
//        listView.setOnItemClickListener(itemClickListener);

        refreshing.set(true);
        new NotificationSync().executeOnExecutor(StaticData.threadPool);

        return rootView;
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

//    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
//        @Override
//        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
//
//            Log.d("Ashish", "notif list click - " + position);
//            final Cursor cursor = (Cursor) parent.getAdapter().getItem(position);
//            Types type = Types.valueOf(cursor.getString(1));
//            switch (type) {
//                case DEFAULT:
//                    throw new IllegalArgumentException("Default notification in list !");
//                case LIKE:
//                    mListener.anchorFooter(true);
//                    break;
//                case BECAME_FRIENDS:
//                    BecameFriends becameFriends = ReachNotificationsHelper.getBecameFriends(cursor).get();
//                    final long hostID = becameFriends.getHostId();
//                    mListener.onOpenLibrary(hostID);
//                    break;
//                case PUSH_ACCEPTED:
//                    mListener.anchorFooter(true);
//                    break;
//            }
//        }
//    };

    private static final class NotificationSync extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            final Optional<List<NotificationBase>> list = MiscUtils.autoRetry(
                    new DoWork<List<NotificationBase>>() {
                        @Override
                        public List<NotificationBase> doWork() throws IOException {
                            if (serverId == 0)
                                return null;
                            return StaticData.notificationApi.getNotifications(serverId, (int) reach.project.database.notifications.NotificationBase.GET_UN_READ).execute().getItems();
                        }
                    }, Optional.<Predicate<List<NotificationBase>>>absent());

            if (!list.isPresent())
                return false;

            /**
             * Clear all notifications and add latest ones
             */
            notifications.clear();
            for (NotificationBase base : list.get()) {

                if(base.getTypes().equals(Types.BECAME_FRIENDS.name())) {

                    final BecameFriends becameFriends = new BecameFriends();
                    becameFriends.portData(base);
                    notifications.add(becameFriends);

                } else if(base.getTypes().equals(Types.LIKE.name())) {

                    final Like like = new Like();
                    like.portData(base);

                    like.setSongName((String) base.get("songName"));
                    notifications.add(like);

                } else if(base.getTypes().equals(Types.PUSH.name())) {

                    final Push push = new Push();
                    push.portData(base);

                    push.setPushContainer((String) base.get("pushContainer"));
                    push.setFirstSongName((String) base.get("firstSongName"));
                    push.setSize(Integer.parseInt(base.get("size").toString()));
                    notifications.add(push);

                } else if(base.getTypes().equals(Types.PUSH_ACCEPTED.name())) {

                    final PushAccepted accepted = new PushAccepted();
                    accepted.portData(base);

                    accepted.setFirstSongName((String) base.get("firstSongName"));
                    accepted.setSize(Integer.parseInt(base.get("size").toString()));
                    notifications.add(accepted);

                } else
                    throw new IllegalArgumentException("Wrong notification type received " + base.getTypes());
            }

            refreshing.set(false);
            return true;
        }
        @Override
        protected void onPostExecute(Boolean aVoid) {
            super.onPostExecute(aVoid);

//            final NotificationFragment fragment;
//            if (!aVoid || reference == null || (fragment = reference.get()) == null || fragment.adapter == null)
//                return;
//            fragment.adapter.notifyDataSetChanged();
        }
    }
}
