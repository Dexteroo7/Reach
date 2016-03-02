package reach.project.player;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import reach.project.R;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 18/11/15.
 */
class SongItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

    public final TextView songName;
    public final TextView artistName;
    public final ImageView downButton;
    public final SimpleDraweeView albumArt;
    private final HandOverMessage<Integer> handOverMessage;


    protected SongItemHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        super(itemView);

        this.handOverMessage = handOverMessage;
        this.songName = (TextView) itemView.findViewById(R.id.songName);
        this.artistName = (TextView) itemView.findViewById(R.id.artistName);
        this.downButton = (ImageView) itemView.findViewById(R.id.downButton);
        this.albumArt = (SimpleDraweeView) itemView.findViewById(R.id.albumArt);
        this.itemView.setOnClickListener(this);
    }

    @Override
    public final void onClick(View v) {
        itemView.setSelected(true);
        handOverMessage.handOverMessage(getAdapterPosition());
    }
}