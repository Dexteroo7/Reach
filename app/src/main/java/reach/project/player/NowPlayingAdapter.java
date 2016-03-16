package reach.project.player;

import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.view.View;

import com.facebook.imagepipeline.common.ResizeOptions;
import com.google.common.base.Optional;

import javax.annotation.Nonnull;

import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.ReachCursorAdapter;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 16/03/16.
 */
public class NowPlayingAdapter extends ReachCursorAdapter<SongItemHolder> {

    private final ResizeOptions resizeOptions = new ResizeOptions(150, 150);
    private static final String TAG = NowPlayingAdapter.class.getSimpleName();

    @Nullable
    private String currentPlayingHash = null;

    private int lastKnownPosition = 0;

    public NowPlayingAdapter(HandOverMessage<Cursor> handOverMessage,
                             int resourceId,
                             @Nullable String currentPlayingHash) {

        super(handOverMessage, resourceId);
        setHasStableIds(true);
        this.currentPlayingHash = currentPlayingHash;
    }

    public void setCurrentPlayingHash(@Nullable String currentPlayingHash) {
        this.currentPlayingHash = currentPlayingHash;
    }

    @Override
    public SongItemHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        return new SongItemHolder(itemView, handOverMessage);
    }

    @Override
    public void onBindViewHolder(SongItemHolder holder, Cursor item) {

        holder.itemView.setSelected(false);
        final String displayName, artist, album, hash;
        displayName = item.getString(3);
        artist = item.getString(5);
        album = item.getString(6);
        hash = item.getString(2);

        if (hash.equals(currentPlayingHash))
            holder.itemView.setSelected(true);

        holder.songName.setText(displayName);
        holder.artistName.setText(artist);

        final Optional<Uri> uriOptional = AlbumArtUri.getUri(album, artist, displayName, false);
        if (uriOptional.isPresent())
            MiscUtils.setUriToView(holder.albumArt, uriOptional.get(), resizeOptions);
        else
            holder.albumArt.setImageBitmap(null);
    }

    @Override
    public long getItemId(@Nonnull Cursor cursor) {
        return cursor.getString(2).hashCode();
    }

    public void updatePosition(int position) {

        notifyItemChanged(position);
        notifyItemChanged(lastKnownPosition);

        this.lastKnownPosition = position;
    }
}
