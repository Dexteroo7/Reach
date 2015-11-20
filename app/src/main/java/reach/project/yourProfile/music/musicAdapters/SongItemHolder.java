package reach.project.yourProfile.music.musicAdapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.squareup.wire.Message;

import reach.project.R;
import reach.project.music.Song;

/**
 * Created by dexter on 18/11/15.
 */
class SongItemHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener {

    public final TextView songName;
    public final TextView artistName;
    public final ImageView downButton;
    public final SimpleDraweeView albumArt;
    private final CacheAdapterInterface cacheAdapterInterface;

    private int position;

    protected SongItemHolder(View itemView, CacheAdapterInterface cacheAdapterInterface) {

        super(itemView);
        itemView.setOnClickListener(this);

        this.songName = (TextView) itemView.findViewById(R.id.songName);
        this.artistName = (TextView) itemView.findViewById(R.id.artistName);
        this.downButton = (ImageView) itemView.findViewById(R.id.downButton);
        this.albumArt = (SimpleDraweeView) itemView.findViewById(R.id.albumArt);
        this.cacheAdapterInterface = cacheAdapterInterface;
    }

    public void bindPosition(int position) {
        this.position = position;
    }

    @Override
    public void onClick(View v) {

        if (cacheAdapterInterface != null) {

            final Message message = cacheAdapterInterface.getItem(position);
            if (message instanceof Song)
                cacheAdapterInterface.handOverSongClick((Song) message);
            else
                throw new IllegalStateException("Song click found on non-Song object");

        }
    }
}