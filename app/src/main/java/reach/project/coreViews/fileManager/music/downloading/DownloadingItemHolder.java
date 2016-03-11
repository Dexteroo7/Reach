package reach.project.coreViews.fileManager.music.downloading;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.common.base.Optional;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import reach.project.R;
import reach.project.coreViews.friends.HandOverMessageExtra;
import reach.project.music.ReachDatabase;
import reach.project.music.SongCursorHelper;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Created by dexter on 23/11/15.
 */
class DownloadingItemHolder extends SingleItemViewHolder implements View.OnClickListener {

    final TextView songName, artistName, downProgress;
    final SimpleDraweeView albumArt;
    final ProgressBar progressBar;
    final ImageView optionsIcon;

    //must set this position
    int position = -1;

    DownloadingItemHolder(View itemView, HandOverMessageExtra<Cursor> handOverMessageExtra) {

        super(itemView, handOverMessageExtra);

        final Context context = itemView.getContext();

        this.songName = (TextView) itemView.findViewById(R.id.songName);
        this.artistName = (TextView) itemView.findViewById(R.id.artistName);
        this.downProgress = (TextView) itemView.findViewById(R.id.downProgress);
        this.albumArt = (SimpleDraweeView) itemView.findViewById(R.id.albumArt);
        this.progressBar = (ProgressBar) itemView.findViewById(R.id.downloadProgress);
        this.optionsIcon = (ImageView) itemView.findViewById(R.id.optionsIcon);
        this.optionsIcon.setOnClickListener(view -> {

            if (position == -1)
                throw new IllegalArgumentException("Position not set for the view holder");

            final ReachDatabase reachDatabase = SongCursorHelper.DOWNLOADING_HELPER.parse(handOverMessageExtra.getExtra(position));
            final String status = reachDatabase.getStatus() == ReachDatabase.Status.PAUSED_BY_USER ? "Resume Download" : "Pause Download";

            final PopupMenu popupMenu = new PopupMenu(context, this.optionsIcon);
            popupMenu.getMenu().findItem(R.id.friends_menu_2).setTitle("Delete");
            popupMenu.getMenu().findItem(R.id.friends_menu_1).setTitle(status);
            popupMenu.inflate(R.menu.manager_popup_menu);
            popupMenu.setOnMenuItemClickListener(item -> {

                switch (item.getItemId()) {

                    case R.id.manager_menu_1:
                        //send
                        return true;
                    case R.id.manager_menu_3:
                        //delete
                        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                                .setMessage("Are you sure you want to delete it?")
                                .setPositiveButton("Yes", handleClick)
                                .setNegativeButton("No", handleClick)
                                .setIcon(R.drawable.up_icon)
                                .create();

                        alertDialog.setOnShowListener(dialog -> alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTag(reachDatabase));
                        alertDialog.show();
                        return true;
                    case R.id.manager_menu_4:
                        //pause
                        pause_unpause(reachDatabase, context);
                        return true;
                    default:
                        return false;
                }
            });

            final MenuItem pauseItem = popupMenu.getMenu().findItem(R.id.manager_menu_4);
            popupMenu.getMenu().findItem(R.id.manager_menu_2).setVisible(false);
            pauseItem.setVisible(true);
            pauseItem.setTitle(status);
            popupMenu.show();
        });
    }

    //////////////////////////////

    /**
     * Pause / Unpause transaction
     */

