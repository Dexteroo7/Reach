package reach.project.coreViews.yourProfile.music;

import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
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
class SongItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    public final TextView songName;
    public final TextView artistName;
    public final ImageView downButton;
    public final SimpleDraweeView albumArt;
    //public final ImageView extraButton;
    //public final ImageView likeButton;
    public final ImageView saveButton;
    private final HandOverMessage<Pair<Integer, Integer>> handOverMessage;
    //public final View divider;

    protected SongItemHolder(View itemView, HandOverMessage<Pair<Integer,Integer>> handOverMessage) {

        super(itemView);

        this.songName = (TextView) itemView.findViewById(R.id.songName);
        this.artistName = (TextView) itemView.findViewById(R.id.artistName);
        this.downButton = (ImageView) itemView.findViewById(R.id.downButton);
        this.albumArt = (SimpleDraweeView) itemView.findViewById(R.id.albumArt);
        //this.extraButton = (ImageView) itemView.findViewById(R.id.extraButton);
        //this.likeButton = (ImageView) itemView.findViewById(R.id.likeButton);
        this.saveButton = (ImageView) itemView.findViewById(R.id.saveSong);
        this.handOverMessage = handOverMessage;
        this.itemView.setTag(1);
        this.itemView.setOnClickListener(this);
        this.saveButton.setTag(2);
        this.saveButton.setOnClickListener(this);


        //this.divider = itemView.findViewById(R.id.divider);
        //itemView.setPadding(MiscUtils.dpToPx(8),MiscUtils.dpToPx(16), MiscUtils.dpToPx(8),0 );
    }

    @Override
    public void onClick(View v) {
        final int action = (Integer) v.getTag();
        handOverMessage.handOverMessage(new Pair<Integer,Integer>(getAdapterPosition(),action));

    }
}