package reach.project.onBoarding;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.viewpagerindicator.CirclePageIndicator;

import java.lang.ref.WeakReference;
import java.util.Random;

import reach.backend.entities.userApi.model.OldUserContainerNew;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.friends.ContactsListFragment;
import reach.project.utils.ForceSyncFriends;
import reach.project.utils.MiscUtils;
import reach.project.utils.SendSMS;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.SuperInterface;

public class NumberVerification extends Fragment {

    private static final String SMS_TEXT = "Your activation code is %s . Enter this in the Reach app to complete phone verification";

    private SuperInterface mListener = null;
    private TextView verifyRetry = null;
    private EditText verifyCode = null;

    private static WeakReference<NumberVerification> reference;

    public static NumberVerification newInstance() {

        final NumberVerification numberVerification = new NumberVerification();
        reference = new WeakReference<>(numberVerification);
        return numberVerification;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_number_verification, container, false);
        verifyCode = (EditText) rootView.findViewById(R.id.verifyCode);
        verifyRetry = (TextView) rootView.findViewById(R.id.verifyRetry);

        rootView.postDelayed(() -> {

            rootView.setBackgroundResource(0);
            rootView.findViewById(R.id.reach_logo).setVisibility(View.GONE);
            rootView.findViewById(R.id.otherStuff).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.numberVerificationPoster).setVisibility(View.VISIBLE);

            final View telephone = rootView.findViewById(R.id.telephoneNumber);
            final ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.logo);

            telephone.requestFocus();
            viewPager.setAdapter(new TourPagerAdapter(rootView.getContext()));
            ((CirclePageIndicator) rootView.findViewById(R.id.circles)).setViewPager(viewPager);

