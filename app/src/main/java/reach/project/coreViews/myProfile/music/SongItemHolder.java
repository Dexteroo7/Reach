//package reach.project.coreViews.myProfile.music;
//
//import android.view.View;
//import android.widget.ImageView;
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
//    public final ImageView toggleButton;
//    public final ImageView toggleButton2;
//    public final TextView toggleText;
//    public final SimpleDraweeView albumArt;
//
//    protected SongItemHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
//
//        super(itemView, handOverMessage);
//
//        this.songName = (TextView) itemView.findViewById(R.id.songName);
//        this.artistName = (TextView) itemView.findViewById(R.id.artistName);
//        this.toggleText = (TextView) itemView.findViewById(R.id.toggleText);
//        this.toggleButton = (ImageView) itemView.findViewById(R.id.toggleButton);
//        this.toggleButton2 = (ImageView) itemView.findViewById(R.id.toggleButton2);
//        this.albumArt = (SimpleDraweeView) itemView.findViewById(R.id.albumArt);
//    }
//}