package reach.project.onBoarding;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.common.base.Optional;

import java.lang.ref.WeakReference;

import reach.backend.entities.userApi.model.OldUserContainerNew;
import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.utils.MiscUtils;
import reach.project.utils.SendSMS;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.UseContext2;

public class CodeVerification extends Fragment {

    private static final String SMS_TEXT = "Your activation code is %s . Enter this in the Reach app to complete phone verification";
    private static final int MY_PERMISSIONS_RECEIVE_SMS = 33;

    private SplashInterface mListener = null;
    private EditText telephoneNumber = null;

    private static String phoneNumber;
    private static String finalAuthKey;
    private static WeakReference<CodeVerification> reference;
    private static OldUserContainerNew containerNew = null;

    public static CodeVerification newInstance(String authKey) {

        final CodeVerification numberVerification = new CodeVerification();
        Bundle bundle = new Bundle();
        bundle.putString("authKey", authKey);
        numberVerification.setArguments(bundle);
        reference = new WeakReference<>(numberVerification);
        return numberVerification;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_code_verification, container, false);

        finalAuthKey = getArguments().getString("authKey");
        rootView.findViewById(R.id.whyAns).setOnClickListener(LocalUtils.retryListener);

        telephoneNumber = (EditText) rootView.findViewById(R.id.telephoneNumber);
        telephoneNumber.requestFocus();
        rootView.findViewById(R.id.verify).setOnClickListener(LocalUtils.verifyCodeListener);
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_RECEIVE_SMS)
            if (grantResults.length > 0 && grantResults[0] == 0)
                getActivity().registerReceiver(LocalUtils.SMSReceiver, LocalUtils.intentFilter);
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();

        try {
            if (Build.VERSION.SDK_INT >= 23) {
                if (ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.RECEIVE_SMS) == 0)
                    getActivity().unregisterReceiver(LocalUtils.SMSReceiver);
            } else
                getActivity().unregisterReceiver(LocalUtils.SMSReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        telephoneNumber = null;
    }


    private enum LocalUtils {
        ;

        private static final IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        private static final BroadcastReceiver SMSReceiver = new BroadcastReceiver() {

            @SuppressWarnings("deprecation")
            private SmsMessage getMessageFromBytes(byte[] toParse) {
                if (toParse == null || toParse.length == 0)
                    return null;
                return SmsMessage.createFromPdu(toParse);
            }

            @SuppressWarnings("SpellCheckingInspection")
            @Override
            public void onReceive(Context context, Intent intent) {

                final SmsMessage[] msgs;

                if (Build.VERSION.SDK_INT >= 19) { //KITKAT

                    try {
                        msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                    } catch (NullPointerException ignored) {

                        //weird null pointer
                        MiscUtils.useContextFromFragment(reference, new UseContext2<Activity>() {
                            @Override
                            public void work(Activity activity) {

                                ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                                        .setCategory("SEVERE ERROR, number verification intent null")
                                        .setAction("Phone Number - " + phoneNumber)
                                        .setValue(1)
                                        .build());
                            }
                        });
                        //fail
                        return;
                    }
                } else {

                    //below KITKAT
                    final Bundle bundle = intent.getExtras();
                    if (bundle == null) {

                        MiscUtils.useContextFromFragment(reference, new UseContext2<Activity>() {
                            @Override
                            public void work(Activity activity) {

                                ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                                        .setCategory("SEVERE ERROR, number verification bundle null")
                                        .setAction("Phone Number - " + phoneNumber)
                                        .setValue(1)
                                        .build());
                            }
                        });
                        //fail
                        return;
                    }

                    final Object[] pdusObj = (Object[]) bundle.get("pdus");

                    if (pdusObj == null || pdusObj.length == 0) {

                        MiscUtils.useContextFromFragment(reference, new UseContext2<Activity>() {
                            @Override
                            public void work(Activity activity) {

                                ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                                        .setCategory("SEVERE ERROR, number verification pdus error")
                                        .setAction("Phone Number - " + phoneNumber)
                                        .setValue(1)
                                        .build());
                            }
                        });                        //fail
                        return;
                    }

                    msgs = new SmsMessage[pdusObj.length];
                    for (int i = 0; i < pdusObj.length; i++)
                        msgs[i] = getMessageFromBytes((byte[]) pdusObj[i]);
                }

                if (msgs == null || msgs.length == 0) {

                    MiscUtils.useContextFromFragment(reference, new UseContext2<Activity>() {
                        @Override
                        public void work(Activity activity) {

                            ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                                    .setCategory("SEVERE ERROR, number verification msgs null")
                                    .setAction("Phone Number - " + phoneNumber)
                                    .setValue(1)
                                    .build());
                        }
                    });
                    //fail
                    return;
                }

                //noinspection unused
                boolean done = false;
                for (SmsMessage smsMessage : msgs) {

                    if (smsMessage == null)
                        continue;

                    if (!smsMessage.getOriginatingAddress().endsWith("REACHA"))
                        continue;

                    final String message = smsMessage.getDisplayMessageBody();
                    final String[] splitter = message.split(" ");

                    if (splitter.length < 5)
                        continue;

                    final String receivedCode = splitter[4];
                    Log.i("SmsReceiver", " message: " + message);

                    //TODO track
                    /*final Map<PostParams, String> simpleParams = MiscUtils.getMap(1);
                    simpleParams.put(PostParams.USER_NUMBER, phoneNumber);
                    try {
                        UsageTracker.trackLogEvent(simpleParams, UsageTracker.OTP_RECEIVED);
                    } catch (JSONException ignored) {}*/

                    MiscUtils.useFragment(reference, fragment -> {

                        if (fragment.telephoneNumber != null) {

                            fragment.telephoneNumber.setText(receivedCode);
                            fragment.telephoneNumber.setSelection(receivedCode.length());
                        }
                    });

                    //noinspection UnusedAssignment
                    done = true;
                    break;
                }

                if (!done) {

                    MiscUtils.useContextFromFragment(reference, new UseContext2<Activity>() {
                        @Override
                        public void work(Activity activity) {

                            ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                                    .setCategory("Number verification done was false")
                                    .setAction("Phone Number - " + phoneNumber)
                                    .setValue(1)
                                    .build());
                        }
                    });
                }
            }
        };

        private static final View.OnClickListener verifyCodeListener = v ->
                MiscUtils.useContextAndFragment(reference, (context, fragment) -> {

                    if (fragment.telephoneNumber == null) {
                        //TODO track
                        return;
                    }

                    final String enteredCode = fragment.telephoneNumber.getText().toString().trim();
                    if (!TextUtils.isEmpty(enteredCode) && enteredCode.equals(finalAuthKey)) {

                        SharedPrefUtils.storePhoneNumber(
                                context.getSharedPreferences("Reach", Context.MODE_PRIVATE),
                                phoneNumber);
                        //TODO Start Account Creation
                        fragment.mListener.onOpenAccountCreation(Optional.fromNullable(containerNew));
                    } else
                        //FAIL
                        Toast.makeText(context, "Wrong verification code. Please try again!", Toast.LENGTH_SHORT).show();
                });

        private static final class SendVerificationCodeAsync extends AsyncTask<String, Void, Boolean> {

            @Override
            protected Boolean doInBackground(String... params) {

                final SendSMS smsObj = new SendSMS();
                //noinspection SpellCheckingInspection
                smsObj.setparams("alerts.sinfini.com", "sms", "A6f5d83ea6aa5984be995761f221c8a9a", "REACHA");
                try {
                    //Toast.makeText(context,params[1],Toast.LENGTH_SHORT).show();
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
            /*
             *  SMS sending failed, Give user UI to try
             *  again three times or else fail.
             */
                    Log.e("Verification", "Code not sent");
                    MiscUtils.useContextFromFragment(reference, context -> {
                        Toast.makeText(context, "Verification code could not be sent. Please try again!", Toast.LENGTH_SHORT).show();
                    });
                } else
            /*
             *  Give UI for entering the code
             *  and error handling
             */
                    Log.i("Verification", "Code sent");
            }
        }

        private static final View.OnClickListener retryListener = view -> new AlertDialog.Builder(view.getContext())
                .setMessage("Send verification code again?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    new SendVerificationCodeAsync().execute(phoneNumber, String.format(SMS_TEXT, finalAuthKey));
                    dialog.dismiss();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }
}