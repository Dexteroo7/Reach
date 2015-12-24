package reach.project.onBoarding;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

import com.google.android.gms.analytics.HitBuilders;
import com.google.common.base.Optional;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import reach.backend.entities.userApi.model.OldUserContainerNew;
import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.ContactsListFragment;
import reach.project.onBoarding.smsRelated.SmsListener;
import reach.project.utils.ForceSyncFriends;
import reach.project.utils.MetaDataScanner;
import reach.project.utils.MiscUtils;
import reach.project.utils.auxiliaryClasses.UseContext2;

public class CodeVerification extends Fragment {

    private static final int MY_PERMISSIONS_RECEIVE_SMS = 33;
    private static final String AUTH_KEY = "AUTH_KEY";
    private static final String PHONE_NUMBER = "PHONE_NUMBER";

    private static String phoneNumber;
    private static String finalAuthKey;

    private static WeakReference<CodeVerification> reference;

    public static CodeVerification newInstance(String authKey, String phoneNumber) {

        final Bundle args;
        CodeVerification fragment;

        if (reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new CodeVerification());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing CodeVerification object :)");
            args = fragment.getArguments();
        }

        args.putString(AUTH_KEY, authKey);
        args.putString(PHONE_NUMBER, phoneNumber);

        return fragment;
    }

    private final ExecutorService oldAccountFetcher = MiscUtils.getRejectionExecutor();

    private final TextWatcher verificationWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable editable) {

            if (finalAuthKey.equals(editable.toString()))
                //TODO proceed to account creation

        }
    };

    private SplashInterface mListener = null;
    @Nullable
    private EditText verificationCode = null;
    @Nullable
    private Future<OldUserContainerNew> containerNewFuture = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_code_verification, container, false);

        phoneNumber = getArguments().getString(PHONE_NUMBER);
        finalAuthKey = getArguments().getString(AUTH_KEY);

        verificationCode = (EditText) rootView.findViewById(R.id.verificationCode);
        verificationCode.requestFocus();
        rootView.findViewById(R.id.verify).setOnClickListener(verifyCodeListener);

        //send sms and wait
        SmsListener.sendSms(phoneNumber, finalAuthKey, messenger, getContext());

        //meanWhile fetch old account
        containerNewFuture = oldAccountFetcher.submit(() -> MiscUtils.autoRetry(() ->
                StaticData.userEndpoint.isAccountPresentNew(phoneNumber).execute(), Optional.absent()).orNull());

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
                getActivity().registerReceiver(SMSReceiver, intentFilter);
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
        getActivity().unregisterReceiver(SMSReceiver);
        verificationCode = null;
    }

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

    private static final Runnable proceedToAccountCreation = () -> {

        final Future<OldUserContainerNew> containerFuture = MiscUtils.useFragment(reference, fragment -> fragment.containerNewFuture).orNull();
        OldUserContainerNew containerNew = null;
        if (containerFuture == null)
            containerNew = null;
        else
            try {
                containerNew = containerFuture.get(5000L, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }

        if (containerNew != null) {

            ContactsListFragment.synchronizeOnce.set(true);
            StaticData.temporaryFix.submit(new ForceSyncFriends(
                    containerNew.getServerId(), //myServerId
                    phoneNumber, //myNumber
                    reference)); //ref to context
        }

        MiscUtils.useContextAndFragment(reference, (context, fragment) -> {

            //start account creation
            fragment.mListener.onOpenAccountCreation(Optional.fromNullable(containerNew));
        });
    };

    private static final class ProceedToAccountCreation extends AsyncTask

    private static final View.OnClickListener verifyCodeListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            final String enteredCode = MiscUtils.useFragment(reference,
                    fragment -> fragment.verificationCode != null ? fragment.verificationCode.getText().toString().trim() : null).orNull();
            final Context context = view.getContext();

            if (!TextUtils.isEmpty(enteredCode) && enteredCode.equals(finalAuthKey)) {

                //start account creation
                StaticData.temporaryFix.submit(proceedToAccountCreation);

            } else
                Toast.makeText(context, "Wrong verification code. Please try again!", Toast.LENGTH_SHORT).show();
        }
    };

    private static final Messenger messenger = new Messenger(new Handler(new Handler.Callback() {

        long songs = 0, playLists = 0;
        private final View.OnClickListener proceed = v -> MiscUtils.useFragment(reference, fragment -> {
            //TODO onAccountCreated
            //fragment.mListener.onAccountCreated();
            return null;
        });

        @Override
        public boolean handleMessage(Message message) {

            if (message == null)
                return false;

            if (message.what == MetaDataScanner.FINISHED) {
                scanCount.setText(songs + "");
                progress.setProgress(100);
                next.setOnClickListener(proceed);
            } else if (message.what == MetaDataScanner.SCANNING_SONGS) {
                scanCount.setText(message.arg1 + "");
                progress.setProgress(message.arg1 * 10);
                songs = message.arg1 + 1;
            }

            return true;
        }
    }));
}