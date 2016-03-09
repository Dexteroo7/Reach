//package reach.project.coreViews.push.music;
//
//import android.view.View;
//import android.widget.CheckBox;
//import android.widget.TextView;
//
//import com.facebook.drawee.view.SimpleDraweeView;
//
//import reach.project.R;
//import reach.project.utils.viewHelpers.HandOverMessage;
//import reach.project.utils.viewHelpers.SingleItemViewHolder;
//
///**
// * Created by dexter on 18/11/15.
// */
//class SongItemHolder extends SingleItemViewHolder {
//
//    public final TextView songName;
//    public final TextView artistName;
//    public final CheckBox checkBox;
//    public final SimpleDraweeView albumArt;
//    public final View mask;
//
//    protected SongItemHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
//
//        super(itemView, handOverMessage);
//
//        this.songName = (TextView) itemView.findViewById(R.id.songName);
//        this.artistName = (TextView) itemView.findViewById(R.id.artistName);
//        this.checkBox = (CheckBox) itemView.findViewById(R.id.checkBox);
//        this.albumArt = (SimpleDraweeView) itemView.findViewById(R.id.albumArt);
//        this.mask = itemView.findViewById(R.id.mask);
//    }
//}