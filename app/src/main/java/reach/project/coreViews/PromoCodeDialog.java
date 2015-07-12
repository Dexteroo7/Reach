package reach.project.coreViews;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import java.io.IOException;
import java.lang.ref.WeakReference;

import reach.backend.entities.userApi.model.MyString;
import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.core.StaticData;
import reach.project.utils.DoWork;
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
            }
        });

        return rootView;
    }

    private final class VerifyPromoCode extends AsyncTask<String, Void, Boolean> {

        final EditText promoCode;
        private VerifyPromoCode(EditText promoCode) {
            this.promoCode = promoCode;
        }

        @Override
        protected Boolean doInBackground(final String... params) {

            if(TextUtils.isEmpty(params[0]))
                return false;

            return MiscUtils.autoRetry(

                    new DoWork<Boolean>() {
                        @Override
                        protected Boolean doWork() throws IOException {

                            final MyString isValid = StaticData.userEndpoint.storePromoCode(
                                    ReachActivity.serverId, params[0]).execute();

                            if(isValid == null || TextUtils.isEmpty(isValid.getString()) ||
                                    isValid.getString().equals("false"))
                                return false;

                            SharedPrefUtils.storePromoCode(
                                    getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS),
                                    params[0]);
                            return true;
                        }
                    }, Optional.<Predicate<Boolean>>absent()).get();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            if(!aBoolean) {
                promoCode.setTextColor(Color.BLACK);
                Toast.makeText(getActivity(), "Please enter a valid code", Toast.LENGTH_SHORT).show();
                promoCode.setEnabled(true);
                return;
            }
            Toast.makeText(getActivity(), "OK", Toast.LENGTH_SHORT).show();
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