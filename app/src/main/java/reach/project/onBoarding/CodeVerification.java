package reach.project.onBoarding;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.appspot.able_door_616.userApi.UserApi;
import com.appspot.able_door_616.userApi.model.UserDataPersistence;
import com.google.android.gms.analytics.HitBuilders;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Optional;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.onBoarding.smsRelated.SmsListener;
import reach.project.onBoarding.smsRelated.Status;
import reach.project.utils.CloudEndPointsUtils;
import reach.project.utils.FireOnce;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

public class CodeVerification extends Fragment {

    private static final int MY_PERMISSIONS_RECEIVE_SMS = 33;
    private static final String AUTH_KEY = "AUTH_KEY";
    private static final String PHONE_NUMBER = "PHONE_NUMBER";
    private static final String COUNTRY_CODE = "COUNTRY_CODE";

    private static WeakReference<CodeVerification> reference;

    public static CodeVerification newInstance(String authKey,
                                               String phoneNumber, String countryCode) {

        final Bundle args;
        CodeVerification fragment;

        reference = new WeakReference<>(fragment = new CodeVerification());
        fragment.setArguments(args = new Bundle());

        args.putString(AUTH_KEY, authKey);
        args.putString(PHONE_NUMBER, phoneNumber);
        args.putString(COUNTRY_CODE, countryCode);

        return fragment;
    }

    private final ExecutorService oldAccountFetcher = MiscUtils.getRejectionExecutor();
    private final ExecutorService accountCreator = MiscUtils.getRejectionExecutor();

    @Nullable
    private SplashInterface mListener = null;
    @Nullable
    private EditText verificationCode = null;
    @Nullable
    private Future<UserDataPersistence> containerNewFuture = null;
    @Nullable
    private String phoneNumber = null, finalAuthKey = null, countryCode = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_code_verification, container, false);

        final Activity activity = getActivity();
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.RECEIVE_SMS) != 0)
                requestPermissions(
                        new String[]{
                                Manifest.permission.RECEIVE_SMS
                        }, MY_PERMISSIONS_RECEIVE_SMS);
            else
                activity.registerReceiver(SMS_RECEIVER, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
        } else
            activity.registerReceiver(SMS_RECEIVER, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));

        phoneNumber = getArguments().getString(PHONE_NUMBER, "");
        countryCode = getArguments().getString(COUNTRY_CODE, "");
        finalAuthKey = getArguments().getString(AUTH_KEY, "");
        final String fullNumber = countryCode + phoneNumber;

        if (TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(countryCode)
                || TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(finalAuthKey)) {
            //should never happen
            activity.finish();
            return rootView;
        }
        Log.i("Ayush", "Final fullNumber = " + fullNumber);

        verificationCode = (EditText) rootView.findViewById(R.id.verificationCode);
        verificationCode.addTextChangedListener(verificationWatcher);
        verificationCode.requestFocus();
        rootView.findViewById(R.id.verify).setOnClickListener(verifyCodeListener);
