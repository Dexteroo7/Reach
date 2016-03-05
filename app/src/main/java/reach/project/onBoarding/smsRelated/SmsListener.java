package reach.project.onBoarding.smsRelated;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.JsonSyntaxException;

import java.util.concurrent.atomic.AtomicBoolean;

import reach.project.utils.SendSMS;

public class SmsListener extends IntentService {

    public SmsListener() {
        super("SmsListener");
    }

    private static final String SMS_TEXT = "Your activation code is %s . Enter this in the Reach app to complete phone verification";

    private static String PHONE_NUMBER = "PHONE_NUMBER";
    private static String VERIFICATION_CODE = "VERIFICATION_CODE";
    private static String MESSENGER = "MESSENGER";
    private static String API_URL = "API_URL";
    private static String SENDER_ID = "SENDER_ID";
    private static String API_KEY = "API_KEY";

    public static void sendSms(String apiUrl, String senderId, String apiKey, String phoneNumber, String verificationCode, Messenger messenger, Context context) {

        final Intent intent = new Intent(context, SmsListener.class);
        intent.putExtra(PHONE_NUMBER, phoneNumber);
        intent.putExtra(VERIFICATION_CODE, verificationCode);
        intent.putExtra(MESSENGER, messenger);
        intent.putExtra(API_URL, apiUrl);
        intent.putExtra(SENDER_ID, senderId);
        intent.putExtra(API_KEY, apiKey);
        context.startService(intent);
    }

    public static AtomicBoolean forceQuit = new AtomicBoolean(true);

    private void sendMessage(@NonNull Messenger messenger,
                             @NonNull Status status) {

        final Message message = Message.obtain();
        message.obj = status; //send error

        try {
            messenger.send(message);
        } catch (RemoteException ignored) {
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent == null)
            throw new IllegalArgumentException("Not expecting empty intent");

        final String phoneNumber = intent.getStringExtra(PHONE_NUMBER);
        final String verificationCode = intent.getStringExtra(VERIFICATION_CODE);
        final Messenger messenger = intent.getParcelableExtra(MESSENGER);
        final String apiUrl = intent.getStringExtra(API_URL);
        final String senderId = intent.getStringExtra(SENDER_ID);
        final String apiKey = intent.getStringExtra(API_KEY);

        if (TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(verificationCode) || messenger == null)
            throw new IllegalArgumentException("Expected parameters not found");

        forceQuit.set(false);
        final SendSMS smsObj = SendSMS.getInstance(apiUrl, senderId, apiKey);

        final SendResponse sendResponse = smsObj.send_sms(
                phoneNumber, //number
                String.format(SMS_TEXT, verificationCode), //message
                "dlr_url"); //web hook

        if (!sendResponse.success || TextUtils.isEmpty(sendResponse.trackingId)) {

            //send error
            sendMessage(messenger, Status.ERROR);
            return; //end
        }

        switch (sendResponse.status) {

            case AWAITED_DLR: {

                sendMessage(messenger, Status.AWAITED_DLR);
                break; //continue to poll
            }
            case DND_NUMBER: {

                sendMessage(messenger, Status.DND_NUMBER);
                return; //failed do not poll
            }
            case OPT_OUT_REJECTION: {

                sendMessage(messenger, Status.OPT_OUT_REJECTION);
                return; //failed do not poll
            }
            case INVALID_NUMBER: {
                sendMessage(messenger, Status.INVALID_NUMBER);
                return; //failed do not poll
            }
        }

        while (!forceQuit.get()) {

            //poll every 5 seconds
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException ignored) {
                return;
            }

            final GroupStatus groupStatus;
            try {
                groupStatus = smsObj.check_status(sendResponse.trackingId);
            } catch (JsonSyntaxException e) {
                sendMessage(messenger, Status.ERROR);
                return;
            }
            if (!groupStatus.success) {

                sendMessage(messenger, Status.ERROR);
                return; //exit
            }

            switch (groupStatus.status) {

                case AWAITED_DLR: {

                    sendMessage(messenger, Status.AWAITED_DLR);
                    break; //continue to poll
                }
                case DND_NUMBER: {

                    sendMessage(messenger, Status.DND_NUMBER);
                    return; //failed do not poll
                }
                case OPT_OUT_REJECTION: {

                    sendMessage(messenger, Status.OPT_OUT_REJECTION);
                    return; //failed do not poll
                }
                case INVALID_NUMBER: {
                    sendMessage(messenger, Status.INVALID_NUMBER);
                    return; //failed do not poll
                }

                case DELIVERED: {
                    sendMessage(messenger, Status.DELIVERED);
                    return; //success do not poll
                }
            }
        }

        //////////////////////
    }
}
