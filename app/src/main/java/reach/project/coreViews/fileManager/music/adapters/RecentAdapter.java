package reach.project.coreViews.fileManager.music.adapters;

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

import reach.project.music.Song;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;
import reach.project.utils.viewHelpers.MoreQualifier;

/**
 * Created by dexter on 18/11/15.
 */
class RecentAdapter extends SimpleRecyclerAdapter<Song, SongItemHolder> implements MoreQualifier {

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

        final List<Song> recentMusic = getMessageList();
        //remove to prevent duplicates
        recentMusic.removeAll(newMessages);
        //add new items
        recentMusic.addAll(newMessages);

        //pick top 20
        final List<Song> newSortedList = Ordering.from(new Comparator<Song>() {
            @Override
            public int compare(Song lhs, Song rhs) {

                final Long a = lhs.dateAdded == null ? 0 : lhs.dateAdded;
                final Long b = rhs.dateAdded == null ? 0 : rhs.dateAdded;

                return a.compareTo(b);
            }
        }).greatestOf(recentMusic, 20);

        //remove all
        recentMusic.clear();
        //add top 20
        recentMusic.addAll(newSortedList);

        notifyDataSetChanged();
        if (adapterWeakReference != null)
            adapterWeakReference.get().notifyDataSetChanged();
    }

    @Override
    public SongItemHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        return new SongItemHolder(itemView, handOverMessage);
    }

    @Override
    public void onBindViewHolder(SongItemHolder holder, Song item) {

        holder.songName.setText(item.displayName);
        holder.artistName.setText(item.artist);
        final Optional<Uri> uriOptional = AlbumArtUri.getUri(item.album, item.artist, item.displayName);

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
        int size = super.getItemCount();
        return size < 4 ? size : 4;
    }

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        this.adapterWeakReference = adapterWeakReference;
    }
}