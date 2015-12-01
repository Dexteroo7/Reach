package reach.project.coreViews.fileManager.music.downloading;

import android.database.Cursor;
import android.view.View;

import java.util.Arrays;

import javax.annotation.Nonnull;

import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.utils.ReachCursorAdapter;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 23/11/15.
 */
public class DownloadingAdapter extends ReachCursorAdapter<DownloadingItemHolder> {

    public DownloadingAdapter(HandOverMessage<Cursor> handOverMessage, int resourceId) {
        super(handOverMessage, resourceId);
    }

    @Override
    public DownloadingItemHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        return new DownloadingItemHolder(itemView, handOverMessage);
    }

    @Override
    public int getItemId(@Nonnull Cursor cursor) {

        final long id = cursor.getLong(0);
        final long length = cursor.getLong(1);
        final long processed = cursor.getLong(3);
        final long senderId = cursor.getLong(2);

        return Arrays.hashCode(new long[]{
                id,
                length,
                processed,
                senderId
        });
    }

    @Override
    public void onBindViewHolder(DownloadingItemHolder holder, Cursor cursorExact) {

        final long id = cursorExact.getLong(0);
        final long length = cursorExact.getLong(1);
//        final long senderId = cursor.getLong(2);
        final long processed = cursorExact.getLong(3);
//        final String path = cursor.getString(4);
        final String displayName = cursorExact.getString(5);
//        final String artistName = cursor.getString(6);

//        final boolean liked;
//        final String temp = cursor.getString(7);
//        liked = !TextUtils.isEmpty(temp) && temp.equals("1");

//        final long duration = cursor.getLong(8);

        ///////////////

        final short status = cursorExact.getShort(9);
        final short operationKind = cursorExact.getShort(10);
        final String userName = cursorExact.getString(11);

//        final byte[] albumArtData = cursor.getBlob(15);
//
//        if (albumArtData != null && albumArtData.length > 0) {
//
//            final AlbumArtData artData;
//            try {
//                artData = new Wire(AlbumArtData.class).parseFrom(albumArtData, AlbumArtData.class);
//                if (artData != null)
//                    Log.i("Ayush", "ReachQueue Adapter " + artData.toString());
//            } catch (IOException ignored) {
//            }
//        }

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
//        viewHolder.userName.setText("");
//        viewHolder.userName.setText("from " + userName);
        ///////////////////////////////////
        /**
         * If finished no need for pause button
         */
        if (finished) {
            holder.progressBar.setVisibility(View.INVISIBLE);
//            viewHolder.pauseQueue.setVisibility(View.GONE);
//            viewHolder.songSize.setText(String.format("%.1f", (float) (length / 1024000.0f)) + " MB");
        } else {

//            viewHolder.pauseQueue.setVisibility(View.VISIBLE);
//            viewHolder.pauseQueue.setTag(id);
//            viewHolder.pauseQueue.setOnClickListener(LocalUtils.pauseListener);

//            if (status == ReachDatabase.WORKING || status == ReachDatabase.RELAY)
//                holder.progressBar.setProgressDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.reachq_progressbar, context.getTheme()));
//            else
//                holder.progressBar.setProgressDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.reachq_progressbar_stop, context.getTheme()));

//            viewHolder.songSize.setText((processed * 100 / length) + "%");
            holder.progressBar.setProgress((int) ((processed * 100) / length));
            holder.progressBar.setVisibility(View.VISIBLE);
        }

//        if (operationKind == 0) {
//            viewHolder.deleteQueue.setTag(new Object[]{id, cursor.getPosition()});
//            viewHolder.deleteQueue.setOnClickListener(LocalUtils.deleteListener);
//        } else {
//            viewHolder.deleteQueue.setVisibility(View.GONE);
//            viewHolder.listToggle.setVisibility(View.GONE);
//            viewHolder.userName.setText("to " + userName);
//        }

//        if (status == ReachDatabase.PAUSED_BY_USER)
//            viewHolder.pauseQueue.setImageResource(R.drawable.ic_file_resume_download_grey600_48dp);
//        else
//            viewHolder.pauseQueue.setImageResource(R.drawable.ic_file_pause_download_grey600_48dp);
//
//        if (status == ReachDatabase.GCM_FAILED)
//            viewHolder.songSize.setText("Network error, retry");
//        else if (status == ReachDatabase.FILE_NOT_FOUND)
//            viewHolder.songSize.setText("404, file not found");
//        else if (status == ReachDatabase.FILE_NOT_CREATED)
//            viewHolder.songSize.setText("Disk Error, retry");
        holder.songName.setText(displayName);
    }
}
