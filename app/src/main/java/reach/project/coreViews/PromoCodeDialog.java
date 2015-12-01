package reach.project.coreViews;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

/**
 * Created by ashish on 03/07/15.
 */
public class PromoCodeDialog extends DialogFragment {

    private static WeakReference<PromoCodeDialog> reference;
    private RelativeLayout lowerPart;
    private ProgressBar promoLoading;

    private Activity activity;

    public static PromoCodeDialog newInstance() {
        PromoCodeDialog fragment;
        if (reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new PromoCodeDialog());
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.invite_code_dialog, container, false);
        activity = getActivity();
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        final EditText promoCode = (EditText) rootView.findViewById(R.id.iCode);
        lowerPart = (RelativeLayout) rootView.findViewById(R.id.lowerContainer);
        promoLoading = (ProgressBar) rootView.findViewById(R.id.promoLoading);

        promoCode.setText(SharedPrefUtils.getPromoCode(activity.getSharedPreferences("Reach", Context.MODE_PRIVATE)));
        rootView.findViewById(R.id.exit).setOnClickListener(exitListener);
        rootView.findViewById(R.id.done).setOnClickListener(v -> {

            if (TextUtils.isEmpty(promoCode.getText())) {
                Toast.makeText(activity, "Please enter a code", Toast.LENGTH_SHORT).show();
                return;
            }
            lowerPart.setVisibility(View.INVISIBLE);
            promoLoading.setVisibility(View.VISIBLE);
            new VerifyPromoCode().executeOnExecutor
                    (StaticData.temporaryFix, promoCode.getText().toString().toUpperCase());
        });
        return rootView;
    }

    private static final class VerifyPromoCode extends AsyncTask<String, Void, String> {

        private String pCode;

        @Override
        protected String doInBackground(final String... params) {

            if (TextUtils.isEmpty(params[0]))
                return null;

            pCode = params[0];
            BufferedReader reader = null;
            try {

                reader = new BufferedReader(new InputStreamReader(new URL(StaticData.dropBoxPromo).openStream()));
                final StringBuilder total = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    total.append(line);
                final String result = total.toString();
                return result.trim();
            } catch (Exception ignored) {
            } finally {
                if (reader != null)
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {

            super.onPostExecute(result);

            MiscUtils.useFragment(reference, fragment -> {
                try {
                    if (TextUtils.isEmpty(result)) {
                        fragment.dismiss();
                        return;
                    }

                    final Activity activity1 = fragment.getActivity();
                    final List<String> codesList = Arrays.asList(result.split(" "));

                    if (!codesList.contains(pCode)) {

                        Toast.makeText(activity1, "Please enter a valid code", Toast.LENGTH_SHORT).show();
                        fragment.lowerPart.setVisibility(View.VISIBLE);
                        fragment.promoLoading.setVisibility(View.INVISIBLE);
                        return;
                    }
                    SharedPrefUtils.storePromoCode(activity1.getSharedPreferences("Reach", Context.MODE_PRIVATE),
                            pCode);

                    final Cursor friendCursor = activity1.getContentResolver().query(ReachFriendsProvider.CONTENT_URI,
                            ReachFriendsHelper.projection, null, null, null);

                    int friendCount = 0;
                    if (friendCursor != null) {
                        friendCount = friendCursor.getCount();
                        friendCursor.close();
                    }

                    ((ReachApplication) activity1.getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                            .setCategory("Promo Code")
                            .setAction("User Name - " + SharedPrefUtils.getUserName(activity1.getSharedPreferences("Reach", Context.MODE_PRIVATE)))
                            .setLabel("Code - " + pCode + ", Friend Count - " + friendCount)
                            .setValue(1)
                            .build());

                    Toast.makeText(activity1, "Promo code applied", Toast.LENGTH_SHORT).show();
                    fragment.dismiss();
                } catch (IllegalStateException ignored) {}
            });
        }
    }

    private final View.OnClickListener exitListener = v -> {
        try {
            dismiss();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    };
}