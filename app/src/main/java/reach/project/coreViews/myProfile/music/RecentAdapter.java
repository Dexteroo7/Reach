package reach.project.coreViews.myProfile.music;

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

import reach.project.R;
import reach.project.music.Song;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;

/**
 * Created by dexter on 18/11/15.
 */
class RecentAdapter extends SimpleRecyclerAdapter<Song, SongItemHolder> implements MoreQualifier {

    private static final Comparator<Song> PRIMARY = (left, right) -> {

        final Long lhs = left == null ? 0 : left.dateAdded;
        final Long rhs = right == null ? 0 : right.dateAdded;

        return lhs.compareTo(rhs);
    };

    private static final Comparator<Song> SECONDARY = (left, right) -> {

        final String lhs = left == null ? "" : left.displayName;
        final String rhs = right == null ? "" : right.displayName;

        return lhs.compareTo(rhs);
    };

    private final ResizeOptions resizeOptions = new ResizeOptions(150, 150);

    public RecentAdapter(List<Song> recentMusic, HandOverMessage<Song> handOverMessage, int resourceId) {
        super(recentMusic, handOverMessage, resourceId);
    }

    @Nullable
    private WeakReference<RecyclerView.Adapter> adapterWeakReference = null;

    /**
     * MUST CALL FROM UI THREAD
     *
     * @param newMessages the new collection to display
     */
    public void updateRecent(List<Song> newMessages) {

        if (newMessages.isEmpty()) {

            synchronized (getMessageList()) {
                getMessageList().clear();
            }
            notifyItemRangeRemoved(0, getItemCount());
            final RecyclerView.Adapter adapter;
            if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
                adapter.notifyItemRangeRemoved(0, adapter.getItemCount());
        } else {

            synchronized (getMessageList()) {

                //remove to prevent duplicates
                getMessageList().removeAll(newMessages);
                //add new items
                getMessageList().addAll(newMessages);

                //pick top 20
                final List<Song> newSortedList = Ordering.from(PRIMARY).compound(SECONDARY).greatestOf(getMessageList(), 20);

                //remove all
                getMessageList().clear();
                //add top 20
                getMessageList().addAll(newSortedList);
            }

            notifyDataSetChanged();
            final RecyclerView.Adapter adapter;
            if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
                adapter.notifyDataSetChanged();
        }
    }

    /**
     * MUST CALL FROM UI THREAD
     *
     * @param songId the song id to toggle visibility for
     */
    public synchronized void updateVisibility(long songId, boolean newVisibility) {

        final List<Song> songItems = getMessageList();

        int position = -1;
        for (int index = 0; index < songItems.size(); index++) {

            final Song songItem = songItems.get(index);
            if (songItem.songId == songId) {

                final Song newSong = new Song.Builder(songItem).visibility(newVisibility).build();
                songItems.set(index, newSong);
                position = index;
                break;
            }
        }

        //recent adapter might not contain everything, as is limited to 4
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

        holder.artistName.setText(item.artist);
        if (item.visibility) {

            holder.toggleButton.setImageResource(R.drawable.icon_everyone);
            holder.toggleButton2.setVisibility(View.GONE);
            holder.toggleText.setText("Everyone");
        } else {

            holder.toggleButton.setImageResource(R.drawable.icon_locked);
            holder.toggleButton2.setVisibility(View.VISIBLE);
            holder.toggleText.setText("Only Me");
        }
        holder.songName.setText(item.displayName);
        final Optional<Uri> uriOptional = AlbumArtUri.getUri(
                item.album,
                item.artist,
                item.displayName,
                false);

        if (uriOptional.isPresent()) {

//            Log.i("Ayush", "Url found = " + uriOptional.get().toString());
            final ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uriOptional.get())
                    .setResizeOptions(resizeOptions)
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
        int size = super.getItemCount();
        return size < 4 ? size : 4;
    }

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        this.adapterWeakReference = adapterWeakReference;
    }
}