package reach.project.onBoarding;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.lang.ref.WeakReference;

import reach.project.R;
import tourguide.tourguide.Overlay;
import tourguide.tourguide.TourGuide;

public class WelcomeFragment extends Fragment {

    private static WeakReference<WelcomeFragment> reference = null;
    private SplashInterface mListener;

    public static Fragment newInstance() {

        final WelcomeFragment fragment = new WelcomeFragment();
        reference = new WeakReference<>(fragment);
        return fragment;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_welcome, container, false);
        Button getStartedBtn = (Button) rootView.findViewById(R.id.getStartedBtn);
        TourGuide.init(getActivity()).with(TourGuide.Technique.Click)
                .setPointer(null)
                .setToolTip(null)
                .setOverlay(new Overlay().setStyle(Overlay.Style.Rectangle))
                .playOn(getStartedBtn);
        getStartedBtn.setOnClickListener(v -> mListener.onOpenNumberVerification());
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