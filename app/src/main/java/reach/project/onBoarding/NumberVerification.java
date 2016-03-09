package reach.project.onBoarding;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import reach.project.R;
import reach.project.utils.SharedPrefUtils;

public class NumberVerification extends Fragment {

    private static final CountryData[] countryDatas = {
            //new CountryData("AX","+358","Åland Islands"),
            //new CountryData("AI","+1","Anguilla"),
            new CountryData("AT","+43","Austria"),
            new CountryData("BD","+880","Bangladesh"),
            //new CountryData("BB","+1","Barbados"),
            //new CountryData("BM","+1","Bermuda"),
            //new CountryData("VG","+1","British Virgin Islands"),
            new CountryData("CA","+1","Canada"),
            //new CountryData("KY","+1","Cayman Islands"),
            new CountryData("CO","+57","Colombia"),
            //new CountryData("DM","+1","Dominica"),
            new CountryData("DO","+1","Dominican Republic"),
            new CountryData("EG","+20","Egypt"),
            new CountryData("FI","+358","Finland"),
            new CountryData("DE","+49","Germany"),
            new CountryData("GR","+30","Greece"),
            //new CountryData("GD","+1","Grenada"),
            //new CountryData("GU","+1","Guam"),
            //new CountryData("GG","+44","Guernsey"),
            new CountryData("IN","+91","India"),
            new CountryData("ID","+62","Indonesia"),
            new CountryData("IT","+39","Italy"),
            //new CountryData("MS","+1","Montserrat"),
            new CountryData("NZ","+64","New Zealand"),
            //new CountryData("IE","+44","Ireland"),
            //new CountryData("MP","+1","Northern Mariana Islands"),
            new CountryData("PK","+92","Pakistan"),
            new CountryData("PH","+63","Philippines"),
            //new CountryData("PR","+1","Puerto Rico"),
            new CountryData("RU","+7","Russia"),
            //new CountryData("VC","+1","Saint Vincent and the Grenadines"),
            //new CountryData("TC","+1","Turks and Caicos Islands"),
            new CountryData("AE","+971","United Arab Emirates"),
            new CountryData("GB","+44","United Kingdom")
            //new CountryData("VA","+379","Vatican City"),
    };

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

    private static final InputFilter LENGTH_FILTER = new InputFilter.LengthFilter(14);
    private Spinner spinner;

    public static NumberVerification newInstance() {
        return new NumberVerification();
    }

    @Nullable
    private EditText telephoneNumber = null;
    @Nullable
    private SplashInterface mListener = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_number_verification, container, false);

        telephoneNumber = (EditText) rootView.findViewById(R.id.telephoneNumber);
        //telephoneNumber.setText("+91-");
        spinner = (Spinner) rootView.findViewById(R.id.countryCodeSpinner);
        spinner.setAdapter(new CustomSpinnerAdapter(getActivity(),android.R.layout.simple_list_item_1,countryDatas));

        final String userCountry = getUserCountry(getContext());
        Log.d("Ashish", "userCountry = " + userCountry);
        if (userCountry != null && userCountry.length() == 2) {
            for (int i = 0; i < countryDatas.length; i++) {
                if (countryDatas[i].isoCode.equalsIgnoreCase(userCountry))
                    spinner.setSelection(i);
            }
        }

        //telephoneNumber.setFilters(new InputFilter[]{LENGTH_FILTER, SEXY_FILTER});

        //spinner.requestFocus();
        telephoneNumber.requestFocus();
        //Selection.setSelection(telephoneNumber.getText(), ENFORCED_LENGTH);
        //Selection.setSelection(" ", ENFORCED_LENGTH);

        /*//clear the shared pref
        final SharedPreferences preferences = getContext().getSharedPreferences("Reach", Context.MODE_PRIVATE);
        final String retainEmailId = SharedPrefUtils.getEmailId(preferences);
        preferences.edit().clear().apply(); //clear everything other than email-id
        SharedPrefUtils.storeEmailId(preferences, retainEmailId);*/

        rootView.findViewById(R.id.verify).setOnClickListener(clickListener);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        telephoneNumber = null;
    }

    @Nullable
    private static String getUserCountry(Context context) {
        try {
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String country = tm.getSimCountryIso();
            if (country != null && country.length() == 2)
                return country;
            if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
                country = tm.getNetworkCountryIso();
                if (country != null && country.length() == 2)
                    return country;
            }
            country = context.getResources().getConfiguration().locale.getCountry();
            if (country != null && country.length() == 2)
                return country;
        }
        catch (Exception ignore) {}
        return null;
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

        final CountryData countryData = (CountryData) spinner.getSelectedItem();
        if (countryData == null || TextUtils.isEmpty(countryData.diallingCode)) {
            Toast.makeText(view.getContext(), "Select Country Code", Toast.LENGTH_SHORT).show();
            return;
        }
        final String phoneNumber = telephoneNumber != null ? telephoneNumber.getText().toString() : null;
        final String parsed;
        Log.i("Ayush", "PhoneNumber = " + phoneNumber);
        if (TextUtils.isEmpty(phoneNumber) || phoneNumber.length() < 10 ||
                TextUtils.isEmpty(parsed = parsePhoneNumber(phoneNumber)) || parsed.length() < 10) {
            Toast.makeText(view.getContext(), "Enter Valid Number", Toast.LENGTH_SHORT).show();
            return;
        }

        //TODO track
        /*final Map<PostParams, String> simpleParams = MiscUtils.getMap(1);
        simpleParams.put(PostParams.USER_NUMBER, parsed.substring(length - 10, length) + "");
        try {
            UsageTracker.trackLogEvent(simpleParams, UsageTracker.NUM_ENTERED);
        } catch (JSONException ignored) {}*/

        Log.i("Verification", "Code sent");
        final Context context = view.getContext();
        final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        SharedPrefUtils.storePhoneNumber(preferences, phoneNumber); //store the phoneNumber
        mListener.onOpenCodeVerification(phoneNumber, countryData.diallingCode);
    };


    @Nullable
    public static String parsePhoneNumber(@NonNull String enteredPhoneNumber) {

        final String cleansedNumber = enteredPhoneNumber.replaceAll("[^0-9]", "");
        final int length = cleansedNumber.length();
        return length >= 10 ? cleansedNumber.substring(length - 10, length) : null;
    }

    private class CustomSpinnerAdapter extends ArrayAdapter<CountryData> {

        private final LayoutInflater inflater;

        public CustomSpinnerAdapter(Context context, int resource, CountryData[] countryDatas) {
            super(context, resource, countryDatas);
            inflater = LayoutInflater.from(context);
        }

        private final class ViewHolder {
            private final TextView textView;
            private ViewHolder(TextView textView) {
                this.textView = textView;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder viewHolder;
            if(convertView==null){
                convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                viewHolder = new ViewHolder((TextView) convertView.findViewById(android.R.id.text1));
                convertView.setTag(viewHolder);
            }
            else
                viewHolder = (ViewHolder) convertView.getTag();
            viewHolder.textView.setText(getItem(position).diallingCode);
            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            final ViewHolder viewHolder;
            if(convertView==null){
                convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                viewHolder = new ViewHolder((TextView) convertView.findViewById(android.R.id.text1));
                convertView.setTag(viewHolder);
            }
            else
                viewHolder = (ViewHolder) convertView.getTag();
            final CountryData countryData = getItem(position);
            viewHolder.textView.setText(countryData.countryName +", " +countryData.diallingCode);
            return convertView;
        }

    }
}