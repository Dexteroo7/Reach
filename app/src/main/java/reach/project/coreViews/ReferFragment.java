package reach.project.coreViews;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.UUID;

import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

public class ReferFragment extends Fragment {

    private static WeakReference<ReferFragment> reference = null;
    public static ReferFragment newInstance() {
        ReferFragment fragment;
        if(reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new ReferFragment());
        return fragment;
    }

    private TextView earning, remaining, heading, note, how, terms;
    private EditText registerText;
    private Button registerBtn;
    private SharedPreferences sharedPrefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_refer, container, false);

        sharedPrefs = getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE);

        Toolbar mToolbar = (Toolbar)rootView.findViewById(R.id.referToolbar);
        mToolbar.setTitle("Earn Rewards");
        mToolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());

        earning = (TextView) rootView.findViewById(R.id.earningText);
        remaining = (TextView) rootView.findViewById(R.id.remainingText);
        heading = (TextView) rootView.findViewById(R.id.headingText);
        note = (TextView) rootView.findViewById(R.id.note);
        how = (TextView) rootView.findViewById(R.id.how);
        terms = (TextView) rootView.findViewById(R.id.terms);
        registerText = (EditText) rootView.findViewById(R.id.registerText);
        registerBtn = (Button) rootView.findViewById(R.id.registerBtn);

        new GetTerms().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        final long userId = SharedPrefUtils.getServerId(sharedPrefs);
        final String url = "http://52.74.117.248:8080/campaign/getUserDetails?userId="
                + userId
                + "&userKey="
                + UUID.randomUUID().toString();

        new GetDetails().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);

        registerBtn.setOnClickListener(view -> {

            final String email = registerText.getText().toString();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(view.getContext(), "Enter an email id first", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(view.getContext(), "Enter a valid email id", Toast.LENGTH_SHORT).show();
                return;
            }

            new SetEmail(email).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, userId);
        });

        return rootView;
    }

    private static class GetDetails extends AsyncTask<String,Void,String> {

        private int getRemainingAmount(int amount) {
            return 100 - (amount%100);
        }

        @Override
        protected String doInBackground(String... params) {

            final Request request = new Request.Builder()
                    .url(params[0])
                    .build();

            try {
                final Response response = ReachApplication.okHttpClient.newCall(request).execute();
                if (response!=null)
                    return response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String string) {

            super.onPostExecute(string);
            MiscUtils.useFragment(reference, fragment -> {

                try {

                    String jsonString = SharedPrefUtils.getCampaignValues(fragment.sharedPrefs);
                    if (TextUtils.isEmpty(string)) {
                        if (TextUtils.isEmpty(jsonString)) {
                            Toast.makeText(fragment.getContext(), "No internet connectivity", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    else {
                        if (TextUtils.isEmpty(jsonString))
                            SharedPrefUtils.storeCampaignValues(fragment.sharedPrefs, string);
                        jsonString = string;
                    }

                    JSONObject jsonObject = new JSONObject(jsonString);
                    String email = jsonObject.getString("emailId");
                    String savedEmail = SharedPrefUtils.getCampaignEmail(fragment.sharedPrefs);
                    if (!TextUtils.isEmpty(savedEmail))
                        email = savedEmail;
                    if (!TextUtils.isEmpty(email)) {
                        fragment.registerText.setEnabled(false);
                        fragment.registerText.setText("You will get updates on your email id");
                        fragment.registerText.setTextColor(Color.BLACK);
                        fragment.registerBtn.setVisibility(View.GONE);
                    }
                    int totalAmount = jsonObject.getInt("totalAmount");
                    if (totalAmount > 300) {
                        fragment.remaining.setText(jsonObject.getString("done"));
                    }
                    else {
                        String milestone = totalAmount < 100 ? "first" : "next";
                        fragment.remaining.setText(Html.fromHtml("<b>" + getRemainingAmount(totalAmount)
                                + " points</b> needed<br />to reach the " + milestone + " milestone"));
                    }

                    int redeemedAmount = jsonObject.getInt("redeemedAmount");
                    int earning = jsonObject.getInt("redeemableAmount") + redeemedAmount;
                    fragment.earning.setText(Html.fromHtml("<b>Rs. " + earning +
                            "</b> earned till now<br />(<b>Rs. " + redeemedAmount + "</b> redeemed)"));
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static class SetEmail extends AsyncTask<Long,Void,Integer> {

        private String email;

        public SetEmail(String email) {
            this.email = email;
        }

        @Override
        protected Integer doInBackground(Long... params) {

            Request request = new Request.Builder()
                    .url("http://52.74.117.248:8080/campaign/SetEmail?emailId="
                            + email + "&userId=" + params[0])
                    .build();
            try {
                Response response = ReachApplication.okHttpClient.newCall(request).execute();
                if (response!=null)
                    return response.code();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return -1;
        }

        @Override
        protected void onPostExecute(Integer code) {

            super.onPostExecute(code);
            MiscUtils.useFragment(reference, fragment -> {
                if (code != 200) {
                    Toast.makeText(fragment.getContext(), "No internet connectivity", Toast.LENGTH_SHORT).show();
                    return;
                }
                SharedPrefUtils.storeCampaignEmail(fragment.sharedPrefs, email);
                if (!TextUtils.isEmpty(email)) {
                    fragment.registerText.setEnabled(false);
                    fragment.registerText.setText("You will get updates on your email id");
                    fragment.registerText.setTextColor(Color.BLACK);
                    fragment.registerBtn.setVisibility(View.GONE);
                }
            });
        }
    }

    private static class GetTerms extends AsyncTask<Void,Void,String> {

        @Override
        protected String doInBackground(Void... params) {
            Request request = new Request.Builder()
                    .url("http://52.74.117.248:8080/campaign/getCampaignTerms")
                    .build();
            try {
                Response response = ReachApplication.okHttpClient.newCall(request).execute();
                if (response!=null)
                    return response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String string) {
            super.onPostExecute(string);
            MiscUtils.useFragment(reference, fragment -> {
                try {
                    String jsonString = SharedPrefUtils.getCampaignTerms(fragment.sharedPrefs);
                    if (TextUtils.isEmpty(string)) {
                        if (TextUtils.isEmpty(jsonString)) {
                            Toast.makeText(fragment.getContext(), "No internet connectivity", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    else {
                        if (TextUtils.isEmpty(jsonString))
                            SharedPrefUtils.storeCampaignTerms(fragment.sharedPrefs, string);
                        jsonString = string;
                    }
                    JSONObject jsonObject = new JSONObject(jsonString);
                    fragment.terms.setText(jsonObject.getString("rules"));
                    fragment.how.setText(jsonObject.getString("howTo"));
                    fragment.heading.setText(jsonObject.getString("heading"));
                    fragment.note.setText(jsonObject.getString("note"));
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
