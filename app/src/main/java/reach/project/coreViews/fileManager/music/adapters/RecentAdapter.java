package reach.project.coreViews.fileManager.music.adapters;

import android.net.Uri;
import android.view.View;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.common.base.Optional;
import com.google.common.collect.Ordering;

import java.util.Comparator;
import java.util.List;

import reach.project.music.Song;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreButtonAdapter;

/**
 * Created by dexter on 18/11/15.
 */
class RecentAdapter extends MoreButtonAdapter<Song, SongItemHolder> {

    private final List<Song> recentMusic;

    public RecentAdapter(List<Song> recentMusic, HandOverMessage<Song> handOverMessage, int resourceId) {
        super(recentMusic, handOverMessage, resourceId);
        this.recentMusic = recentMusic;
    }

    /**
     * MUST CALL FROM UI THREAD
     *
     * @param newMessages the new collection to display
     */
    public void updateRecent(List<Song> newMessages) {
        
        recentMusic.removeAll(newMessages);
        
        final List<Song> newSortedList;
        synchronized (recentMusic) {

            recentMusic.addAll(newMessages);
            newSortedList = Ordering.from(new Comparator<Song>() {
                @Override
                public int compare(Song lhs, Song rhs) {

                    final Long a = lhs.dateAdded == null ? 0 : lhs.dateAdded;
                    final Long b = rhs.dateAdded == null ? 0 : rhs.dateAdded;

                    return a.compareTo(b);
                }
            }).greatestOf(recentMusic, 20);
            recentMusic.clear();
            recentMusic.addAll(newSortedList);
        }

        notifyDataSetChanged();
    }

    @Override
    public SongItemHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        return new SongItemHolder(itemView, handOverMessage);
    }

    @Override
    public void onBindViewHolder(SongItemHolder holder, int position) {

        final Song song = getItem(position);
        holder.bindPosition(position);

        holder.songName.setText(song.displayName);
        holder.artistName.setText(song.artist);
        final Optional<Uri> uriOptional = AlbumArtUri.getUri(song.album, song.artist, song.displayName);

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
}