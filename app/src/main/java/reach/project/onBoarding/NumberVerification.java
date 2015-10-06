package reach.project.onBoarding;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import reach.backend.entities.userApi.model.OldUserContainerNew;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.friends.ContactsListFragment;
import reach.project.utils.ForceSyncFriends;
import reach.project.utils.MiscUtils;
import reach.project.utils.SendSMS;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.SuperInterface;

public class NumberVerification extends Fragment{

    private SuperInterface mListener;
    private String SMS_TEXT = "Your activation code is %s . Enter this in the Reach app to complete phone verification";
    private EditText verifyCode;
    private TextView verifyRetry;
    private BroadcastReceiver SMSReceiver;

    public static NumberVerification newInstance () {
        return new NumberVerification();
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
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

    private final class TourPagerAdapter extends PagerAdapter {

        private final String[] tourTexts = new String[]{"Browse through files on\nthe mobile devices of your friends",
                                                               "Build a network\nto have more fun",
                                                               "Connect with your friends\n",
                                                               "Hide the files\nyou don't wish others to see"};
        private final int[] tourImages = new int[]{R.drawable.library_view,
                                                          R.drawable.reach_queue,
                                                          R.drawable.my_reach,
                                                          R.drawable.hide};

        private final Context mContext;
        private final LayoutInflater mLayoutInflater;

        public TourPagerAdapter (Context context) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount () {
            return tourTexts.length;
        }

        @Override
        public boolean isViewFromObject (View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem (ViewGroup container, int position) {

            final View itemView = mLayoutInflater.inflate(R.layout.tour_item, container, false);
            ((TextView) itemView.findViewById(R.id.tour_text)).setText(tourTexts[position]);
            ((ImageView) itemView.findViewById(R.id.tour_image)).setImageResource(tourImages[position]);
            //Picasso.with(container.getContext()).load(tourImages[position]).noFade().into((ImageView) itemView.findViewById(R.id.tour_image));
            container.addView(itemView);
            return itemView;
        }

        @Override
        public void destroyItem (ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }
    }

    private final class ClickListener implements View.OnClickListener {

        private final View bottomPart1, bottomPart2, bottomPart3;
        private final LinearLayout verifyNext;
        private final EditText telephoneNumber;

        private ClickListener (View ... views) {
            this.bottomPart1 = views[0];
            this.bottomPart2 = views[1];
            this.bottomPart3 = views[2];
            this.verifyNext = (LinearLayout) views[3];
            this.telephoneNumber = (EditText) views[4];
        }

        @Override
        public void onClick (View view) {

            final String phoneNumber = telephoneNumber.getText().toString();
            final String parsed;
            if (TextUtils.isEmpty(phoneNumber) || (parsed = phoneNumber.replaceAll("[^0-9]", "")).length() < 10) {
                Toast.makeText(view.getContext(), "Enter Valid Number", Toast.LENGTH_SHORT).show();
                return;
            }

            final int length = parsed.length();
            //take last 10 digits
            new GetOldAccount().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, parsed.substring(length - 10, length));
            bottomPart1.setVisibility(View.INVISIBLE);
            bottomPart3.setVisibility(View.VISIBLE);
        }

        final class GetOldAccount extends AsyncTask<String, Void, Pair<OldUserContainerNew, String>> {

            @Override
            protected final Pair<OldUserContainerNew, String> doInBackground (final String... params) {

                final OldUserContainerNew container = MiscUtils.autoRetry(() ->
                        StaticData.userEndpoint.isAccountPresentNew(params[0]).execute(), Optional.absent()).orNull();

                //start sync
                ContactsListFragment.synchronizeOnce.set(true);
                AsyncTask.THREAD_POOL_EXECUTOR.execute(new ForceSyncFriends(getActivity(), container == null ? 0 : container.getServerId(), params[0]));
                return new Pair<>(container, params[0]);
            }

            @Override
            protected void onPostExecute (final Pair<OldUserContainerNew, String> pair) {

                super.onPostExecute(pair);

                final FragmentActivity activity = getActivity();
                if (isRemoving() || isDetached() || isCancelled() || activity == null || activity.isFinishing() || mListener == null)
                    return;

                final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
                sharedPreferences.edit().clear().apply();

                // If the number is not present inside DB
                if (pair.first == null) {
                    bottomPart3.setVisibility(View.INVISIBLE);
                    bottomPart2.setVisibility(View.VISIBLE);
                    /*
                     *  Generate Auth Key &
                     *  Send SMS verification
                     */
                    String authKey = SharedPrefUtils.getAuthKey(sharedPreferences);
                    if (TextUtils.isEmpty(authKey)) {
                        authKey = String.valueOf(generateSecureRandom());
                        SharedPrefUtils.storeAuthKey(sharedPreferences, authKey);
                    } else {
                        authKey = SharedPrefUtils.getAuthKey(sharedPreferences);
                    }
                    Log.i("Verification", "" + authKey);
                    final String finalAuthKey = authKey;

                    SendVerificationCodeAsync.OnTaskCompleted onTaskCompleted = aBoolean -> {
                        if (!aBoolean) {
                            /*
                             *  -- TODO --
                             *  SMS sending failed, Give user UI to try
                             *  again three times or else fail.
                             */
                            Log.e("Verification", "Code not sent");
                            Toast.makeText(getContext(), "Verification code could not be sent. Please try again!", Toast.LENGTH_SHORT).show();
                        } else {
                            /*
                             *  -- TODO --
                             *  Give UI for entering the code
                             *  and error handling
                             */
                            Log.i("Verification", "Code sent");
                            verifyNext.setOnClickListener(v -> {
                                String enteredCode = String.valueOf(verifyCode.getText());
                                if (enteredCode.equals(finalAuthKey)) {
                                    SharedPrefUtils.storePhoneNumber(sharedPreferences, pair.second);
                                    // Start Account Creation
                                    mListener.startAccountCreation(Optional.fromNullable(null));
                                } else {
                                    Toast.makeText(getContext(), "Wrong verification code. Please try again!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    };

                    verifyRetry.setOnClickListener(v -> {
                        final AlertDialog alertDialog = new AlertDialog.Builder(getContext())
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

                    new SendVerificationCodeAsync(onTaskCompleted).execute(pair.second, String.format(SMS_TEXT, finalAuthKey));
                }
                // If the number is present inside DB
                else {
                    SharedPrefUtils.storePhoneNumber(sharedPreferences, pair.second);
                    // Start Account Creation
                    mListener.startAccountCreation(Optional.fromNullable(pair.first));
                }
            }
        }
    }

    private int generateSecureRandom() {
        try {

            // Create a secure random number generator using the SHA1PRNG algorithm
            SecureRandom secureRandomGenerator = SecureRandom.getInstance("SHA1PRNG");
            return 100000 + secureRandomGenerator.nextInt(900000);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public void onAttach (Context context) {
        super.onAttach(context);
        try {
            mListener = (SuperInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                                                 + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach () {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        /*
          * Receive SMS and enter the code.
          */

        IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        SMSReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    final Object[] pdusObj = (Object[]) bundle.get("pdus");
                    for (Object aPdusObj : pdusObj) {
                        SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) aPdusObj);
                        String message = currentMessage.getDisplayMessageBody();
                        String receivedCode = message.split(" ")[4];
                        if (verifyCode!=null) {
                            verifyCode.setText(receivedCode);
                            verifyCode.setSelection(receivedCode.length());
                        }
                        if (verifyRetry!=null)
                            verifyRetry.setVisibility(View.GONE);
                        Log.i("SmsReceiver", " message: " + message);
                    }
                }
            }
        };
        getActivity().registerReceiver(SMSReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (SMSReceiver != null)
            getActivity().unregisterReceiver(SMSReceiver);
    }

    public static class SendVerificationCodeAsync extends AsyncTask<String, Void, Boolean> {

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