    private static boolean pause_unpause(ReachDatabase reachDatabase, Context context) {

        final ContentResolver resolver = context.getContentResolver();
        final Uri uri = Uri.parse(SongProvider.CONTENT_URI + "/" + reachDatabase.getId());
        final boolean paused;

        ///////////////

        if (reachDatabase.getStatus() != ReachDatabase.Status.PAUSED_BY_USER) {

            //pause operation (both upload/download case)
            final ContentValues values = new ContentValues();
            values.put(SongHelper.COLUMN_STATUS, ReachDatabase.Status.PAUSED_BY_USER.getValue());
            paused = context.getContentResolver().update(
                    uri,
                    values,
                    SongHelper.COLUMN_ID + " = ?",
                    new String[]{reachDatabase.getId() + ""}) > 0;
        } else if (reachDatabase.getOperationKind() == ReachDatabase.OperationKind.UPLOAD_OP) {

            //un-paused upload operation
            paused = context.getContentResolver().delete(
                    uri,
                    SongHelper.COLUMN_ID + " = ?",
                    new String[]{reachDatabase.getId() + ""}) > 0;
        } else {

            //un-paused download operation
            final Optional<Runnable> optional = reset(reachDatabase, resolver, context, uri);
            if (optional.isPresent()) {
                AsyncTask.SERIAL_EXECUTOR.execute(optional.get());
                paused = false;
            } else { //should never happen
                Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
                paused = true;
            }
            Log.i("Ayush", "Un-pausing");
        }

        Log.i("Ayush", "Pause status " + paused);
        return paused;
    }

    /**
     * Resets the transaction, reset download only. Updates memory cache and disk table both.
     * Update happens in MiscUtils.startDownloadOperation() method.
     *
     * @param reachDatabase the transaction to reset
     */
    private static Optional<Runnable> reset(ReachDatabase reachDatabase,
                                            ContentResolver resolver,
                                            Context context,
                                            Uri uri) {

        reachDatabase.setLogicalClock((short) (reachDatabase.getLogicalClock() + 1));
        reachDatabase.setStatus(ReachDatabase.Status.NOT_WORKING);

        final ContentValues values = new ContentValues();
        values.put(SongHelper.COLUMN_STATUS, ReachDatabase.Status.NOT_WORKING.getValue());
        values.put(SongHelper.COLUMN_LOGICAL_CLOCK, reachDatabase.getLogicalClock());

        final boolean updateSuccess = resolver.update(
                uri,
                values,
                SongHelper.COLUMN_ID + " = ?",
                new String[]{reachDatabase.getId() + ""}) > 0;

        if (updateSuccess)
            //send REQ gcm
            return Optional.of((Runnable) MiscUtils.startDownloadOperation(
                    context,
                    reachDatabase,
                    reachDatabase.getReceiverId(), //myID
                    reachDatabase.getSenderId(),   //the uploaded
                    reachDatabase.getId()));

        return Optional.absent(); //update failed !
    }

    private static final DialogInterface.OnClickListener handleClick = (dialog, which) -> {

        if (which == AlertDialog.BUTTON_NEGATIVE) {
            dialog.dismiss();
            return;
        }

        final AlertDialog alertDialog = (AlertDialog) dialog;
        final ContentResolver resolver = alertDialog.getContext().getContentResolver();

        final ReachDatabase reachDatabase = (ReachDatabase) alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).getTag();
        final Uri uri = Uri.parse(SongProvider.CONTENT_URI + "/" + reachDatabase.getMetaHash());

        //find path and delete the file
        final Cursor pathCursor = resolver.query(
                uri,
                new String[]{SongHelper.COLUMN_PATH},
                SongHelper.COLUMN_META_HASH + " = ?",
                new String[]{reachDatabase.getMetaHash() + ""}, null);

        if (pathCursor != null) {

            if (pathCursor.moveToFirst()) {

                final String path = pathCursor.getString(0);
                if (!TextUtils.isEmpty(path) && !path.equals("hello_world")) {

                    final File toDelete = new File(path);
                    try {
                        final RandomAccessFile randomAccessFile = new RandomAccessFile(toDelete, "rws");
                        randomAccessFile.setLength(0);
                        randomAccessFile.close();
                    } catch (IOException ignored) {
                    } finally {

                        toDelete.delete();
                        toDelete.deleteOnExit();
                    }
                }
            }
            pathCursor.close();
        }

        //delete the database entry
        final boolean deleted = resolver.delete(
                uri,
                SongHelper.COLUMN_META_HASH + " = ?",
                new String[]{reachDatabase.getMetaHash() + ""}) > 0;

        Log.i("Downloader", "Deleting " + reachDatabase.getDisplayName() + " " + deleted);
        dialog.dismiss();
    };
}