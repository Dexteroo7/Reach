package reach.project.explore;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import reach.project.R;
import reach.project.music.MySongsHelper;
import reach.project.music.MySongsProvider;
import reach.project.utils.MiscUtils;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ExploreFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ExploreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ExploreFragment extends Fragment implements ExploreAdapter.Explore,
        ExploreBuffer.Exploration<ExploreContainer> {

    private final ExploreBuffer<ExploreContainer> buffer = ExploreBuffer.getInstance(this);

    private OnFragmentInteractionListener mListener;
    private ExploreAdapter exploreAdapter;

    private static WeakReference<ExploreFragment> reference;

    public static ExploreFragment newInstance() {

        ExploreFragment fragment;
        if (reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new ExploreFragment());

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_explore, container, false);

        final ViewPager explorePager = (ViewPager) rootView.findViewById(R.id.explorer);
        exploreAdapter = new ExploreAdapter(getActivity(), this);

        explorePager.setAdapter(exploreAdapter);
        explorePager.setOffscreenPageLimit(3);
        explorePager.setPageMargin(20);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        buffer.close();
        exploreAdapter = null;
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
    public synchronized Callable<Collection<ExploreContainer>> fetchNextBatch() {
//        Toast.makeText(getContext(), "Server Fetching next batch of 10", Toast.LENGTH_SHORT).show();
        return fetchNextBatch;
    }

    private static int counter = 0;
    private static final Callable<Collection<ExploreContainer>> fetchNextBatch = () -> {

        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }

        final Cursor cursor = MiscUtils.useContextFromFragment(reference, context -> {
            return context.getContentResolver().query(MySongsProvider.CONTENT_URI,
                    new String[]{MySongsHelper.COLUMN_DISPLAY_NAME},
                    null, null, null);
        }).orNull();

        if (cursor == null)
            return null;

        final List<ExploreContainer> containers = new ArrayList<>(10);
        while (cursor.moveToNext())
            containers.add(new ExploreContainer(cursor.getString(0), ExploreTypes.MUSIC));

        counter += containers.size();
        if (counter > 20) {

//            MiscUtils.runOnUiThreadFragment(reference, context -> {
//                Toast.makeText(context, "Server sending done for today", Toast.LENGTH_SHORT).show();
//            });

            containers.clear();
            containers.add(new ExploreContainer(
                    ExploreTypes.DONE_FOR_TODAY.getTitle(),
                    ExploreTypes.DONE_FOR_TODAY));
        }

        return containers;
    };

    @Override
    public ExploreContainer getContainerForIndex(int index) {

        //return data
        return buffer.getViewItem(index);
    }

    @Override
    public boolean isDoneForDay(ExploreContainer container) {
        return container.getTypes().equals(ExploreTypes.DONE_FOR_TODAY);
    }

    @Override
    public boolean isLoading(ExploreContainer container) {
        return container.getTypes().equals(ExploreTypes.LOADING);
    }

    @Override
    public ExploreContainer getLoadingResponse() {
        return new ExploreContainer(
                ExploreTypes.LOADING.getTitle(),
                ExploreTypes.LOADING);
    }

    @Override
    public int getCount() {
        return buffer.currentBufferSize();
    }

    @Override
    public synchronized void notifyDataAvailable() {

        //This is UI thread !
        exploreAdapter.notifyDataSetChanged();
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

}
