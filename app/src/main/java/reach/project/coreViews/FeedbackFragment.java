package reach.project.coreViews;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;

import reach.backend.entities.feedBackApi.model.FeedBack;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.SharedPrefUtils;

public class FeedbackFragment extends Fragment {

    private static WeakReference<FeedbackFragment> reference = null;
    public static FeedbackFragment newInstance() {

        FeedbackFragment fragment;
        if(reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new FeedbackFragment());
        return fragment;
    }
    public FeedbackFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_feedback, container, false);
        final ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
        if(actionBar != null)
            actionBar.setTitle("feedback");
        final TextView fb = (TextView) rootView.findViewById(R.id.query);
        rootView.findViewById(R.id.send_feedback).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final FeedBack feedBack = new FeedBack();
                feedBack.setClientId(SharedPrefUtils.getServerId(getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS)));
                feedBack.setReply1("");
                feedBack.setReply2("");
                feedBack.setReply3(fb.getText().toString());
                StaticData.threadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            StaticData.feedBackApi.insert(feedBack).execute();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                getActivity().onBackPressed();
            }
        });
        return rootView;
    }

}
