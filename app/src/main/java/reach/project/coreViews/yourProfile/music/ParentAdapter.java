package reach.project.coreViews.yourProfile.music;

import android.net.Uri;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.coreViews.yourProfile.blobCache.CacheAdapterInterface;
import reach.project.music.Song;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.ThreadLocalRandom;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.RecyclerViewMaterialAdapter;

/**
 * Created by dexter on 13/11/15.
 */
class ParentAdapter<T extends Message> extends RecyclerViewMaterialAdapter<RecyclerView.ViewHolder> implements View.OnClickListener {

    private static final byte SONG_ITEM_TYPE = 1;
    private static final byte RECENT_LIST_TYPE = 2;
    private static final byte SMART_LIST_TYPE = 3;

    private final long recentHolderId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
    private final long smartHolderId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);

    private final CacheAdapterInterface<T, Song> cacheAdapterInterface;
    private final HandOverPerformActionTask handOverPerformAction;

    public ParentAdapter(CacheAdapterInterface<T, Song> cacheAdapterInterface, HandOverPerformActionTask handOverPerformAction) {

        this.cacheAdapterInterface = cacheAdapterInterface;
        this.handOverPerformAction = handOverPerformAction;
        setHasStableIds(true);
    }

    @Override
    protected void newBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final Message message = cacheAdapterInterface.getItem(position);
        if (message instanceof Song && holder instanceof SongItemHolder) {

            final Song song = (Song) message;
            final SongItemHolder songSongItemHolder = (SongItemHolder) holder;

            //songSongItemHolder.extraButton.setVisibility(View.INVISIBLE);
            //songSongItemHolder.likeButton.setVisibility(View.INVISIBLE);
            songSongItemHolder.songName.setText(song.displayName);
            songSongItemHolder.artistName.setText(song.artist);
            songSongItemHolder.saveButton.setTag(position);
            songSongItemHolder.saveButton.setOnClickListener(this);

            //TODO: Figure Out The Error
            //songSongItemHolder.downButton.setImageResource(R.drawable.icon_download_gray);

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

        } else if (message instanceof RecentSong && holder instanceof MoreListHolder) {


            final RecentSong recentSong = (RecentSong) message;
            final MoreListHolder simpleListHolder = (MoreListHolder) holder;
            if(recentSong.songList!=null && recentSong.songList.size()==0){
                simpleListHolder.itemView.setVisibility(View.GONE);
                return;
            }
            simpleListHolder.headerText.setText(recentSong.title);
            //simpleListHolder.itemView.setBackgroundResource(R.drawable.border_shadow1);
            if (simpleListHolder.listOfItems.getLayoutManager() == null)
                simpleListHolder.listOfItems.setLayoutManager(new CustomLinearLayoutManager(simpleListHolder.listOfItems.getContext()));
            simpleListHolder.listOfItems.setAdapter(new MoreAdapter(recentSong.songList, cacheAdapterInterface, R.layout.song_list_item,handOverPerformAction));

        } else if (message instanceof SmartSong && holder instanceof MoreListHolder) {

            final SmartSong smartSong = (SmartSong) message;
            final MoreListHolder simpleListHolder = (MoreListHolder) holder;
            simpleListHolder.itemView.setBackgroundResource(R.drawable.border_shadow1);
            simpleListHolder.headerText.setText(smartSong.title);
            if (simpleListHolder.listOfItems.getLayoutManager() == null)
                simpleListHolder.listOfItems.setLayoutManager(new CustomLinearLayoutManager(simpleListHolder.listOfItems.getContext(), LinearLayoutManager.HORIZONTAL, false));
            simpleListHolder.listOfItems.setAdapter(new MoreAdapter(smartSong.songList, cacheAdapterInterface, R.layout.song_grid_item,handOverPerformAction));
        }
    }

    @Override
    protected RecyclerView.ViewHolder newCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case SONG_ITEM_TYPE:
                return new SongItemHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.song_list_item, parent, false), new HandOverMessage<Pair<Integer, Integer>>() {
                    @Override
                    public void handOverMessage(@Nonnull Pair<Integer, Integer> position) {

                        if (position.first < 1)
                            throw new IllegalArgumentException(" Invalid position: " + (position.first - 1)
                                    + " provided in HandOverMessage of YourProfile->SongItemHolder");

                        final T message = cacheAdapterInterface.getItem(position.first - 1);
                        if (message instanceof Song) {
                            if(position.second == 1)
                            cacheAdapterInterface.handOverMessage((Song) message);
                            else
                                handOverPerformAction.performAction(1,(Song)message);

                        }
                        else
                            throw new IllegalArgumentException("Song item holder passed on an illegal value type");
                    }
                });
            case RECENT_LIST_TYPE:
                return new MoreListHolder(parent,
                        R.layout.list_with_more_button_padding, //Main resource id
                        R.id.headerText, //id for header text
                        R.id.listOfItems, //id for list (recycler view)
                        R.id.moreButton,
                        true); //id of more button

            case SMART_LIST_TYPE:
                return new MoreListHolder(parent,true);

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
        final Message message = cacheAdapterInterface.getItem(position);
        if (message instanceof Song)
            return cacheAdapterInterface.getItem(position).hashCode();
        else if (message instanceof RecentSong)
            return recentHolderId;
        else if (message instanceof SmartSong)
            return smartHolderId;
        else
            throw new IllegalArgumentException("Unknown message found in list");
    }

    @Override
    protected RecyclerView.ViewHolder inflatePlaceHolder(View view) {
        return new RecyclerView.ViewHolder(view) {
        };
    }

    @Override
    public void onClick(View v) {
        final int position = (int) v.getTag();
        final Song song = (Song) cacheAdapterInterface.getItem(position);
        handOverPerformAction.performAction(0,song);

    }

    public interface HandOverPerformActionTask{
        public void performAction(int action, Song song);
    }

}