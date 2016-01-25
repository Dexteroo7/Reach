package reach.project.coreViews.fileManager.music.myLibrary;

import android.view.View;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import reach.project.R;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Created by dexter on 18/11/15.
 */
class SongItemHolder extends SingleItemViewHolder {

    public final TextView songName;
    public final TextView artistName;
    public final SimpleDraweeView downButton;
    public final SimpleDraweeView albumArt;

    protected SongItemHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        super(itemView, handOverMessage);

        this.songName = (TextView) itemView.findViewById(R.id.songName);
        this.artistName = (TextView) itemView.findViewById(R.id.artistName);
        this.downButton = (SimpleDraweeView) itemView.findViewById(R.id.downButton);
        this.albumArt = (SimpleDraweeView) itemView.findViewById(R.id.albumArt);
    }
}