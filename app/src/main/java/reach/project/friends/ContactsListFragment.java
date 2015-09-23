package reach.project.friends;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import reach.project.R;
import reach.project.core.DialogActivity;
import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;
import reach.project.utils.QuickSyncFriends;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.SuperInterface;

public class ContactsListFragment extends Fragment implements
        SearchView.OnQueryTextListener,
        SearchView.OnCloseListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private SwipeRefreshLayout swipeRefreshLayout;
    private SearchView searchView;

    private SharedPreferences sharedPrefs;
    private SuperInterface mListener;

    private ReachContactsAdapter reachContactsAdapter;

    private GridView gridView;

    public static final AtomicBoolean synchronizeOnce = new AtomicBoolean(false); ////have we already synchronized ?
    private static final AtomicBoolean
            pinging = new AtomicBoolean(false), //are we pinging ?
            synchronizing = new AtomicBoolean(false); //are we synchronizing ?

    private String mCurFilter, selection;
    private String[] selectionArguments;

    private static String phoneNumber = "";

    private static WeakReference<ContactsListFragment> reference = null;

    private static long serverId;

//    public static void checkNewNotifications() {
//
//        MiscUtils.useFragment(reference, fragment -> {
//
//            if (fragment.notificationCount == null)
//                return null;
//
//            final int friendRequestCount = FriendRequestFragment.receivedRequests.size();
//            final int notificationsCount = NotificationFragment.notifications.size();
//
//            if (friendRequestCount == 0 && notificationsCount == 0)
//                fragment.notificationCount.setVisibility(View.GONE);
//            else {
//                fragment.notificationCount.setVisibility(View.VISIBLE);
//                fragment.notificationCount.setText(String.valueOf(friendRequestCount + notificationsCount));
//            }
//            return null;
//        });
//    }

    private final AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {

        final class SendRequest extends AsyncTask<Long, Void, Long> {

            @Override
            protected Long doInBackground(final Long... params) {

                /**
                 * params[0] = other id
                 * params[1] = my id
                 * params[2] = status
                 */

                final reach.backend.entities.messaging.model.MyString dataAfterWork = MiscUtils.autoRetry(
                        () -> StaticData.messagingEndpoint.requestAccess(params[1], params[0]).execute(),
                        Optional.of(input -> (input == null || TextUtils.isEmpty(input.getString()) || input.getString().equals("false")))).orNull();

                final String toParse;
                if (dataAfterWork == null || TextUtils.isEmpty(toParse = dataAfterWork.getString()) || toParse.equals("false"))
                    return params[0];
                return null;
            }

            @Override
            protected void onPostExecute(final Long response) {

                super.onPostExecute(response);

                if (response != null && response > 0) {

                    //response becomes the id of failed person
                    MiscUtils.useContextFromFragment(reference, context -> {

                        Toast.makeText(context, "Request Failed", Toast.LENGTH_SHORT).show();
                        final ContentValues values = new ContentValues();
                        values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.REQUEST_NOT_SENT);
                        context.getContentResolver().update(
                                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + response),
                                values,
                                ReachFriendsHelper.COLUMN_ID + " = ?",
                                new String[]{response + ""});
                        return null;
                    });
                }

            }
        }

        @Override
        public void onItemClick(final AdapterView<?> adapterView, final View view, int position, long l) {

            final Cursor cursor = (Cursor) adapterView.getAdapter().getItem(position);
            final long id = cursor.getLong(0);
            final short status = cursor.getShort(5);
            final short networkType = cursor.getShort(4);

            if (mListener != null) {
                if (status < 2) {

                    if (networkType == 5)
                        Snackbar.make(adapterView, "The user has disabled Uploads", Snackbar.LENGTH_LONG)
                                .show();
                    mListener.onOpenLibrary(id);

                } else if (status == ReachFriendsHelper.REQUEST_NOT_SENT) {

                    final long clientId = cursor.getLong(0);
                    new AlertDialog.Builder(getActivity())
                            .setMessage("Send a friend request to " + cursor.getString(2) + " ?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                new SendRequest().executeOnExecutor(
                                        AsyncTask.THREAD_POOL_EXECUTOR,
                                        clientId, serverId, (long) status);

                                Snackbar.make(adapterView, "Access Request sent", Snackbar.LENGTH_SHORT).show();
                                //Toast.makeText(getActivity(), "Access Request sent", Toast.LENGTH_SHORT).show();
                                final ContentValues values = new ContentValues();
                                values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED);
                                getActivity().getContentResolver().update(
                                        Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + clientId),
                                        values,
                                        ReachFriendsHelper.COLUMN_ID + " = ?",
                                        new String[]{clientId + ""});
                                dialog.dismiss();
                            })
                            .setNegativeButton("No", (dialog, which) -> {
                                dialog.dismiss();
                            }).create().show();
                }
            }
        }
    };

    public static ContactsListFragment newInstance() {

        ContactsListFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            Log.i("Ayush", "Creating new instance of contacts list fragment");
            reference = new WeakReference<>(fragment = new ContactsListFragment());
        } else
            Log.i("Ayush", "Reusing contacts list fragment object :)");

        return fragment;
    }

    @Override
    public void onDestroyView() {

        //clean up
        pinging.set(false);
        synchronizing.set(false);

        getLoaderManager().destroyLoader(StaticData.FRIENDS_LOADER);
        if (reachContactsAdapter != null && reachContactsAdapter.getCursor() != null && !reachContactsAdapter.getCursor().isClosed())
            reachContactsAdapter.getCursor().close();

//        if (inviteAdapter != null)
//            inviteAdapter.cleanUp();

        //listView.setOnScrollListener(null);
        super.onDestroyView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        final Activity activity = getActivity();
        if (activity == null || activity.isFinishing())
            return;

        /*if (getArguments().getBoolean("first", false))
            new InfoDialog().show(getChildFragmentManager(),"info_dialog");*/
        sharedPrefs = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        serverId = SharedPrefUtils.getServerId(sharedPrefs);
        phoneNumber = SharedPrefUtils.getPhoneNumber(sharedPrefs);

        //clean up
        pinging.set(false);
        synchronizing.set(false);
        synchronizeOnce.set(false);

        reachContactsAdapter = new ReachContactsAdapter(activity, R.layout.myreach_item, null, 0);
    }

    public void setSearchView(SearchView sView) {
        searchView = sView;
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);
        searchView.setQuery(null, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_contacts, container, false);

        if (serverId == 0 || TextUtils.isEmpty(phoneNumber))
            return null;

        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeContainerContacts);
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(getContext(), R.color.reach_color),
                ContextCompat.getColor(getContext(), R.color.reach_grey));
        swipeRefreshLayout.setBackgroundResource(R.color.white);
        swipeRefreshLayout.setOnRefreshListener(LocalUtils.refreshListener);

        gridView = MiscUtils.addLoadingToGridView((GridView) rootView.findViewById(R.id.contactsList));
        gridView.setOnItemClickListener(clickListener);
        //gridView.setOnScrollListener(scrollListener);
        gridView.setAdapter(reachContactsAdapter);

        selection = null;
        selectionArguments = null;
        final boolean isOnline = MiscUtils.isOnline(getActivity());
        //we have not already synchronized !
        if (!synchronizeOnce.get() && !synchronizing.get()) {
            //contact sync will call send ping as well

            if (isOnline) {
                synchronizing.set(true);
                pinging.set(true);
                swipeRefreshLayout.post(() -> swipeRefreshLayout.setRefreshing(true));
                new LocalUtils.ContactsSync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

        } else if (!pinging.get() && isOnline) {
            //if not pinging send a ping !
            pinging.set(true);
            swipeRefreshLayout.post(() -> swipeRefreshLayout.setRefreshing(true));
            new LocalUtils.SendPing().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        getLoaderManager().initLoader(StaticData.FRIENDS_LOADER, null, this);
        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.FRIENDS_LOADER)
            return new CursorLoader(getActivity(),
                    ReachFriendsProvider.CONTENT_URI,
                    ReachContactsAdapter.requiredProjection,
                    selection,
                    selectionArguments,
                    ReachFriendsHelper.COLUMN_STATUS + " ASC, " + ReachFriendsHelper.COLUMN_NEW_SONGS + " DESC");
        else
            return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (loader.getId() != StaticData.FRIENDS_LOADER || data == null || data.isClosed())
            return;

        reachContactsAdapter.swapCursor(data);
        final int count = data.getCount();

        if (count == 0)
            MiscUtils.setEmptyTextforGridView(gridView, "No contacts found");
        else {

            if (!SharedPrefUtils.getFirstIntroSeen(sharedPrefs)) {
                AsyncTask.THREAD_POOL_EXECUTOR.execute(LocalUtils.devikaSendMeSomeLove);
                SharedPrefUtils.setFirstIntroSeen(sharedPrefs);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (loader.getId() == StaticData.FRIENDS_LOADER)
            reachContactsAdapter.swapCursor(null);
    }

    @Override
    public boolean onClose() {

//        selection = null;
//        selectionArguments = null;
//        searchView.setQuery(null, true);
//
//        inviteAdapter.getFilter().filter(null);
//        getLoaderManager().restartLoader(StaticData.FRIENDS_LOADER, null, this);
//        return false;
        return onQueryTextChange(null);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {

        if (searchView == null)
            return false;
        /**
         * Called when the action bar search text has changed.
         * Update the search filter.
         * Restart the loader to do a new query with this filter.
         * Don't do anything if the filter hasn't actually changed.
         * Prevents restarting the loader when restoring state.
         */
        if (TextUtils.isEmpty(mCurFilter) && TextUtils.isEmpty(newText))
            return true;
        if (!TextUtils.isEmpty(mCurFilter) && mCurFilter.equals(newText))
            return true;
        mCurFilter = newText;

        if (TextUtils.isEmpty(mCurFilter)) {
            selection = null;
            selectionArguments = null;
            searchView.setQuery(null, true);
        } else {
            selection = ReachFriendsHelper.COLUMN_USER_NAME + " LIKE ?";
            selectionArguments = new String[]{"%" + mCurFilter + "%"};
        }
        getLoaderManager().restartLoader(StaticData.FRIENDS_LOADER, null, this);
        return true;
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        try {
            mListener = (SuperInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private enum LocalUtils {
        ;

        private static Optional<ContentResolver> getResolver() {

            final Fragment fragment;
            if (reference == null || (fragment = reference.get()) == null)
                return Optional.absent();
            final Activity activity = fragment.getActivity();
            if (activity == null || activity.isFinishing())
                return Optional.absent();
            return Optional.fromNullable(activity.getContentResolver());
        }

        public static final class ContactsSync extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... params) {

                /**
                 * Invalidate everyone
                 */
                final ContentValues contentValues = new ContentValues();
                contentValues.put(ReachFriendsHelper.COLUMN_LAST_SEEN, System.currentTimeMillis() + 31 * 1000);
                contentValues.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.OFFLINE_REQUEST_GRANTED);
                contentValues.put(ReachFriendsHelper.COLUMN_NETWORK_TYPE, (short) 0);

                final Optional<ContentResolver> optional = getResolver();
                if (!optional.isPresent())
                    return null;

                optional.get().update(
                        ReachFriendsProvider.CONTENT_URI,
                        contentValues,
                        ReachFriendsHelper.COLUMN_STATUS + " = ? and " +
                                ReachFriendsHelper.COLUMN_LAST_SEEN + " < ?",
                        new String[]{ReachFriendsHelper.ONLINE_REQUEST_GRANTED + "",
                                (System.currentTimeMillis() - (60 * 1000)) + ""});
                contentValues.clear();
                StaticData.networkCache.clear();

                try {
                    new QuickSyncFriends(reference.get().getActivity(), serverId, phoneNumber).call();
                } catch (NullPointerException ignored) {
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {

                super.onPostExecute(aVoid);
                synchronizeOnce.set(true);
                //we are still refreshing !
                pinging.set(true);
                new SendPing().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }

        public static final class SendPing extends AsyncTask<Void, String, Void> {

            @Override
            protected Void doInBackground(Void... params) {

                StaticData.networkCache.clear();
                MiscUtils.autoRetry(() -> StaticData.userEndpoint.pingMyReach(serverId).execute(), Optional.absent()).orNull();

                /**
                 * Invalidate those who were online 30 secs ago
                 * and send PING
                 */
                final long currentTime = System.currentTimeMillis();
                final ContentValues contentValues = new ContentValues();
                contentValues.put(ReachFriendsHelper.COLUMN_LAST_SEEN, currentTime + 31 * 1000);
                contentValues.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.OFFLINE_REQUEST_GRANTED);
                contentValues.put(ReachFriendsHelper.COLUMN_NETWORK_TYPE, (short) 0);

                final Optional<ContentResolver> optional = getResolver();
                if (!optional.isPresent())
                    return null;

                optional.get().update(
                        ReachFriendsProvider.CONTENT_URI,
                        contentValues,
                        ReachFriendsHelper.COLUMN_STATUS + " = ? and " +
                                ReachFriendsHelper.COLUMN_LAST_SEEN + " < ?",
                        new String[]{ReachFriendsHelper.ONLINE_REQUEST_GRANTED + "", (currentTime - 30 * 1000) + ""});
                contentValues.clear();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {

                super.onPostExecute(aVoid);
                //finally relax !
                synchronizing.set(false);
                pinging.set(false);
                MiscUtils.useFragment(reference, fragment ->
                {
                    fragment.swipeRefreshLayout.post(() -> fragment.swipeRefreshLayout.setRefreshing(false));
                });
            }
        }

        public static final SwipeRefreshLayout.OnRefreshListener refreshListener = () -> {

            if (!pinging.get()) {

                Log.i("Ayush", "Starting refresh !");
                pinging.set(true);
                new LocalUtils.SendPing().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        };

        public static final Runnable devikaSendMeSomeLove = () -> {

            final ContactsListFragment fragment;
            if (reference == null || (fragment = reference.get()) == null)
                return;
            final Activity activity = fragment.getActivity();
            if (activity == null || activity.isFinishing())
                return;

            Bitmap bmp = null;
            final NotificationManagerCompat managerCompat = NotificationManagerCompat.from(activity);
            final int px = MiscUtils.dpToPx(64);
            try {
                bmp = Picasso.with(activity)
                        .load("https://scontent-sin1-1.xx.fbcdn.net/hphotos-xap1/v/t1.0-9/1011255_638449632916744_321328860_n.jpg?oh=5c1daa8d7d015f7ce698ee1793d5a929&oe=55EECF36&dl=1")
                        .centerCrop()
                        .resize(px, px)
                        .get();
            } catch (IOException e) {
                e.printStackTrace();
            }

            final Intent intent = new Intent(activity, DialogActivity.class);
            intent.putExtra("type", 1);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            final PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(activity)
                    .setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND)
                    .setSmallIcon(R.drawable.ic_icon_notif)
                    .setLargeIcon(bmp)
                    .setContentIntent(pendingIntent)
                            //.addAction(0, "Okay! I got it", pendingIntent)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("I am Devika from Team Reach! \n" +
                                    "Send me an access request by clicking on the lock icon beside my name to view my Music collection. \n" +
                                    "Keep Reaching ;)"))
                    .setContentTitle("Hey!")
                    .setTicker("Hey! I am Devika from Team Reach! Send me an access request by clicking on the lock icon beside my name to view my Music collection. Keep Reaching ;)")
                    .setContentText("I am Devika from Team Reach! \n" +
                            "Send me an access request by clicking on the lock icon beside my name to view my Music collection. \n" +
                            "Keep Reaching ;)")
                    .setPriority(NotificationCompat.PRIORITY_MAX);
            managerCompat.notify(99910, builder.build());
        };
    }
}
