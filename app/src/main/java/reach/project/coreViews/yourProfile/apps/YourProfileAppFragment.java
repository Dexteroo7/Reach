//package reach.project.coreViews.yourProfile.apps;
//
//import android.os.Bundle;
//import android.support.annotation.Nullable;
//import android.support.v4.app.Fragment;
//import android.support.v7.widget.RecyclerView;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//
//import com.github.florent37.materialviewpager.MaterialViewPagerHelper;
//import com.github.florent37.materialviewpager.adapter.RecyclerViewMaterialAdapter;
//import com.squareup.wire.Message;
//import com.squareup.wire.Wire;
//
//import java.io.File;
//import java.io.IOException;
//import java.lang.ref.WeakReference;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.concurrent.Callable;
//
//import reach.project.R;
//import reach.project.apps.App;
//import reach.project.apps.AppList;
//import reach.project.utils.CloudStorageUtils;
//import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
//import reach.project.coreViews.yourProfile.blobCache.Cache;
//import reach.project.coreViews.yourProfile.blobCache.CacheInjectorCallbacks;
//import reach.project.coreViews.yourProfile.blobCache.CacheType;
//import reach.project.utils.viewHelpers.CacheAdapterInterface;
//import reach.project.coreViews.yourProfile.music.ParentAdapter;
//
///**
// * Created by dexter on 15/11/15.
// */
//public class YourProfileAppFragment extends Fragment implements CacheInjectorCallbacks<Message>,
//        CacheAdapterInterface<Message, App> {
//
//    private static WeakReference<YourProfileAppFragment> reference = null;
//    private static long userId = 0;
//
//    public static YourProfileAppFragment newInstance(long userId) {
//
//        final Bundle args;
//        YourProfileAppFragment fragment;
//        if (reference == null || (fragment = reference.get()) == null) {
//            reference = new WeakReference<>(fragment = new YourProfileAppFragment());
//            fragment.setArguments(args = new Bundle());
//        } else {
//            Log.i("Ayush", "Reusing YourProfileAppFragment object :)");
//            args = fragment.getArguments();
//        }
//        args.putLong("userId", userId);
//        return fragment;
//    }
//
//    private final List<Message> appData = new ArrayList<>(100);
//
//    private RecyclerViewMaterialAdapter materialAdapter = null;
//    private Cache fullListCache = null;
//    private Cache recentAppCache = null;
//    private View rootView = null;
//
//    @Nullable
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//
//        userId = getArguments().getLong("userId", 0L);
//
//        fullListCache = new Cache(this, CacheType.APPLICATIONS_FULL_LIST, userId) {
//            @Override
//            protected Callable<List<? extends Message>> fetchFromNetwork() {
//
//                //TODO
//                return () -> CloudStorageUtils.fetchSongs(userId, new WeakReference<>(getContext()));
//            }
//
//            @Override
//            protected Message getItem(byte[] source, int offset, int count) throws IOException {
//                return new Wire(App.class).parseFrom(source, offset, count, App.class);
//            }
//        };
//
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
//
//        rootView = inflater.inflate(R.layout.fragment_simple_recycler, container, false);
//        final RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
//        //mRecyclerView.setHasFixedSize(true);
//
//        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(getActivity()));
//        mRecyclerView.setAdapter(materialAdapter = new RecyclerViewMaterialAdapter(new ParentAdapter<>(this)));
//        MaterialViewPagerHelper.registerRecyclerView(getActivity(), mRecyclerView, null);
//
//        return rootView;
//    }
//
//    @Override
//    public int getItemCount() {
//
//        final int size = appData.size();
//        if (size == 0)
//            recentAppCache.loadMoreElements(true);
//        return size;
//    }
//
//    @Override
//    public Message getItem(int position) {
//        return null;
//    }
//
//    @Override
//    public void handOverMessage(App item) {
//
//        /**
//         * OPEN PLAY STORE HERE
//         */
//    }
//
//    @Override
//    public File getCacheDirectory() {
//        return getContext().getExternalCacheDir();
//    }
//
//    @Override
//    public long getItemId(Message item) {
//
//        final int id;
//
//        if (item instanceof App)
//            id = ((App) item).packageName.hashCode();
//
//        else if (item instanceof AppList) {
//
//            final AppList appList = (AppList) item;
//            final long[] songIds = new long[appList.app.size()];
//
//            int index;
//            for (index = 0; index < appList.app.size(); index++)
//                songIds[index] = appList.app.get(index).packageName.hashCode();
//
//            return Arrays.hashCode(songIds);
//        } else
//            id = 0;
//
//        return id;
//    }
//
//    @Override
//    public void injectElements(List<Message> elements, boolean overWrite, boolean removeLoading) {
//
//    }
//
//    //TODO
//    private static final Callable<List<? extends Message>> getRecent = new Callable<List<? extends Message>>() {
//        @Override
//        public List<? extends Message> call() throws Exception {
//            return null;
//        }
//    };
//}