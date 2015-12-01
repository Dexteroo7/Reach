package reach.project.coreViews.fileManager.music.downloading;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import reach.project.R;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Created by dexter on 23/11/15.
 */
class DownloadingItemHolder extends SingleItemViewHolder {

    public final TextView songName;
    public final SimpleDraweeView albumArt;
    public final ProgressBar progressBar;

    public DownloadingItemHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        super(itemView, handOverMessage);

        this.songName = (TextView) itemView.findViewById(R.id.songName);
        this.albumArt = (SimpleDraweeView) itemView.findViewById(R.id.albumArt);
        this.progressBar = (ProgressBar) itemView.findViewById(R.id.downloadProgress);
    }
}
