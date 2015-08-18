package reach.project.adapter;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.swipe.adapters.CursorSwipeAdapter;
import com.google.common.base.Optional;

import java.io.File;
import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.database.contentProvider.ReachDatabaseProvider;
import reach.project.database.sql.ReachDatabaseHelper;
import reach.project.utils.MiscUtils;
import reach.project.utils.auxiliaryClasses.ReachDatabase;

/**
 * Created by dexter on 7/8/14.
 */
public class ReachQueueAdapter extends CursorSwipeAdapter {

    //TODO improve warnings

    private static WeakReference<ReachQueueAdapter> reference;
    public ReachQueueAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        reference = new WeakReference<>(this);
    }

    @Override
    public int getSwipeLayoutResourceId(int i) {
        return R.id.swipeLayout;
    }

    @Override
    public void closeAllItems() {
    }

    private final class ViewHolder {

        private final TextView songTitle, userName, songSize;
        private final ProgressBar progressBar;
        private final ImageView listToggle, pauseQueue, deleteQueue;

        private ViewHolder(ImageView albumArt,
                           TextView songTitle,
                           TextView userName,
                           TextView songSize,
                           ProgressBar progressBar,
                           ImageView listToggle,
                           ImageView pauseQueue,
                           ImageView deleteQueue) {

//            this.albumArt = albumArt;
            this.songTitle = songTitle;
            this.userName = userName;
            this.songSize = songSize;
            this.progressBar = progressBar;
            this.listToggle = listToggle;
            this.pauseQueue = pauseQueue;
            this.deleteQueue = deleteQueue;
        }
    }

    public void bindView(final View view, final Context context, final Cursor cursor) {

        final ViewHolder viewHolder = (ViewHolder) view.getTag();

        final long id = cursor.getLong(0);
        final long length = cursor.getLong(1);
//        final long senderId = cursor.getLong(2);
        final long processed = cursor.getLong(3);
//        final String path = cursor.getString(4);
        final String displayName = cursor.getString(5);
//        final String artistName = cursor.getString(6);

//        final boolean liked;
//        final String temp = cursor.getString(7);
//        liked = !TextUtils.isEmpty(temp) && temp.equals("1");

//        final long duration = cursor.getLong(8);

        ///////////////

        final short status = cursor.getShort(9);
        final short operationKind = cursor.getShort(10);
        final String userName = cursor.getString(11);

//        final long receiverId = cursor.getLong(2);
//        final short logicalClock = cursor.getShort(9);
//        final long songId = cursor.getLong(10);

        final boolean finished = (processed + 1400 >= length) ||
                status == ReachDatabase.FINISHED;
        ///////////////////////////////////
        /**
         * If download has finished no need to display pause button
         * Prevent last trickle downloads as they might give errors
         */
        ///////////////////////////////////
        viewHolder.userName.setText("");
        viewHolder.userName.setText("from " + userName);
        ///////////////////////////////////
        /**
         * If finished no need for pause button
         */
        if (finished) {
            viewHolder.progressBar.setVisibility(View.INVISIBLE);
            viewHolder.pauseQueue.setVisibility(View.GONE);
            viewHolder.songSize.setText(String.format("%.1f", (float) (length / 1024000.0f)) + " MB");
        } else {

            viewHolder.pauseQueue.setVisibility(View.VISIBLE);
            viewHolder.pauseQueue.setTag(id);
            viewHolder.pauseQueue.setOnClickListener(LocalUtils.pauseListener);

            if (status == ReachDatabase.WORKING || status == ReachDatabase.RELAY)
                viewHolder.progressBar.setProgressDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.reachq_progressbar, context.getTheme()));
            else
                viewHolder.progressBar.setProgressDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.reachq_progressbar_stop, context.getTheme()));

            viewHolder.songSize.setText((processed * 100 / length) + "%");
            viewHolder.progressBar.setProgress((int) ((processed * 100) / length));
            viewHolder.progressBar.setVisibility(View.VISIBLE);
        }

        if (operationKind == 0) {
            viewHolder.deleteQueue.setTag(new Object[]{id, cursor.getPosition()});
            viewHolder.deleteQueue.setOnClickListener(LocalUtils.deleteListener);
        } else {
            viewHolder.deleteQueue.setVisibility(View.GONE);
            viewHolder.listToggle.setVisibility(View.GONE);
            viewHolder.userName.setText("to " + userName);
        }

        if (status == ReachDatabase.PAUSED_BY_USER)
            viewHolder.pauseQueue.setImageResource(R.drawable.ic_file_resume_download_grey600_48dp);
        else
            viewHolder.pauseQueue.setImageResource(R.drawable.ic_file_pause_download_grey600_48dp);

        if (status == ReachDatabase.GCM_FAILED)
            viewHolder.songSize.setText("Network error, retry");
        else if (status == ReachDatabase.FILE_NOT_FOUND)
            viewHolder.songSize.setText("404, file not found");
        else if (status == ReachDatabase.FILE_NOT_CREATED)
            viewHolder.songSize.setText("Disk Error, retry");
        viewHolder.songTitle.setText(displayName);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        final View view = LayoutInflater.from(context).inflate(R.layout.reach_queue_item, parent, false);
        final ViewHolder viewHolder = new ViewHolder(
                (ImageView) view.findViewById(R.id.albumArt),
                (TextView) view.findViewById(R.id.songTitle),
                (TextView) view.findViewById(R.id.from),
                (TextView) view.findViewById(R.id.songSize),
                (ProgressBar) view.findViewById(R.id.progressBar),
                (ImageView) view.findViewById(R.id.listToggle),
                (ImageView) view.findViewById(R.id.pauseQueue),
                (ImageView) view.findViewById(R.id.deleteQueue));
        view.setTag(viewHolder);
        return view;
    }

    private enum LocalUtils {
        ;

        public static final View.OnClickListener deleteListener = new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                final Object[] tag = (Object[]) view.getTag();
                final long id = (long) tag[0];
                final int position = (int) tag[1];

                new AlertDialog.Builder(view.getContext())
                        .setMessage("Are you sure you want to delete it?")
                        .setPositiveButton("Yes", new ConfirmDelete(id, position))
                        .setNegativeButton("No", cancelDelete).create().show();
            }
        };

        private static final DialogInterface.OnClickListener cancelDelete = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
            }
        };

        private static final class ConfirmDelete implements DialogInterface.OnClickListener {

            final long id;
            final int position;

            private ConfirmDelete(long id, int position) {
                this.id = id;
                this.position = position;
            }

            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                /**
                 * Can not remove from memory cache just yet, because some operation might be underway
                 * in connection manager
                 **/

                final Context context = ((Dialog) dialogInterface).getContext();
                //find path and delete the file
                final Cursor pathCursor = context.getContentResolver().query(
                        ReachDatabaseProvider.CONTENT_URI,
                        new String[]{ReachDatabaseHelper.COLUMN_PATH},
                        ReachDatabaseHelper.COLUMN_ID + " = ?",
                        new String[]{id + ""}, null);

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
                        id + " " +
                        context.getContentResolver().delete(
                                Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + id),
                                ReachDatabaseHelper.COLUMN_ID + " = ?",
                                new String[]{id + ""}));

                if (reference != null) {

                    final CursorSwipeAdapter adapter = reference.get();
                    if (adapter != null)
                        adapter.closeItem(position);

                }
                dialogInterface.dismiss();
            }
        }

        public static final View.OnClickListener pauseListener = new View.OnClickListener() {

            /**
             * Resets the transaction, reset download only. Updates memory cache and disk table both.
             * Update happens in MiscUtils.startDownloadOperation() method.
             *
             * @param reachDatabase    the transaction to reset
             */
            private Optional<Runnable> reset(ReachDatabase reachDatabase,
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

            @Override
            public void onClick(View view) {

                final long id = (long) view.getTag();
                final Context context = view.getContext();
                final ContentResolver resolver = context.getContentResolver();

                final Cursor cursor = resolver.query(
                        ReachDatabaseProvider.CONTENT_URI,
                        ReachDatabaseHelper.projection,
                        ReachDatabaseHelper.COLUMN_ID + " = ?",
                        new String[]{id + ""}, null);

                if (cursor == null)
                    return;
                if (!cursor.moveToFirst()) {
                    cursor.close();
                    return;
                }

                final ReachDatabase database = ReachDatabaseHelper.cursorToProcess(cursor);
                final Uri uri = Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + id);

                ///////////////

                if (database.getStatus() != ReachDatabase.PAUSED_BY_USER) {

                    //pause operation (both upload/download case)
                    final ContentValues values = new ContentValues();
                    values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.PAUSED_BY_USER);
                    context.getContentResolver().update(
                            uri,
                            values,
                            ReachDatabaseHelper.COLUMN_ID + " = ?",
                            new String[]{id + ""});
                    Log.i("Ayush", "Pausing");
                } else if (database.getOperationKind() == 1) {

                    //un-paused upload operation
                    context.getContentResolver().delete(
                            uri,
                            ReachDatabaseHelper.COLUMN_ID + " = ?",
                            new String[]{id + ""});
                } else {

                    //un-paused download operation
                    final Optional<Runnable> optional = reset(database, resolver, context, uri);
                    if (optional.isPresent())
                        AsyncTask.THREAD_POOL_EXECUTOR.execute(optional.get());
                    else //should never happen
                        Toast.makeText(context, "Failed", Toast.LENGTH_SHORT);
                    Log.i("Ayush", "Un-pausing");
                }
            }
        };
    }
}