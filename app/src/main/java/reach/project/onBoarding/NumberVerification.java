package reach.project.onBoarding;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.common.base.Optional;

import java.lang.ref.WeakReference;
import java.util.Random;

import reach.backend.entities.userApi.model.OldUserContainerNew;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.ContactsListFragment;
import reach.project.utils.ForceSyncFriends;
import reach.project.utils.MiscUtils;
import reach.project.utils.SendSMS;

public class NumberVerification extends Fragment {

    private static final String SMS_TEXT = "Your activation code is %s . Enter this in the Reach app to complete phone verification";
    private static final String enforced = "+91 ";
    private static final byte enforcedLength = 4;

    private SplashInterface mListener = null;
    private static String phoneNumber;
    private static WeakReference<NumberVerification> reference;
    private static OldUserContainerNew containerNew = null;

    public static NumberVerification newInstance() {

        final NumberVerification numberVerification = new NumberVerification();
        reference = new WeakReference<>(numberVerification);
        return numberVerification;
    }

    private final TextWatcher enforcer = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable editable) {

            if (editable.subSequence(0, 3) != enforced) {

                //replace all
                editable.replace(0, editable.length(), enforced);
                Selection.setSelection(editable, 4); //fixed length of 4
            }
        }
    };

    @Nullable
    private EditText telephoneNumber = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_number_verification, container, false);

        telephoneNumber = (EditText) rootView.findViewById(R.id.telephoneNumber);
        telephoneNumber.addTextChangedListener(enforcer);
        telephoneNumber.requestFocus();
        Selection.setSelection(telephoneNumber.getText(), enforcedLength);

        rootView.findViewById(R.id.verify).setOnClickListener(clickListener);
        return rootView;
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        try {
            mListener = (SplashInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement SplashInterface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private static final class SendVerificationCodeAsync extends AsyncTask<String, Void, Boolean> {

        private String key;

        @Override
        protected Boolean doInBackground(String... params) {

            key = params[1];
            final SendSMS smsObj = new SendSMS();
            //noinspection SpellCheckingInspection
            smsObj.setparams("alerts.sinfini.com", "sms", "A6f5d83ea6aa5984be995761f221c8a9a", "REACHA");
            try {
                //Toast.makeText(context,params[1],Toast.LENGTH_SHORT).show();
                smsObj.send_sms(params[0], String.format(SMS_TEXT, key), "dlr_url");
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
                /*
                 *  SMS sending failed, Give user UI to try
                 *  again three times or else fail.
                 */
                Log.e("Verification", "Code not sent");
                MiscUtils.useContextFromFragment(reference, context -> {
                    Toast.makeText(context, "Verification code could not be sent. Please try again!", Toast.LENGTH_SHORT).show();
                });
            } else {
                /*
                 *  Give UI for entering the code
                 *  and error handling
                 */
                Log.i("Verification", "Code sent");
                MiscUtils.useFragment(reference, fragment -> {
                    fragment.mListener.onOpenCodeVerification(key, phoneNumber);
                });
            }
        }
    }

    private static final class GetOldAccount extends AsyncTask<String, Void, Pair<OldUserContainerNew, String>> {

        @Override
        protected final Pair<OldUserContainerNew, String> doInBackground(final String... params) {

            final OldUserContainerNew container = MiscUtils.autoRetry(() ->
                    StaticData.userEndpoint.isAccountPresentNew(params[0]).execute(), Optional.absent()).orNull();

            //start sync
            ContactsListFragment.synchronizeOnce.set(true);
            MiscUtils.useContextFromFragment(reference, context -> {
                StaticData.temporaryFix.execute(new ForceSyncFriends(
                        new WeakReference<>(context), //context
                        container == null ? 0 : container.getServerId(), //myServerId
                        params[0])); //myNumber
            });
            return new Pair<>(container, params[0]);
        }

        @Override
        protected void onPostExecute(final Pair<OldUserContainerNew, String> pair) {

            super.onPostExecute(pair);

            MiscUtils.useContextFromFragment(reference, context -> {
                context.getSharedPreferences("Reach", Context.MODE_PRIVATE).edit().clear().apply();
            });

            containerNew = pair.first;
                /*
                 *  Generate Auth Key &
                 *  Send SMS verification
                 */
            final Random random = new Random();
            phoneNumber = pair.second;
            String finalAuthKey = String.valueOf(1000 + random.nextInt(10000 - 1000 + 1));
            Log.i("Verification", "" + finalAuthKey);
            new SendVerificationCodeAsync().execute(pair.second, finalAuthKey);
        }
    }

    private static final View.OnClickListener clickListener = view -> {

        final String phoneNumber = MiscUtils.useFragment(reference, fragment -> fragment.telephoneNumber != null ? fragment.telephoneNumber.getText().toString() : null).orNull();

        Log.i("Ayush", "PhoneNumber = " + phoneNumber);

        final String parsed;
        //replace every non-digit, will retain a minimum of 2 digits (91)
        if (TextUtils.isEmpty(phoneNumber) || (parsed = phoneNumber.replaceAll("[^0-9]", "")).substring(2).length() < 10) {

            Toast.makeText(view.getContext(), "Enter Valid Number", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.i("Ayush", "Final phoneNumber = " + parsed);

        final int length = parsed.length();

        //TODO track
            /*final Map<PostParams, String> simpleParams = MiscUtils.getMap(1);
            simpleParams.put(PostParams.USER_NUMBER, parsed.substring(length - 10, length) + "");
            try {
                UsageTracker.trackLogEvent(simpleParams, UsageTracker.NUM_ENTERED);
            } catch (JSONException ignored) {}*/

        new GetOldAccount().executeOnExecutor(StaticData.temporaryFix, parsed);
    };
}