package reach.project.coreViews.yourProfile.music;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import reach.project.R;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Created by dexter on 18/11/15.
 */
class SongItemHolder extends SingleItemViewHolder {

    public final TextView songName;
    public final TextView artistName;
    public final ImageView downButton;
    public final SimpleDraweeView albumArt;
    public final ImageView extraButton;
    public final ImageView likeButton;
    //public final View divider;

    protected SongItemHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        super(itemView, handOverMessage);

        this.songName = (TextView) itemView.findViewById(R.id.songName);
        this.artistName = (TextView) itemView.findViewById(R.id.artistName);
        this.downButton = (ImageView) itemView.findViewById(R.id.downButton);
        this.albumArt = (SimpleDraweeView) itemView.findViewById(R.id.albumArt);
        this.extraButton = (ImageView) itemView.findViewById(R.id.extraButton);
        this.likeButton = (ImageView) itemView.findViewById(R.id.likeButton);
        //this.divider = itemView.findViewById(R.id.divider);
        //itemView.setPadding(MiscUtils.dpToPx(8),MiscUtils.dpToPx(16), MiscUtils.dpToPx(8),0 );
    }
}