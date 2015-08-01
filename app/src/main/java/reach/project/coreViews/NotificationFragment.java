package reach.project.coreViews;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import reach.backend.notifications.notificationApi.model.NotificationBase;
import reach.project.R;
import reach.project.adapter.ReachNotificationAdapter;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachNotificationsProvider;
import reach.project.database.notifications.BecameFriends;
import reach.project.database.notifications.Types;
import reach.project.database.sql.ReachNotificationsHelper;
import reach.project.utils.auxiliaryClasses.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.SuperInterface;

public class NotificationFragment extends Fragment {

    private SuperInterface mListener;

    private ReachNotificationAdapter adapter;
    private ListView listView;
    private Activity mActivity;

    private
    private static final AtomicBoolean refreshing = new AtomicBoolean(false);
    private static WeakReference<NotificationFragment> reference = null;

    public static NotificationFragment newInstance() {

        NotificationFragment fragment;
        if (reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new NotificationFragment());
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        listView = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.listView));
        listView.setPadding(0, MiscUtils.dpToPx(10), 0, 0);
        listView.setBackgroundColor(getResources().getColor(R.color.grey));
        adapter = new ReachNotificationAdapter(mActivity, R.layout.notification_item, null, 0, getActivity().getApplication(),
                SharedPrefUtils.getServerId(container.getContext().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)));
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(LocalUtils.itemClickListener);

        getLoaderManager().initLoader(StaticData.NOTIFICATIONS_LOADER, null, this);
        refreshing.set(true);
        new NotificationSync().executeOnExecutor(StaticData.threadPool);

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        try {
            mListener = (SuperInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SuperInterface");
        }
    }

    @Override
    public void onDestroyView() {
        getLoaderManager().destroyLoader(StaticData.NOTIFICATIONS_LOADER);
        if (adapter != null &&
                adapter.getCursor() != null &&
                !adapter.getCursor().isClosed())
            adapter.getCursor().close();
        super.onDestroyView();

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private enum LocalUtils {;

        public static AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                Log.d("Ashish", "notif list click - " + position);
                final Cursor cursor = (Cursor) parent.getAdapter().getItem(position);
                Types type = Types.valueOf(cursor.getString(1));
                switch (type) {
                    case DEFAULT:
                        throw new IllegalArgumentException("Default notification in list !");
                        break;
                    case LIKE:
                        mListener.anchorFooter(true);
                        break;
                    case BECAME_FRIENDS:
                        BecameFriends becameFriends = ReachNotificationsHelper.getBecameFriends(cursor).get();
                        final long hostID = becameFriends.getHostId();

                        final LongSparseArray<Future<?>> isMusicFetching = new LongSparseArray<>();
                        final Future<?> fetching = isMusicFetching.get(hostID, null);
                        if (fetching == null || fetching.isDone() || fetching.isCancelled()) {


                            isMusicFetching.append(hostID, StaticData.threadPool.submit(new GetMusic(hostID,
                                    mActivity.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS))));
                            //Inform only when necessary
                            //if(cursor.getInt(7) == 0)
                            //    Toast.makeText(mActivity, "Refreshing music list", Toast.LENGTH_SHORT).show();
                        }
                        mListener.onOpenLibrary(hostID);
                        break;
                    case PUSH_ACCEPTED:
                        mListener.anchorFooter(true);
                        break;
                }
            }
        };

        private final class NotificationSync extends AsyncTask<Void, Void, Boolean> {

            @Override
            protected Boolean doInBackground(Void... params) {

                final Optional<List<NotificationBase>> list = MiscUtils.autoRetry(
                        new DoWork<List<NotificationBase>>() {
                            @Override
                            protected List<NotificationBase> doWork() throws IOException {
                                long myId = SharedPrefUtils.getServerId(mActivity.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS));
                                if(myId == 0)
                                    return null;
                                return StaticData.notificationApi.getNotifications(myId, (int) reach.project.database.notifications.NotificationBase.GET_UN_READ).execute().getItems();
                            }
                        }, Optional.<Predicate<List<NotificationBase>>>absent());

                if (!list.isPresent())
                    return false;

                final ContentValues[] values = ReachNotificationsHelper.extractValues(list.get());

                final ContentResolver resolver = mActivity.getContentResolver();
                if (resolver == null)
                    return false;

                //delete all rows
                resolver.delete(ReachNotificationsProvider.CONTENT_URI, null, null);
                //insert new rows
                resolver.bulkInsert(ReachNotificationsProvider.CONTENT_URI, values);
                for (ContentValues value : values)
                    value.clear();
                refreshing.set(false);
                return true;
            }
//        @Override
//        protected void onPostExecute(Boolean aVoid) {
//            super.onPostExecute(aVoid);
//            //TODO use boolean
//        }
        }
    }
}
