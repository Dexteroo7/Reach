package reach.project.coreViews;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
    private TextView totalEarning, redeemableAmount, redeemedAmount,
            referralEarning, transferEarning, how, terms;
    private EditText registerText;
    private Button registerBtn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_refer, container, false);

        Toolbar mToolbar = (Toolbar)rootView.findViewById(R.id.referToolbar);
        mToolbar.setTitle("Refer and Earn");
        mToolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());

        totalEarning = (TextView) rootView.findViewById(R.id.totalEarning);
        redeemableAmount = (TextView) rootView.findViewById(R.id.redeemableAmount);
        redeemedAmount = (TextView) rootView.findViewById(R.id.redeemedAmount);
        referralEarning = (TextView) rootView.findViewById(R.id.referralEarning);
        transferEarning = (TextView) rootView.findViewById(R.id.transferEarning);
        how = (TextView) rootView.findViewById(R.id.how);
        terms = (TextView) rootView.findViewById(R.id.terms);
        registerText = (EditText) rootView.findViewById(R.id.registerText);
        registerBtn = (Button) rootView.findViewById(R.id.registerBtn);

        String userId = String.valueOf(SharedPrefUtils.getServerId(
                getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE)));
        String shortId = userId.substring(2, (userId.length()-3));

        String md5 = "";

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(shortId.getBytes());
            byte[] digestedBytes = messageDigest.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte digestedByte : digestedBytes)
                hexString.append(Integer.toHexString(0xFF & digestedByte));
            md5 = hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        String url = "http://54.169.227.37:8080/campaign/getUserDetails?userId="
                + userId
                + "&userKey="
                + md5;

        new GetDetails().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);

        registerBtn.setOnClickListener(v -> {
            String email = registerText.getText().toString();
            if (!TextUtils.isEmpty(email)) {
                registerText.setText("Registered with " + email);
                registerText.setEnabled(false);
                registerBtn.setVisibility(View.GONE);
            }
        });

        return rootView;
    }

    private static class GetDetails extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... params) {
            Request request = new Request.Builder()
                    .url(params[0])
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
                    JSONObject jsonObject = new JSONObject(string);
                    String email = jsonObject.getString("emailId");
                    if (!TextUtils.isEmpty(email)) {
                        fragment.registerText.setText("Registered with " + email);
                        fragment.registerText.setEnabled(false);
                        fragment.registerBtn.setVisibility(View.GONE);
                    }
                    fragment.totalEarning.setText("Rs. " + jsonObject.getString("totalAmount"));
                    fragment.redeemableAmount.setText("Rs. " + jsonObject.getString("redeemableAmount"));
                    fragment.redeemedAmount.setText("Rs. " + jsonObject.getString("redeemedAmount"));
                    fragment.referralEarning.setText(jsonObject.getString("referralAmount"));
                    fragment.transferEarning.setText("Rs. " + jsonObject.getString("transfersAmount"));
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
