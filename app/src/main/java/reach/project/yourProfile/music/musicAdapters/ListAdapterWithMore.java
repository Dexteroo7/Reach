package reach.project.yourProfile.music.musicAdapters;

import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.common.base.Optional;

import java.util.List;

import reach.project.R;
import reach.project.music.Song;
import reach.project.utils.AlbumArtUri;

/**
 * Created by dexter on 18/11/15.
 */
class ListAdapterWithMore extends RecyclerView.Adapter<SongItemHolder> implements CacheAdapterInterface<Song> {

    private final List<Song> songList;
    private final HandOverSongClick handOverSongClick;

    protected ListAdapterWithMore(List<Song> songsList, HandOverSongClick handOverSongClick) {
        this.songList = songsList;
        this.handOverSongClick = handOverSongClick;
        setHasStableIds(true);
    }

    @Override
    public SongItemHolder onCreateViewHolder(ViewGroup parent, int mType) {
        return new SongItemHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.song_list_item, parent, false), this);
    }

    @Override
    public void onBindViewHolder(SongItemHolder holder, int position) {

        final Song song = songList.get(position);

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

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    @Override
    public long getItemId(Song item) {
        return item.hashCode();
    }

    @Override
    public Song getItem(int position) {
        return songList.get(position);
    }

    @Override
    public void handOverSongClick(Song song) {
        handOverSongClick.handOverSongClick(song);
    }

    public interface HandOverSongClick {
        void handOverSongClick(Song song);
    }
}