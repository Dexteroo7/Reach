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
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.common.base.Optional;

import java.io.File;

import reach.project.R;
import reach.project.music.ReachDatabase;
import reach.project.music.ReachDatabaseHelper;
import reach.project.music.ReachDatabaseProvider;
import reach.project.coreViews.friends.HandOverMessageExtra;
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
        final long reachDatabaseId = handOverMessageExtra.getExtra(position).getLong(0);
        final boolean isPaused = handOverMessageExtra.getExtra(position).getShort(9) == ReachDatabase.PAUSED_BY_USER;

        this.songName = (TextView) itemView.findViewById(R.id.songName);
        this.artistName = (TextView) itemView.findViewById(R.id.artistName);
        this.downProgress = (TextView) itemView.findViewById(R.id.downProgress);
        this.albumArt = (SimpleDraweeView) itemView.findViewById(R.id.albumArt);
        this.progressBar = (ProgressBar) itemView.findViewById(R.id.downloadProgress);
        this.optionsIcon = (ImageView) itemView.findViewById(R.id.optionsIcon);
        this.optionsIcon.setOnClickListener(view -> {

            if (position == -1)
                throw new IllegalArgumentException("Position not set for the view holder");

            final PopupMenu popupMenu = new PopupMenu(context, this.optionsIcon);
            popupMenu.inflate(R.menu.friends_popup_menu);
            popupMenu.getMenu().findItem(R.id.friends_menu_2).setTitle("Delete");
            final String status;
            status = isPaused ? "Resume Download" : "Pause Download";
            popupMenu.getMenu().findItem(R.id.friends_menu_1).setTitle(status);

            popupMenu.setOnMenuItemClickListener(item -> {

                switch (item.getItemId()) {
                    case R.id.friends_menu_1:
                        //pause
                        if (pause_unpause(reachDatabaseId, context))
                            item.setTitle("Resume Download");
                        else
                            item.setTitle("Pause Download");
                        return true;
                    case R.id.friends_menu_2:
                        //delete
                        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                                .setMessage("Are you sure you want to delete it?")
                                .setPositiveButton("Yes", handleClick)
                                .setNegativeButton("No", handleClick)
                                .setIcon(R.drawable.up_icon)
                                .create();

                        alertDialog.setOnShowListener(dialog -> alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTag(reachDatabaseId));
                        alertDialog.show();
                        return true;
                    default:
                        return false;
                }
            });
            popupMenu.show();
        });
    }

    //////////////////////////////

    /**
     * Pause / Unpause transaction
     */
    private static boolean pause_unpause(long reachDatabaseId, Context context) {

        final ContentResolver resolver = context.getContentResolver();
        final Uri uri = Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + reachDatabaseId);

        final Cursor cursor = resolver.query(
                uri,
                ReachDatabaseHelper.projection,
                ReachDatabaseHelper.COLUMN_ID + " = ?",
                new String[]{reachDatabaseId + ""}, null);

        if (cursor == null)
            return false;
        if (!cursor.moveToFirst()) {
            cursor.close();
            return false;
        }

        final ReachDatabase database = ReachDatabaseHelper.cursorToProcess(cursor);
        final boolean paused;

        ///////////////

        if (database.getStatus() != ReachDatabase.PAUSED_BY_USER) {

            //pause operation (both upload/download case)
            final ContentValues values = new ContentValues();
            values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.PAUSED_BY_USER);
            paused = context.getContentResolver().update(
                    uri,
                    values,
                    ReachDatabaseHelper.COLUMN_ID + " = ?",
                    new String[]{reachDatabaseId + ""}) > 0;
        } else if (database.getOperationKind() == 1) {

            //un-paused upload operation
            paused = context.getContentResolver().delete(
                    uri,
                    ReachDatabaseHelper.COLUMN_ID + " = ?",
                    new String[]{reachDatabaseId + ""}) > 0;
        } else {

            //un-paused download operation
            final Optional<Runnable> optional = reset(database, resolver, context, uri);
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
        reachDatabase.setStatus(ReachDatabase.NOT_WORKING);

        final ContentValues values = new ContentValues();
        values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.NOT_WORKING);
        values.put(ReachDatabaseHelper.COLUMN_LOGICAL_CLOCK, reachDatabase.getLogicalClock());

        final boolean updateSuccess = resolver.update(
                uri,
                values,
                ReachDatabaseHelper.COLUMN_ID + " = ?",
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

        /**
         * Can not remove from memory cache just yet, because some operation might be underway
         * in connection manager
         **/
        final AlertDialog alertDialog = (AlertDialog) dialog;
        final ContentResolver resolver = alertDialog.getContext().getContentResolver();
        final long reachDatabaseId = (long) alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).getTag();
        final Uri uri = Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + reachDatabaseId);

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