            rootView.findViewById(R.id.verify).setOnClickListener(new ClickListener(
                    rootView.findViewById(R.id.bottomPart1),
                    rootView.findViewById(R.id.bottomPart2),
                    rootView.findViewById(R.id.bottomPart3),
                    rootView.findViewById(R.id.verifyNext),
                    telephone));
        }, 2000);
        return rootView;
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        try {
            mListener = (SuperInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        //Receive SMS and enter the code.
        getActivity().registerReceiver(SMSReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(SMSReceiver);
    }

    private static final class TourPagerAdapter extends PagerAdapter {

        private static final String[] tourTexts = new String[]{"Browse through files on\nthe mobile devices of your friends",
                "Build a network\nto have more fun",
                "Connect with your friends\n",
                "Hide the files\nyou don't wish others to see"};
        private static final int[] tourImages = new int[]{R.drawable.library_view,
                R.drawable.reach_queue,
                R.drawable.my_reach,
                R.drawable.hide};

        private final Context mContext;
        private final LayoutInflater mLayoutInflater;

        public TourPagerAdapter(Context context) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return tourTexts.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            final View itemView = mLayoutInflater.inflate(R.layout.tour_item, container, false);
            ((TextView) itemView.findViewById(R.id.tour_text)).setText(tourTexts[position]);
            ((ImageView) itemView.findViewById(R.id.tour_image)).setImageResource(tourImages[position]);
            //Picasso.with(container.getContext()).load(tourImages[position]).noFade().into((ImageView) itemView.findViewById(R.id.tour_image));
            container.addView(itemView);
            return itemView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }
    }

    private static final class ClickListener implements View.OnClickListener {

        private final View bottomPart1, bottomPart2, bottomPart3;
        private final LinearLayout verifyNext;
        private final EditText telephoneNumber;

        private ClickListener(View... views) {

            this.bottomPart1 = views[0];
            this.bottomPart2 = views[1];
            this.bottomPart3 = views[2];
            this.verifyNext = (LinearLayout) views[3];
            this.telephoneNumber = (EditText) views[4];
        }

        @Override
        public void onClick(View view) {

            final String phoneNumber = telephoneNumber.getText().toString();
            final String parsed;
            if (TextUtils.isEmpty(phoneNumber) || (parsed = phoneNumber.replaceAll("[^0-9]", "")).length() < 10) {

                Toast.makeText(view.getContext(), "Enter Valid Number", Toast.LENGTH_SHORT).show();
                return;
            }

            final int length = parsed.length();
            //take last 10 digits
            new GetOldAccount(bottomPart2, bottomPart3, verifyNext).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, parsed.substring(length - 10, length));
            bottomPart1.setVisibility(View.INVISIBLE);
            bottomPart3.setVisibility(View.VISIBLE);
        }

        private static final class GetOldAccount extends AsyncTask<String, Void, Pair<OldUserContainerNew, String>> {

            private final Random random = new Random();

            private final View bottomPart2, bottomPart3;
            private final LinearLayout verifyNext;
            private final Context context;

            private GetOldAccount(View bottomPart2, View bottomPart3, LinearLayout verifyNext) {

                this.bottomPart2 = bottomPart2;
                this.bottomPart3 = bottomPart3;
                this.verifyNext = verifyNext;
                this.context = verifyNext.getContext();
            }

            @Override
            protected final Pair<OldUserContainerNew, String> doInBackground(final String... params) {

                final OldUserContainerNew container = MiscUtils.autoRetry(() ->
                        StaticData.userEndpoint.isAccountPresentNew(params[0]).execute(), Optional.absent()).orNull();

                //start sync
                ContactsListFragment.synchronizeOnce.set(true);
                AsyncTask.THREAD_POOL_EXECUTOR.execute(new ForceSyncFriends(context, container == null ? 0 : container.getServerId(), params[0]));
                return new Pair<>(container, params[0]);
            }

            @Override
            protected void onPostExecute(final Pair<OldUserContainerNew, String> pair) {

                super.onPostExecute(pair);

                final SharedPreferences sharedPreferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
                sharedPreferences.edit().clear().apply();
                bottomPart3.setVisibility(View.INVISIBLE);
                bottomPart2.setVisibility(View.VISIBLE);
                /*
                 *  Generate Auth Key &
                 *  Send SMS verification
                 */
                final String finalAuthKey = String.valueOf(1000 + random.nextInt(10000 - 1000 + 1));
                Log.i("Verification", "" + finalAuthKey);

                final SendVerificationCodeAsync.OnTaskCompleted onTaskCompleted = aBoolean -> {

                    if (!aBoolean) {
                        /*
                         *  -- TODO --
                         *  SMS sending failed, Give user UI to try
                         *  again three times or else fail.
                         */
                        Log.e("Verification", "Code not sent");
                        Toast.makeText(context, "Verification code could not be sent. Please try again!", Toast.LENGTH_SHORT).show();
                    } else {
                        /*
                         *  -- TODO --
                         *  Give UI for entering the code
                         *  and error handling
                         */
                        Log.i("Verification", "Code sent");
                        verifyNext.setOnClickListener(v -> {

                            final String enteredCode = MiscUtils.useFragment(reference, fragment -> {
                                return fragment.verifyCode.getText().toString().trim();
                            }).orNull();

                            if (!TextUtils.isEmpty(enteredCode) && enteredCode.equals(finalAuthKey)) {

                                SharedPrefUtils.storePhoneNumber(sharedPreferences, pair.second);
                                // Start Account Creation
                                MiscUtils.useFragment(reference, fragment -> {
                                    fragment.mListener.startAccountCreation(Optional.fromNullable(pair.first));
                                });
                            } else {
                                Toast.makeText(context, "Wrong verification code. Please try again!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                };

                MiscUtils.useFragment(reference, fragment -> {

                    fragment.verifyRetry.setOnClickListener(v -> {

                        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                                .setMessage("Send verification code again?")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    new SendVerificationCodeAsync(onTaskCompleted).execute(pair.second, String.format(SMS_TEXT, finalAuthKey));
                                    dialog.dismiss();
                                })
                                .setNegativeButton("No", (dialog, which) -> {
                                    dialog.dismiss();
                                })
                                .setIcon(R.drawable.icon_grey)
                                .create();
                        alertDialog.show();
                    });
                });

                new SendVerificationCodeAsync(onTaskCompleted).execute(pair.second, String.format(SMS_TEXT, finalAuthKey));
            }
        }
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

            if (Build.VERSION.SDK_INT >= 19) //KITKAT
                msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            else {

                final Bundle bundle = intent.getExtras();
                if (bundle == null) {
                    //TODO track
                    //fail
                    return;
                }

                final Object[] pdusObj = (Object[]) bundle.get("pdus");

                if (pdusObj == null || pdusObj.length == 0) {
                    //TODO track
                    //fail
                    return;
                }

                msgs = new SmsMessage[pdusObj.length];
                for (int i = 0; i < pdusObj.length; i++)
                    msgs[i] = getMessageFromBytes((byte[]) pdusObj[i]);
            }

            if (msgs == null || msgs.length == 0) {
                //TODO track
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

                    if (fragment.verifyCode != null) {

                        fragment.verifyCode.setText(receivedCode);
                        fragment.verifyCode.setSelection(receivedCode.length());
                    }
                    if (fragment.verifyRetry != null)
                        fragment.verifyRetry.setVisibility(View.GONE);
                });

                //noinspection UnusedAssignment
                done = true;
                break;
            }

            //TODO track done, should always be true !
        }
    };

    private static final class SendVerificationCodeAsync extends AsyncTask<String, Void, Boolean> {

        public interface OnTaskCompleted {
            void onTaskCompleted(Boolean aBoolean);
        }

        private OnTaskCompleted listener;

        public SendVerificationCodeAsync(OnTaskCompleted listener) {
            this.listener = listener;
        }

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
            listener.onTaskCompleted(aBoolean);
        }
    }
}