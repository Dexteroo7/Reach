package reach.project.coreViews.yourProfile.music;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.florent37.materialviewpager.MaterialViewPagerHelper;
import com.github.florent37.materialviewpager.adapter.RecyclerViewMaterialAdapter;
import com.google.common.base.Optional;
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
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import reach.backend.entities.userApi.model.SimpleSong;
import reach.project.R;
import reach.project.core.ReachApplication;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.coreViews.yourProfile.blobCache.Cache;
import reach.project.coreViews.yourProfile.blobCache.CacheAdapterInterface;
import reach.project.coreViews.yourProfile.blobCache.CacheInjectorCallbacks;
import reach.project.coreViews.yourProfile.blobCache.CacheType;
import reach.project.music.Song;
import reach.project.utils.CloudStorageUtils;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Full list loads from cache, and checks network for update, if update is found, whole data is reloaded.
 * Recent depends on full list. Loads from cache and follows full list update, just that 0 index is reloaded only.
 * Smart list, loads same as full list, independent.
 * <p>
 * A placeholder fragment containing a simple view.
 */
public class YourProfileMusicFragment extends Fragment implements CacheInjectorCallbacks<Message>,
        CacheAdapterInterface<Message, Song> {

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
    private final SecureRandom secureRandom = new SecureRandom();

    private HandOverMessage<ReachDatabase> addSongToQueue = null;
    private RecyclerViewMaterialAdapter materialAdapter = null;
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
    public void onAttach(Context context) {

        super.onAttach(context);
        try {
            //noinspection unchecked
            addSongToQueue = (HandOverMessage<ReachDatabase>) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {

        super.onDetach();
        addSongToQueue = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        userId = getArguments().getLong("userId", 0L);

        fullListCache = new Cache(this, CacheType.MUSIC_FULL_LIST, userId) {
            @Override
            protected Callable<List<? extends Message>> fetchFromNetwork() {

                return () -> CloudStorageUtils.fetchSongs(userId, new WeakReference<>(getContext()));
            }

            @Override
            protected Message getItem(byte[] source, int offset, int count) throws IOException {
                return new Wire(Song.class).parseFrom(source, offset, count, Song.class);
            }
        };

        smartListCache = new Cache(this, CacheType.MUSIC_SMART_LIST, userId) {
            @Override
            protected Callable<List<? extends Message>> fetchFromNetwork() {
                return getSmartList;
            }

            @Override
            protected Message getItem(byte[] source, int offset, int count) throws IOException {
                return new Wire(SmartSong.class).parseFrom(source, offset, count, SmartSong.class);
            }
        };

        recentMusicCache = new Cache(this, CacheType.MUSIC_RECENT_LIST, userId) {
            @Override
            protected Callable<List<? extends Message>> fetchFromNetwork() {
                return getRecent;
            }

            @Override
            protected Message getItem(byte[] source, int offset, int count) throws IOException {
                return new Wire(RecentSong.class).parseFrom(source, offset, count, RecentSong.class);
            }
        };

        final View rootView = inflater.inflate(R.layout.fragment_simple_recycler, container, false);
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
    public boolean verifyItemVisibility(Message item) {
        //TODO
        return true;
    }

    @Override
    public int getItemCount() {

        final int size = musicData.size();
        if (size == 0) {
            recentMusicCache.loadMoreElements(true);
            fullListCache.loadMoreElements(false);
        }

        return size;
    }

    @Override
    public Message getItem(int position) {

        if (position > lastPosition)
            lastPosition = position;

        final int currentSize = musicData.size();

        //if reaching end of story and are not done yet
        if (position > currentSize - 5)
            //request a partial load
            fullListCache.loadMoreElements(false);

        return musicData.get(position);
    }

    @Override
    public void handOverMessage(@NonNull Song song) {

        final Cursor senderCursor = getActivity().getContentResolver().query(
                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
                new String[]{ReachFriendsHelper.COLUMN_USER_NAME,
                        ReachFriendsHelper.COLUMN_STATUS,
                        ReachFriendsHelper.COLUMN_NETWORK_TYPE},
                ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{userId + ""}, null);

        if (senderCursor == null)
            return;
        if (!senderCursor.moveToFirst()) {
            senderCursor.close();
            return;
        }

        final SharedPreferences sharedPreferences = getActivity().getSharedPreferences("Reach", Context.MODE_PRIVATE);
//        if (SharedPrefUtils.getReachQueueSeen(sharedPreferences)) {
//
//            Snackbar.make(rootView, song.displayName + " added to your Queue", Snackbar.LENGTH_LONG)
//                    .setAction("VIEW", v -> {
//                        mListener.anchorFooter();
//                    })
//                    .show();
//        } else {
//            SharedPrefUtils.setReachQueueSeen(sharedPreferences);
//            mListener.anchorFooter();
//        }

        final ReachDatabase reachDatabase = new ReachDatabase();

        reachDatabase.setId(-1);
        reachDatabase.setSongId(song.songId);
        reachDatabase.setReceiverId(SharedPrefUtils.getServerId(sharedPreferences));
        reachDatabase.setSenderId(userId);

        reachDatabase.setOperationKind((short) 0);
        reachDatabase.setPath("hello_world");
        reachDatabase.setSenderName(senderCursor.getString(0));
        reachDatabase.setOnlineStatus(senderCursor.getShort(1) + "");

        reachDatabase.setArtistName(song.artist);
        reachDatabase.setIsLiked(false);
        reachDatabase.setDisplayName(song.displayName);
        reachDatabase.setActualName(song.actualName);
        reachDatabase.setLength(song.size);
        reachDatabase.setProcessed(0);
        reachDatabase.setAdded(System.currentTimeMillis());
        reachDatabase.setUniqueId(secureRandom.nextInt(Integer.MAX_VALUE));

        reachDatabase.setDuration(song.duration);
        reachDatabase.setLogicalClock((short) 0);
        reachDatabase.setStatus(ReachDatabase.NOT_WORKING);

        reachDatabase.setLastActive(0);
        reachDatabase.setReference(0);

        reachDatabase.setAlbumName(song.album);
        reachDatabase.setGenre(song.genre);

        reachDatabase.setVisibility((short) 1);

        addSongToQueue.handOverMessage(reachDatabase); //genre
        senderCursor.close();
    }

    @Override
    public void injectElements(List<Message> elements, boolean overWrite, boolean loadingDone) {

        if (elements == null || elements.isEmpty())
            return;

        final Message typeCheckerInstance = elements.get(0);
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
    }

    private void intelligentOverwrite(List<? extends Message> elements, Class typeChecker) {

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
                    messageIterator.remove(); //must remove

                } else {
                    musicData.remove(index);  //remove as this item is no longer valid
                    updatedSize--;
                }
            }
        }
    }

    private void painter(List<? extends Message> elements, Class typeChecker) {

        if (musicData.isEmpty())
            synchronized (musicData) {
                musicData.addAll(elements);
            }

        else if (typeChecker == RecentSong.class)

            synchronized (musicData) {
                musicData.addAll(0, elements);
            }

        else if (typeChecker == SmartSong.class) {

            final int size = musicData.size();
            if (lastPosition > size)
                lastPosition = size;
            synchronized (musicData) {
                musicData.addAll(lastPosition, elements);
            }

        } else if (typeChecker == Song.class)

            synchronized (musicData) {
                musicData.addAll(elements);
            }
    }

    private static final Callable<List<? extends Message>> getSmartList = () -> {

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
        }

        if (songs.isEmpty())
            return Collections.emptyList();

        return Collections.singletonList(new SmartSong.Builder().songList(songs).title("Recently Played").build());
    };

    private static final Callable<List<? extends Message>> getRecent = () -> {

        final List<SimpleSong> simpleSongs = MiscUtils.autoRetry(
                () -> StaticData.userEndpoint.fetchRecentSongs(userId).execute().getItems(), Optional.absent()
        ).or(Collections.emptyList());

        if (simpleSongs == null || simpleSongs.isEmpty())
            return Collections.emptyList();

        final List<Song> toReturn = new ArrayList<>(simpleSongs.size());
        for (SimpleSong simpleSong : simpleSongs) {

            final Song.Builder songBuilder = new Song.Builder();

            songBuilder.actualName = simpleSong.getActualName();
            songBuilder.album = simpleSong.getAlbum();
            songBuilder.artist = simpleSong.getArtist();
            songBuilder.dateAdded = simpleSong.getDateAdded();
            songBuilder.displayName = simpleSong.getDisplayName();
            songBuilder.duration = simpleSong.getDuration();
            songBuilder.fileHash = simpleSong.getFileHash();
            songBuilder.genre = simpleSong.getGenre();
            songBuilder.path = simpleSong.getPath();
            songBuilder.isLiked = simpleSong.getIsLiked();
            songBuilder.size = simpleSong.getSize();
            songBuilder.songId = simpleSong.getSongId();
            songBuilder.visibility = simpleSong.getVisibility();
            songBuilder.year = simpleSong.getYear();
            toReturn.add(songBuilder.build());
        }

        final RecentSong.Builder recentBuilder = new RecentSong.Builder();
        recentBuilder.title("Recently Added");
        recentBuilder.songList(toReturn);

        return Collections.singletonList(recentBuilder.build());
    };
}