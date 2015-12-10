package reach.project.friends;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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

import com.firebase.client.Firebase;
import com.google.common.base.Optional;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import reach.project.R;
import reach.project.core.GcmIntentService;
import reach.project.core.StaticData;
import reach.project.devikaChat.Chat;
import reach.project.devikaChat.ChatActivity;
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

    private SharedPreferences sharedPreferences;
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

        getLoaderManager().destroyLoader(StaticData.FRIENDS_FULL_LOADER);
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
        sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        serverId = SharedPrefUtils.getServerId(sharedPreferences);
        phoneNumber = SharedPrefUtils.getPhoneNumber(sharedPreferences);

        //clean up
        pinging.set(false);
        synchronizing.set(false);
        synchronizeOnce.set(false);

        reachContactsAdapter = new ReachContactsAdapter(activity, R.layout.myreach_item, null, 0);
    }

    public void setSearchView(SearchView sView) {

        onClose();

        if (sView == null && searchView != null) {
            //invalidate old
            searchView.setOnQueryTextListener(null);
            searchView.setOnCloseListener(null);
            searchView = null;
        } else if (sView != null) {
            //set new
            searchView = sView;
            searchView.setOnQueryTextListener(this);
            searchView.setOnCloseListener(this);
        }
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
        gridView.setOnItemClickListener(LocalUtils.clickListener);
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
                new LocalUtils.ContactsSync().executeOnExecutor(StaticData.temporaryFix);
            }

        } else if (!pinging.get() && isOnline) {
            //if not pinging send a ping !
            pinging.set(true);
            swipeRefreshLayout.post(() -> swipeRefreshLayout.setRefreshing(true));
            new LocalUtils.SendPing().executeOnExecutor(StaticData.temporaryFix);
        }

        getLoaderManager().initLoader(StaticData.FRIENDS_FULL_LOADER, null, this);
        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.FRIENDS_FULL_LOADER)
            return new CursorLoader(getActivity(),
                    ReachFriendsProvider.CONTENT_URI,
                    ReachContactsAdapter.requiredProjection,
                    selection,
                    selectionArguments,
                    ReachFriendsHelper.COLUMN_USER_NAME + " ASC");
        else
            return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (loader.getId() != StaticData.FRIENDS_FULL_LOADER || data == null || data.isClosed())
            return;

        reachContactsAdapter.swapCursor(data);
        final int count = data.getCount();

        if (count == 0)
            MiscUtils.setEmptyTextforGridView(gridView, "No contacts found");
        else {

            if (!SharedPrefUtils.getFirstIntroSeen(sharedPreferences)) {

                SharedPrefUtils.setFirstIntroSeen(sharedPreferences);

                final Activity activity = getActivity();
                final Chat chat = new Chat();
                chat.setMessage("Hey! I am Devika. I handle customer relations at Reach. I will help you with any problems you face inside the app. So ping me here if you face any difficulties :)");
                chat.setTimestamp(0);
                chat.setAdmin(Chat.ADMIN);

                final Optional<Firebase> firebaseOptional = mListener.getFireBase();
                if (firebaseOptional.isPresent()) {

                    firebaseOptional.get().child("chat").child(
                            SharedPrefUtils.getChatUUID(sharedPreferences)).push().setValue(chat);
                    final Intent viewIntent = new Intent(activity, ChatActivity.class);
                    final PendingIntent viewPendingIntent = PendingIntent.getActivity(activity, GcmIntentService.NOTIFICATION_ID_CHAT, viewIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    final NotificationCompat.Builder notificationBuilder =
                            new NotificationCompat.Builder(activity)
                                    .setAutoCancel(true)
                                    .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND)
                                    .setSmallIcon(R.drawable.ic_icon_notif)
                                    .setContentTitle("Devika sent you a message")
                                    .setContentIntent(viewPendingIntent)
                                    .setPriority(NotificationCompat.PRIORITY_MAX)
                                    .setWhen(System.currentTimeMillis());
                    final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(activity);
                    notificationManager.notify(GcmIntentService.NOTIFICATION_ID_CHAT, notificationBuilder.build());
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (loader.getId() == StaticData.FRIENDS_FULL_LOADER)
            reachContactsAdapter.swapCursor(null);
    }

    @Override
    public boolean onClose() {

//        selection = null;
//        selectionArguments = null;
////        searchView.setQuery(null, true);
////
////        inviteAdapter.getFilter().filter(null);
////        getLoaderManager().restartLoader(StaticData.FRIENDS_FULL_LOADER, null, this);
////        return false;
        if (searchView != null) {

            searchView.setQuery(null, false);
            searchView.clearFocus();
        }

        onQueryTextChange(null);
        return false;
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
        try {
            getLoaderManager().restartLoader(StaticData.FRIENDS_FULL_LOADER, null, this);
        } catch (IllegalStateException ignored) {
        }
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
                new SendPing().executeOnExecutor(StaticData.temporaryFix);
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
                                ReachFriendsHelper.COLUMN_LAST_SEEN + " < ? and " +
                                ReachFriendsHelper.COLUMN_ID + " != ?",
                        new String[]{ReachFriendsHelper.ONLINE_REQUEST_GRANTED + "", (currentTime - 60 * 1000) + "", StaticData.devika + ""});
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
                new LocalUtils.SendPing().executeOnExecutor(StaticData.temporaryFix);
            }
        };

        private static final Dialog.OnClickListener positiveButton = (dialog, which) -> {

            final AlertDialog alertDialog = (AlertDialog) dialog;

            final Object[] tag = (Object[]) alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).getTag();

            if (tag == null || tag.length != 3)
                return;

            final long clientId = (long) tag[0];
            final short status = (short) tag[1];
            if (tag[2] != null && tag[2] instanceof WeakReference) {

                final Object object = ((WeakReference) tag[2]).get();
                if (object != null && object instanceof View)
                    Snackbar.make((View) object, "Access Request sent", Snackbar.LENGTH_SHORT).show();
            }

            new SendRequest().executeOnExecutor(
                    StaticData.temporaryFix,
                    clientId, serverId, (long) status);

            //Toast.makeText(getActivity(), "Access Request sent", Toast.LENGTH_SHORT).show();
            final ContentValues values = new ContentValues();
            values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED);
            alertDialog.getContext().getContentResolver().update(
                    Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + clientId),
                    values,
                    ReachFriendsHelper.COLUMN_ID + " = ?",
                    new String[]{clientId + ""});
            dialog.dismiss();
        };

        public static final AdapterView.OnItemClickListener clickListener = (adapterView, view, position, l) -> {

            final Cursor cursor = (Cursor) adapterView.getAdapter().getItem(position);
            final long id = cursor.getLong(0);
            final short status = cursor.getShort(5);
            final short networkType = cursor.getShort(4);

            if (status < 2) {

                if (networkType == 5)
                    Snackbar.make(adapterView, "The user has disabled Uploads", Snackbar.LENGTH_LONG).show();
                MiscUtils.useFragment(reference, fragment -> {
                    fragment.mListener.onOpenLibrary(id);
                });

            } else if (status >= 2) {

                final long clientId = cursor.getLong(0);

                final AlertDialog alertDialog = new AlertDialog.Builder(adapterView.getContext())
                        .setMessage("Send a friend request to " + cursor.getString(2) + " ?")
                        .setPositiveButton("Yes", positiveButton)
                        .setNegativeButton("No", (dialog, which) -> {
                            dialog.dismiss();
                        }).create();

                alertDialog.setOnShowListener(dialog -> alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTag(
                        //set tag to use when positive button click
                        new Object[]{clientId, status, new WeakReference<>(adapterView)}
                ));
                alertDialog.show();
            }
        };

        private static final class SendRequest extends AsyncTask<Long, Void, Long> {

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
        ///////
    }
}
