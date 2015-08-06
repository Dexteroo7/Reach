package reach.project.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import reach.project.R;
import reach.project.utils.auxiliaryClasses.ReachDatabase;

/**
 * Created by dexter on 7/8/14.
 */
public class ReachQueueAdapter extends ResourceCursorAdapter {

    //TODO improve warnings

    public ReachQueueAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
    }

    private final class ViewHolder{

        private final TextView songTitle ,userName, songSize;
        private final ProgressBar progressBar;
        private final ImageView albumArt ,listToggle;

        private ViewHolder(ImageView albumArt,
                           TextView songTitle,
                           TextView userName,
                           TextView songSize,
                           ProgressBar progressBar,
                           ImageView listToggle) {

            this.albumArt = albumArt;
            this.songTitle = songTitle;
            this.userName = userName;
            this.songSize = songSize;
            this.progressBar = progressBar;
            this.listToggle = listToggle;
        }
    }

    public void bindView(final View view, final Context context, final Cursor cursor) {

        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        final long id = cursor.getLong(0);
        final long length = cursor.getLong(1);
        final long processed = cursor.getLong(3);
        final String path = cursor.getString(4);
        final String displayName = cursor.getString(5);
        final short status = cursor.getShort(6);
        final short operationKind = cursor.getShort(7);
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
            //viewHolder.progressText.setText("");
            viewHolder.songSize.setText(String.format("%.1f", (float) (length / 1024000.0f)) + " MB");
        } else {

            /*if (status == ReachDatabase.WORKING ||
                status == ReachDatabase.RELAY ||
                processed + 1400 >= length) {

                viewHolder.progressBar.setProgressDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.circle_progress_green, context.getTheme()));
            }
            else {
                viewHolder.progressBar.setProgressDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.circle_progress_red, context.getTheme()));
            }*/

            viewHolder.songSize.setText((processed*100/length) + "%");
            /*viewHolder.progressText.setText(String.format("%.2f", (float) (processed / 1024000.0f)) + "/" +
                    String.format("%.1f", (float) (length / 1024000.0f)) + " MB");*/
            viewHolder.progressBar.setProgress((int) ((processed * 100) / length));
            viewHolder.progressBar.setVisibility(View.VISIBLE);
        }

        if (operationKind == 0) {
            /*viewHolder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder
                        .setMessage("Are you sure you want to delete it?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
<<<<<<< HEAD

=======
                                *//**
                                 * Can not remove from memory cache just yet, because some operation might be underway
                                 * in connection manager
                                 *//*
>>>>>>> 25793b11e7ea60ad5e95ea7e24763831ec13c7bc
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
            });*/
        } else {
            /*viewHolder.albumArt.setImageResource(R.drawable.note_white);
            viewHolder.albumArt.setBackgroundResource(R.drawable.button_background);
            viewHolder.delete.setOnClickListener(null);
            viewHolder.delete.setVisibility(View.INVISIBLE);*/
            viewHolder.listToggle.setVisibility(View.GONE);
        }

        /*if (status == ReachDatabase.GCM_FAILED)
            viewHolder.progressText.setText("Network error, retry");
        else if (status == ReachDatabase.FILE_NOT_FOUND)
            viewHolder.progressText.setText("404, file not found");
        else if (status == ReachDatabase.FILE_NOT_CREATED)
            viewHolder.progressText.setText("Disk Error, retry");*/
        viewHolder.songTitle.setText(displayName);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        final View view = super.newView(context, cursor, parent);
        final ViewHolder viewHolder = new ViewHolder(
                (ImageView) view.findViewById(R.id.albumArt),
                (TextView) view.findViewById(R.id.songTitle),
                (TextView) view.findViewById(R.id.from),
                (TextView) view.findViewById(R.id.songSize),
                (ProgressBar) view.findViewById(R.id.progressBar),
                (ImageView) view.findViewById(R.id.listToggle));
        view.setTag(viewHolder);
        return view;
    }
}