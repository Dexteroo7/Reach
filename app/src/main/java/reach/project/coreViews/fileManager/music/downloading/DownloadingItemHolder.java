package reach.project.coreViews.fileManager.music.downloading;

import android.support.v7.widget.PopupMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import reach.project.R;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Created by dexter on 23/11/15.
 */
class DownloadingItemHolder extends SingleItemViewHolder implements View.OnClickListener {

    public final TextView songName;
    public final TextView artisName;
    public final TextView downProgress;
    public final SimpleDraweeView albumArt;
    public final ProgressBar progressBar;
    public final ImageView optionsIcon;
    public PopupMenu popupMenu;

    public DownloadingItemHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        super(itemView, handOverMessage);

        this.songName = (TextView) itemView.findViewById(R.id.songName);
        this.artisName = (TextView) itemView.findViewById(R.id.artistName);
        this.downProgress = (TextView) itemView.findViewById(R.id.downProgress);
        this.albumArt = (SimpleDraweeView) itemView.findViewById(R.id.albumArt);
        this.progressBar = (ProgressBar) itemView.findViewById(R.id.downloadProgress);
        this.optionsIcon = (ImageView) itemView.findViewById(R.id.optionsIcon);
        this.optionsIcon.setOnClickListener(popupListener);

        this.popupMenu = new PopupMenu(itemView.getContext(), optionsIcon);
        this.popupMenu.inflate(R.menu.downloading_menu);
        this.popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.downloading_menu_1:
                    //pause
                    return true;
                case R.id.downloading_menu_2:
                    //delete
                    return true;
                default:
                    return false;
            }
        });
    }

    private final View.OnClickListener popupListener = v -> popupMenu.show();

}
