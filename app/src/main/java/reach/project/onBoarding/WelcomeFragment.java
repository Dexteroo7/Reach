package reach.project.onBoarding;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.ancillaryViews.TermsActivity;

public class WelcomeFragment extends Fragment {

    private SplashInterface mListener;

    private static WeakReference<WelcomeFragment> reference;
    public static Fragment newInstance() {

        WelcomeFragment welcomeFragment;
        reference = new WeakReference<>(welcomeFragment = new WelcomeFragment());

        return welcomeFragment;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_welcome, container, false);
        rootView.findViewById(R.id.getStartedBtn).setOnClickListener(v ->
                mListener.onOpenNumberVerification());
        rootView.findViewById(R.id.agreeTerms).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), TermsActivity.class)));
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
}