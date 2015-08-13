package reach.project.coreViews;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.merge.MergeAdapter;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import reach.backend.entities.userApi.model.MyString;
import reach.project.R;
import reach.project.adapter.ReachAllContactsAdapter;
import reach.project.adapter.ReachContactsAdapter;
import reach.project.core.PushActivity;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.utils.MiscUtils;
import reach.project.utils.QuickSyncFriends;
import reach.project.utils.SendSMS;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.SuperInterface;
import reach.project.utils.auxiliaryClasses.DoWork;
import reach.project.utils.auxiliaryClasses.UseFragment;
import reach.project.viewHelpers.Contact;

public class ContactsListFragment extends Fragment implements
        SearchView.OnQueryTextListener,
        SearchView.OnCloseListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private TextView notificationCount;
    private View emptyFriends, emptyInvite;

    private FloatingActionsMenu actionMenu;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SearchView searchView;

    private SharedPreferences sharedPrefs;
    private SuperInterface mListener;

    private ReachContactsAdapter reachContactsAdapter;
    private ReachAllContactsAdapter inviteAdapter;
    private MergeAdapter mergeAdapter;

    public static final AtomicBoolean synchronizeOnce = new AtomicBoolean(false); ////have we already synchronized ?
    private static final AtomicBoolean
            pinging = new AtomicBoolean(false), //are we pinging ?
            synchronizing = new AtomicBoolean(false); //are we synchronizing ?

    private final String inviteKey = "invite_sent";

    private String mCurFilter, selection;
    private String[] selectionArguments;

    private static String phoneNumber = "";

    private static WeakReference<ContactsListFragment> reference = null;

    private static long serverId;

    private static int friendRequestCount = 0;

    private static int notificationsCount = 0;

    public static void checkNewNotifications() {

        final ContactsListFragment fragment;
        if (reference == null || (fragment = reference.get()) == null || fragment.notificationCount == null)
            return;
        MiscUtils.useFragment(FriendRequestFragment.getReference(), new UseFragment<Void, FriendRequestFragment>() {
            @Override
            public Void work(FriendRequestFragment fragment) {
                friendRequestCount = fragment.adapter.getCount();
                return null;
            }
        });
        MiscUtils.useFragment(NotificationFragment.getReference(), new UseFragment<Void, NotificationFragment>() {
            @Override
            public Void work(NotificationFragment fragment) {
                notificationsCount = fragment.adapter.getCount();
                return null;
            }
        });
        if (friendRequestCount == 0 && notificationsCount == 0)
            fragment.notificationCount.setVisibility(View.GONE);
        else {
            fragment.notificationCount.setVisibility(View.VISIBLE);
            fragment.notificationCount.setText(String.valueOf(friendRequestCount+notificationsCount));
        }
    }

    private final View.OnClickListener openNotification = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mListener.onOpenNotificationDrawer();
        }
    };

    private final AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {

        final class SendRequest extends AsyncTask<Long, Void, Long> {

            @Override
            protected Long doInBackground(final Long... params) {

                /**
                 * params[0] = other id
                 * params[1] = my id
                 * params[2] = status
                 */

                final reach.backend.entities.messaging.model.MyString dataAfterWork = MiscUtils.autoRetry(new DoWork<reach.backend.entities.messaging.model.MyString>() {
                    @Override
                    public reach.backend.entities.messaging.model.MyString doWork() throws IOException {
                        return StaticData
                                .messagingEndpoint
                                .messagingEndpoint()
                                .requestAccess(params[1], params[0]).execute();
                    }
                }, Optional.<Predicate<reach.backend.entities.messaging.model.MyString>>of(new Predicate<reach.backend.entities.messaging.model.MyString>() {
                    @Override
                    public boolean apply(reach.backend.entities.messaging.model.MyString input) {
                        return (input == null || TextUtils.isEmpty(input.getString()) || input.getString().equals("false"));
                    }
                })).orNull();

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
                    MiscUtils.useFragment(reference, new UseFragment<Void, ContactsListFragment>() {
                        @Override
                        public Void work(ContactsListFragment fragment) {
                            Toast.makeText(reference.get().getActivity(), "Request Failed", Toast.LENGTH_SHORT).show();
                            final ContentValues values = new ContentValues();
                            values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.REQUEST_NOT_SENT);
                            reference.get().getActivity().getContentResolver().update(
                                    Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + response),
                                    values,
                                    ReachFriendsHelper.COLUMN_ID + " = ?",
                                    new String[]{response + ""});
                            return null;
                        }
                    });
                }

            }

        }

        @Override
        public void onItemClick(final AdapterView<?> adapterView, final View view, int position, long l) {

            final Object object = adapterView.getAdapter().getItem(position);

            if (object instanceof Cursor) {

                final Cursor cursor = (Cursor) object;
                final long id = cursor.getLong(0);
                final short status = cursor.getShort(5);


                if (mListener != null) {
                    if (status < 2)
                        mListener.onOpenLibrary(id);
                    else {
                        final long clientId = cursor.getLong(0);

                        new SendRequest().executeOnExecutor(
                                StaticData.threadPool,
                                clientId, serverId, (long) status);

                        Toast.makeText(getActivity(), "Access Request sent", Toast.LENGTH_SHORT).show();
                        final ContentValues values = new ContentValues();
                        values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.REQUEST_SENT_NOT_GRANTED);
                        getActivity().getContentResolver().update(
                                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + clientId),
                                values,
                                ReachFriendsHelper.COLUMN_ID + " = ?",
                                new String[]{clientId + ""});
                    }
                }
            } else if (object instanceof Contact) {

                final Contact contact = (Contact) object;
                if (contact.isInviteSent())
                    return;

                LocalUtils.showAlert(inviteAdapter, contact, view.getContext());
            }
        }
    };

    private final SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {

            if (!pinging.get()) {

                Log.i("Ayush", "Starting refresh !");
                pinging.set(true);
                new LocalUtils.SendPing().executeOnExecutor(StaticData.threadPool, swipeRefreshLayout);
            }
        }
    };

    private final AbsListView.OnScrollListener scrollListener = new AbsListView.OnScrollListener() {

        @Override
        public void onScrollStateChanged(AbsListView absListView, int i) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {

            boolean enable = false;
            if (view.getChildCount() > 0) {

                final boolean firstItemVisible = view.getFirstVisiblePosition() == 0;
                final boolean topOfFirstItemVisible = view.getChildAt(0).getTop() == 0;
                enable = firstItemVisible && topOfFirstItemVisible;
            }

            swipeRefreshLayout.setEnabled(enable);
        }
    };

    private final View.OnClickListener pushLibraryListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            mListener.onOpenPushLibrary();
        }
    };

    private final View.OnClickListener inviteFriendListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mListener.onOpenInvitePage();
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

        sharedPrefs.edit().putStringSet(inviteKey, LocalUtils.inviteSentTo).apply();
        //listView.setOnScrollListener(null);
        if (searchView != null) {
            searchView.setOnQueryTextListener(null);
            searchView.setOnCloseListener(null);
            searchView.setQuery(null, false);
            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }

        searchView = null;
        super.onDestroyView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        final Activity activity = getActivity();
        if (activity == null || activity.isFinishing())
            return;

        setHasOptionsMenu(true);
        mListener.setUpDrawer();
        mListener.toggleDrawer(false);
        mListener.toggleSliding(true);
        /*if (getArguments().getBoolean("first", false))
            new InfoDialog().show(getChildFragmentManager(),"info_dialog");*/
        sharedPrefs = activity.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
        serverId = SharedPrefUtils.getServerId(sharedPrefs);
        phoneNumber = SharedPrefUtils.getPhoneNumber(sharedPrefs);

        //clean up
        pinging.set(false);
        synchronizing.set(false);
        synchronizeOnce.set(false);
        LocalUtils.contactData.clear();
        LocalUtils.inviteSentTo.clear();
        //create adapters
        inviteAdapter = new ReachAllContactsAdapter(activity, R.layout.allcontacts_user, LocalUtils.contactData) {

            @Override
            protected void onEmptyContacts() {
                mergeAdapter.setActive(emptyInvite, true);
                mergeAdapter.setActive(inviteAdapter, false);
            }

            @Override
            protected void onNotEmptyContacts() {
                mergeAdapter.setActive(emptyInvite, false);
                mergeAdapter.setActive(inviteAdapter, true);
            }
        };

        reachContactsAdapter = new ReachContactsAdapter(activity, R.layout.myreach_item, null, 0);
        mergeAdapter = new MergeAdapter();
        //setup friends adapter
        mergeAdapter.addView(LocalUtils.createFriendsHeader(activity));
        mergeAdapter.addView(emptyFriends = LocalUtils.friendsEmpty(activity), false);
        mergeAdapter.setActive(emptyFriends, false);
        mergeAdapter.addAdapter(reachContactsAdapter);
        //setup invite adapter
        mergeAdapter.addView(LocalUtils.createInviteHeader(activity));
        mergeAdapter.addView(emptyInvite = LocalUtils.inviteEmpty(activity), false);
        mergeAdapter.setActive(emptyInvite, false);
        mergeAdapter.addAdapter(inviteAdapter);
        //mark those who we have already invited !
        LocalUtils.inviteSentTo.addAll(sharedPrefs.getStringSet(inviteKey, new HashSet<String>()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_contacts, container, false);
        if (serverId == 0 || TextUtils.isEmpty(phoneNumber))
            return null;

        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
            actionBar.setTitle("Reach");
            mListener.setUpNavigationViews();
        }

        actionMenu = (FloatingActionsMenu) rootView.findViewById(R.id.right_labels);

        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeContainerContacts);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.reach_color), getResources().getColor(R.color.reach_grey));
        swipeRefreshLayout.setBackgroundResource(R.color.white);
        swipeRefreshLayout.setOnRefreshListener(refreshListener);

        rootView.findViewById(R.id.share_music_fab).setOnClickListener(pushLibraryListener);
        rootView.findViewById(R.id.invite_friend_fab).setOnClickListener(inviteFriendListener);

        final ListView listView = (ListView) rootView.findViewById(R.id.contactsList);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setOnItemClickListener(clickListener);
        listView.setOnScrollListener(scrollListener);
        listView.setAdapter(mergeAdapter);

        selection = null;
        selectionArguments = null;

        final boolean isOnline = MiscUtils.isOnline(getActivity());
        //we have not already synchronized !
        if (!synchronizeOnce.get() && !synchronizing.get()) {
            //contact sync will call send ping as well

            if (isOnline) {
                synchronizing.set(true);
                pinging.set(true);
                swipeRefreshLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(true);
                    }
                });
                new LocalUtils.ContactsSync().executeOnExecutor(StaticData.threadPool, swipeRefreshLayout);
            }
            new LocalUtils.InitializeData(mergeAdapter, inviteAdapter, emptyInvite).executeOnExecutor(StaticData.threadPool);

        } else if (!pinging.get() && isOnline) {
            //if not pinging send a ping !
            pinging.set(true);
            swipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    swipeRefreshLayout.setRefreshing(true);
                }
            });
            new LocalUtils.SendPing().executeOnExecutor(StaticData.threadPool, swipeRefreshLayout);
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
                    ReachFriendsHelper.COLUMN_STATUS + " ASC, " +
                            ReachFriendsHelper.COLUMN_USER_NAME + " ASC");
        else
            return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (loader.getId() != StaticData.FRIENDS_LOADER || data == null || data.isClosed())
            return;

        reachContactsAdapter.swapCursor(data);
        final int count = data.getCount();

        if (count == 0) {

            mergeAdapter.setActive(emptyFriends, true);
            mergeAdapter.setActive(reachContactsAdapter, false);
            actionMenu.setVisibility(View.GONE);
        } else {

            mergeAdapter.setActive(emptyFriends, false);
            mergeAdapter.setActive(reachContactsAdapter, true);
            if (!SharedPrefUtils.getFirstIntroSeen(sharedPrefs)) {
                StaticData.threadPool.execute(LocalUtils.devikaSendMeSomeLove);
                SharedPrefUtils.setFirstIntroSeen(sharedPrefs);
            }
            actionMenu.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (loader.getId() == StaticData.FRIENDS_LOADER) {
            reachContactsAdapter.swapCursor(null);
            actionMenu.setVisibility(View.GONE);
        }
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

        inviteAdapter.getFilter().filter(mCurFilter);
        getLoaderManager().restartLoader(StaticData.FRIENDS_LOADER, null, this);
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        if (menu == null || inflater == null)
            return;
        menu.clear();

        inflater.inflate(R.menu.myreach_menu, menu);

        searchView = (SearchView) menu.findItem(R.id.search_button).getActionView();
        if (searchView == null)
            return;
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);

        final MenuItem notificationButton = menu.findItem(R.id.notif_button);
        if (notificationButton == null)
            return;
        notificationButton.setActionView(R.layout.reach_queue_counter);
        final View notificationContainer = notificationButton.getActionView().findViewById(R.id.counterContainer);
        notificationContainer.setOnClickListener(openNotification);
        notificationCount = (TextView) notificationContainer.findViewById(R.id.reach_q_count);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (SuperInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
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

        public static final HashSet<String> inviteSentTo = new HashSet<>();
        public static final List<Contact> contactData = new ArrayList<>();

        private static boolean isDead() {

            final Fragment fragment;
            if (reference == null || (fragment = reference.get()) == null)
                return true;
            final Activity activity = fragment.getActivity();
            return activity == null || activity.isFinishing();

        }

        private static Optional<ContentResolver> getResolver() {

            final Fragment fragment;
            if (reference == null || (fragment = reference.get()) == null)
                return Optional.absent();
            final Activity activity = fragment.getActivity();
            if (activity == null || activity.isFinishing())
                return Optional.absent();
            return Optional.of(activity.getContentResolver());
        }

        public static final class InitializeData extends AsyncTask<Void, Void, Boolean> {

            private final MergeAdapter mergeAdapter;
            private final ReachAllContactsAdapter inviteAdapter;
            private final View emptyInvite;

            public InitializeData(MergeAdapter mergeAdapter,
                                  ReachAllContactsAdapter inviteAdapter,
                                  View emptyInvite) {

                this.mergeAdapter = mergeAdapter;
                this.inviteAdapter = inviteAdapter;
                this.emptyInvite = emptyInvite;
            }

            @Override
            protected Boolean doInBackground(Void... voids) {

                Optional<ContentResolver> optional = getResolver();
                if (!optional.isPresent())
                    return false;
                final Cursor phones = optional.get().query(ContactsContract.
                        CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                if (phones == null)
                    return false;

                final HashSet<Contact> contacts = new HashSet<>(phones.getCount());
                while (phones.moveToNext()) {

                    final Contact contact;
                    final String number, displayName;
                    final long userID;

                    if (phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER) == -1 ||
                            phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME) == -1 ||
                            phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID) == -1)
                        continue;

                    number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    displayName = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    userID = phones.getLong(phones.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID));

                    if (TextUtils.isEmpty(displayName))
                        continue;
                    contact = new Contact(displayName, number, userID);

                    if (inviteSentTo.contains(contact.toString()))
                        contact.setInviteSent(true);
                    contacts.add(contact);
                }
                phones.close();

                if (contacts.isEmpty())
                    return false;

                contactData.clear();
                contactData.addAll(contacts);
                Collections.sort(contactData, new Comparator<Contact>() {
                    @Override
                    public int compare(Contact lhs, Contact rhs) {
                        return lhs.getUserName().compareToIgnoreCase(rhs.getUserName());
                    }
                });

                return true;
            }

            @Override
            protected void onPostExecute(Boolean success) {

                super.onPostExecute(success);

                if (mergeAdapter == null || inviteAdapter == null || emptyInvite == null || isDead())
                    return;

                inviteAdapter.notifyDataSetChanged();
                if (success) {
                    mergeAdapter.setActive(emptyInvite, false);
                    mergeAdapter.setActive(inviteAdapter, true);
                } else {
                    mergeAdapter.setActive(emptyInvite, true);
                    mergeAdapter.setActive(inviteAdapter, false);
                }
            }
        }

        public static final class ContactsSync extends AsyncTask<SwipeRefreshLayout, Void, SwipeRefreshLayout> {

            @Override
            protected SwipeRefreshLayout doInBackground(SwipeRefreshLayout... params) {


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
                    final QuickSyncFriends.Status status = new QuickSyncFriends(reference.get().getActivity(), serverId, phoneNumber).call();
                    if (status == QuickSyncFriends.Status.FULL_SYNC) {
                        //full sync was performed
//                    new ForceSyncFriends(weakReference, serverId, phoneNumber).run();
                    }
                } catch (NullPointerException ignored) {
                }
                return params[0];
            }

            @Override
            protected void onPostExecute(SwipeRefreshLayout swipeRefreshLayout) {

                super.onPostExecute(swipeRefreshLayout);
                if (swipeRefreshLayout == null || isDead())
                    return;
                synchronizeOnce.set(true);
                //we are still refreshing !
                pinging.set(true);
                new SendPing().executeOnExecutor(StaticData.threadPool, swipeRefreshLayout);
            }
        }

        public static final class SendPing extends AsyncTask<SwipeRefreshLayout, String, SwipeRefreshLayout> {
            @Override
            protected SwipeRefreshLayout doInBackground(SwipeRefreshLayout... params) {

                StaticData.networkCache.clear();
                MiscUtils.autoRetry(new DoWork<MyString>() {
                    @Override
                    public MyString doWork() throws IOException {
                        return StaticData.userEndpoint.pingMyReach(serverId).execute();
                    }
                }, Optional.<Predicate<MyString>>absent()).orNull();

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
                return params[0];
            }

            @Override
            protected void onPostExecute(final SwipeRefreshLayout refreshLayout) {

                super.onPostExecute(refreshLayout);
                if (refreshLayout == null || isDead())
                    return;
                //finally relax !
                synchronizing.set(false);
                pinging.set(false);
                refreshLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        refreshLayout.setRefreshing(false);
                    }
                });
            }
        }

        public static final Runnable devikaSendMeSomeLove = new Runnable() {
            @Override
            public void run() {

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
                final Intent intent = new Intent(activity, PushActivity.class);
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
            }
        };

