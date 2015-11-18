package reach.project.yourProfile.music;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.florent37.materialviewpager.MaterialViewPagerHelper;
import com.github.florent37.materialviewpager.adapter.RecyclerViewMaterialAdapter;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.wire.Message;
import com.squareup.wire.Wire;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import reach.project.R;
import reach.project.core.ReachApplication;
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
    private static long userId = 0;

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
    private RecyclerViewMaterialAdapter materialAdapter = null;
    //private RecyclerView.Adapter mainAdapter = null;
    private Cache fullListCache = null;
    private Cache smartListCache = null;
    private Cache recentMusicCache = null;

    private int lastPosition = 0;

    @Override
    public void onDestroyView() {

        materialAdapter = null;
        userId = 0;

        MiscUtils.closeQuietly(fullListCache, smartListCache, recentMusicCache);
        fullListCache = smartListCache = recentMusicCache = null;

        musicData.clear();

        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        userId = getArguments().getLong("userId", 0L);
        //musicData.add(new Song.Builder().build());

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
                return getSmartList;
            }

            @Override
            protected Message getItem(byte[] source, int offset, int count) throws IOException {
                return new Wire(SmartSong.class).parseFrom(source, offset, count, SmartSong.class);
            }
        };

        recentMusicCache = new Cache(this, CacheType.MUSIC_RECENT_LIST, userId) {
            @Override
            protected Callable<Collection<? extends Message>> fetchFromNetwork() {
                return getRecent;
            }

            @Override
            protected Message getItem(byte[] source, int offset, int count) throws IOException {
                return new Wire(RecentSong.class).parseFrom(source, offset, count, RecentSong.class);
            }
        };

        final View rootView = inflater.inflate(R.layout.fragment_yourprofile_page, container, false);
        final RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        //mRecyclerView.setHasFixedSize(true);

        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(materialAdapter = new RecyclerViewMaterialAdapter(new MusicAdapter<>(this)));
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

        final int size = musicData.size();
        if (size == 0)
            recentMusicCache.loadMoreElements(true);

        return size;
    }

    @Override
    public long getItemId(Message item) {

        final Long id;

        if (item instanceof Song)
            id = ((Song) item).songId;

        else if (item instanceof RecentSong) {

            final RecentSong recentSong = (RecentSong) item;
            final long[] songIds = new long[recentSong.songList.size()];

            int index;
            for (index = 0; index < recentSong.songList.size(); index++) {
                final Long songId = recentSong.songList.get(index).songId;
                songIds[index] = songId == null ? 0 : songId;
            }

            return Arrays.hashCode(songIds);
        } else if (item instanceof SmartSong) {

            final SmartSong recentSong = (SmartSong) item;
            final long[] songIds = new long[recentSong.songList.size()];

            int index;
            for (index = 0; index < recentSong.songList.size(); index++) {
                final Long songId = recentSong.songList.get(index).songId;
                songIds[index] = songId == null ? 0 : songId;
            }

            return Arrays.hashCode(songIds);
        } else
            id = null;

        return id == null ? 0 : id;
    }

    @Override
    public Message getItem(int position) {

        if (position > lastPosition)
            lastPosition = position;

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

        final Message typeCheckerInstance = elements.iterator().next();
        final Class typeChecker;
        if (typeCheckerInstance instanceof Song)
            typeChecker = Song.class;
        else if (typeCheckerInstance instanceof SmartSong)
            typeChecker = SmartSong.class;
        else if (typeCheckerInstance instanceof RecentSong)
            typeChecker = RecentSong.class;
        else
            return;

        Log.i("Ayush", "Inserting " + elements.size() + " new items " + typeChecker.getName());

        ///////////

        if (overWrite)
            intelligentOverwrite(elements, typeChecker);
        if (!elements.isEmpty())
            painter(elements, typeChecker);

        //notify
        if (materialAdapter != null) {
            Log.i("Ayush", "Reloading list " + musicData.size());
            materialAdapter.notifyDataSetChanged();
        }

        /**
         * If loading has finished request a full injection of smart lists
         * Else request partial injection
         */
        if (typeChecker == Song.class)
            smartListCache.loadMoreElements(true);
//            smartListCache.loadMoreElements(loadingDone || overWrite);
    }

    private void intelligentOverwrite(Collection<? extends Message> elements, Class typeChecker) {

        //nothing to overwrite
        if (musicData.isEmpty())
            return;

        final Iterator<? extends Message> messageIterator = elements.iterator();
        int updatedSize = musicData.size();
        int index;

        synchronized (musicData) {

            for (index = 0; index < updatedSize; index++) {

                //ignore if element is not of same class type
                if (!musicData.get(index).getClass().equals(typeChecker))
                    continue;

                //get the next message to overwrite if present
                if (messageIterator.hasNext()) {

                    //we have a message to overwrite, do it
                    musicData.set(index, messageIterator.next());
                    try {
                        messageIterator.remove(); //must remove
                    } catch (UnsupportedOperationException ignored) {
                    }
                } else {
                    musicData.remove(index);  //remove as this item is no longer valid
                    updatedSize--;
                }
            }
        }
    }

    private void painter(Collection<? extends Message> elements, Class typeChecker) {

        if (musicData.isEmpty())
            synchronized (musicData) {
                musicData.addAll(elements);
            }

        if (typeChecker == RecentSong.class) {

            synchronized (musicData) {
                musicData.addAll(0, elements);
            }

        } else if (typeChecker == SmartSong.class) {

            final int size = musicData.size();
            if (lastPosition > size)
                lastPosition = size;
            synchronized (musicData) {
                musicData.addAll(lastPosition, elements);
            }

        } else if (typeChecker == Song.class) {

            synchronized (musicData) {
                musicData.addAll(elements);
            }
        }
    }

    private static final Callable<Collection<? extends Message>> getSmartList = () -> {

        final Request request = new Request.Builder()
                .url("http://52.74.175.56:8080/explore/getRecentlyPlayed?userId=" + userId)
                .build();

        Log.i("Ayush", "Fetching smart lists " + request.urlString());

        final Response response = ReachApplication.okHttpClient.newCall(request).execute();
        final JSONArray receivedData = new JSONArray(response.body().string());
        final List<Song> songs = new ArrayList<>();

        JSONObject smartData;
        for (int index = 0; index < receivedData.length(); index++) {

            smartData = receivedData.getJSONObject(index);

            final Song.Builder songBuilder = new Song.Builder();
            songBuilder.songId(smartData.getLong("songId"));
            songBuilder.size(smartData.getLong("size"));
            songBuilder.dateAdded(smartData.getLong("timeAdded"));
            songBuilder.duration(smartData.getLong("duration"));

            songBuilder.actualName(smartData.getString("actualName"));
            songBuilder.displayName(smartData.getString("displayName"));
            songBuilder.artist(smartData.getString("artistName"));

            //userName fetch else where
            try {
                songBuilder.album(smartData.getString("albumName"));
                songBuilder.genre(smartData.getString("genre"));
            } catch (JSONException | NullPointerException ignored) {
            }

            songBuilder.isLiked(smartData.getBoolean("isLiked"));

            songBuilder.visibility(true);
            songs.add(songBuilder.build());
            Log.i("Ayush", "Found smart list " + songBuilder.displayName);
        }

        if (songs.isEmpty())
            return Collections.emptyList();

        return Collections.singletonList(new SmartSong.Builder().songList(songs).title("Recently Played").build());
    };

    private static final Callable<Collection<? extends Message>> getRecent = () -> {

        final Fragment fragment = reference.get();
        final List<Song> musicData = CloudStorageUtils.fetchSongs(userId, new WeakReference<>(fragment.getContext()));

        if (musicData.isEmpty())
            return Collections.emptyList();

        //get only songs
        final List<Song> temp = new ArrayList<>(musicData.size());
        for (Message message : musicData)
            if (message instanceof Song)
                temp.add((Song) message);

        if (temp.isEmpty())
            return Collections.emptyList();

        //sort
        Collections.sort(temp, (lhs, rhs) -> {

            final Long a = lhs.dateAdded == null ? 0 : lhs.dateAdded;
            final Long b = rhs.dateAdded == null ? 0 : rhs.dateAdded;
            return a.compareTo(b);
        });

        final RecentSong.Builder builder = new RecentSong.Builder();
        builder.title("Recent Songs");
        if (temp.size() < 20)
            builder.songList(temp);
        else
            builder.songList(temp.subList(0, 20));

        return Collections.singletonList(builder.build());
    };
}
