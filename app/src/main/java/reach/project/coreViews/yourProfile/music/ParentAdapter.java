package reach.project.coreViews.yourProfile.music;

import android.net.Uri;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.common.base.Optional;
import com.squareup.wire.Message;

import reach.project.R;
import reach.project.coreViews.yourProfile.blobCache.CacheAdapterInterface;
import reach.project.music.Song;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.ListHolder;
import reach.project.utils.viewHelpers.RecyclerViewMaterialAdapter;


/**
 * Created by dexter on 13/11/15.
 */
class ParentAdapter<T extends Message> extends RecyclerViewMaterialAdapter<RecyclerView.ViewHolder> {

    private static final byte SONG_ITEM_TYPE = 1;
    private static final byte RECENT_LIST_TYPE = 2;
    private static final byte SMART_LIST_TYPE = 3;

    private final CacheAdapterInterface<T, Song> cacheAdapterInterface;

    public ParentAdapter(CacheAdapterInterface<T, Song> cacheAdapterInterface) {
        this.cacheAdapterInterface = cacheAdapterInterface;
        setHasStableIds(true);
    }

    @Override
    protected void newBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final Message message = cacheAdapterInterface.getItem(position);
        if (message instanceof Song && holder instanceof SongItemHolder) {

            final Song song = (Song) message;
            final SongItemHolder songSongItemHolder = (SongItemHolder) holder;

            songSongItemHolder.bindPosition(position);
            songSongItemHolder.songName.setText(song.displayName);
            songSongItemHolder.artistName.setText(song.artist);

            final Optional<Uri> uriOptional = AlbumArtUri.getUri(song.album, song.artist, song.displayName, false);

            if (uriOptional.isPresent()) {

//                Log.i("Ayush", "Url found = " + uriOptional.get().toString());

                final ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uriOptional.get())
                        .setResizeOptions(new ResizeOptions(50, 50))
                        .build();

                final DraweeController controller = Fresco.newDraweeControllerBuilder()
                        .setOldController(songSongItemHolder.albumArt.getController())
                        .setImageRequest(request)
                        .build();

                songSongItemHolder.albumArt.setController(controller);
            } else
                songSongItemHolder.albumArt.setImageBitmap(null);

        } else if (message instanceof RecentSong && holder instanceof ListHolder) {

            final RecentSong recentSong = (RecentSong) message;
            final ListHolder listHolder = (ListHolder) holder;
            listHolder.itemView.setBackgroundResource(R.drawable.border_shadow1);
            listHolder.headerText.setText(recentSong.title);
            listHolder.listOfItems.setLayoutManager(new CustomGridLayoutManager(holder.itemView.getContext(), 2));

            Log.i("Ayush", "Found recent items with size " + recentSong.songList.size() + " ");
            listHolder.listOfItems.setAdapter(new MoreAdapter(recentSong.songList, cacheAdapterInterface, R.layout.song_grid_item));

        } else if (message instanceof SmartSong && holder instanceof ListHolder) {

            final SmartSong smartSong = (SmartSong) message;
            final ListHolder listHolder = (ListHolder) holder;
            listHolder.itemView.setBackgroundResource(R.drawable.border_shadow3);
            listHolder.headerText.setText(smartSong.title);
            listHolder.listOfItems.setLayoutManager(new CustomLinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            listHolder.listOfItems.setAdapter(new MoreAdapter(smartSong.songList, cacheAdapterInterface, R.layout.song_grid_item));
        }
    }

    @Override
    protected RecyclerView.ViewHolder newCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {

            case SONG_ITEM_TYPE:
                return new SongItemHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.song_list_item, parent, false), position -> {

                    final T message = cacheAdapterInterface.getItem(position);
                    if (message instanceof Song)
                        cacheAdapterInterface.handOverMessage((Song) message);
                    else
                        throw new IllegalArgumentException("Song item holder passed on an illegal value type");
                });
            case RECENT_LIST_TYPE:
                return new ListHolder(parent);
            case SMART_LIST_TYPE:
                return new ListHolder(parent);
            default:
                throw new IllegalArgumentException("Unknown view type found");
        }
    }

    @Override
    protected int newGetItemCount() {
        return cacheAdapterInterface.getItemCount();
    }

    @Override
    protected int newGetItemViewType(int position) {
        final Message message = cacheAdapterInterface.getItem(position);
        if (message instanceof Song)
            return SONG_ITEM_TYPE;
        else if (message instanceof RecentSong)
            return RECENT_LIST_TYPE;
        else if (message instanceof SmartSong)
            return SMART_LIST_TYPE;
        else
            throw new IllegalArgumentException("Unknown message found in list");
    }

    @Override
    protected long newGetItemId(int position) {
        return cacheAdapterInterface.getItem(position).hashCode();
    }

    @Override
    protected RecyclerView.ViewHolder inflatePlaceHolder(View view) {
        return new RecyclerView.ViewHolder(view) {
        };
    }
}