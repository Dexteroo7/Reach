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
 * A placeholder fragment containing a simple view.
 */
public class YourProfileMusicFragment extends Fragment implements CacheInjectorCallbacks<Message>,
        MusicAdapter.CacheAdapterInterface<Message> {

    //default loading indicator
    private static final Song DEFAULT_LOADING_INDICATOR = new Song.Builder().build();

    private static WeakReference<YourProfileMusicFragment> reference = null;

    public static YourProfileMusicFragment newInstance(long userId) {

        final Bundle args;
        YourProfileMusicFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new YourProfileMusicFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing UserMusicLibrary object :)");
            args = fragment.getArguments();
        }
        args.putLong("userId", userId);
        return fragment;
    }

    private final List<Message> musicData = new ArrayList<>(100);
    private RecyclerView.Adapter mainAdapter = null;
    private Cache fullListCache = null;
    private Cache smartListCache = null;
    private long userId = 0;

    @Override
    public void onDestroyView() {

        mainAdapter = null;
        userId = 0;

        MiscUtils.closeQuietly(fullListCache, smartListCache);
        fullListCache = smartListCache = null;

        if (musicData != null)
            musicData.clear();

        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        userId = getArguments().getLong("userId", 0L);
        musicData.add(DEFAULT_LOADING_INDICATOR);

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
    public void injectElements(Collection<Message> elements, boolean overWrite, boolean removeLoading) {

        if (elements == null || elements.isEmpty())
            return;

        Log.i("Ayush", "Inserting " + elements.size() + " new items");

        synchronized (musicData) {

            if (overWrite) {

                musicData.clear(); //remove all
                musicData.addAll(elements); //add all
            } else {

                final int size = musicData.size();

                //no loading indicator present
                if (size == 0)
                    musicData.addAll(elements);

                //indicator must be present, removal requested
                else if (removeLoading) {

                    Log.i("Ayush", "Loading finished");
                    musicData.remove(size - 1);
                    musicData.addAll(elements);
                }

                //indicator must be present, insert before it
                else {

                    Log.i("Ayush", "Partial loading done");
                    musicData.addAll(size - 1, elements);
                }
            }
        }

        /**
         * If loading has finished request a full injection of smart lists
         * Else request partial injection
         */
        if (removeLoading || overWrite) {
            //full injection
        } else {
            //else partial injection
        }

        //notify
        notifyDataSetChanged();
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
