package reach.project.coreViews.yourProfile.music;

import android.net.Uri;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.common.base.Optional;
import java.lang.ref.WeakReference;
import java.util.List;
import reach.project.R;
import reach.project.music.Song;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;

/**
 * Created by dexter on 18/11/15.
 */
class MoreAdapter extends SimpleRecyclerAdapter<Song, SongItemHolder> implements MoreQualifier {

    public MoreAdapter(List<Song> messageList, HandOverMessage<Song> handOverMessage, int resourceId, ParentAdapter.HandOverPerformActionTask performActionTask) {
        super(messageList, handOverMessage, resourceId,performActionTask);
    }

    @Override
    public int getItemCount() {
        int size = super.getItemCount();
        return size < 4 ? size : 4;
    }

    @Override
    public SongItemHolder getViewHolder(View itemView, HandOverMessage<Pair<Integer,Integer>> handOverMessage) {
        return new SongItemHolder(itemView, handOverMessage);
    }

    @Override
    public long getItemId(Song item) {
        return item.songId;
    }

    @Override
    public void onBindViewHolder(SongItemHolder holder, Song item) {
        holder.songName.setText(item.displayName);
        //holder.itemView.setBackgroundResource(0);
        //holder.likeButton.setVisibility(View.INVISIBLE);
        //holder.downButton.setImageResource(R.drawable.icon_download_gray);
        holder.artistName.setText(item.artist);
        //holder.extraButton.setVisibility(View.INVISIBLE);
        final Optional<Uri> uriOptional = AlbumArtUri.getUri(item.album, item.artist, item.displayName, false);

        if (uriOptional.isPresent()) {
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
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        //ignored as updates will not happen
    }
}