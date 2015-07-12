package reach.project.adapter;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.ResourceCursorAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.database.ReachDatabase;
import reach.project.database.contentProvider.ReachDatabaseProvider;
import reach.project.database.sql.ReachDatabaseHelper;
import reach.project.utils.MiscUtils;

/**
 * Created by dexter on 7/8/14.
 */
public class ReachQueueAdapter extends ResourceCursorAdapter {

    //TODO improve warnings

    public ReachQueueAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
    }

    private final class ViewHolder{

        private final TextView songTitle ,userName, songSize, netType, progressText;
        private final ProgressBar progressBar;
        private final ImageView albumArt, networkStatus;
        private final ImageButton delete,pause;

        private ViewHolder(ImageView albumArt,
                           TextView songTitle,
                           TextView userName,
                           TextView songSize,
                           TextView netType,
                           TextView progressText,
                           ProgressBar progressBar,
                           ImageView networkStatus,
                           ImageButton delete,
                           ImageButton pause) {

            this.albumArt = albumArt;
            this.songTitle = songTitle;
            this.userName = userName;
            this.songSize = songSize;
            this.netType = netType;
            this.progressText = progressText;
            this.progressBar = progressBar;
            this.networkStatus = networkStatus;
            this.delete = delete;
            this.pause = pause;
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
        viewHolder.netType.setText("");
        viewHolder.networkStatus.setImageBitmap(null);
        viewHolder.userName.setText(userName);
//
        if(!finished) {

//            final short onlineStatus = Short.parseShort(cursor.getString(12));
            short networkType = -1;
            try {
                if (cursor.getString(13)!=null && !cursor.getString(13).equals("hello_world"))
                    networkType = Short.parseShort(cursor.getString(13));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            if (networkType == 1) {
                Picasso.with(context).load(R.drawable.wifi).into(viewHolder.networkStatus);
                viewHolder.netType.setText("");
            } else if (networkType > 1 && networkType < 5) {
                Picasso.with(context).load(R.drawable.phone).into(viewHolder.networkStatus);
                viewHolder.netType.setTextColor(context.getResources().getColor(R.color.darkgrey));
                viewHolder.netType.setText(networkType + "G");
            } else if (networkType == 5 && status == ReachDatabase.NOT_WORKING) {
                Picasso.with(context).load(R.drawable.phone).into(viewHolder.networkStatus);
                viewHolder.netType.setTextColor(Color.RED);
                viewHolder.netType.setText("Uploads disabled");
            } else {
                viewHolder.networkStatus.setImageBitmap(null);
                viewHolder.netType.setText("");
            }
        }
        ///////////////////////////////////
        /**
         * If finished no need for pause button
         */
        if (finished) {

            viewHolder.pause.setOnClickListener(null);
            viewHolder.pause.setVisibility(View.INVISIBLE);
            viewHolder.progressBar.setVisibility(View.INVISIBLE);
            viewHolder.progressText.setText("");
            viewHolder.songSize.setText(String.format("%.1f", (float) (length / 1024000.0f)) + " MB");
        } else {

            viewHolder.pause.setOnClickListener(new View.OnClickListener() {
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
                        StaticData.threadPool.submit(MiscUtils.startDownloadOperation(reachDatabase, view.getContext().getContentResolver()));
                        Log.i("Ayush", "Un-pausing");
                    }
                }
            });

            if (status == ReachDatabase.WORKING ||
                status == ReachDatabase.RELAY ||
                processed + 1400 >= length) {

                viewHolder.progressBar.setProgressDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.circle_progress_green, context.getTheme()));
            }
            else {
                viewHolder.progressBar.setProgressDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.circle_progress_red, context.getTheme()));
            }

            viewHolder.songSize.setText("");
            viewHolder.progressText.setText(String.format("%.2f", (float) (processed / 1024000.0f)) + "/" +
                    String.format("%.1f", (float) (length / 1024000.0f)) + " MB");
            viewHolder.progressBar.setProgress((int) ((processed * 100) / length));
            viewHolder.pause.setVisibility(View.VISIBLE);
            viewHolder.progressBar.setVisibility(View.VISIBLE);
        }

        if (operationKind == 0) {
            if (status == ReachDatabase.PAUSED_BY_USER)
                viewHolder.pause.setImageResource(R.drawable.restart_brown);
            else
                viewHolder.pause.setImageResource(R.drawable.stop_brown);
            viewHolder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder
                        .setMessage("Are you sure you want to delete it?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                /**
                                 * Can not remove from memory cache just yet, because some operation might be underway
                                 * in connection manager
                                 */
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
            viewHolder.albumArt.setImageResource(R.drawable.note_white);
            viewHolder.albumArt.setBackgroundResource(R.drawable.button_background);
            if (status == ReachDatabase.PAUSED_BY_USER)
                viewHolder.pause.setImageResource(R.drawable.reach_q_play_selector);
            else
                viewHolder.pause.setImageResource(R.drawable.reach_q_pause_selector);
            viewHolder.delete.setOnClickListener(null);
            viewHolder.delete.setVisibility(View.INVISIBLE);
        }

        if (status == ReachDatabase.GCM_FAILED)
            viewHolder.progressText.setText("Network error, retry");
        else if (status == ReachDatabase.FILE_NOT_FOUND)
            viewHolder.progressText.setText("404, file not found");
        else if (status == ReachDatabase.FILE_NOT_CREATED)
            viewHolder.progressText.setText("Disk Error, retry");
        viewHolder.songTitle.setText(displayName);
    }

//    public static class DeleteDialog extends DialogFragment {
//        @Override
//        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//            final View rootView = inflater.inflate(R.layout.update_dialog, container, false);
//            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
//            TextView textView = (TextView) rootView.findViewById(R.id.textView);
//            textView.setText("Are you sure\nyou wish to delete it?");
//            Button yes  = (Button) rootView.findViewById(R.id.yes);
//            Button no  = (Button) rootView.findViewById(R.id.no);
//            yes.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//
//                    dismiss();
//                }
//            });
//            no.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    dismiss();
//                }
//            });
//            return rootView;
//        }
//    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        final View view = super.newView(context, cursor, parent);
        final ViewHolder viewHolder = new ViewHolder(
                (ImageView) view.findViewById(R.id.albumArt),
                (TextView) view.findViewById(R.id.songTitle),
                (TextView) view.findViewById(R.id.from),
                (TextView) view.findViewById(R.id.songSize),
                (TextView) view.findViewById(R.id.netType),
                (TextView) view.findViewById(R.id.progressText),
                (ProgressBar) view.findViewById(R.id.progressBar),
                (ImageView) view.findViewById(R.id.status),
                (ImageButton) view.findViewById(R.id.delete_btn),
                (ImageButton) view.findViewById(R.id.play_pause));
        view.setTag(viewHolder);
        return view;
    }
}