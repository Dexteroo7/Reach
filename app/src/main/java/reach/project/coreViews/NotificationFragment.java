package reach.project.coreViews;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.LongSparseArray;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import reach.backend.entities.userApi.model.MusicContainer;
import reach.backend.notifications.notificationApi.model.NotificationBase;
import reach.project.R;
import reach.project.adapter.ReachNotificationAdapter;
import reach.project.core.StaticData;
import reach.project.database.ReachAlbum;
import reach.project.database.ReachArtist;
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
    private Activity mActivity;

    public static NotificationFragment newInstance() {

        NotificationFragment fragment;
        if (reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new NotificationFragment());
        return fragment;
    }

    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

            Log.d("Ashish", "notif list click - " + position);
            Cursor cursor = (Cursor) parent.getAdapter().getItem(position);
            Types type = Types.valueOf(cursor.getString(1));
            switch (type) {
                case DEFAULT:
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

    private final class GetMusic implements Runnable {

        private final long hostId;
        private final SharedPreferences sharedPreferences;

        private GetMusic(long hostId, SharedPreferences sharedPreferences) {
            this.hostId = hostId;
            this.sharedPreferences = sharedPreferences;
        }

        @Override
        public void run() {
            final long serverId = SharedPrefUtils.getServerId(mActivity.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS));

            //fetch music
            final MusicContainer musicContainer = MiscUtils.autoRetry(new DoWork<MusicContainer>() {
                @Override
                protected MusicContainer doWork() throws IOException {

                    return StaticData.userEndpoint.getMusicWrapper(
                            hostId,
                            serverId,
                            SharedPrefUtils.getPlayListCodeForUser(hostId, sharedPreferences),
                            SharedPrefUtils.getSongCodeForUser(hostId, sharedPreferences)).execute();
                }
            }, Optional.<Predicate<MusicContainer>>of(new Predicate<MusicContainer>() {
                @Override
                public boolean apply(@Nullable MusicContainer input) {
                    return input == null;
                }
            })).orNull();

            if (musicContainer == null && mActivity != null)
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mActivity, "Music fetch failed", Toast.LENGTH_SHORT).show();
                    }
                });

            if (mActivity == null || mActivity.isFinishing() || musicContainer == null)
                return;

            if (musicContainer.getSongsChanged()) {

                if (musicContainer.getReachSongs() == null || musicContainer.getReachSongs().size() == 0)
                    //All the songs got deleted
                    MiscUtils.deleteSongs(hostId, mActivity.getContentResolver());
                else {
                    final Pair<Collection<ReachAlbum>, Collection<ReachArtist>> pair =
                            MiscUtils.getAlbumsAndArtists(new HashSet<>(musicContainer.getReachSongs()));
                    final Collection<ReachAlbum> reachAlbums = pair.first;
                    final Collection<ReachArtist> reachArtists = pair.second;
                    MiscUtils.bulkInsertSongs(new HashSet<>(musicContainer.getReachSongs()),
                            reachAlbums,
                            reachArtists,
                            mActivity.getContentResolver());
                }
                SharedPrefUtils.storeSongCodeForUser(hostId, musicContainer.getSongsHash(), sharedPreferences);
                Log.i("Ayush", "Fetching songs, song hash changed for " + hostId + " " + musicContainer.getSongsHash());
            }

            if (musicContainer.getPlayListsChanged() && mActivity != null && !mActivity.isFinishing()) {

                if (musicContainer.getReachPlayLists() == null || musicContainer.getReachPlayLists().size() == 0)
                    //All playLists got deleted
                    MiscUtils.deletePlayLists(hostId, mActivity.getContentResolver());
                else
                    MiscUtils.bulkInsertPlayLists(new HashSet<>(musicContainer.getReachPlayLists()), mActivity.getContentResolver());
                SharedPrefUtils.storePlayListCodeForUser(hostId, musicContainer.getPlayListHash(), sharedPreferences);
                Log.i("Ayush", "Fetching playLists, playList hash changed for " + hostId + " " + musicContainer.getPlayListHash());
            }
        }
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
                            long myId = SharedPrefUtils.getServerId(mActivity.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS));
                            if(myId == 0)
                                return null;
                            return ImmutableList.copyOf(StaticData.notificationApi.get(myId).execute().getItems()).reverse();
                        }
                    }, Optional.<Predicate<ImmutableList<NotificationBase>>>absent());

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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(mActivity,
                ReachNotificationsProvider.CONTENT_URI,
                ReachNotificationsHelper.projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == StaticData.NOTIFICATIONS_LOADER && data != null && !data.isClosed()) {
            adapter.swapCursor(data);
            if (!refreshing.get() && data.getCount() == 0)
                MiscUtils.setEmptyTextforListView(listView, "No notifications for you!");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == StaticData.NOTIFICATIONS_LOADER)
            adapter.swapCursor(null);
    }
}
