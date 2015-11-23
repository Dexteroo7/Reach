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
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import reach.project.friends.friendsAdapters.FriendsAdapter;
import reach.project.utils.MiscUtils;
import reach.project.utils.QuickSyncFriends;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.SuperInterface;
import reach.project.utils.viewHelpers.HandOverMessage;


public class ContactsListFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, HandOverMessage<FriendsAdapter.ClickData> {

    private final FriendsAdapter friendsAdapter = new FriendsAdapter(this);

    private View rootView;

    private SharedPreferences sharedPreferences;
    private SuperInterface mListener;

    public static final AtomicBoolean synchronizeOnce = new AtomicBoolean(false);//have we already synchronized ?
    private static final AtomicBoolean
            pinging = new AtomicBoolean(false),        //are we pinging ?
            synchronizing = new AtomicBoolean(false),  //are we synchronizing ?
            firstTimeLoad = new AtomicBoolean(true);   //is this first time load ?

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

        getLoaderManager().destroyLoader(StaticData.FRIENDS_VERTICAL_LOADER);
        getLoaderManager().destroyLoader(StaticData.FRIENDS_HORIZONTAL_LOADER);
        if (friendsAdapter != null) {
            friendsAdapter.setHorizontalCursor(null);
            friendsAdapter.setVerticalCursor(null);
        }

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
        firstTimeLoad.set(SharedPrefUtils.getFirstIntroSeen(sharedPreferences));
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_contacts, container, false);

        if (serverId == 0 || TextUtils.isEmpty(phoneNumber))
            return null;

        //gridView = MiscUtils.addLoadingToGridView((GridView) rootView.findViewById(R.id.contactsList));
        //gridView.setOnItemClickListener(LocalUtils.clickListener);
        //gridView.setOnScrollListener(scrollListener);
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.contactsList);
        GridLayoutManager manager = new GridLayoutManager(getActivity(), 2);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {

                if (position == 0 || friendsAdapter.getItemViewType(position) == FriendsAdapter.VIEW_TYPE_LOCKED)
                    return 2;
                else
                    return 1;
            }
        });
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(friendsAdapter);

        final boolean isOnline = MiscUtils.isOnline(getActivity());
        //we have not already synchronized !
        if (!synchronizeOnce.get() && !synchronizing.get()) {
            //contact sync will call send ping as well

            if (isOnline) {
                synchronizing.set(true);
                pinging.set(true);
                new LocalUtils.ContactsSync().executeOnExecutor(StaticData.temporaryFix);
            }

        } else if (!pinging.get() && isOnline) {
            //if not pinging send a ping !
            pinging.set(true);
            new LocalUtils.SendPing().executeOnExecutor(StaticData.temporaryFix);
        }

        getLoaderManager().initLoader(StaticData.FRIENDS_VERTICAL_LOADER, null, this);
        getLoaderManager().initLoader(StaticData.FRIENDS_HORIZONTAL_LOADER, null, this);
        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.FRIENDS_VERTICAL_LOADER)
            return new CursorLoader(getActivity(),
                    ReachFriendsProvider.CONTENT_URI,
                    ReachContactsAdapter.requiredProjection,
                    ReachFriendsHelper.COLUMN_STATUS + " != ?",
                    new String[]{ReachFriendsHelper.REQUEST_NOT_SENT + ""},
                    ReachFriendsHelper.COLUMN_USER_NAME + " ASC");
        else if (id == StaticData.FRIENDS_HORIZONTAL_LOADER)
            return new CursorLoader(getActivity(),
                    ReachFriendsProvider.CONTENT_URI,
                    ReachContactsAdapter.requiredProjection,
                    ReachFriendsHelper.COLUMN_STATUS + " = ?",
                    new String[]{ReachFriendsHelper.REQUEST_NOT_SENT + ""},
                    ReachFriendsHelper.COLUMN_USER_NAME + " ASC");
        else
            return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed())
            return;

        if (loader.getId() == StaticData.FRIENDS_VERTICAL_LOADER)
            friendsAdapter.setVerticalCursor(data);

        else if (loader.getId() == StaticData.FRIENDS_HORIZONTAL_LOADER)
            friendsAdapter.setHorizontalCursor(data);

        final int count = data.getCount();
        //TODO hanlde empty view

        if (count != 0) {

            if (firstTimeLoad.get()) {

                SharedPrefUtils.setFirstIntroSeen(sharedPreferences);
                firstTimeLoad.set(false);

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

        if (loader.getId() == StaticData.FRIENDS_VERTICAL_LOADER)
            friendsAdapter.setVerticalCursor(null);
        else if (loader.getId() == StaticData.FRIENDS_HORIZONTAL_LOADER)
            friendsAdapter.setHorizontalCursor(null);
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

    @Override
    public void handOverMessage(FriendsAdapter.ClickData clickData) {

        if (rootView == null)
            return;

        if (clickData.status < 2) {

            if (clickData.networkType == 5)
                Snackbar.make(rootView, "The user has disabled Uploads", Snackbar.LENGTH_LONG).show();
            MiscUtils.useFragment(reference, fragment -> {
                fragment.mListener.onOpenLibrary(clickData.friendId);
            });

        } else if (clickData.status >= 2) {

            final AlertDialog alertDialog = new AlertDialog.Builder(rootView.getContext())
                    .setMessage("Send a friend request to " + clickData.userName + " ?")
                    .setPositiveButton("Yes", LocalUtils.positiveButton)
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                    }).create();

            alertDialog.setOnShowListener(dialog -> alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTag(
                    //set tag to use when positive button click
                    new Object[]{clickData.friendId, clickData.status, new WeakReference<>(rootView)}
            ));
            alertDialog.show();
        }
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
