package reach.project.coreViews.fileManager.music.myLibrary;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.io.File;

import reach.project.R;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.fileManager.ReachDatabaseProvider;
import reach.project.coreViews.friends.HandOverMessageExtra;
import reach.project.music.MySongsHelper;
import reach.project.music.MySongsProvider;
import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Created by dexter on 18/11/15.
 */
class SongItemHolder extends SingleItemViewHolder {

    public final TextView songName, artistName;
    public final ImageView extraButton, likeButton;
    public final SimpleDraweeView albumArt, userImage;
    private static PopupMenu popupMenu;

    //must set this position
    int position = -1;

    protected SongItemHolder(View itemView, HandOverMessageExtra<Object> handOverMessageExtra) {

        super(itemView, handOverMessageExtra);

        this.songName = (TextView) itemView.findViewById(R.id.songName);
        this.artistName = (TextView) itemView.findViewById(R.id.artistName);
        this.extraButton = (ImageView) itemView.findViewById(R.id.extraButton);
        this.likeButton = (ImageView) itemView.findViewById(R.id.likeButton);
        this.albumArt = (SimpleDraweeView) itemView.findViewById(R.id.albumArt);
        this.userImage = (SimpleDraweeView) itemView.findViewById(R.id.userImage);

        final Context context = itemView.getContext();
        final Object object = handOverMessageExtra.getExtra(position);
        final ContentResolver resolver = context.getContentResolver();

        this.likeButton.setOnClickListener(v -> {
            if (object instanceof MusicData) {
                final MusicData musicData = (MusicData) object;
                musicData.setIsLiked(!musicData.isLiked());
            }
            else if (object instanceof Cursor) {
                final Cursor cursor = (Cursor) object;
                if (cursor.getColumnCount() == MySongsHelper.DISK_LIST.length) {
                    if (cursor.getShort(12) == 1)

                } else if (cursor.getColumnCount() == ReachDatabaseHelper.MUSIC_DATA_LIST.length) {
                    if (cursor.getString(7).equalsIgnoreCase("TRUE"))

                } else
                    throw new IllegalArgumentException("Unknown column count found");
            }
            else
                throw new IllegalArgumentException("Invalid Object type detected");
        });
        this.likeButton.setOnClickListener(v -> ((ImageView) v).setImageResource(R.drawable.icon_heart_pink));
        this.extraButton.setOnClickListener(v -> {
            if (position == -1)
                throw new IllegalArgumentException("Position not set for the view holder");

            popupMenu = new PopupMenu(context, this.extraButton);
            popupMenu.inflate(R.menu.manager_popup_menu);
            popupMenu.setOnMenuItemClickListener(item -> {

                switch (item.getItemId()) {
                    case R.id.manager_menu_1:
                        //send
                        return true;
                    case R.id.manager_menu_2:
                        //hide
                        return true;
                    case R.id.manager_menu_3:
                        //delete
                        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                                .setMessage("Are you sure you want to delete it?")
                                .setPositiveButton("Yes", handleClick)
                                .setNegativeButton("No", handleClick)
                                .create();
                        alertDialog.setOnShowListener(dialog -> alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTag(object));
                        alertDialog.show();
                        return true;
                    default:
                        return false;
                }
            });
            popupMenu.show();
        });
    }

    private static final DialogInterface.OnClickListener handleClick = (dialog, which) -> {

        if (which == AlertDialog.BUTTON_NEGATIVE) {
            dialog.dismiss();
            return;
        }

        /**
         * Can not remove from memory cache just yet, because some operation might be underway
         * in connection manager
         **/
        final AlertDialog alertDialog = (AlertDialog) dialog;
        final ContentResolver resolver = alertDialog.getContext().getContentResolver();

        final Object object = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).getTag();
        Uri contentUri = null;
        long reachDatabaseId = 0;
        if (object instanceof MusicData) {
            final MusicData musicData = (MusicData) object;
            if (musicData.getType() == MusicData.DOWNLOADED)
                contentUri = ReachDatabaseProvider.CONTENT_URI;
            else if (musicData.getType() == MusicData.MY_LIBRARY)
                contentUri = MySongsProvider.CONTENT_URI;
            else
                throw new IllegalArgumentException("Invalid MusicData type detected");
            reachDatabaseId = musicData.getId();
        }
        else if (object instanceof Cursor) {
            final Cursor cursor = (Cursor) object;
            if (cursor.getColumnCount() == MySongsHelper.DISK_LIST.length) {
                contentUri = MySongsProvider.CONTENT_URI;
                reachDatabaseId = cursor.getLong(7);
            } else if (cursor.getColumnCount() == ReachDatabaseHelper.MUSIC_DATA_LIST.length) {
                contentUri = ReachDatabaseProvider.CONTENT_URI;
                reachDatabaseId = cursor.getLong(0);
            } else
                throw new IllegalArgumentException("Unknown column count found");
        }
        else
            throw new IllegalArgumentException("Invalid Object type detected");

        if (contentUri == null || reachDatabaseId == 0)
            return;
        final Uri uri = Uri.parse(contentUri+ "/" + reachDatabaseId);

        //find path and delete the file
        final Cursor pathCursor = resolver.query(
                uri,
                new String[]{ReachDatabaseHelper.COLUMN_PATH},
                ReachDatabaseHelper.COLUMN_ID + " = ?",
                new String[]{reachDatabaseId + ""}, null);

        if (pathCursor != null) {

            if (pathCursor.moveToFirst()) {

                final String path = pathCursor.getString(0);
                if (!TextUtils.isEmpty(path) && !path.equals("hello_world")) {

                    final File toDelete = new File(path);
                    Log.i("Ayush", "Deleting " + toDelete.delete());
                }
            }
            pathCursor.close();
        }

        //delete the database entry
        Log.i("Downloader", "Deleting " +
                reachDatabaseId + " " +
                resolver.delete(
                        uri,
                        ReachDatabaseHelper.COLUMN_ID + " = ?",
                        new String[]{reachDatabaseId + ""}));
        dialog.dismiss();
    };
}