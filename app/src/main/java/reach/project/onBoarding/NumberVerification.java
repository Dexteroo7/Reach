package reach.project.onBoarding;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.text.Selection;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.utils.SharedPrefUtils;

public class NumberVerification extends Fragment {

    private static final byte ENFORCED_LENGTH = 4;

    private static final InputFilter SEXY_FILTER = (source, start, end, dest, destinationStart, destinationEnd) -> {
        if (destinationStart < ENFORCED_LENGTH)
            if (destinationEnd < ENFORCED_LENGTH)
                return dest.subSequence(destinationStart, destinationEnd);
            else
                return dest.subSequence(destinationStart, ENFORCED_LENGTH);
        else
            return null;
    };

    private static WeakReference<NumberVerification> reference = null;

    public static NumberVerification newInstance() {

        NumberVerification numberVerification;
        if (reference == null || (numberVerification = reference.get()) == null)
            reference = new WeakReference<>(numberVerification = new NumberVerification());
        return numberVerification;
    }

    @Nullable
    private EditText telephoneNumber = null;
    private SplashInterface mListener = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_number_verification, container, false);

        telephoneNumber = (EditText) rootView.findViewById(R.id.telephoneNumber);
        telephoneNumber.setFilters(new InputFilter[]{SEXY_FILTER});
        telephoneNumber.requestFocus();
        Selection.setSelection(telephoneNumber.getText(), ENFORCED_LENGTH);

        //clear the shared pref
        final SharedPreferences preferences = rootView.getContext().getSharedPreferences("Reach", Context.MODE_PRIVATE);
        preferences.edit().clear().apply();

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

    private final View.OnClickListener clickListener = view -> {

        final String phoneNumber = telephoneNumber != null ? telephoneNumber.getText().toString() : null;

        Log.i("Ayush", "PhoneNumber = " + phoneNumber);

        final String parsed;
        //replace every non-digit, will retain a minimum of 2 digits (91)
        if (TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(parsed = parsePhoneNumber(phoneNumber)) || parsed.length() < 10) {

            Toast.makeText(view.getContext(), "Enter Valid Number", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.i("Ayush", "Final phoneNumber = " + parsed);

        //TODO track
            /*final Map<PostParams, String> simpleParams = MiscUtils.getMap(1);
            simpleParams.put(PostParams.USER_NUMBER, parsed.substring(length - 10, length) + "");
            try {
                UsageTracker.trackLogEvent(simpleParams, UsageTracker.NUM_ENTERED);
            } catch (JSONException ignored) {}*/

        Log.i("Verification", "Code sent");
        final Context context = view.getContext();
        final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        SharedPrefUtils.storePhoneNumber(preferences, parsed); //store the phoneNumber
        mListener.onOpenCodeVerification(parsed);
    };

    @Nullable
    public static String parsePhoneNumber(@NonNull String enteredPhoneNumber) {

        final String cleansedNumber = enteredPhoneNumber.replaceAll("[^0-9]", "");
        final int length = cleansedNumber.length();
        return cleansedNumber.substring(length - 10, length);
    }
}