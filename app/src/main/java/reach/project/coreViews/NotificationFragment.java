package reach.project.coreViews;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import reach.backend.notifications.notificationApi.model.NotificationBase;
import reach.project.R;
import reach.project.adapter.ReachNotificationAdapter;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachNotificationsProvider;
import reach.project.database.notifications.BecameFriends;
import reach.project.database.notifications.Types;
import reach.project.database.sql.ReachNotificationsHelper;
import reach.project.utils.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.SuperInterface;

public class NotificationFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static WeakReference<NotificationFragment> reference = null;
    private SuperInterface mListener;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);
    private ReachNotificationAdapter adapter;
    private ListView listView;

    public static NotificationFragment newInstance() {

        NotificationFragment fragment;
        if(reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new NotificationFragment());
        return fragment;
    }
    public NotificationFragment() {
        // Required empty public constructor
    }

    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Types type = Types.DEFAULT;
            switch (type) {
                case DEFAULT:
                    break;
                case LIKE:
                    mListener.anchorFooter(true);
                    break;
                case PUSH:
                    mListener.anchorFooter(true);
                    break;
                case BECAME_FRIENDS:
                    BecameFriends becameFriends = new BecameFriends();
                    mListener.onOpenLibrary(becameFriends.getHostId());
                    break;
                case PUSH_ACCEPTED:
                    mListener.anchorFooter(true);
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        listView = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.listView));
        listView.setPadding(0,MiscUtils.dpToPx(10),0,0);
        adapter = new ReachNotificationAdapter(getActivity(),R.layout.notification_item,null,0);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(itemClickListener);

        getLoaderManager().initLoader(StaticData.NOTIFICATIONS_LOADER, null, this);
        refreshing.set(true);
        new NotificationSync().executeOnExecutor(StaticData.threadPool);

        return rootView;
    }

    private final class NotificationSync extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            final Optional<ImmutableList<NotificationBase>> list = MiscUtils.autoRetry(
                    new DoWork<ImmutableList<NotificationBase>>() {
                        @Override
                        protected ImmutableList<NotificationBase> doWork() throws IOException {
                            long myId = SharedPrefUtils.getServerId(getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS));
                            return ImmutableList.copyOf(StaticData.notificationApi.get(myId).execute().getItems());
                        }
                    }, Optional.<Predicate<ImmutableList<NotificationBase>>>absent());

            if (!list.isPresent())
                return false;

            final ContentValues[] values = ReachNotificationsHelper.extractValues(list.get());

            final ContentResolver resolver = getActivity().getContentResolver();
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

        @Override
        protected void onPostExecute(Boolean aVoid) {
            super.onPostExecute(aVoid);

            //TODO use boolean
        }
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                ReachNotificationsProvider.CONTENT_URI,
                ReachNotificationsHelper.projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == StaticData.NOTIFICATIONS_LOADER && data != null && !data.isClosed()) {
            adapter.swapCursor(data);
            if(data.getCount() == 0)
                MiscUtils.setEmptyTextforListView(listView, "No notifications for you!");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == StaticData.NOTIFICATIONS_LOADER)
            adapter.swapCursor(null);
    }
}
