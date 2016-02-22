package reach.project.coreViews.yourProfile.music;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.florent37.materialviewpager.MaterialViewPagerHelper;
import com.google.common.base.Optional;
import com.squareup.wire.Message;
import com.squareup.wire.Wire;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import reach.backend.entities.userApi.model.SimpleSong;
import reach.project.R;
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
import reach.project.utils.ThreadLocalRandom;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;

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
    private static long hostId = 0;
    private static long myId = 0;
    private RecyclerView mRecyclerView;
    private View emptyView;

    public static YourProfileMusicFragment newInstance(long hostId) {

        final Bundle args;
        YourProfileMusicFragment fragment;
        reference = new WeakReference<>(fragment = new YourProfileMusicFragment());
        fragment.setArguments(args = new Bundle());
        args.putLong("hostId", hostId);
        return fragment;
    }

    private final List<Message> musicData = new ArrayList<>(100);
    private final ExecutorService musicUpdaterService = MiscUtils.getRejectionExecutor();

    @Nullable
    private Cache fullListCache = null, smartListCache = null, recentMusicCache = null;
    @Nullable
    private View rootView = null;
    @Nullable
    private ParentAdapter parentAdapter;

    private int lastPosition = 0;

    @Override
    public void onDestroyView() {

        super.onDestroy();
        hostId = 0;

        MiscUtils.closeQuietly(fullListCache, smartListCache, recentMusicCache);
        fullListCache = smartListCache = recentMusicCache = null;
        musicData.clear();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_simple_recycler, container, false);
         mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        emptyView = rootView.findViewById(R.id.empty_imageView);
        final TextView emptyViewText = (TextView) rootView.findViewById(R.id.empty_textView);
        emptyViewText.setText(StaticData.NO_SONGS_TEXT);
        final Activity activity = getActivity();
        //mRecyclerView.setHasFixedSize(true);

        parentAdapter = new ParentAdapter<>(this);
        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(activity));
        mRecyclerView.setAdapter(parentAdapter);
        MaterialViewPagerHelper.registerRecyclerView(activity, mRecyclerView, null);

        hostId = getArguments().getLong("hostId", 0L);

        //get the caches
        fullListCache = new Cache(this, CacheType.MUSIC_FULL_LIST, hostId) {
            @Override
            protected Callable<List<? extends Message>> fetchFromNetwork() {

                return () -> CloudStorageUtils.fetchSongs(hostId, new WeakReference<>(getContext()));
            }

            @Override
            protected Message getItem(byte[] source, int offset, int count) throws IOException {
                return new Wire(Song.class).parseFrom(source, offset, count, Song.class);
            }
        };

        smartListCache = new Cache(this, CacheType.MUSIC_SMART_LIST, hostId) {
            @Override
            protected Callable<List<? extends Message>> fetchFromNetwork() {
                return getSmartList;
            }

            @Override
            protected Message getItem(byte[] source, int offset, int count) throws IOException {
                return new Wire(SmartSong.class).parseFrom(source, offset, count, SmartSong.class);
            }
        };

        recentMusicCache = new Cache(this, CacheType.MUSIC_RECENT_LIST, hostId) {
            @Override
            protected Callable<List<? extends Message>> fetchFromNetwork() {
                return getRecent;
            }

            @Override
            protected Message getItem(byte[] source, int offset, int count) throws IOException {
                return new Wire(RecentSong.class).parseFrom(source, offset, count, RecentSong.class);
            }
        };

        final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        myId = SharedPrefUtils.getServerId(sharedPreferences);

        //update music
        musicUpdaterService.submit(musicUpdater);
        return rootView;
    }

    /////////////////////////
    @Override
    public File getCacheDirectory() {
        return getContext().getCacheDir();
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
            if (recentMusicCache != null)
                recentMusicCache.loadMoreElements(true);
            if (fullListCache != null)
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
        if (position > currentSize - 5 && fullListCache != null)
            //request a partial load
            fullListCache.loadMoreElements(false);

        return musicData.get(position);
    }

    @Override
    public void handOverMessage(@NonNull Song song) {

        final Activity activity = getActivity();
        final Cursor senderCursor = activity.getContentResolver().query(
                Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + hostId),
                new String[]{ReachFriendsHelper.COLUMN_USER_NAME,
                        ReachFriendsHelper.COLUMN_STATUS},
                ReachFriendsHelper.COLUMN_ID + " = ?",
                new String[]{hostId + ""}, null);

        if (senderCursor == null)
            return;
        if (!senderCursor.moveToFirst()) {
            senderCursor.close();
            return;
        }

        final ReachDatabase reachDatabase = new ReachDatabase();

        reachDatabase.setId(-1);
        reachDatabase.setSongId(song.songId);
        reachDatabase.setReceiverId(myId);
        reachDatabase.setSenderId(hostId);

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
        reachDatabase.setUniqueId(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE));

        reachDatabase.setDuration(song.duration);
        reachDatabase.setLogicalClock((short) 0);
        reachDatabase.setStatus(ReachDatabase.NOT_WORKING);

        reachDatabase.setLastActive(0);
        reachDatabase.setReference(0);

        reachDatabase.setAlbumName(song.album);
        reachDatabase.setGenre(song.genre);

        reachDatabase.setVisibility((short) 1);

        MiscUtils.startDownload(reachDatabase, activity, rootView, "YOUR_PROFILE");
        senderCursor.close();
    }

    @Override
    public void injectElements(List<Message> elements, boolean overWrite, boolean loadingDone) {

        if (elements == null || elements.isEmpty()){
            /*emptyView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);*/
            return;

        }

       /* emptyView.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);*/

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
        Log.i("Ayush", "Reloading list " + musicData.size());
        if (parentAdapter != null)
            parentAdapter.notifyDataSetChanged();

        /**
         * If loading has finished request a full injection of smart lists
         * Else request partial injection
         */
        if (typeChecker == Song.class && smartListCache != null)
            smartListCache.loadMoreElements(fullListCache == null || fullListCache.isLoadingDone());
    }

    private void intelligentOverwrite(List<? extends Message> elements, Class typeChecker) {

        //nothing to overwrite
        if (musicData.isEmpty())
            return;

        final Iterator<? extends Message> messageIterator = elements.iterator();
        int updatedSize = musicData.size();
        int index;

        for (index = 0; index < updatedSize; index++) {

            //ignore if element is not of same class type
            if (!musicData.get(index).getClass().equals(typeChecker))
                continue;

            //get the next message to overwrite if present
            if (messageIterator.hasNext()) {

                //we have a message to overwrite, do it
                synchronized (musicData) {
                    musicData.set(index, messageIterator.next());
                }
                messageIterator.remove(); //must remove

            } else {

                synchronized (musicData) {
                    musicData.remove(index);  //remove as this item is no longer valid
                }
                updatedSize--;
            }
        }
    }

    /**
     * @param elements    the items to insert
     * @param typeChecker the type of item to help in overwriting
     * @return true if elements got added
     */
    private boolean painter(List<? extends Message> elements, Class typeChecker) {

        if (musicData.isEmpty())
            synchronized (musicData) {
                return musicData.addAll(elements);
            }

        else if (typeChecker == RecentSong.class)

            synchronized (musicData) {
                return musicData.addAll(0, elements);
            }

        else if (typeChecker == SmartSong.class) {

            final int size = musicData.size();
            if (lastPosition > size)
                lastPosition = size;
            synchronized (musicData) {
                return musicData.addAll(lastPosition, elements);
            }

        } else if (typeChecker == Song.class)

            synchronized (musicData) {
                return musicData.addAll(elements);
            }

        return false;
    }

    private static final Callable<List<? extends Message>> getSmartList = Collections::emptyList;

