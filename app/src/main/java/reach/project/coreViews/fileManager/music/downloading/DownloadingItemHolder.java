package reach.project.coreViews.fileManager.music.downloading;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.coreViews.friends.HandOverWithContext;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Created by dexter on 23/11/15.
 */
class DownloadingItemHolder extends SingleItemViewHolder implements View.OnClickListener {

    public final TextView songName, artisName, downProgress;
    public final SimpleDraweeView albumArt;
    public final ProgressBar progressBar;
    public ImageView optionsIcon = null;

    @Nullable
    private static WeakReference<HandOverWithContext> reference = null;

    public DownloadingItemHolder(View itemView, HandOverWithContext handOverWithContext) {

        super(itemView, handOverWithContext);
        reference = new WeakReference<>(handOverWithContext);

        this.songName = (TextView) itemView.findViewById(R.id.songName);
        this.artisName = (TextView) itemView.findViewById(R.id.artistName);
        this.downProgress = (TextView) itemView.findViewById(R.id.downProgress);
        this.albumArt = (SimpleDraweeView) itemView.findViewById(R.id.albumArt);
        this.progressBar = (ProgressBar) itemView.findViewById(R.id.downloadProgress);
        this.optionsIcon = (ImageView) itemView.findViewById(R.id.optionsIcon);
        this.optionsIcon.setOnClickListener(popupListener);
        this.optionsIcon.setTag(null);
    }

    public DownloadingItemHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        super(itemView, handOverMessage);
        reference = null; //not used

        this.songName = (TextView) itemView.findViewById(R.id.songName);
        this.artisName = (TextView) itemView.findViewById(R.id.artistName);
        this.downProgress = (TextView) itemView.findViewById(R.id.downProgress);
        this.albumArt = (SimpleDraweeView) itemView.findViewById(R.id.albumArt);
        this.progressBar = (ProgressBar) itemView.findViewById(R.id.downloadProgress);
        this.optionsIcon = null;
    }

    private final View.OnClickListener popupListener = view -> {
        final Context context = view.getContext();
        final int position = getAdapterPosition();

        final Object object = view.getTag();
        final PopupMenu popupMenu;
        if (object == null) {
            popupMenu = new PopupMenu(itemView.getContext(), this.optionsIcon);
            popupMenu.inflate(R.menu.friends_popup_menu);
            view.setTag(popupMenu);
        }
        else
            popupMenu = (PopupMenu) object;

        final Cursor cursor = MiscUtils.useReference(reference, handOverWithContext -> {
            return handOverWithContext.getCursor(position);
        }).orNull();

        if (cursor == null)
            return;

        popupMenu.setOnMenuItemClickListener(item -> {

            final WeakReference<ContentResolver> weakReference = new WeakReference<>(context.getContentResolver());

            switch (item.getItemId()) {
                case R.id.friends_menu_1:
                    //pause
                    return true;
                case R.id.friends_menu_2:
                    //cancel
                    return true;
                default:
                    return false;
            }
        });

        popupMenu.show();
    };

}