//        rootView.findViewById(R.id.sendAgain).setOnClickListener(retryListener);

        //send sms and wait
        SmsListener.forceQuit.set(true); //quit current
        if (countryCode.equals("+91"))
            SmsListener.sendSms("alerts.sinfini.com", "REACHA", "A6f5d83ea6aa5984be995761f221c8a9a", fullNumber, finalAuthKey, MESSENGER, getContext());
        else
            SmsListener.sendSms("global.sinfini.com", "REACHAPP", "A93aa2cac66304ce4a754b10dc609ef7b", fullNumber, finalAuthKey, MESSENGER, getContext());

        final SharedPreferences preferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        final HttpTransport transport = new NetHttpTransport();
        final JsonFactory factory = new JacksonFactory();
        final GoogleAccountCredential credential = GoogleAccountCredential
                .usingAudience(activity, StaticData.SCOPE)
                .setSelectedAccountName(SharedPrefUtils.getEmailId(preferences));
        final UserApi userApi = CloudEndPointsUtils.updateBuilder(new UserApi.Builder(transport, factory, credential))
                .setRootUrl("https://1-dot-client-module-dot-able-door-616.appspot.com/_ah/api/").build();

        //meanWhile fetch old account
        containerNewFuture = oldAccountFetcher.submit(
                () -> MiscUtils.autoRetry(() -> userApi.fetchOldAccountData(phoneNumber).execute(), Optional.absent()).orNull());

        Log.i("Ayush", "Using credential " + credential.getSelectedAccountName());

        return rootView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_RECEIVE_SMS)
            if (grantResults.length > 0 && grantResults[0] == 0)
                getActivity().registerReceiver(SMS_RECEIVER, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
        try {
            getActivity().unregisterReceiver(SMS_RECEIVER);
        } catch (IllegalArgumentException ignored) {
        }
        verificationCode = null;
    }

    private final TextWatcher verificationWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable editable) {

            final String enteredCode = editable.toString();
            if (!TextUtils.isEmpty(enteredCode) && enteredCode.equals(finalAuthKey))
                accountCreator.submit(proceedToAccountCreation); //start account creation
        }
    };

    private final Runnable proceedToAccountCreation = () -> {

        UserDataPersistence userDataPersistence = null;
        if (containerNewFuture == null)
            userDataPersistence = null;
        else
            try {
                userDataPersistence = containerNewFuture.get(5000L, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }

        if (userDataPersistence != null) {

            Log.i("Ayush", userDataPersistence.getUserName() + " " + userDataPersistence.getUserId());
            FireOnce.contactSync(
                    new WeakReference<>(getActivity().getApplicationContext()),
                    userDataPersistence.getUserId(),
                    phoneNumber);
        }

        if (mListener != null)
            mListener.onOpenAccountCreation(Optional.fromNullable(userDataPersistence));
    };

    private final View.OnClickListener verifyCodeListener = view -> {

        final String enteredCode = verificationCode != null ? verificationCode.getText().toString().trim() : "";
        final Context context = view.getContext();

        if (!TextUtils.isEmpty(enteredCode) && enteredCode.equals(finalAuthKey)) {

            //start account creation
            accountCreator.submit(proceedToAccountCreation);
        } else
            Toast.makeText(context, "Wrong verification code. Please try again!", Toast.LENGTH_SHORT).show();
    };

    private static final BroadcastReceiver SMS_RECEIVER = new BroadcastReceiver() {

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
                    MiscUtils.useContextAndFragment(reference, (activity, fragment) -> ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                            .setCategory("SEVERE ERROR, number verification intent null")
                            .setAction("Phone Number - " + fragment.phoneNumber)
                            .setValue(1)
                            .build()));
                    //fail
                    return;
                }
            } else {

                //below KITKAT
                final Bundle bundle = intent.getExtras();
                if (bundle == null) {

                    MiscUtils.useContextAndFragment(reference, (activity, fragment) -> ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                            .setCategory("SEVERE ERROR, number verification bundle null")
                            .setAction("Phone Number - " + fragment.phoneNumber)
                            .setValue(1)
                            .build()));
                    //fail
                    return;
                }

                final Object[] pdusObj = (Object[]) bundle.get("pdus");

                if (pdusObj == null || pdusObj.length == 0) {

                    MiscUtils.useContextAndFragment(reference, (activity, fragment) -> ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                            .setCategory("SEVERE ERROR, number verification pdus error")
                            .setAction("Phone Number - " + fragment.phoneNumber)
                            .setValue(1)
                            .build()));//fail
                    return;
                }

                msgs = new SmsMessage[pdusObj.length];
                for (int i = 0; i < pdusObj.length; i++)
                    msgs[i] = getMessageFromBytes((byte[]) pdusObj[i]);
            }

            if (msgs == null || msgs.length == 0) {

                MiscUtils.useContextAndFragment(reference, (activity, fragment) -> ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                        .setCategory("SEVERE ERROR, number verification msgs null")
                        .setAction("Phone Number - " + fragment.phoneNumber)
                        .setValue(1)
                        .build()));
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

                MiscUtils.useFragment(reference, fragment -> {

                    if (fragment.verificationCode != null) {

                        fragment.verificationCode.setText(receivedCode);
                        fragment.verificationCode.setSelection(receivedCode.length());
                    }
                });

                //noinspection UnusedAssignment
                done = true;
                break;
            }

            if (!done) {

                MiscUtils.useContextAndFragment(reference, (activity, fragment) -> ((ReachApplication) activity.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                        .setCategory("Number verification done was false")
                        .setAction("Phone Number - " + fragment.phoneNumber)
                        .setValue(1)
                        .build()));
            }
        }
    };

    private static final Messenger MESSENGER = new Messenger(new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message message) {

            if (message == null)
                return false;

            final Object object = message.obj;
            if (!(object instanceof Status))
                throw new IllegalArgumentException("Sms listener expecting status");

            final Status status = (Status) object;

            switch (status) {

                case AWAITED_DLR: {

                    MiscUtils.useContextFromFragment(reference, context -> {
                        Toast.makeText(context, "Waiting for code", Toast.LENGTH_SHORT).show();
                    });
                    break; //continue to poll
                }

                case DND_NUMBER: {

                    SmsListener.forceQuit.set(true); //quit current
                    MiscUtils.useContextAndFragment(reference, (context, fragment) -> {
                        Toast.makeText(context, "DND number", Toast.LENGTH_SHORT).show();
                        if (fragment.mListener != null)
                            fragment.mListener.onOpenNumberVerification(); //enter new number
                        //track
                    });
                    return false; //failed
                }
                case OPT_OUT_REJECTION: {

                    SmsListener.forceQuit.set(true); //quit current
                    MiscUtils.useContextAndFragment(reference, (context, fragment) -> {
                        Toast.makeText(context, "OPT_OUT number", Toast.LENGTH_SHORT).show();
                        if (fragment.mListener != null)
                            fragment.mListener.onOpenNumberVerification(); //enter new number
                        //track
                    });
                    return false; //failed do not poll
                }

                case DELIVERED: {

                    SmsListener.forceQuit.set(true); //quit current
                    MiscUtils.useContextAndFragment(reference, (context, fragment) -> {
                        Toast.makeText(context, "DELIVERED sms", Toast.LENGTH_SHORT).show();
                        //track
                    });
                    return false; //failed do not poll
                }

                case INVALID_NUMBER: {

                    SmsListener.forceQuit.set(true); //quit current
                    MiscUtils.useContextAndFragment(reference, (context, fragment) -> {
                        Toast.makeText(context, "INVALID number", Toast.LENGTH_SHORT).show();
                        if (fragment.mListener != null)
                            fragment.mListener.onOpenNumberVerification(); //enter new number
                        //track
                    });
                    return false; //failed
                }
                case ERROR: {
                    SmsListener.forceQuit.set(true); //quit current
                    MiscUtils.useContextAndFragment(reference, (context, fragment) -> {
                        Toast.makeText(context, "Some error occurred, plz try again", Toast.LENGTH_SHORT).show();
                        if (fragment.mListener != null)
                            fragment.mListener.onOpenNumberVerification(); //enter new number
                        //track
                    });
                    return false; //failed
                }
            }
            return true;
        }
    }));

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
}