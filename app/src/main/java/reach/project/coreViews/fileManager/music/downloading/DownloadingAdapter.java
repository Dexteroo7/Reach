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

import reach.project.music.ReachDatabase;
import reach.project.coreViews.friends.HandOverMessageExtra;
import reach.project.music.SongCursorHelper;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.ReachCursorAdapter;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 23/11/15.
 */
class DownloadingAdapter extends ReachCursorAdapter<DownloadingItemHolder> {

    final ResizeOptions resizeOptions = new ResizeOptions(150, 150);

    DownloadingAdapter(HandOverMessage<Cursor> handOverMessage, int resourceId) {
        super(handOverMessage, resourceId);
    }

    private final HandOverMessageExtra<Cursor> handOverMessageExtra = new HandOverMessageExtra<Cursor>() {
        @Override
        public void handOverMessage(@Nonnull Integer position) {

            final Optional<Cursor> cursorOptional = getItem(position);
            if (cursorOptional.isPresent())
                DownloadingAdapter.this.handOverMessage.handOverMessage(cursorOptional.get());
            else
                throw new IllegalStateException("Resource cursor has been corrupted");
        }

        @Override
        public Cursor getExtra(@Nonnull Integer position) {

            final Optional<Cursor> cursorOptional = getItem(position);
            if (cursorOptional.isPresent())
                return cursorOptional.get();
            else
                throw new IllegalStateException("Resource cursor has been corrupted");
        }
    };

    @Override
    public DownloadingItemHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        Log.i("Ayush", "Creating new view holder" + DownloadingAdapter.class);
        return new DownloadingItemHolder(itemView, handOverMessageExtra);
    }

    @Override
    public long getItemId(@Nonnull Cursor cursor) {

        return cursor.getLong(0); //_id
    }

    @Override
    public void onBindViewHolder(DownloadingItemHolder holder, Cursor cursorExact) {

        holder.position = cursorExact.getPosition();

        final ReachDatabase reachDatabase = SongCursorHelper.DOWNLOADING_HELPER.parse(cursorExact);

        ///////////////

        final boolean finished = (reachDatabase.getProcessed() + 1400 >= reachDatabase.getLength()) ||
                reachDatabase.getStatus() == ReachDatabase.Status.FINISHED;
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

            int prog = (int) ((reachDatabase.getProcessed() * 100) / reachDatabase.getLength());
            holder.progressBar.setProgress(prog);
            holder.downProgress.setText(prog + "%");
            holder.progressBar.setVisibility(View.VISIBLE);

            switch (reachDatabase.getStatus()) {

                case FILE_NOT_CREATED: {
                    holder.downProgress.setText("File not found");
                    //The file for this song was moved / deleted from local storage
                    break;
                }

                case GCM_FAILED: {
                    holder.downProgress.setText("User deleted the app");
                    //Sender could not be notified to initiate upload
                    break;
                }

                case PAUSED_BY_USER: {
                    holder.downProgress.setText("Paused");
                    //The download has been paused
                    break;
                }

                case FILE_NOT_FOUND: {
                    holder.downProgress.setText("User deleted the file");
                    //The file could not be found on sender's side
                    break;
                }

                case NOT_WORKING: {

                    //Download has not started yet
                    break;
                }

                case RELAY: {

                    //Download is working
                    break;
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
        final Optional<Uri> uriOptional = AlbumArtUri.getUri(
                reachDatabase.getAlbumName(),
                reachDatabase.getArtistName(),
                reachDatabase.getDisplayName(), false);

        if (uriOptional.isPresent()) {

//                Log.i("Ayush", "Url found = " + uriOptional.get().toString());

            final ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uriOptional.get())
                    .setResizeOptions(resizeOptions)
                    .build();

            final DraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setOldController(holder.albumArt.getController())
                    .setImageRequest(request)
                    .build();

            holder.albumArt.setController(controller);
        } else
            holder.albumArt.setImageBitmap(null);
        holder.songName.setText(reachDatabase.getDisplayName());
        holder.artistName.setText(reachDatabase.getArtistName());
    }
}