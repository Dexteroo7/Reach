package reach.project.coreViews.invite;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;
import reach.project.utils.SendSMS;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.HandOverMessage;

public class AllContactsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,HandOverMessage<Cursor> {

    private AllContactsAdapter inviteAdapter;
    public static AllContactsFragment newInstance() {
        return new AllContactsFragment();
    }

    public static void showAlert(String name, String number, Context context) {

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

        new AlertDialog.Builder(context)
                .setMessage("Send an invite to " + name + " ?")
                .setView(input)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    final class SendInvite extends AsyncTask<String, Void, Boolean> {

                        @Override
                        protected Boolean doInBackground(String... params) {
                            final SendSMS smsObj = new SendSMS("alerts.sinfini.com", "sms",
                                    "Aed8065339b18aedfbad998aeec2ce9b3", "REACHM", "https://");
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
                            if (aBoolean) {
                                Toast.makeText(context, "Invitation sent", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final TextView inputText = (TextView) input.getChildAt(0);
                        final String txt = inputText.getText().toString();

                        if (!TextUtils.isEmpty(txt))
                            new SendInvite().execute(number, txt);
                        else
                            Toast.makeText(context, "Please enter an invite message", Toast.LENGTH_SHORT).show();

                        dialog.dismiss();
                    }
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                }).create().show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_allcontacts, container, false);

        final Activity activity = getActivity();
        if (activity == null || activity.isFinishing())
            return null;

        final RecyclerView recyclerView = (RecyclerView) rootView;
        inviteAdapter = new AllContactsAdapter(this, R.layout.allcontacts_user);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(inviteAdapter);
        getLoaderManager().initLoader(StaticData.ALL_CONTACTS_LOADER, null, this);

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == StaticData.ALL_CONTACTS_LOADER) {

            final String selection = ContactsContract.CommonDataKinds.Phone.IN_VISIBLE_GROUP + " = '"
                    + ("1") + "'" + " AND " + ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1";
            final String sortOrder = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    + " COLLATE LOCALIZED ASC";

            final String[] projection = new String[]{
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID};

            return new CursorLoader(getActivity(),
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    selection, null,
                    sortOrder);
        } else
            return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.isClosed() || loader.getId() != StaticData.ALL_CONTACTS_LOADER)
            return;

        inviteAdapter.setCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (loader.getId() != StaticData.ALL_CONTACTS_LOADER)
            return;

        inviteAdapter.setCursor(null);
    }

    @Override
    public void handOverMessage(@Nonnull Cursor message) {
        showAlert(message.getString(1), message.getString(0), getContext());
    }
}
