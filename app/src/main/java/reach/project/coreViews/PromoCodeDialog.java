package reach.project.coreViews;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import java.io.IOException;
import java.lang.ref.WeakReference;

import reach.backend.entities.userApi.model.MyString;
import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.utils.auxiliaryClasses.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

/**
 * Created by ashish on 03/07/15.
 */
public class PromoCodeDialog extends DialogFragment {

    private static WeakReference<PromoCodeDialog> reference;
    public static PromoCodeDialog newInstance() {
        PromoCodeDialog fragment;
        if(reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new PromoCodeDialog());
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.invite_code_dialog, container, false);
        final EditText promoCode = (EditText) rootView.findViewById(R.id.iCode);

        promoCode.setText(SharedPrefUtils.getPromoCode(getActivity().getSharedPreferences("Reach",Context.MODE_MULTI_PROCESS)));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        rootView.findViewById(R.id.exit).setOnClickListener(exitListener);
        rootView.findViewById(R.id.done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new VerifyPromoCode(promoCode).executeOnExecutor
                        (StaticData.threadPool, promoCode.getText().toString());
                promoCode.setEnabled(false);
                promoCode.setTextColor(Color.RED);
            }
        });

        return rootView;
    }

    private final class VerifyPromoCode extends AsyncTask<String, Void, Boolean> {

        private final EditText promoCode;
        private String pCode;
        private VerifyPromoCode(EditText promoCode) {
            this.promoCode = promoCode;
        }

        @Override
        protected Boolean doInBackground(final String... params) {

            if(TextUtils.isEmpty(params[0]))
                return false;

            pCode = params[0];
            final boolean result = MiscUtils.autoRetry(

                    new DoWork<Boolean>() {
                        @Override
                        public Boolean doWork() throws IOException {

                            final MyString isValid = StaticData.userEndpoint.storePromoCode(
                                    ReachActivity.serverId, params[0]).execute();

                            return !(isValid == null || TextUtils.isEmpty(isValid.getString()) ||
                                    isValid.getString().equals("false"));
                        }
                    }, Optional.<Predicate<Boolean>>absent()).get();

            if(result)
                SharedPrefUtils.storePromoCode(
                        getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS),
                        params[0]);
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            super.onPostExecute(result);

            final FragmentActivity activity = getActivity();
            if (isRemoving() || isDetached() || isCancelled() || activity == null || activity.isFinishing())
                return;

            if(!result) {

                promoCode.setTextColor(Color.BLACK);
                Toast.makeText(activity, "Please enter a valid code", Toast.LENGTH_SHORT).show();
                promoCode.setEnabled(true);
                return;
            }

            if (!StaticData.debugMode) {
                ((ReachApplication) getActivity().getApplication()).getTracker().send(new HitBuilders.EventBuilder()
                        .setCategory("Promo Code")
                        .setAction("user Name - " + SharedPrefUtils.getUserName(getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)))
                        .setLabel("Code - " + pCode)
                        .setValue(1)
                        .build());
            }

            Toast.makeText(activity, "OK", Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }

    private final View.OnClickListener exitListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dismiss();
        }
    };
}