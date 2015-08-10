package reach.project.adapter;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.CursorSwipeAdapter;

import java.io.File;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.database.contentProvider.ReachDatabaseProvider;
import reach.project.database.sql.ReachDatabaseHelper;
import reach.project.utils.MiscUtils;
import reach.project.utils.auxiliaryClasses.ReachDatabase;

/**
 * Created by dexter on 7/8/14.
 */
public class ReachQueueAdapter extends CursorSwipeAdapter {

    //TODO improve warnings

    public ReachQueueAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public int getSwipeLayoutResourceId(int i) {
        return R.id.swipeLayout;
    }

    @Override
    public void closeAllItems() {
    }

    private final class ViewHolder{

        private final TextView songTitle ,userName, songSize;
        private final ProgressBar progressBar;
        private final ImageView albumArt ,listToggle, pauseQueue, deleteQueue;
        private final SwipeLayout swipeLayout;

        private ViewHolder(ImageView albumArt,
                           TextView songTitle,
                           TextView userName,
                           TextView songSize,
                           ProgressBar progressBar,
                           ImageView listToggle,
                           ImageView pauseQueue,
                           ImageView deleteQueue,
                           SwipeLayout swipeLayout) {

            this.albumArt = albumArt;
            this.songTitle = songTitle;
            this.userName = userName;
            this.songSize = songSize;
            this.progressBar = progressBar;
            this.listToggle = listToggle;
            this.pauseQueue = pauseQueue;
            this.deleteQueue = deleteQueue;
            this.swipeLayout = swipeLayout;
        }
    }

    public void bindView(final View view, final Context context, final Cursor cursor) {

        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        final long id = cursor.getLong(0);
        final long length = cursor.getLong(1);
        final long receiverId = cursor.getLong(2);
        final long processed = cursor.getLong(3);
        final String path = cursor.getString(4);
        final String displayName = cursor.getString(5);
        final short status = cursor.getShort(6);
        final short operationKind = cursor.getShort(7);
        final long senderId = cursor.getLong(8);
        final short logicalClock = cursor.getShort(9);
        final long songId = cursor.getLong(10);
        final String userName = cursor.getString(11);

        final boolean finished = (processed + 1400 >= length) ||
                                  status == ReachDatabase.FINISHED;
        ///////////////////////////////////
        /**
         * If download has finished no need to display pause button
         * Prevent last trickle downloads as they might give errors
         */
        ///////////////////////////////////
        viewHolder.userName.setText("");
        viewHolder.userName.setText(userName);
        ///////////////////////////////////
        /**
         * If finished no need for pause button
         */
        if (finished) {
            viewHolder.progressBar.setVisibility(View.INVISIBLE);
            viewHolder.pauseQueue.setVisibility(View.GONE);
            viewHolder.songSize.setText(String.format("%.1f", (float) (length / 1024000.0f)) + " MB");
        } else {

            viewHolder.pauseQueue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    final Uri uri = Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + id);

                    if (status != ReachDatabase.PAUSED_BY_USER) {

                        final ContentValues values = new ContentValues();
                        values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.PAUSED_BY_USER);
                        context.getContentResolver().update(uri, values,
                                ReachDatabaseHelper.COLUMN_ID + " = ?",
                                new String[]{id + ""});
                        Log.i("Ayush", "Pausing");
                    } else if (operationKind == 1) {

                        context.getContentResolver().delete(uri, ReachDatabaseHelper.COLUMN_ID + " = ?",
                                new String[]{id + ""});
                    } else {

                        final ContentValues values = new ContentValues();
                        values.put(ReachDatabaseHelper.COLUMN_STATUS, ReachDatabase.NOT_WORKING);
                        values.put(ReachDatabaseHelper.COLUMN_LOGICAL_CLOCK, logicalClock + 1);
                        context.getContentResolver().update(uri, values,
                                ReachDatabaseHelper.COLUMN_ID + " = ?",
                                new String[]{id + ""});
                        final ReachDatabase reachDatabase = new ReachDatabase();
                        reachDatabase.setSenderId(senderId);
                        reachDatabase.setReceiverId(receiverId);
                        reachDatabase.setSongId(songId);
                        reachDatabase.setProcessed(processed);
                        reachDatabase.setLength(length);
                        reachDatabase.setLogicalClock(logicalClock);
                        reachDatabase.setId(id);
                        StaticData.threadPool.submit(MiscUtils.startDownloadOperation(
                                context,
                                MiscUtils.generateRequest(reachDatabase),
                                reachDatabase.getReceiverId(), //myID
                                reachDatabase.getSenderId(),   //the uploaded
                                reachDatabase.getId()));
                        Log.i("Ayush", "Un-pausing");
                    }
                }
            });

            if (status == ReachDatabase.WORKING ||
                    status == ReachDatabase.RELAY ||
                    processed + 1400 >= length) {

                viewHolder.progressBar.setProgressDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.reachq_progressbar, context.getTheme()));
            }
            else {
                viewHolder.progressBar.setProgressDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.reachq_progressbar_stop, context.getTheme()));
            }

            viewHolder.songSize.setText((processed*100/length) + "%");
            viewHolder.progressBar.setProgress((int) ((processed * 100) / length));
            viewHolder.progressBar.setVisibility(View.VISIBLE);
        }

        if (operationKind == 0) {
            viewHolder.deleteQueue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder
                        .setMessage("Are you sure you want to delete it?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.i("Downloader", "Deleting " +
                                        id + " " +
                                        context.getContentResolver().delete(
                                                Uri.parse(ReachDatabaseProvider.CONTENT_URI + "/" + id),
                                                ReachDatabaseHelper.COLUMN_ID + " = ?",
                                                new String[]{id + ""}));
                                final File toDelete = new File(path);
                                Log.i("Ayush", "Deleting " + toDelete.delete());
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
        } else {
            viewHolder.deleteQueue.setVisibility(View.GONE);
            viewHolder.listToggle.setVisibility(View.GONE);
        }

        if (status == ReachDatabase.PAUSED_BY_USER)
            viewHolder.pauseQueue.setImageResource(R.drawable.restart);
        else
            viewHolder.pauseQueue.setImageResource(R.drawable.stop);

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
                (ImageView) view.findViewById(R.id.deleteQueue),
                (SwipeLayout) view.findViewById(getSwipeLayoutResourceId(0)));
        view.setTag(viewHolder);
        return view;
    }
}