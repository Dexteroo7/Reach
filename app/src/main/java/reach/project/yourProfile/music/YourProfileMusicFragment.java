package reach.project.yourProfile.music;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.florent37.materialviewpager.MaterialViewPagerHelper;
import com.squareup.wire.Message;
import com.squareup.wire.Wire;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import reach.project.R;
import reach.project.music.Song;
import reach.project.utils.CloudStorageUtils;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.yourProfile.blobCache.Cache;
import reach.project.yourProfile.blobCache.CacheInjectorCallbacks;
import reach.project.yourProfile.blobCache.CacheType;

/**
 * Full list loads from cache, and checks network for update, if update is found, whole data is reloaded.
 * Recent depends on full list. Loads from cache and follows full list update, just that 0 index is reloaded only.
 * Smart list, loads same as full list, independent.
 * <p>
 * A placeholder fragment containing a simple view.
 */
public class YourProfileMusicFragment extends Fragment implements CacheInjectorCallbacks<Message>,
        MusicAdapter.CacheAdapterInterface<Message> {

    private static WeakReference<YourProfileMusicFragment> reference = null;

    public static YourProfileMusicFragment newInstance(long userId) {

        final Bundle args;
        YourProfileMusicFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new YourProfileMusicFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing YourProfileMusicFragment object :)");
            args = fragment.getArguments();
        }
        args.putLong("userId", userId);
        return fragment;
    }

    private final List<Message> musicData = new ArrayList<>(100);
    private RecyclerView.Adapter mainAdapter = null;
    private Cache fullListCache = null;
    private Cache smartListCache = null;
    private Cache recentMusicCache = null;

    private long userId = 0;

    @Override
    public void onDestroyView() {

        mainAdapter = null;
        userId = 0;

        MiscUtils.closeQuietly(fullListCache, smartListCache, recentMusicCache);
        fullListCache = smartListCache = recentMusicCache = null;

        if (musicData != null)
            musicData.clear();

        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        userId = getArguments().getLong("userId", 0L);

        fullListCache = new Cache(this, CacheType.MUSIC_FULL_LIST, userId) {
            @Override
            protected Callable<Collection<? extends Message>> fetchFromNetwork() {

                return () -> CloudStorageUtils.fetchSongs(userId, new WeakReference<>(getContext()));
            }

            @Override
            protected Message getItem(byte[] source, int offset, int count) throws IOException {
                return new Wire(Song.class).parseFrom(source, offset, count, Song.class);
            }
        };

        smartListCache = new Cache(this, CacheType.MUSIC_SMART_LIST, userId) {
            @Override
            protected Callable<Collection<? extends Message>> fetchFromNetwork() {
                return null;
            }

            @Override
            protected Message getItem(byte[] source, int offset, int count) {
                return null;
            }
        };

        recentMusicCache = new Cache(this, CacheType.MUSIC_RECENT_LIST, userId) {
            @Override
            protected Callable<Collection<? extends Message>> fetchFromNetwork() {

                /**
                 * It is safe to assume that at this point, the music list is fully loaded.
                 * Hence we simple sort the list and return top 20 elements.
                 */

                return null;
            }

            @Override
            protected Message getItem(byte[] source, int offset, int count) throws IOException {
                return null;
            }
        };

        final View rootView = inflater.inflate(R.layout.fragment_yourprofile_page, container, false);
        final RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        //mRecyclerView.setHasFixedSize(true);

        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mainAdapter = new MusicAdapter<>(this));
        MaterialViewPagerHelper.registerRecyclerView(getActivity(), mRecyclerView, null);
        return rootView;
    }

    /////////////////////////

    @Override
    public File getCacheDirectory() {
        return getContext().getExternalCacheDir();
    }

    @Override
    public int getCount() {
        return musicData.size();
    }

    @Override
    public long getItemId(Message item) {

        final Long id;
        if (item instanceof Song)
            id = ((Song) item).songId;
        else
            id = 0L;

        if (id == null)
            return 0L;

        return id;
    }

    @Override
    public Message getItem(int position) {

        final int currentSize = musicData.size();

        //if reaching end of story and are not done yet
        if (position > currentSize - 3)
            //request a partial load
            fullListCache.loadMoreElements(false);

        return musicData.get(position);
    }

    @Override
    public void injectElements(Collection<Message> elements, boolean overWrite, boolean loadingDone) {

        if (elements == null || elements.isEmpty())
            return;

        Log.i("Ayush", "Inserting " + elements.size() + " new items");
        final Message typeCheckerInstance = elements.iterator().next();
        final Class typeChecker;
        if (typeCheckerInstance instanceof Song)
            typeChecker = Song.class;
        else
            typeChecker = Song.class; //TODO handle other cases

        ///////////

        if (overWrite)
            intelligentOverwrite(elements, typeChecker);
        else
            synchronized (musicData) {
                musicData.addAll(elements);
            }

        //notify
        notifyDataSetChanged();

        if (typeChecker != Song.class)
            return;

        /**
         * If loading has finished request a full injection of smart lists
         * Else request partial injection
         */
//        smartListCache.loadMoreElements(loadingDone || overWrite);

        /**
         * Fresh batch has been fetched from network.
         * Invalidate recent cache and recreate
         */
        if (overWrite) {
            recentMusicCache.invalidateCache();
            recentMusicCache.loadMoreElements(true);
        }
    }

    private void intelligentOverwrite(Collection<? extends Message> elements, Class typeChecker) {

        final Iterator<? extends Message> messageIterator = elements.iterator();

        int index;

        synchronized (musicData) {

            int updatedSize = musicData.size();
            for (index = 0; index < updatedSize; index++) {

                //ignore if element is not of same class type
                if (!musicData.get(index).getClass().equals(typeChecker))
                    continue;

                //get the next message to overwrite if present
                if (messageIterator.hasNext()) {

                    //we have a message to overwrite, do it
                    musicData.set(index, messageIterator.next());
                    messageIterator.remove(); //must remove
                } else {
                    musicData.remove(index); //remove as this item is no longer valid
                    updatedSize--;
                }
            }

            //insert any remaining elements
            musicData.addAll(elements);
        }
    }

    private void notifyDataSetChanged() {

        if (mainAdapter != null)
            MiscUtils.runOnUiThreadFragment(reference, context -> {

                if (mainAdapter != null) {
                    Log.i("Ayush", "Reloading list " + musicData.size());
                    mainAdapter.notifyDataSetChanged();
                }
            });
    }
}
