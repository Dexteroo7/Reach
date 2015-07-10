package reach.project.onBoarding;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.viewpagerindicator.CirclePageIndicator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import reach.backend.entities.userApi.model.OldUserContainer;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.SuperInterface;

public class NumberVerification extends Fragment {

    private final String[] tourTexts = new String[]{
            "Browse and Access files\nof your Network",
            "Easy file transfer logs\nthrough Reach Queue",
            "Privacy and security\noptions",
            "Synced media files\n"};
    private final int[] tourImages = new int[]{
            R.drawable.library_view,
            R.drawable.reach_queue,
            R.drawable.my_reach,
            R.drawable.hide
    };

    private SuperInterface mListener;
    private View bottomPart1, bottomPart2;
    private EditText telephoneNumber;

    public static NumberVerification newInstance() {
        return new NumberVerification();
    }

    private final ArrayList<String> codeList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_number_verification, container, false);
        final ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.logo);
        final CirclePageIndicator circlePageIndicator = (CirclePageIndicator) rootView.findViewById(R.id.circles);

        telephoneNumber = (EditText) rootView.findViewById(R.id.telephoneNumber);
        bottomPart1 = rootView.findViewById(R.id.bottomPart1);
        bottomPart2 = rootView.findViewById(R.id.bottomPart2);
        viewPager.setAdapter(new TourPagerAdapter(getActivity()));
        circlePageIndicator.setFillColor(getResources().getColor(R.color.reach_color));
        circlePageIndicator.setPageColor(getResources().getColor(R.color.white));
        circlePageIndicator.setRadius(10f);
        circlePageIndicator.setStrokeColor(0);
        circlePageIndicator.setViewPager(viewPager);
        telephoneNumber.requestFocus();

        rootView.findViewById(R.id.verify).setOnClickListener(listener);

        return rootView;
    }

    private final View.OnClickListener listener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {

            final String phoneNumber = telephoneNumber.getText().toString();
            if (TextUtils.isEmpty(phoneNumber) || phoneNumber.length() < 10) {
                Toast.makeText(getActivity(), "Enter Valid Number", Toast.LENGTH_SHORT).show();
                return;
            }
            new GetOldAccount(StaticData.threadPool.submit(getCodes)).executeOnExecutor(StaticData.threadPool, phoneNumber.replaceAll("[^0-9]", ""));
            bottomPart1.setVisibility(View.INVISIBLE);
            bottomPart2.setVisibility(View.VISIBLE);
        }

        private final Callable<List<String>> getCodes = new Callable<List<String>>() {
            @Override
            public List<String> call() throws IOException {

                final URL url = new URL("https://www.dropbox.com/s/fhfvodxoce2qum9/codes.txt?dl=1");
                final BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));
                final ImmutableList.Builder<String> builder = ImmutableList.builder();
                String line;
                while (!TextUtils.isEmpty(line = r.readLine()))
                    builder.add(line);
                r.close();
                return builder.build();
            }
        };

        final class GetOldAccount extends AsyncTask<String, Void, Pair<OldUserContainer, String>> {

            private final Future<List<String>> codes;
            private GetOldAccount(Future<List<String>> codes) {
                this.codes = codes;
            }

            private void proceed(Pair<OldUserContainer, String> pair) {

                final SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
                SharedPrefUtils.purgeReachUser(sharedPreferences);
                SharedPrefUtils.storePhoneNumber(sharedPreferences, pair.second);

                if (pair.first != null) {
                    SharedPrefUtils.storeOldFirstName(sharedPreferences, pair.first.getFirstName());
                    SharedPrefUtils.storeOldLastName(sharedPreferences, pair.first.getLastName());
                    SharedPrefUtils.storeOldImageId(sharedPreferences, pair.first.getImageId());
                }

                mListener.startAccountCreation();
            }

            @Override
            protected final Pair<OldUserContainer, String> doInBackground(final String... params) {

                final OldUserContainer container = MiscUtils.autoRetry(new DoWork<OldUserContainer>() {
                    @Override
                    protected OldUserContainer doWork() throws IOException {
                        return StaticData.userEndpoint.isAccountPresent(params[0]).execute();
                    }
                }, Optional.<Predicate<OldUserContainer>>absent()).orNull();

                if (container == null)
                    try {
                        codeList.addAll(codes.get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                return new Pair<>(container, params[0]);
            }

            @Override
            protected void onPostExecute(final Pair<OldUserContainer, String> pair) {

                super.onPostExecute(pair);
                final FragmentActivity activity = getActivity();
                if (isRemoving() || isDetached() || isCancelled() || activity == null || activity.isFinishing())
                    return;

                if (pair.first != null) {
                    proceed(pair);
                    return;
                }

                try {

                    final SharedPreferences editor = getActivity().getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
                    SharedPrefUtils.purgeReachUser(editor);
                    SharedPrefUtils.storePhoneNumber(editor, pair.second);
                    mListener.startAccountCreation();

                } catch (Exception ignored) {
                    proceed(pair);
                }
            }
        }
    };

    private class TourPagerAdapter extends PagerAdapter {

        private final Context mContext;
        private final LayoutInflater mLayoutInflater;

        public TourPagerAdapter(Context context) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return tourTexts.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            final View itemView = mLayoutInflater.inflate(R.layout.tour_item, container, false);
            ((TextView) itemView.findViewById(R.id.tour_text)).setText(tourTexts[position]);
            ((ImageView) itemView.findViewById(R.id.tour_image)).setImageResource(tourImages[position]);
            //Picasso.with(container.getContext()).load(tourImages[position]).noFade().into((ImageView) itemView.findViewById(R.id.tour_image));
            container.addView(itemView);
            return itemView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (SuperInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
