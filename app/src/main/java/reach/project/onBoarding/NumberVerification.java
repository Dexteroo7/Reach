package reach.project.onBoarding;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
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

    private static final Pair[] countryCodeData = {
            new Pair<>("","Åland Islands"),
            new Pair<>("","Anguilla"),
            new Pair<>("","Austria"),
            new Pair<>("","Bangladesh"),
            new Pair<>("","Barbados"),
            new Pair<>("","Bermuda"),
            new Pair<>("","British Virgin Islands"),
            new Pair<>("","Canada"),
            new Pair<>("","Cayman Islands"),
            new Pair<>("","Colombia"),
            new Pair<>("","Dominica"),
            new Pair<>("","Dominican Republic"),
            new Pair<>("","Egypt"),
            new Pair<>("","United Kingdom"),
            new Pair<>("","Finland"),
            new Pair<>("","Germany"),
            new Pair<>("","Greece"),
            new Pair<>("","Grenada"),
            new Pair<>("","Guam"),
            new Pair<>("","Guernsey"),
            new Pair<>("+91","India"),
            new Pair<>("","Indonesia"),
            new Pair<>("","Italy"),
            new Pair<>("","Montserrat"),
            new Pair<>("","New Zealand"),
            new Pair<>("","Northern Ireland"),
            new Pair<>("","Northern Mariana Islands"),
            new Pair<>("","Pakistan"),
            new Pair<>("","Philippines"),
            new Pair<>("","Puerto Rico"),
            new Pair<>("","Russia"),
            new Pair<>("","Saint Vincent and the Grenadines"),
            new Pair<>("","Scotland"),
            new Pair<>("","Turks and Caicos Islands"),
            new Pair<>("","United Arab Emirates"),
            new Pair<>("","United Kingdom"),
            new Pair<>("","Vatican City"),
            new Pair<>("","British Virgin Islands"),
            new Pair<>("","Wales")
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
        spinner.setAdapter(new CustomSpinnerAdapter(getActivity(),android.R.layout.simple_list_item_1,countryCodeData));
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

        final StringBuilder sb = new StringBuilder();
        final Pair<String, String> pair;
        if(telephoneNumber !=null && (pair = (Pair<String, String>) spinner.getSelectedItem())!=null){
            sb.append(pair.first);
            sb.append(telephoneNumber.getText().toString());

        }
        //final String phoneNumber = telephoneNumber != null ? telephoneNumber.getText().toString() : null;
        final String phoneNumber = sb.toString();
        Log.i("Ayush", "PhoneNumber = " + phoneNumber);

        if (TextUtils.isEmpty(phoneNumber)) {
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
        mListener.onOpenCodeVerification(phoneNumber);
    };

    private class CustomSpinnerAdapter extends ArrayAdapter<Pair<String, String>> {

        private final LayoutInflater inflater;

        public CustomSpinnerAdapter(Context context, int resource, Pair<String, String>[] objects) {
            super(context, resource, objects);
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
            viewHolder.textView.setText(getItem(position).first);
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
            final Pair<String, String> pair = getItem(position);
            viewHolder.textView.setText(pair.second +", " +pair.first);
            return convertView;
        }

    }
}