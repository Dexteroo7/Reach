package reach.project.yourProfile.music;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.wire.Message;
import com.squareup.wire.Wire;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import reach.project.music.Song;
import reach.project.utils.CloudStorageUtils;
import reach.project.yourProfile.blobCache.Cache;
import reach.project.yourProfile.blobCache.CacheInjectorCallbacks;
import reach.project.yourProfile.blobCache.CacheType;

/**
 * Replace song with appropriate wrapper
 * Created by dexter on 07/11/15.
 */
public class TestFragment extends Fragment implements CacheInjectorCallbacks<Message> {

    private final List<Message> musicData = new ArrayList<>(100);

    {
        //TODO insert Loading indicator into list
    }

    private Cache fullListCache;
    private Cache smartListCache;
    private long userId = 0;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        fullListCache = new Cache(this, CacheType.MUSIC_FULL_LIST, 0) {
            @Override
            protected Callable<Collection<? extends Message>> fetchFromNetwork() {
                return () -> CloudStorageUtils.fetchSongs(userId, new WeakReference<>(getContext()));
            }

            @Override
            protected Message getItem(byte[] source, int offset, int count) throws IOException {
                return new Wire(Song.class).parseFrom(source, offset, count, Song.class);
            }

            @Override
            protected void signalExternalInjection(boolean complete) {
                Log.i("Ayush", "Requesting external injection " + complete);
                //TODO use complete some how
                smartListCache.loadMoreElements(); //request smart data
            }

            @Override
            protected void loadingDone() {
                Log.i("Ayush", "Loading done");
                synchronized (musicData) {

                    final int size = musicData.size();
                    if (size == 0)
                        return; //illegal state

                    musicData.remove(size - 1); //remove loading indicator
                }
            }
        };

        smartListCache = new Cache(this, CacheType.MUSIC_SMART_LIST, 0) {
            @Override
            protected Callable<Collection<? extends Message>> fetchFromNetwork() {
                return null;
            }

            @Override
            protected Message getItem(byte[] source, int offset, int count) {
                return null;
            }

            @Override
            protected void signalExternalInjection(boolean complete) {
                //ignored
            }

            @Override
            protected void loadingDone() {
                //ignored
            }
        };

        final View rootView = super.onCreateView(inflater, container, savedInstanceState);

        return rootView;
    }

    @Override
    public File getCacheDirectory() {
        return getContext().getExternalCacheDir();
    }

    @Override
    public Long getItemId(Message item) {

        if (item instanceof Song)
            return ((Song) item).songId;

        return 0L;
    }

    @Override
    public void injectElements(Collection<Message> elements, boolean overWrite) {

        Log.i("Ayush", "Inserting " + elements.size() + " new items");

        synchronized (musicData) {

            if (overWrite) {

                musicData.clear(); //remove all
                musicData.addAll(elements); //add all
            } else {

                final int size = musicData.size();
                if (size == 0)
                    musicData.addAll(elements); //no loading indicator present
                else
                    musicData.addAll(size - 1, elements); //loading indicator present
            }
        }
    }
}