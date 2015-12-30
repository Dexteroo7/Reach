package reach.project.coreViews.friends.invite;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.viewHelpers.HandOverMessage;

public class AllContactsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private SharedPreferences sharedPrefs;

    private ReachAllContactsAdapter inviteAdapter;

    private final String inviteKey = "invite_sent";

    private static WeakReference<AllContactsFragment> reference = null;

    /*private final AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(final AdapterView<?> adapterView, final View view, int position, long l) {

            ImageView listToggle = (ImageView) view.findViewById(R.id.listToggle);
            final Contact contact = (Contact) adapterView.getAdapter().getItem(position);
            if (contact.isInviteSent())
                return;
            LocalUtils.showAlert(inviteAdapter, contact, listToggle, view.getContext());
        }
    };*/

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

        /*if (sharedPrefs != null && !TextUtils.isEmpty(inviteKey))
            sharedPrefs.edit().putStringSet(inviteKey, LocalUtils.inviteSentTo).apply();*/

        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_allcontacts, container, false);

        final Activity activity = getActivity();
        if (activity == null || activity.isFinishing())
            return null;
        sharedPrefs = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);

        final RecyclerView recyclerView = (RecyclerView) rootView;

        inviteAdapter = new ReachAllContactsAdapter(new HandOverMessage<Cursor>() {
            @Override
            public void handOverMessage(@Nonnull Cursor message) {

            }
        }, R.layout.allcontacts_user);

        //mark those who we have already invited !
        //LocalUtils.inviteSentTo.addAll(sharedPrefs.getStringSet(inviteKey, new HashSet<>()));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(inviteAdapter);
        getLoaderManager().initLoader(StaticData.ALL_CONTACTS_LOADER, null, this);
        //new LocalUtils.InitializeData(recyclerView).executeOnExecutor(StaticData.TEMPORARY_FIX);

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == StaticData.ALL_CONTACTS_LOADER)
            return new CursorLoader(getActivity(),
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID},
                    ContactsContract.CommonDataKinds.Phone.NUMBER + " != ?", new String[]{""},
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
        else
            return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.isClosed() || loader.getId() != StaticData.ALL_CONTACTS_LOADER)
            return;

        Log.i("Ayush", "Setting new cursor " + data.getCount());
        inviteAdapter.setCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() != StaticData.ALL_CONTACTS_LOADER)
            return;
        Log.i("Ayush", "Invalidating downloading cursor");
        inviteAdapter.setCursor(null);
    }

    /*private enum LocalUtils {
        ;

        public static final HashSet<String> inviteSentTo = new HashSet<>();

        public static void showAlert(ArrayAdapter adapter, final Contact contact, final ImageView listToggle, final Context context) {

            final String msg = "Hey! Checkout and download my phone Music collection with just a click!" +
                    ".\nhttp://letsreach.co/app\n--\n" +
                    SharedPrefUtils.getUserName(context.getSharedPreferences("Reach", Context.MODE_PRIVATE));

            final LinearLayout input = new LinearLayout(context);
            final EditText inputText = new EditText(context);
            inputText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            inputText.setTextColor(ContextCompat.getColor(context, R.color.darkgrey));
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
                                    listToggle.setImageResource(R.drawable.add_pink);

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
                                listToggle.setImageResource(R.drawable.icon_organize_tick_white);
                                adapter.notifyDataSetChanged();
                                new SendInvite().executeOnExecutor(StaticData.TEMPORARY_FIX, contact.getPhoneNumber(), txt);
                            } else
                                Toast.makeText(context, "Please enter an invite message", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                    }).create().show();
        }
    }*/
}
