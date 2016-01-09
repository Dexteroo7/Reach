package reach.project.coreViews.fileManager.music.downloading;

import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.common.base.Optional;

import javax.annotation.Nonnull;

import reach.project.coreViews.fileManager.ReachDatabase;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.ReachCursorAdapter;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 23/11/15.
 */
class DownloadingAdapter extends ReachCursorAdapter<DownloadingItemHolder> {

    public DownloadingAdapter(HandOverMessage<Cursor> handOverMessage, int resourceId) {
        super(handOverMessage, resourceId);
    }

    @Override
    public DownloadingItemHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        Log.i("Ayush", "Creating new view holder" + DownloadingAdapter.class);
        return new DownloadingItemHolder(itemView, handOverMessage);
    }

    @Override
    public long getItemId(@Nonnull Cursor cursor) {

        return cursor.getLong(0); //_id
    }

    @Override
    public void onBindViewHolder(DownloadingItemHolder holder, Cursor cursorExact) {

//        final long id = cursorExact.getLong(0);
        final long length = cursorExact.getLong(1);
//        final long senderId = cursor.getLong(2);
        final long processed = cursorExact.getLong(3);
//        final String path = cursor.getString(4);
        final String displayName = cursorExact.getString(5);
        final String artistName = cursorExact.getString(6);
        final String albumName = cursorExact.getString(15);

//        final boolean liked;
//        final String temp = cursor.getString(7);
//        liked = !TextUtils.isEmpty(temp) && temp.equals("1");

//        final long duration = cursor.getLong(8);

        ///////////////

        final short status = cursorExact.getShort(9);
//        final short operationKind = cursorExact.getShort(10);
//        final String userName = cursorExact.getString(11);

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

            switch (status) {

                case ReachDatabase.FILE_NOT_CREATED: {

                    //The file for this song was moved / deleted from local storage
                }

                case ReachDatabase.GCM_FAILED: {

                    //Sender could not be notified to initiate upload
                }

                case ReachDatabase.PAUSED_BY_USER: {

                    //The download has been paused
                }

                case ReachDatabase.FILE_NOT_FOUND: {

                    //The file could not be found on sender's side
                }

                case ReachDatabase.NOT_WORKING: {

                    //Download has not started yet
                }

                case ReachDatabase.RELAY: {

                    //Download is working
                }
            }

//            viewHolder.pauseQueue.setVisibility(View.VISIBLE);
//            viewHolder.pauseQueue.setTag(id);
//            viewHolder.pauseQueue.setOnClickListener(LocalUtils.pauseListener);

//            if (status == ReachDatabase.WORKING || status == ReachDatabase.RELAY)
//                holder.progressBar.setProgressDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.reachq_progressbar, context.getTheme()));
//            else
//                holder.progressBar.setProgressDrawable(ResourcesCompat.getDrawable(context.getResources(), R.drawable.reachq_progressbar_stop, context.getTheme()));

//            viewHolder.songSize.setText((processed * 100 / length) + "%");
            int prog = (int) ((processed * 100) / length);
            holder.progressBar.setProgress(prog);
            holder.downProgress.setText(prog + "%");
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
        final Optional<Uri> uriOptional = AlbumArtUri.getUri(albumName, artistName, displayName, false);

        if (uriOptional.isPresent()) {

//                Log.i("Ayush", "Url found = " + uriOptional.get().toString());

            final ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uriOptional.get())
                    .setResizeOptions(new ResizeOptions(50, 50))
                    .build();

            final DraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setOldController(holder.albumArt.getController())
                    .setImageRequest(request)
                    .build();

            holder.albumArt.setController(controller);
        } else
            holder.albumArt.setImageBitmap(null);
        holder.songName.setText(displayName);
        holder.artisName.setText(artistName);
    }
}
