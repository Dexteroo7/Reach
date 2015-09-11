package reach.project.coreViews;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import reach.project.R;
import reach.project.adapter.ReachAllContactsAdapter;
import reach.project.utils.MiscUtils;
import reach.project.utils.SendSMS;
import reach.project.utils.SharedPrefUtils;
import reach.project.viewHelpers.Contact;

public class AllContactsFragment extends Fragment implements
        SearchView.OnQueryTextListener,
        SearchView.OnCloseListener {

    private SearchView searchView;

    private SharedPreferences sharedPrefs;

    private ReachAllContactsAdapter inviteAdapter;

    private final String inviteKey = "invite_sent";

    private static WeakReference<AllContactsFragment> reference = null;


    private final AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(final AdapterView<?> adapterView, final View view, int position, long l) {

            ImageView listToggle = (ImageView) view.findViewById(R.id.listToggle);
            final Contact contact = (Contact) adapterView.getAdapter().getItem(position);
            if (contact.isInviteSent())
                return;
            LocalUtils.showAlert(inviteAdapter, contact, listToggle, view.getContext());
        }
    };

    public static AllContactsFragment newInstance() {

        AllContactsFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            Log.i("Ayush", "Creating new instance of all contacts fragment");
            reference = new WeakReference<>(fragment = new AllContactsFragment());
        } else
            Log.i("Ayush", "Reusing contacts list fragment object :)");

        return fragment;
    }

    @Override
    public void onDestroyView() {

//        if (inviteAdapter != null)
//            inviteAdapter.cleanUp();

        sharedPrefs.edit().putStringSet(inviteKey, LocalUtils.inviteSentTo).apply();
        //listView.setOnScrollListener(null);

        super.onDestroyView();
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

        final View rootView = inflater.inflate(R.layout.fragment_allcontacts, container, false);

        final Activity activity = getActivity();
        if (activity == null || activity.isFinishing())
            return null;
        sharedPrefs = activity.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);

        final ListView listView = MiscUtils.addLoadingToListView((ListView) rootView.findViewById(R.id.allContactsList));
        listView.setOnItemClickListener(clickListener);

        inviteAdapter = new ReachAllContactsAdapter(activity, R.layout.allcontacts_user, LocalUtils.contactData) {

            @Override
            protected void onEmptyContacts() {
                MiscUtils.setEmptyTextForListView(listView, "No contacts found");
            }
        };

        //mark those who we have already invited !
        LocalUtils.inviteSentTo.addAll(sharedPrefs.getStringSet(inviteKey, new HashSet<>()));
        listView.setAdapter(inviteAdapter);
        new LocalUtils.InitializeData(listView).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return rootView;
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
        if (TextUtils.isEmpty(newText))
            return true;

        inviteAdapter.getFilter().filter(newText);
        return true;
    }

    private enum LocalUtils {
        ;

        public static final HashSet<String> inviteSentTo = new HashSet<>();
        public static final List<Contact> contactData = new ArrayList<>();

        private static Optional<ContentResolver> getResolver() {

            final Fragment fragment;
            if (reference == null || (fragment = reference.get()) == null)
                return Optional.absent();
            final Activity activity = fragment.getActivity();
            if (activity == null || activity.isFinishing())
                return Optional.absent();
            return Optional.fromNullable(activity.getContentResolver());
        }

        public static final class InitializeData extends AsyncTask<Void, Void, HashSet<Contact>> {

            private ListView lView;

            private InitializeData(ListView listView) {
                this.lView = listView;
            }

            @Override
            protected HashSet<Contact> doInBackground(Void... voids) {

                final Optional<ContentResolver> optional = getResolver();
                if (!optional.isPresent())
                    return null;

                final Cursor phones = optional.get().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{
                                ContactsContract.CommonDataKinds.Phone.NUMBER,
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID},
                        null, null, null);
                if (phones == null)
                    return null;

                final HashSet<Contact> contacts = new HashSet<>(phones.getCount());
                while (phones.moveToNext()) {

                    final Contact contact;
                    final String number, displayName;
                    final long userID;

                    number = phones.getString(0);
                    displayName = phones.getString(1);
                    userID = phones.getLong(2);

                    if (TextUtils.isEmpty(displayName) || TextUtils.isEmpty(number))
                        continue;
                    contact = new Contact(displayName, number, userID);

                    if (inviteSentTo.contains(String.valueOf(contact.hashCode())))
                        contact.setInviteSent(true);
                    contacts.add(contact);
                }
                phones.close();
                return contacts;
            }

            @Override
            protected void onPostExecute(HashSet<Contact> contactHashSet) {

                super.onPostExecute(contactHashSet);

                MiscUtils.useFragment(reference, fragment -> {

                    if (contactHashSet == null || contactHashSet.isEmpty()) {

                        MiscUtils.setEmptyTextForListView(lView, "No contacts found");
                        fragment.inviteAdapter.clear();
                    } else {

                        contactData.clear();
                        contactData.addAll(contactHashSet);
                        Collections.sort(contactData, (lhs, rhs) -> lhs.getUserName().compareToIgnoreCase(rhs.getUserName()));
                    }
                    fragment.inviteAdapter.notifyDataSetChanged();
                    fragment.inviteAdapter.getFilter().filter("");
                    return null;
                });
            }
        }

        public static void showAlert(ArrayAdapter adapter, final Contact contact, final ImageView listToggle, final Context context) {

            final String msg = "Hey! Checkout and download my phone Music collection with just a click!" +
                    ".\nhttp://letsreach.co/app\n--\n" +
                    SharedPrefUtils.getUserName(context.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS));

            final LinearLayout input = new LinearLayout(context);
            final EditText inputText = new EditText(context);
            inputText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            inputText.setTextColor(context.getResources().getColor(R.color.darkgrey));
            final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
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
                                smsObj.setparams("alerts.sinfini.com", "sms", "Aed8065339b18aedfbad998aeec2ce9b3", "REACHM");
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
                                    LocalUtils.inviteSentTo.remove(String.valueOf(contact.hashCode()));
                                    Picasso.with(context).load(R.drawable.icon_invite).into(listToggle);

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

                            if (!TextUtils.isEmpty(txt)) {
                                Log.i("Ayush", "Marking true " + contact.getUserName());
                                LocalUtils.inviteSentTo.add(String.valueOf(contact.hashCode()));
                                contact.setInviteSent(true);
                                Picasso.with(context).load(R.drawable.add_tick).into(listToggle);
                                adapter.notifyDataSetChanged();
                                new SendInvite().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, contact.getPhoneNumber(), txt);
                            } else
                                Toast.makeText(context, "Please enter an invite message", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                    }).create().show();
        }
    }
}
