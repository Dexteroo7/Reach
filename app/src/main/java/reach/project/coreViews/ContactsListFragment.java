package reach.project.coreViews;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
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
import android.support.v4.util.LongSparseArray;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.merge.MergeAdapter;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import reach.backend.entities.userApi.model.MusicContainer;
import reach.backend.entities.userApi.model.MyString;
import reach.project.R;
import reach.project.adapter.ReachAllContactsAdapter;
import reach.project.adapter.ReachContactsAdapter;
import reach.project.core.PushActivity;
import reach.project.core.StaticData;
import reach.project.database.ReachAlbum;
import reach.project.database.ReachArtist;
import reach.project.database.contentProvider.ReachFriendsProvider;
import reach.project.database.sql.ReachFriendsHelper;
import reach.project.utils.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.SendSMS;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.SuperInterface;
import reach.project.viewHelpers.Contact;

public class ContactsListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    private ListView listView;
    private View rootView;
    private SearchView searchView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionsMenu actionMenu;
    private SharedPreferences sharedPrefs;
    private MergeAdapter mergeAdapter;
    private ReachContactsAdapter reachContactsAdapter = null;
    private ReachAllContactsAdapter adapter = null;
    private final HashSet<String> inviteSentTo = new HashSet<>();
    private final String inviteKey = "invite_sent";
    private ProgressBar loading;
    private TextView emptyTV1, emptyTV2, notificationCount;

    private SuperInterface mListener;
    private String mCurFilter, selection;
    private String[] selectionArguments;
    private long serverId;
    private final AtomicBoolean pinging = new AtomicBoolean(false);
    public static final LongSparseArray<Future<?>> isMusicFetching = new LongSparseArray<>();

    public static void setNotificationCount(int count) {
        final ContactsListFragment fragment;
        if(reference == null || (fragment = reference.get()) == null || fragment.notificationCount == null)
            return;
        fragment.notificationCount.setText("" + count);
    }

    private final AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

            Object object = mergeAdapter.getItem(position);

            if (object instanceof Cursor) {

                final Cursor cursor = (Cursor) object;
                final long id = cursor.getLong(0);
                final short status = cursor.getShort(9);

                final Future<?> fetching = isMusicFetching.get(id, null);
                if (fetching == null || fetching.isDone() || fetching.isCancelled()) {


                    isMusicFetching.append(id, StaticData.threadPool.submit(new GetMusic(id,
                            getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS))));
                    //Inform only when nece ssary
                    if (status < 2 && cursor.getInt(7) == 0)
                        Toast.makeText(getActivity(), "Refreshing music list", Toast.LENGTH_SHORT).show();
                }

                if (mListener != null) {
                    if (status < 2)
                        mListener.onOpenLibrary(id);
                    else
                        mListener.onOpenProfileView(id);
                }
            } else if (object instanceof Contact) {

                final Contact contact = (Contact) object;
                if (contact.isInviteSent()) return;
                String msg = "Hey! Checkout and download m y  phone music collection with just  a click!" + ".\nhttp://msg.mn/reach\n--\n" + SharedPrefUtils.getUserName(getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS));
                final EditText input = new EditText(getActivity());
                input.setBackgroundResource(0);
                input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                input.setTextColor(getResources().getColor(R.color.darkgrey));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                input.setLayoutParams(lp);
                input.setText(msg);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder
                        .setMessage("Send an invite to " + contact.getUserName() + " ?")
                        .setView(input)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                StaticData.threadPool.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            String txt = input.getText().toString();
                                            if (!TextUtils.isEmpty(txt))
                                                sendSMS(contact.getPhoneNumber(), txt);
                                            else
                                                Toast.makeText(getActivity(), "Please enter an invite message", Toast.LENGTH_SHORT).show();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                                contact.setInviteSent(true);
                                adapter.notifyDataSetChanged();
                                inviteSentTo.add(contact.toString());
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    };

    public void sendSMS(String number, String msg) throws Exception {
        SendSMS smsObj = new SendSMS();
        smsObj.setparams("alerts.sinfini.com ", "sms", "Aed8065339b18aedfbad998aeec2ce9b3", "REACHM");
        smsObj.send_sms(number, msg, "dlr_url");
    }

    private final SwipeRefreshLayout.OnRefreshListener refreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {

            if (!pinging.get()) {
                pinging.set(true);
                new SendPing().executeOnExecutor(StaticData
                        .threadPool);
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
            if (listView.getChildCount() > 0) {

                final boolean firstItemVisible = listView.getFirstVisiblePosition() == 0;
                final boolean topOfFirstItemVisible = listView.getChildAt(0).getTop() == 0;
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

    public ContactsListFragment() {
    }

    private static WeakReference<ContactsListFragment> reference = null;

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
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mListener.setUpDrawer();
        mListener.toggleDrawer(false);
        mListener.toggleSliding(true);

        serverId = SharedPrefUtils.getServerId(getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS));
        /**
         * Invalidate everyone
         */
        final ContentValues contentValues = new ContentValues();
        contentValues.put(ReachFriendsHelper.COLUMN_LAST_SEEN, System.currentTimeMillis() + 31 * 1000);
        contentValues.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.OFFLINE_REQUEST_GRANTED);
        contentValues.put(ReachFriendsHelper.COLUMN_NETWORK_TYPE, (short) 0);
        getActivity().getContentResolver().update(
                ReachFriendsProvider.CONTENT_URI,
                contentValues,
                ReachFriendsHelper.COLUMN_STATUS + " = ?",
                new String[]{ReachFriendsHelper.ONLINE_REQUEST_GRANTED + ""});
        contentValues.clear();
        StaticData.networkCache.clear();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        return new CursorLoader(getActivity(),
                ReachFriendsProvider.CONTENT_URI,
                ReachFriendsHelper.projection,
                selection,
                selectionArguments,
                ReachFriendsHelper.COLUMN_USER_NAME + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        if (cursorLoader.getId() == StaticData.FRIENDS_LOADER && cursor != null && !cursor.isClosed()) {

            if (cursor.getCount() > 0) {

                if (!SharedPrefUtils.getFirstIntroSeen(sharedPrefs)) {
                    StaticData.threadPool.execute(devika1);
                    SharedPrefUtils.setFirstIntroSeen(sharedPrefs);
                }
                mergeAdapter.setActive(loading, false);
                mergeAdapter.setActive(emptyTV1, false);
                actionMenu.setVisibility(View.VISIBLE);
            } else
                actionMenu.setVisibility(View.GONE);
            reachContactsAdapter.swapCursor(cursor);

            if (!StaticData.syncingContacts.get() && cursor.getCount() == 0) {

                Log.i("Downloader", "LOADING EMPTY VIEW ON LOAD FINISHED");
                mergeAdapter.setActive(emptyTV1, true);
            }
        }
    }

    private final Runnable devika1 = new Runnable() {
        @Override
        public void run() {
            if (getActivity() == null)
                return;
            Bitmap bmp = null;
            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(getActivity());
            int px = MiscUtils.dpToPx(64);
            try {
                bmp = Picasso.with(getActivity())
                        .load("https://scontent-sin1-1.xx.fbcdn.net/hphotos-xap1/v/t1.0-9/1011255_638449632916744_321328860_n.jpg?oh=5c1daa8d7d015f7ce698ee1793d5a929&oe=55EECF36&dl=1")
                        .centerCrop()
                        .resize(px, px)
                        .get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Intent intent = new Intent(getActivity(), PushActivity.class);
            intent.putExtra("type", 1);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity())
                    .setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND)
                    .setSmallIcon(R.drawable.ic_icon_notif)
                    .setLargeIcon(bmp)
                    .setContentIntent(pendingIntent)
                            //.addAction(0, "Okay! I got it", pendingIntent)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("I am Devika from Team Reach! \n" +
                                    "Send me an access request by clicking on the lock icon beside my name to view my music collection. \n" +
                                    "Keep Reaching ;)"))
                    .setContentTitle("Hey!")
                    .setTicker("Hey! I am Devika from Team Reach! Send me an access request by clicking on the lock icon beside my name to view my music collection. Keep Reaching ;)")
                    .setContentText("I am Devika from Team Reach! \n" +
                            "Send me an access request by clicking on the lock icon beside my name to view my music collection. \n" +
                            "Keep Reaching ;)")
                    .setPriority(NotificationCompat.PRIORITY_MAX);
            managerCompat.notify(99910, builder.build());
        }
    };

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (cursorLoader.getId() == StaticData.FRIENDS_LOADER) {
            reachContactsAdapter.swapCursor(null);
            actionMenu.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {

        getLoaderManager().destroyLoader(StaticData.FRIENDS_LOADER);
        if (reachContactsAdapter != null && reachContactsAdapter.getCursor() != null && !reachContactsAdapter.getCursor().isClosed())
            reachContactsAdapter.getCursor().close();
        reachContactsAdapter = null;

        if (adapter != null)
            adapter.cleanUp();

        sharedPrefs.edit().putStringSet(inviteKey, inviteSentTo).apply();

        swipeRefreshLayout.setOnRefreshListener(null);
        listView.setOnItemClickListener(null);
        //listView.setOnScrollListener(null);

        actionMenu = null;
        rootView = null;
        if (searchView != null) {
            searchView.setOnQueryTextListener(null);
            searchView.setOnCloseListener(null);
            searchView.setQuery(null, false);
            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }

        searchView = null;
        listView = null;
        swipeRefreshLayout = null;
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_contacts, container, false);
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
            actionBar.setTitle("Reach");
            mListener.setUpNavigationViews();
        }
        sharedPrefs = getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
        inviteSentTo.clear();
        inviteSentTo.addAll(sharedPrefs.getStringSet(inviteKey, new HashSet<String>()));
        /**
         * All cursor operations should be background, else lag
         */

        if (reachContactsAdapter == null)
            reachContactsAdapter = new ReachContactsAdapter(getActivity(), R.layout.myreach_item, null, 0);
        selection = null;
        selectionArguments = null;

        if (serverId == 0)
            return rootView;

        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeContainerContacts);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.reach_color), getResources().getColor(R.color.reach_blue));
        swipeRefreshLayout.setBackgroundResource(R.color.white);
        swipeRefreshLayout.setOnRefreshListener(refreshListener);

        listView = (ListView) rootView.findViewById(R.id.contactsList);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setOnItemClickListener(clickListener);
        mergeAdapter = new MergeAdapter();

        TextView textView = new TextView(getActivity());
        textView.setText("My Friends");
        textView.setTextColor(getResources().getColor(R.color.reach_color));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        textView.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(15), 0, MiscUtils.dpToPx(10));
        mergeAdapter.addView(textView);

        loading = new ProgressBar(getActivity());
        loading.setIndeterminate(true);
        loading.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT));
        mergeAdapter.addView(loading);
        mergeAdapter.addAdapter(reachContactsAdapter);

        emptyTV1 = new TextView(getActivity());
        emptyTV1.setText("No friends found");
        emptyTV1.setTextColor(getResources().getColor(R.color.darkgrey));
        emptyTV1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        //emptyTV1.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT));
        emptyTV1.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(15), 0, MiscUtils.dpToPx(15));
        mergeAdapter.addView(emptyTV1, false);
        mergeAdapter.setActive(emptyTV1, false);

        TextView textView2 = new TextView(getActivity());
        textView2.setText("Invite Friends");
        textView2.setTextColor(getResources().getColor(R.color.reach_color));
        textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        textView2.setTypeface(textView2.getTypeface(), Typeface.BOLD);
        textView2.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(15), 0, MiscUtils.dpToPx(15));
        mergeAdapter.addView(textView2);

        emptyTV2 = new TextView(getActivity());
        emptyTV2.setText("No contacts found");
        emptyTV2.setTextColor(getResources().getColor(R.color.darkgrey));
        emptyTV2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        //emptyTV2.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT));
        emptyTV2.setPadding(MiscUtils.dpToPx(15), MiscUtils.dpToPx(15), 0, MiscUtils.dpToPx(15));
        mergeAdapter.addView(emptyTV2, false);
        mergeAdapter.setActive(emptyTV2, false);

        actionMenu = (FloatingActionsMenu) rootView.findViewById(R.id.right_labels);
        listView.setOnScrollListener(scrollListener);
        FloatingActionButton actionButton = (FloatingActionButton) rootView.findViewById(R.id.share_music_fab);
        actionButton.setOnClickListener(pushLibraryListener);

        if (!pinging.get()) {
            swipeRefreshLayout.setRefreshing(true);
            pinging.set(true);
            new SendPing().executeOnExecutor(StaticData.threadPool);
        }

        new InitializeData().executeOnExecutor(StaticData.threadPool);
        getLoaderManager().initLoader(StaticData.FRIENDS_LOADER, null, this);
        return rootView;
    }

    private class InitializeData extends AsyncTask<Void, String, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            if (getActivity() == null)
                return false;
            final Cursor phones = getActivity().getContentResolver().query(ContactsContract.
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

                if (TextUtils.isEmpty(displayName)) continue;
                contact = new Contact(displayName, number, userID);
                if (inviteSentTo.contains(contact.toString()))
                    contact.setInviteSent(true);
                contacts.add(contact);
            }
            phones.close();
            if (contacts.size() == 0 || getActivity() == null)
                return false;
            final ArrayList<Contact> contactArrayList = new ArrayList<>(contacts);
            Collections.sort(contactArrayList, new Comparator<Contact>() {
                @Override
                public int compare(Contact lhs, Contact rhs) {
                    return lhs.getUserName().compareToIgnoreCase(rhs.getUserName());
                }
            });
            adapter = new ReachAllContactsAdapter(getActivity(), R.layout.allcontacts_user, contactArrayList);
            adapter.setOnEmptyContactsListener(new ReachAllContactsAdapter.OnEmptyContactsListener() {
                @Override
                public void onEmptyContacts() {
                    mergeAdapter.setActive(emptyTV2, true);
                }

                @Override
                public void onNotEmptyContacts() {
                    mergeAdapter.setActive(emptyTV2, false);
                }
            });
            return true;
        }

        @Override
        protected void onPostExecute(Boolean gg) {
            super.onPostExecute(gg);

            if (isCancelled() || getActivity() == null || getActivity().isFinishing() || listView == null)
                return;

            if (gg) {
                mergeAdapter.setActive(emptyTV2, false);
                mergeAdapter.addAdapter(adapter);
                listView.setAdapter(mergeAdapter);
            } else {
                mergeAdapter.setActive(emptyTV2, true);
                MiscUtils.setEmptyTextforListView(listView, "No contacts found");
                Toast.makeText(getActivity(), "No contacts found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onClose() {

        searchView.setQuery(null, true);
        if (adapter != null)
            adapter.getFilter().filter(null);
        selection = null;
        selectionArguments = null;
        getLoaderManager().restartLoader(StaticData.FRIENDS_LOADER, null, this);
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

        if (adapter != null)
            adapter.getFilter().filter(newText);

// Called when the action bar search text has changed.  Update
// the search filter, and restart the loader to do a new query
// with this filter.
        final String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
        // Don't do anything if the filter hasn't actually changed.
        // Prevents restarting the loader when restoring state.
        if (mCurFilter == null && newFilter == null) {
            return true;
        }
        if (mCurFilter != null && mCurFilter.equals(newFilter)) {
            return true;
        }
        mCurFilter = newFilter;

        if (TextUtils.isEmpty(newText)) {
            selection = null;
            selectionArguments = null;
        } else {
            selection = ReachFriendsHelper.COLUMN_USER_NAME + " LIKE ?";
            selectionArguments = new String[]{"%" + mCurFilter + "%"};
        }
        getLoaderManager().restartLoader(StaticData.FRIENDS_LOADER, null, this);
        return true;
    }

    private final class SendPing extends AsyncTask<Void, String, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            StaticData.networkCache.clear();
            MiscUtils.autoRetry(new DoWork<MyString>() {
                @Override
                protected MyString doWork() throws IOException {
                    return StaticData.userEndpoint.pingMyReach(serverId).execute();
                }
            }, Optional.<Predicate<MyString>>absent()).orNull();
            if (isCancelled() || getActivity() == null || getActivity().isFinishing() || swipeRefreshLayout == null)
                return null;
            /**
             * Invalidate those who were online 30 secs ago
             * and send PING
             */
            final ContentValues contentValues = new ContentValues();
            contentValues.put(ReachFriendsHelper.COLUMN_LAST_SEEN, System.currentTimeMillis() + 31 * 1000);
            contentValues.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.OFFLINE_REQUEST_GRANTED);
            contentValues.put(ReachFriendsHelper.COLUMN_NETWORK_TYPE, (short) 0);
            getActivity().getContentResolver().update(
                    ReachFriendsProvider.CONTENT_URI,
                    contentValues,
                    ReachFriendsHelper.COLUMN_STATUS + " = ? and " +
                            ReachFriendsHelper.COLUMN_LAST_SEEN + " < ?",
                    new String[]{ReachFriendsHelper.ONLINE_REQUEST_GRANTED + "", (System.currentTimeMillis() - 30 * 1000) + ""});
            contentValues.clear();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            pinging.set(false);
            if (isCancelled() || getActivity() == null || getActivity().isFinishing() || swipeRefreshLayout == null)
                return;
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private final class GetMusic implements Runnable {

        private final long hostId;
        private final SharedPreferences sharedPreferences;

        private GetMusic(long hostId, SharedPreferences sharedPreferences) {
            this.hostId = hostId;
            this.sharedPreferences = sharedPreferences;
        }

        @Override
        public void run() {

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

            if (musicContainer == null && getActivity() != null)
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), "Music fetch failed", Toast.LENGTH_SHORT).show();
                    }
                });

            if (getActivity() == null || getActivity().isFinishing() || musicContainer == null)
                return;

            if (musicContainer.getSongsChanged()) {

                if (musicContainer.getReachSongs() == null || musicContainer.getReachSongs().size() == 0)
                    //All the songs got deleted
                    MiscUtils.deleteSongs(hostId, getActivity().getContentResolver());
                else {
                    final Pair<Collection<ReachAlbum>, Collection<ReachArtist>> pair =
                            MiscUtils.getAlbumsAndArtists(new HashSet<>(musicContainer.getReachSongs()));
                    final Collection<ReachAlbum> reachAlbums = pair.first;
                    final Collection<ReachArtist> reachArtists = pair.second;
                    MiscUtils.bulkInsertSongs(new HashSet<>(musicContainer.getReachSongs()),
                            reachAlbums,
                            reachArtists,
                            getActivity().getContentResolver());
                }
                SharedPrefUtils.storeSongCodeForUser(hostId, musicContainer.getSongsHash(), sharedPreferences);
                Log.i("Ayush", "Fetching songs, song hash changed for " + hostId + " " + musicContainer.getSongsHash());
            }

            if (musicContainer.getPlayListsChanged() && getActivity() != null && !getActivity().isFinishing()) {

                if (musicContainer.getReachPlayLists() == null || musicContainer.getReachPlayLists().size() == 0)
                    //All playLists got deleted
                    MiscUtils.deletePlayLists(hostId, getActivity().getContentResolver());
                else
                    MiscUtils.bulkInsertPlayLists(new HashSet<>(musicContainer.getReachPlayLists()), getActivity().getContentResolver());
                SharedPrefUtils.storePlayListCodeForUser(hostId, musicContainer.getPlayListHash(), sharedPreferences);
                Log.i("Ayush", "Fetching playLists, playList hash changed for " + hostId + " " + musicContainer.getPlayListHash());
            }
        }
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
        notificationContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onOpenNotificationDrawer();
            }
        });
        notificationCount = (TextView) notificationContainer.findViewById(R.id.reach_q_count);
        notificationCount.setText("0");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setHasOptionsMenu(true);
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
}
