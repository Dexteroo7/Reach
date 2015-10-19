package reach.project.explore;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Optional;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import reach.project.R;
import reach.project.explore.internal.InfinitePager;
import reach.project.music.songs.ReachSongHelper;
import reach.project.music.songs.ReachSongProvider;
import reach.project.utils.MiscUtils;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ExploreFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ExploreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ExploreFragment extends Fragment implements InfinitePager.OnInfinitePageChangeListener<Integer>,
        ExploreAdapter.Explorer,
        ExploreBuffer.Exploration<ExploreContainer> {

    private final ExploreBuffer<ExploreContainer> buffer = ExploreBuffer.getInstance(20, this);

    private OnFragmentInteractionListener mListener;
    private InfinitePager<Integer> explorePager;
    private ExploreAdapter exploreAdapter;

    private static WeakReference<ExploreFragment> reference;

    public static ExploreFragment newInstance() {

        ExploreFragment fragment;
        if (reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new ExploreFragment());

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_explore, container, false);

        explorePager = (InfinitePager) rootView.findViewById(R.id.explorer);
        exploreAdapter = new ExploreAdapter(0, getActivity(), this);

        explorePager.setAdapter(exploreAdapter);
        explorePager.setPageMargin(20);
        explorePager.setOnInfinitePageChangeListener(this);

        return rootView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public ExploreContainer getContainer(int indicator) {

        //return data
        final Optional<ExploreContainer> containerOptional = buffer.getViewItem(indicator);
        if (containerOptional.isPresent())
            return containerOptional.get(); //data found

        return new ExploreContainer(
                ExploreTypes.DONE_FOR_TODAY.getTitle(),
                ExploreTypes.DONE_FOR_TODAY);
    }

    @Override
    public void notifyDataAvailable() {

        //This is UI thread !
        exploreAdapter.notifyDataSetChanged();
        explorePager.destroyDrawingCache();

        Log.i("Ayush", "DATA AVAILABLE !!!!");
    }

    @Override
    public Callable<Collection<ExploreContainer>> fetchNextBatch() {

        return () -> MiscUtils.autoRetry(() -> {

            final Cursor cursor = getActivity().getContentResolver().query(ReachSongProvider.CONTENT_URI,
                    new String[]{ReachSongHelper.COLUMN_DISPLAY_NAME},
                    null, null, null);

            if (cursor == null)
                return null;

            final List<ExploreContainer> containers = new ArrayList<>(10);
            int count = 0;
            while (cursor.moveToNext() && count++ < 10)
                containers.add(new ExploreContainer(cursor.getString(0), ExploreTypes.MUSIC));

            return containers;
        }, Optional.absent()).orNull();
    }

    @Override
    public void onPageScrolled(Integer indicator, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(Integer indicator) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

}
