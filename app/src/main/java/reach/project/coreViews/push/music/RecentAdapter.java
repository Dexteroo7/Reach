package reach.project.coreViews.push.music;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.common.base.Optional;
import com.google.common.collect.Ordering;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.List;

import reach.project.core.ReachActivity;
import reach.project.music.Song;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;

/**
 * Created by dexter on 18/11/15.
 */
class RecentAdapter extends SimpleRecyclerAdapter<Song, SongItemHolder> implements MoreQualifier {

    public RecentAdapter(List<Song> recentMusic, HandOverMessage<Song> handOverMessage, int resourceId) {
        super(recentMusic, handOverMessage, resourceId);
    }

    private final static Comparator<Song> PRIMARY = (left, right) -> {

        final Long lhs = left == null ? 0 : left.dateAdded;
        final Long rhs = right == null ? 0 : right.dateAdded;

        return lhs.compareTo(rhs);
    };

    private final static Comparator<Song> SECONDARY = (left, right) -> {
        final String lhs = left == null ? "" : left.displayName;
        final String rhs = right == null ? "" : right.displayName;

        return lhs.compareTo(rhs);
    };

    @Nullable
    private WeakReference<RecyclerView.Adapter> adapterWeakReference = null;

    /**
     * MUST CALL FROM UI THREAD
     *
     * @param newMessages the new collection to display
     */
    public void updateRecent(List<Song> newMessages) {

        if (newMessages.isEmpty()) {

            notifyItemRangeRemoved(0, getItemCount());
            final RecyclerView.Adapter adapter;
            if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
                adapter.notifyItemRangeRemoved(0, adapter.getItemCount());
        }

        final List<Song> recentMusic = getMessageList();
        //remove to prevent duplicates
        recentMusic.removeAll(newMessages);
        //add new items
        recentMusic.addAll(newMessages);

        //pick top 20
        final List<Song> newSortedList = Ordering.from(PRIMARY).compound(SECONDARY).greatestOf(recentMusic, 20);

        //remove all
        recentMusic.clear();
        //add top 20
        recentMusic.addAll(newSortedList);

        notifyDataSetChanged();

        final RecyclerView.Adapter adapter;
        if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
            adapter.notifyDataSetChanged();
    }

    public void toggleSelected(long songId) {

        int position = 0;
        for (Song song : getMessageList()) {

            if (song.songId != null && song.songId == songId)
                break;
            position++;
        }

        if (position < getItemCount())
            notifyItemChanged(position);

        final RecyclerView.Adapter adapter;
        if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
            adapter.notifyItemChanged(position);
    }

    @Override
    public SongItemHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        return new SongItemHolder(itemView, handOverMessage);
    }

    @Override
    public long getItemId(Song item) {
        return item.songId;
    }

    @Override
    public void onBindViewHolder(SongItemHolder holder, Song item) {

        holder.songName.setText(item.displayName);
        holder.artistName.setText(item.artist);
        final boolean selected = ReachActivity.SELECTED_SONG_IDS.get(item.songId, false);
        holder.mask.setVisibility(selected ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selected);

//        Log.i("Ayush", "Selected state " + item.displayName + " " + ReachActivity.SELECTED_SONG_IDS.get(item.songId, false));

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
                    .setOldController(holder.albumArt.getController())
                    .setImageRequest(request)
                    .build();

            holder.albumArt.setController(controller);
        } else
            holder.albumArt.setImageBitmap(null);
    }

    @Override
    public int getItemCount() {

        final int length = super.getItemCount();
        return length > 4 ? 4 : length;
    }

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        this.adapterWeakReference = adapterWeakReference;
    }
}