package reach.project.coreViews.yourProfile.music;

import android.net.Uri;
import android.view.View;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.common.base.Optional;

import java.util.List;

import reach.project.music.Song;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreButtonAdapter;

/**
 * Created by dexter on 18/11/15.
 */
class ListAdapterWithMore extends MoreButtonAdapter<Song, SongItemHolder> {

    public ListAdapterWithMore(List<Song> messageList, HandOverMessage<Song> handOverMessage, int resourceId) {
        super(messageList, handOverMessage, resourceId);
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