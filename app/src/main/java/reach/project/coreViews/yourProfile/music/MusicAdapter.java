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
import reach.project.music.Song;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.CacheAdapterInterface;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.ListHolder;


/**
 * Created by dexter on 13/11/15.
 */
class MusicAdapter<T extends Message> extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements View.OnClickListener, HandOverMessage<Song> {

    private static final byte SONG_ITEM_TYPE = 1;
    private static final byte RECENT_LIST_TYPE = 2;
    private static final byte SMART_LIST_TYPE = 3;

    private final CacheAdapterInterface<T, Song> cacheAdapterInterface;

    public MusicAdapter(CacheAdapterInterface<T, Song> cacheAdapterInterface) {
        this.cacheAdapterInterface = cacheAdapterInterface;
        setHasStableIds(true);
    }

    @Override
    public int getItemViewType(int position) {

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
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

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
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final Message message = cacheAdapterInterface.getItem(position);
        if (message instanceof Song && holder instanceof SongItemHolder) {

            final Song song = (Song) message;
            final SongItemHolder songSongItemHolder = (SongItemHolder) holder;

            songSongItemHolder.bindPosition(position);
            songSongItemHolder.songName.setText(song.displayName);
            songSongItemHolder.artistName.setText(song.artist);

            final Optional<Uri> uriOptional = AlbumArtUri.getUri(song.album, song.artist, song.displayName);

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
            listHolder.moreButton.setTag(position);
            listHolder.moreButton.setOnClickListener(this);
            listHolder.headerText.setText(recentSong.title);
            listHolder.listOfItems.setLayoutManager(new CustomGridLayoutManager(holder.itemView.getContext(), 2));

            Log.i("Ayush", "Found recent items with size " + recentSong.songList.size() + " ");
            if (recentSong.songList.size() < 4)
                listHolder.listOfItems.setAdapter(new ListAdapterWithMore(recentSong.songList, this, R.layout.song_list_item));
            else
                listHolder.listOfItems.setAdapter(new ListAdapterWithMore(recentSong.songList.subList(0, 4), this, R.layout.song_list_item));

        } else if (message instanceof SmartSong && holder instanceof ListHolder) {

            final SmartSong smartSong = (SmartSong) message;
            final ListHolder listHolder = (ListHolder) holder;
            listHolder.moreButton.setTag(position);
            listHolder.moreButton.setOnClickListener(this);
            listHolder.headerText.setText(smartSong.title);
            listHolder.listOfItems.setLayoutManager(new CustomLinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            if (smartSong.songList.size() < 4)
                listHolder.listOfItems.setAdapter(new ListAdapterWithMore(smartSong.songList, this, R.layout.song_list_item));
            else
                listHolder.listOfItems.setAdapter(new ListAdapterWithMore(smartSong.songList.subList(0, 4), this, R.layout.song_list_item));
        }
    }

    @Override
    public void onClick(View v) {

        Object object = v.getTag();
        if (!(object instanceof Integer))
            return;
        int pos = (int) object;

        final Message message = cacheAdapterInterface.getItem(pos);
        if (message instanceof RecentSong) {
            RecentSong recentSong = (RecentSong) message;
            //open MoreMusicFragment
        } else if (message instanceof SmartSong) {
            SmartSong smartSong = (SmartSong) message;
        }
    }

    @Override
    public long getItemId(int position) {
        return cacheAdapterInterface.getItemId(cacheAdapterInterface.getItem(position));
    }

    @Override
    public int getItemCount() {
//        Log.i("Ayush", "Returning new size " + cacheAdapterInterface.getItemCount());
        return cacheAdapterInterface.getItemCount();
    }

//    @Override
//    public void handOverMessage(Song song) {
//        cacheAdapterInterface.handOverMessage(song);
//    }
//
//    @Override
//    public void handOverMessage(int position) {
//
//        final T item = cacheAdapterInterface.getItem(position);
//        if (item instanceof Song)
//            cacheAdapterInterface.handOverMessage((Song) item);
//        else
//            throw new IllegalArgumentException("Expecting Song, found something else");
//    }

    @Override
    public void handOverMessage(Song song) {
        cacheAdapterInterface.handOverMessage(song);
    }
}
