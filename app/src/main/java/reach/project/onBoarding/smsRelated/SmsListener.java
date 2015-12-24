package reach.project.onBoarding.smsRelated;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Messenger;
import android.text.TextUtils;

import reach.project.utils.SendSMS;

public class SmsListener extends IntentService {

    public SmsListener() {
        super("SmsListener");
    }
    private static final String SMS_TEXT = "Your activation code is %s . Enter this in the Reach app to complete phone verification";

    private static String PHONE_NUMBER = "PHONE_NUMBER";
    private static String VERIFICATION_CODE = "VERIFICATION_CODE";
    private static String MESSENGER = "MESSENGER";

    public static void sendSms(String phoneNumber, String verificationCode, Messenger messenger, Context context) {

        final Intent intent = new Intent(context, SmsListener.class);
        intent.putExtra(PHONE_NUMBER, phoneNumber);
        intent.putExtra(VERIFICATION_CODE, verificationCode);
        intent.putExtra(MESSENGER, messenger);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent == null)
            throw new IllegalArgumentException("Not expecting empty intent");

        final String phoneNumber = intent.getStringExtra(PHONE_NUMBER);
        final String verificationCode = intent.getStringExtra(VERIFICATION_CODE);
        final Messenger messenger = intent.getParcelableExtra(MESSENGER);

        if (TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(verificationCode) || messenger == null)
            throw new IllegalArgumentException("Expected parameters not found");

        final SendSMS smsObj = SendSMS.getInstance();

        SendResponse sendResponse;
        try {
            sendResponse = smsObj.send_sms(
                    phoneNumber, //number
                    String.format(SMS_TEXT, verificationCode), //message
                    "dlr_url"); //web hook
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!TextUtils.isEmpty(groupId))
            try {
                smsObj.messagedelivery_status(groupId);
            } catch (Exception e) {
                e.printStackTrace();
            }

    }
}
