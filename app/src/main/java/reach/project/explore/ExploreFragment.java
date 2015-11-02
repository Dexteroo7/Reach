package reach.project.explore;

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
import java.util.Random;
import java.util.concurrent.Callable;

import reach.project.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ExploreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ExploreFragment extends Fragment implements ExploreAdapter.Explore,
        ExploreBuffer.Exploration<ExploreContainer> {

    private final ExploreBuffer<ExploreContainer> buffer = ExploreBuffer.getInstance(this);

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

    @Override
    public synchronized Callable<Collection<ExploreContainer>> fetchNextBatch() {
//        Toast.makeText(getContext(), "Server Fetching next batch of 10", Toast.LENGTH_SHORT).show();
        return fetchNextBatch;
    }

    private static int counter = 0;
    private static final Callable<Collection<ExploreContainer>> fetchNextBatch = () -> {

        try {
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }

        /*final Cursor cursor = MiscUtils.useContextFromFragment(reference, context -> {
            return context.getContentResolver().query(ReachSongProvider.CONTENT_URI,
                    new String[]{ReachSongHelper.COLUMN_DISPLAY_NAME},
                    null, null, null);
        }).orNull();

        if (cursor == null)
            return null;*/

        final List<ExploreContainer> containers = new ArrayList<>(10);
        for (int i=0; i<20; i++)
            containers.add(new ExploreContainer("Page " + i, "Subtitle", "imageId", "userImageId",
                    "handle", 3.0f, ExploreTypes.MUSIC, new Random().nextLong()));
        /*while (cursor.moveToNext())
            containers.add(new ExploreContainer(cursor.getString(0), ExploreTypes.MUSIC));*/

        counter += containers.size();
        if (counter > 50) {

//            MiscUtils.runOnUiThreadFragment(reference, context -> {
//                Toast.makeText(context, "Server sending done for today", Toast.LENGTH_SHORT).show();
//            });

            containers.clear();
            containers.add(new ExploreContainer(ExploreTypes.DONE_FOR_TODAY, new Random().nextLong()));
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
        return new ExploreContainer(ExploreTypes.LOADING, new Random().nextLong());
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

}