//    private static final Callable<List<? extends Message>> getSmartList = () -> {
//
//        final Request request = new Request.Builder()
//                .url("http://52.74.175.56:8080/explore/getRecentlyPlayed?hostId=" + hostId)
//                .build();
//
//        Log.i("Ayush", "Fetching smart lists " + request.urlString());
//
//        final Response response = ReachApplication.OK_HTTP_CLIENT.newCall(request).execute();
//        final JSONArray receivedData = new JSONArray(response.body().string());
//        final List<Song> songs = new ArrayList<>();
//
//        JSONObject smartData;
//        for (int index = 0; index < receivedData.length(); index++) {
//
//            smartData = receivedData.getJSONObject(index);
//
//            final Song.Builder songBuilder = new Song.Builder();
//            songBuilder.songId(smartData.getLong("songId"));
//            songBuilder.size(smartData.getLong("size"));
//            songBuilder.dateAdded(smartData.getLong("timeAdded"));
//            songBuilder.duration(smartData.getLong("duration"));
//
//            songBuilder.actualName(smartData.getString("actualName"));
//            songBuilder.displayName(smartData.getString("displayName"));
//            songBuilder.artist(smartData.getString("artistName"));
//
//            //userName fetch else where
//            try {
//                songBuilder.album(smartData.getString("albumName"));
//                songBuilder.genre(smartData.getString("genre"));
//            } catch (JSONException | NullPointerException ignored) {
//            }
//
//            songBuilder.isLiked(smartData.getBoolean("isLiked"));
//
//            songBuilder.visibility(true);
//            songs.add(songBuilder.build());
//        }
//
//        if (songs.isEmpty())
//            return Collections.emptyList();
//
//        return Arrays.asList(new SmartSong.Builder().songList(songs).title("Recently Played").build());
//    };

    private static final Callable<List<? extends Message>> getRecent = () -> {

        final List<SimpleSong> simpleSongs = MiscUtils.autoRetry(
                () -> StaticData.USER_API.fetchRecentSongs(hostId).execute().getItems(), Optional.absent()
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

        return Arrays.asList(recentBuilder.build());
    };

    private static final Runnable musicUpdater = () -> {

        final String localHash = MiscUtils.useContextFromFragment(reference, context -> {
            return SharedPrefUtils.getCloudStorageFileHash(
                    context.getSharedPreferences("Reach", Context.MODE_PRIVATE),
                    MiscUtils.getMusicStorageKey(hostId));
        }).or("");

        final InputStream keyStream = MiscUtils.useContextFromFragment(reference, context -> {
            try {
                return context.getAssets().open("key.p12");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }).orNull();

        if (TextUtils.isEmpty(localHash) || keyStream == null)
            return; //dead

        if (CloudStorageUtils.isNewMusicAvailable(hostId, keyStream, localHash)) {

            //new music available, invalidate everything
            MiscUtils.useFragment(reference, fragment -> {

                if (fragment.fullListCache != null) {
                    fragment.fullListCache.invalidateCache();
                    fragment.fullListCache.loadMoreElements(true);
                }
                if (fragment.smartListCache != null) {
                    fragment.smartListCache.invalidateCache();
                    fragment.smartListCache.loadMoreElements(true);
                }
                if (fragment.recentMusicCache != null) {
                    fragment.recentMusicCache.invalidateCache();
                    fragment.recentMusicCache.loadMoreElements(true);
                }
            });
        }
    };
}