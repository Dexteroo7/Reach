package reach.project.coreViews.push;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.View;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.common.base.Optional;
import com.google.common.collect.Ordering;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import reach.project.core.StaticData;
import reach.project.music.Song;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;

/**
 * Created by dexter on 18/11/15.
 */
class RecentMusicAdapter extends SimpleRecyclerAdapter<Song, PushItemHolder> implements MoreQualifier {

    public RecentMusicAdapter(List<Song> recentMusic, HandOverMessage<Song> handOverMessage, int resourceId) {
        super(recentMusic, handOverMessage, resourceId);
    }

    @Nullable
    private WeakReference<RecyclerView.Adapter> adapterWeakReference = null;

    /**
     * MUST CALL FROM UI THREAD
     *
     * @param newMessages the new collection to display
     */
    public synchronized void updateRecent(List<Song> newMessages) {

        final Set<String> names = MiscUtils.getSet(newMessages.size());
        for (Song song : newMessages)
            names.add(song.displayName);

        final List<Song> recentMusic = getMessageList();

        //remove to prevent duplicates
//        Log.i("Ayush", "Previous size " + recentMusic.size());
        final ListIterator<Song> songListIterator = recentMusic.listIterator();
        while (songListIterator.hasNext()) {

            final Song currentSong = songListIterator.next();
            if (names.contains(currentSong.displayName)) {

                Log.i("Ayush", "Removing " + currentSong);
                songListIterator.remove(); //remove id is same found
            }
        }
//        Log.i("Ayush", "After removal size " + recentMusic.size());

        //add new items
        recentMusic.addAll(newMessages);

        //sort
        final List<Song> newSortedList = Ordering
                .from(StaticData.primaryMusic)
                .compound(StaticData.secondaryMusic).immutableSortedCopy(recentMusic);

        //remove all
        recentMusic.clear();
        //add the sorted version
        recentMusic.addAll(newSortedList);

        notifyDataSetChanged();
        final RecyclerView.Adapter adapter;
        if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
            adapter.notifyDataSetChanged();
    }

    public synchronized void selectionChanged(long songId) {

        final List<Song> recentMusic = getMessageList();

        int position = -1;
        for (int index = 0; index < recentMusic.size(); index++) {

            final Long songIdCurrent = recentMusic.get(index).songId;
            if (songIdCurrent == null || songIdCurrent == songId) {
                position = index;
                break;
            }
        }

        Log.i("Ayush", hasObservers() + " obs");

        //will pick the new visibility from the map
        if (position > -1)
            notifyItemChanged(position);

        final RecyclerView.Adapter adapter;
        if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
            adapter.notifyDataSetChanged();
    }

    @Override
    public PushItemHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        return new PushItemHolder(itemView, handOverMessage);
    }

    public final LongSparseArray<Boolean> selected = new LongSparseArray<>(100);

    @Override
    public void onBindViewHolder(PushItemHolder holder, Song item) {

        holder.text.setText(item.displayName);
        final Optional<Uri> uriOptional = AlbumArtUri.getUri(
                item.album,
                item.artist,
                item.displayName,
                false);

        if (uriOptional.isPresent()) {

//            Log.i("Ayush", "Url found = " + uriOptional.get().toString());
            final ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uriOptional.get())
                    .setResizeOptions(new ResizeOptions(200, 200))
                    .build();

            final DraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setOldController(holder.image.getController())
                    .setImageRequest(request)
                    .build();

            holder.image.setController(controller);
        } else
            holder.image.setImageBitmap(null);

        final boolean isSelected = selected.get(item.songId == null ? 0 : item.songId, false);
        holder.checkBox.setChecked(isSelected);
        holder.mask.setVisibility(isSelected ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {

        final int size = super.getItemCount();
        return size > 6 ? 6 : size;
    }

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        this.adapterWeakReference = adapterWeakReference;
    }
}