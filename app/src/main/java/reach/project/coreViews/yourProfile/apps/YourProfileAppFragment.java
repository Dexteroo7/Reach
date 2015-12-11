package reach.project.coreViews.yourProfile.apps;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.florent37.materialviewpager.MaterialViewPagerHelper;
import com.squareup.wire.Message;
import com.squareup.wire.Wire;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import reach.project.R;
import reach.project.apps.App;
import reach.project.apps.AppList;
import reach.project.coreViews.yourProfile.blobCache.Cache;
import reach.project.coreViews.yourProfile.blobCache.CacheAdapterInterface;
import reach.project.coreViews.yourProfile.blobCache.CacheInjectorCallbacks;
import reach.project.coreViews.yourProfile.blobCache.CacheType;
import reach.project.utils.CloudStorageUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;

/**
 * Created by dexter on 15/11/15.
 */
public class YourProfileAppFragment extends Fragment implements CacheInjectorCallbacks<Message>, CacheAdapterInterface<Message, App> {

    private static WeakReference<YourProfileAppFragment> reference = null;
    private static long userId = 0;

    public static YourProfileAppFragment newInstance(long userId) {

        final Bundle args;
        YourProfileAppFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new YourProfileAppFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing YourProfileAppFragment object :)");
            args = fragment.getArguments();
        }
        args.putLong("userId", userId);
        return fragment;
    }

    private final List<Message> appData = new ArrayList<>(100);
    private final ParentAdapter parentAdapter = new ParentAdapter<>(this);

    private Cache fullListCache = null;
//    private Cache smartListCache = null;
//    private Cache recentAppCache = null;

    private int lastPosition = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        userId = getArguments().getLong("userId", 0L);

        fullListCache = new Cache(this, CacheType.APPLICATIONS_FULL_LIST, userId) {
            @Override
            protected Callable<List<? extends Message>> fetchFromNetwork() {

                return () -> CloudStorageUtils.fetchApps(userId, new WeakReference<>(getContext()));
            }

            @Override
            protected Message getItem(byte[] source, int offset, int count) throws IOException {
                return new Wire(App.class).parseFrom(source, offset, count, App.class);
            }
        };

//        recentAppCache = new Cache(this, CacheType.APPLICATIONS_RECENT_LIST, userId) {
//            @Override
//            protected Callable<List<? extends Message>> fetchFromNetwork() {
//                return getRecent;
//            }
//
//            @Override
//            protected Message getItem(byte[] source, int offset, int count) throws IOException {
//                return new Wire(AppList.class).parseFrom(source, offset, count, AppList.class);
//            }
//        };

        final View rootView = inflater.inflate(R.layout.fragment_simple_recycler, container, false);
        final RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        //mRecyclerView.setHasFixedSize(true);

        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(parentAdapter);
        MaterialViewPagerHelper.registerRecyclerView(getActivity(), mRecyclerView, null);

        return rootView;
    }

    @Override
    public int getItemCount() {

        final int size = appData.size();
        if (size == 0) {
//            recentAppCache.loadMoreElements(true);
            fullListCache.loadMoreElements(false);
        }

        return size;
    }

    @Override
    public Message getItem(int position) {

        if (position > lastPosition)
            lastPosition = position;

        final int currentSize = appData.size();

        //if reaching end of story and are not done yet
        if (position > currentSize - 5)
            //request a partial load
            fullListCache.loadMoreElements(false);

        return appData.get(position);
    }

    @Override
    public void handOverMessage(@NonNull App item) {

        //TODO
        Toast.makeText(getContext(), item.applicationName, Toast.LENGTH_SHORT).show();
        /**
         * OPEN PLAY STORE HERE
         */
    }

    @Override
    public File getCacheDirectory() {
        return getContext().getExternalCacheDir();
    }

    @Override
    public boolean verifyItemVisibility(Message item) {
        return true;
    }

    @Override
    public void injectElements(List<Message> elements, boolean overWrite, boolean loadingDone) {

        if (elements == null || elements.isEmpty())
            return;

        final Message typeCheckerInstance = elements.get(0);
        final Class typeChecker;
        if (typeCheckerInstance instanceof App)
            typeChecker = App.class;
        else if (typeCheckerInstance instanceof AppList)
            typeChecker = AppList.class;
        else
            return;

        Log.i("Ayush", "Inserting " + elements.size() + " new items " + typeChecker.getName());

        ///////////

//        if (overWrite)
//            intelligentOverwrite(elements, typeChecker);
//        if (!elements.isEmpty())
//            painter(elements, typeChecker);
        appData.addAll(elements);

        //notify
        Log.i("Ayush", "Reloading list " + appData.size());
        parentAdapter.notifyDataSetChanged();

        /**
         * If loading has finished request a full injection of smart lists
         * Else request partial injection
         */
//        if (typeChecker == App.class)
//            smartListCache.loadMoreElements(true);
    }

//    private void intelligentOverwrite(List<? extends Message> elements, Class typeChecker) {
//
//        //nothing to overwrite
//        if (appData.isEmpty())
//            return;
//
//        final Iterator<? extends Message> messageIterator = elements.iterator();
//        int updatedSize = appData.size();
//        int index;
//
//        synchronized (appData) {
//
//            for (index = 0; index < updatedSize; index++) {
//
//                final Message item = appData.get(index);
//                if (item instanceof AppList) {
//
//                    final AppList
//                }
//
//                //ignore if element is not of same class type
//                if (!musicData.get(index).getClass().equals(typeChecker))
//                    continue;
//
//                //get the next message to overwrite if present
//                if (messageIterator.hasNext()) {
//
//                    //we have a message to overwrite, do it
//                    musicData.set(index, messageIterator.next());
//                    messageIterator.remove(); //must remove
//
//                } else {
//                    musicData.remove(index);  //remove as this item is no longer valid
//                    updatedSize--;
//                }
//            }
//        }
//    }
//
//    private void painter(List<? extends Message> elements, Class typeChecker) {
//
//        if (musicData.isEmpty())
//            synchronized (musicData) {
//                musicData.addAll(elements);
//            }
//
//        else if (typeChecker == RecentSong.class)
//
//            synchronized (musicData) {
//                musicData.addAll(0, elements);
//            }
//
//        else if (typeChecker == SmartSong.class) {
//
//            final int size = musicData.size();
//            if (lastPosition > size)
//                lastPosition = size;
//            synchronized (musicData) {
//                musicData.addAll(lastPosition, elements);
//            }
//
//        } else if (typeChecker == Song.class)
//
//            synchronized (musicData) {
//                musicData.addAll(elements);
//            }
//    }

    //TODO
    private static final Callable<List<? extends Message>> getRecent = Collections::emptyList;
}