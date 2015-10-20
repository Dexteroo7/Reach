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
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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

import com.google.android.gms.analytics.HitBuilders;
import com.google.common.base.Optional;
import com.viewpagerindicator.CirclePageIndicator;

import java.lang.ref.WeakReference;
import java.util.Random;

import reach.backend.entities.userApi.model.OldUserContainerNew;
import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.friends.ContactsListFragment;
import reach.project.utils.ForceSyncFriends;
import reach.project.utils.MiscUtils;
import reach.project.utils.SendSMS;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.auxiliaryClasses.SuperInterface;
import reach.project.utils.auxiliaryClasses.UseContext2;

public class NumberVerification extends Fragment {

    private static final String SMS_TEXT = "Your activation code is %s . Enter this in the Reach app to complete phone verification";
    private static final int MY_PERMISSIONS_RECEIVE_SMS = 33;

    private View bottomPart1, bottomPart2, bottomPart3;
    private SuperInterface mListener = null;
    private EditText verifyCode = null, telephoneNumber = null;

    private static String phoneNumber;
    private static String finalAuthKey;
    private static WeakReference<NumberVerification> reference;
    private static OldUserContainerNew containerNew = null;
    public static boolean newUser = false;

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

        rootView.findViewById(R.id.verifyRetry).setOnClickListener(LocalUtils.retryListener);
        rootView.findViewById(R.id.verifyNext).setOnClickListener(LocalUtils.verifyCodeListener);

        //stuff that hides
        bottomPart1 = rootView.findViewById(R.id.bottomPart1);
        bottomPart2 = rootView.findViewById(R.id.bottomPart2);
        bottomPart3 = rootView.findViewById(R.id.bottomPart3);
        //stuff that auto fills
        verifyCode = (EditText) rootView.findViewById(R.id.verifyCode);

        final ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.logo);
        (telephoneNumber = (EditText) rootView.findViewById(R.id.telephoneNumber)).requestFocus();
        TourPagerAdapter tourPagerAdapter = new TourPagerAdapter(rootView.getContext());
        viewPager.setAdapter(tourPagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
            /*if (position == tourPagerAdapter.getCount())
                ((ReachApplication)getActivity().getApplication())
                        .trackGA(Optional.of("OnBoarding"),
                                Optional.of("Completed App Tour"),
                                Optional.of(""),
                                1);*/
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        ((CirclePageIndicator) rootView.findViewById(R.id.circles)).setViewPager(viewPager);
        rootView.findViewById(R.id.verify).setOnClickListener(LocalUtils.clickListener);
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
            }
            else
                getActivity().unregisterReceiver(LocalUtils.SMSReceiver);
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        verifyCode = telephoneNumber = null;
        bottomPart1 = bottomPart2 = bottomPart3;
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

                if (Build.VERSION.SDK_INT >= 19) //KITKAT
                    msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                else {

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

                    MiscUtils.useFragment(reference, fragment -> {

                        if (fragment.verifyCode != null) {

                            fragment.verifyCode.setText(receivedCode);
                            fragment.verifyCode.setSelection(receivedCode.length());
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

        private static final class GetOldAccount extends AsyncTask<String, Void, Pair<OldUserContainerNew, String>> {

            @Override
            protected final Pair<OldUserContainerNew, String> doInBackground(final String... params) {

                final OldUserContainerNew container = MiscUtils.autoRetry(() ->
                        StaticData.userEndpoint.isAccountPresentNew(params[0]).execute(), Optional.absent()).orNull();

                //start sync
                ContactsListFragment.synchronizeOnce.set(true);
                MiscUtils.useContextFromFragment(reference, context -> {
                    AsyncTask.SERIAL_EXECUTOR.execute(new ForceSyncFriends(context, container == null ? 0 : container.getServerId(), params[0]));
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
                newUser = containerNew == null;

                MiscUtils.useFragment(reference, fragment -> {
                    fragment.bottomPart3.setVisibility(View.INVISIBLE);
                    fragment.bottomPart2.setVisibility(View.VISIBLE);
                });
                /*
                 *  Generate Auth Key &
                 *  Send SMS verification
                 */
                final Random random = new Random();
                phoneNumber = pair.second;
                finalAuthKey = String.valueOf(1000 + random.nextInt(10000 - 1000 + 1));
                Log.i("Verification", "" + finalAuthKey);
                new SendVerificationCodeAsync(onTaskCompleted).execute(pair.second, String.format(SMS_TEXT, finalAuthKey));
//                new SendVerificationCodeAsync(onTaskCompleted).execute(pair.second, String.format(SMS_TEXT, finalAuthKey));
            }
        }

        private static final SendVerificationCodeAsync.OnTaskCompleted onTaskCompleted = aBoolean -> {

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
        };

        private static final View.OnClickListener verifyCodeListener = v ->
                MiscUtils.useContextAndFragment(reference, (context, fragment) -> {

                    final String enteredCode = fragment.verifyCode.getText().toString().trim();
                    if (!TextUtils.isEmpty(enteredCode) && enteredCode.equals(finalAuthKey)) {

                        SharedPrefUtils.storePhoneNumber(
                                context.getSharedPreferences("Reach", Context.MODE_PRIVATE),
                                phoneNumber);
                        // Start Account Creation
                        fragment.mListener.startAccountCreation(Optional.fromNullable(containerNew));
                    } else
                        //FAIL
                        Toast.makeText(context, "Wrong verification code. Please try again!", Toast.LENGTH_SHORT).show();
                });

        private static final View.OnClickListener retryListener = view -> new AlertDialog.Builder(view.getContext())
                .setMessage("Send verification code again?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    new SendVerificationCodeAsync(onTaskCompleted).execute(phoneNumber, String.format(SMS_TEXT, finalAuthKey));
                    dialog.dismiss();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(R.drawable.icon_grey)
                .show();

        private static final View.OnClickListener clickListener = view -> {

            final String phoneNumber1 = MiscUtils.useFragment(reference, fragment -> {
                return fragment.telephoneNumber.getText().toString().trim();
            }).orNull();

            Log.i("Ayush", "PhoneNumber = " + phoneNumber1);

            final String parsed;
            if (TextUtils.isEmpty(phoneNumber1) || (parsed = phoneNumber1.replaceAll("[^0-9]", "")).length() < 10) {

                Toast.makeText(view.getContext(), "Enter Valid Number", Toast.LENGTH_SHORT).show();
                return;
            }

            MiscUtils.useFragment(reference, fragment -> {
                Activity activity = fragment.getActivity();
                if (Build.VERSION.SDK_INT >= 23) {
                    if (ContextCompat.checkSelfPermission(activity,
                            Manifest.permission.RECEIVE_SMS) != 0)
                        fragment.requestPermissions(
                                new String[]{
                                        Manifest.permission.RECEIVE_SMS
                                }, MY_PERMISSIONS_RECEIVE_SMS);
                    else
                        activity.registerReceiver(LocalUtils.SMSReceiver, LocalUtils.intentFilter);
                }
                else
                    activity.registerReceiver(LocalUtils.SMSReceiver, LocalUtils.intentFilter);
                fragment.bottomPart1.setVisibility(View.INVISIBLE);
                fragment.bottomPart3.setVisibility(View.VISIBLE);
            });

            final int length = parsed.length();
            //take last 10 digits
            new GetOldAccount().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, parsed.substring(length - 10, length));
        };
    }
}