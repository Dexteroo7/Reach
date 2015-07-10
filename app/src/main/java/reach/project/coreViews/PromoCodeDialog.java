package reach.project.coreViews;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.SuperInterface;

/**
 * Created by ashish on 03/07/15.
 */
public class PromoCodeDialog extends DialogFragment {

    public static PromoCodeDialog newInstance(String phoneNumber, ArrayList<String> codes) {

        final PromoCodeDialog promoCodeDialog = new PromoCodeDialog();
        final Bundle bundle = new Bundle();
        bundle.putString("phone_number", phoneNumber);
        bundle.putStringArrayList("codes", codes);
        promoCodeDialog.setArguments(bundle);
        return promoCodeDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.invite_code_dialog, container, false);
        final TextView done = (TextView) rootView.findViewById(R.id.done);
        final EditText iCode = (EditText) rootView.findViewById(R.id.iCode);

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setCancelable(false);

        rootView.findViewById(R.id.exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        iCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(final Editable s) {

                final String text = s.toString().trim();
                if (!(TextUtils.isEmpty(text)) &&

                        (MiscUtils.isInviteCodeValid(s.toString().trim()))) {

                    iCode.setTextColor(getResources().getColor(R.color.reach_color));
                    done.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            setCancelable(true);
                            ///
                            dismiss();
                        }
                    });
                }
                else {

                    iCode.setTextColor(Color.BLACK);
                    done.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(getActivity(),"Please enter a valid code",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
        return rootView;
    }
}