//        public static ScaleAnimation createScaleAnimation() {
//            final ScaleAnimation translation = new ScaleAnimation(1f, 0.8f, 1f, 0.8f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
//            translation.setDuration(1000);
//            translation.setInterpolator(new BounceInterpolator());
//            return translation;
//        }

        public static View createFriendsHeader(Context context) {

            final TextView friendsHeader = new TextView(context);
            friendsHeader.setText("Friends on Reach");
            friendsHeader.setTextColor(context.getResources().getColor(R.color.reach_color));
            friendsHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
            friendsHeader.setTypeface(friendsHeader.getTypeface(), Typeface.BOLD);
            friendsHeader.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(15), 0, MiscUtils.dpToPx(10));
            return friendsHeader;
        }

        public static View friendsEmpty(Context context) {

            final TextView emptyFriends = new TextView(context);
            emptyFriends.setText("No friends found");
            emptyFriends.setTextColor(context.getResources().getColor(R.color.darkgrey));
            emptyFriends.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            //emptyTV1.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT));
            emptyFriends.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(15), 0, MiscUtils.dpToPx(15));
            return emptyFriends;
        }

        public static View createInviteHeader(Context context) {

            final TextView inviteHeader = new TextView(context);
            inviteHeader.setText("Invite Friends");
            inviteHeader.setTextColor(context.getResources().getColor(R.color.reach_color));
            inviteHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
            inviteHeader.setTypeface(inviteHeader.getTypeface(), Typeface.BOLD);
            inviteHeader.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(15), 0, MiscUtils.dpToPx(15));
            return inviteHeader;
        }

        public static View inviteEmpty(Context context) {

            final TextView emptyInvite = new TextView(context);
            emptyInvite.setText("No contacts found");
            emptyInvite.setTextColor(context.getResources().getColor(R.color.darkgrey));
            emptyInvite.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            //emptyTV2.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT));
            emptyInvite.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(15), 0, MiscUtils.dpToPx(15));
            return emptyInvite;
        }

        public static void showAlert(ArrayAdapter adapter, final Contact contact, final Context context) {

            final String msg = "Hey! Checkout and download my phone Music collection with just a click!" +
                    ".\nhttp://letsreach.co/app\n--\n" +
                    SharedPrefUtils.getUserName(context.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS));

            final LinearLayout input = new LinearLayout(context);
            final EditText inputText = new EditText(context);
            inputText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            inputText.setTextColor(context.getResources().getColor(R.color.darkgrey));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            int margin = MiscUtils.dpToPx(20);
            lp.setMargins(margin, 0, margin, 0);
            inputText.setLayoutParams(lp);
            inputText.setText(msg);
            input.addView(inputText);

            final WeakReference<ArrayAdapter> arrayAdapterReference = new WeakReference<>(adapter);

            new AlertDialog.Builder(context)
                    .setMessage("Send an invite to " + contact.getUserName() + " ?")
                    .setView(input)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                        final class SendInvite extends AsyncTask<String, Void, Boolean> {

                            @Override
                            protected Boolean doInBackground(String... params) {

                                final SendSMS smsObj = new SendSMS();
                                smsObj.setparams("alerts.sinfini.com ", "sms", "Aed8065339b18aedfbad998aeec2ce9b3", "REACHM");
                                try {
                                    smsObj.send_sms(params[0], params[1], "dlr_url");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return false;
                                }
                                return true;
                            }

                            @Override
                            protected void onPostExecute(Boolean aBoolean) {

                                super.onPostExecute(aBoolean);

                                if (!aBoolean) {
                                    //fail
                                    contact.setInviteSent(false);
                                    LocalUtils.inviteSentTo.remove(contact.toString());

                                    final ArrayAdapter adapter = arrayAdapterReference.get();
                                    if (adapter == null)
                                        return;
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }

                        @Override
                        public void onClick(DialogInterface dialog, int which) {


                            final ArrayAdapter adapter = arrayAdapterReference.get();
                            if (adapter == null) {
                                dialog.dismiss();
                                return;
                            }

                            final TextView inputText = (TextView) input.getChildAt(0);
                            final String txt = inputText.getText().toString();
                            if (!TextUtils.isEmpty(txt))
                                new SendInvite().executeOnExecutor(StaticData.threadPool, contact.getPhoneNumber(), txt);
                            else
                                Toast.makeText(context, "Please enter an invite message", Toast.LENGTH_SHORT).show();

                            Log.i("Ayush", "Marking true " + contact.getUserName());
                            LocalUtils.inviteSentTo.add(contact.toString());

                            contact.setInviteSent(true);
                            adapter.notifyDataSetChanged();
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();
        }
    }
}
