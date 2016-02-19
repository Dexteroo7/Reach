package reach.project.player;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.common.base.Optional;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.music.MySongsHelper;
import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.ThreadLocalRandom;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreListHolder;

/**
 * Created by dexter on 25/11/15.
 */
class MusicListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Closeable {

    public static final byte VIEW_TYPE_RECENT = 0;
    public static final byte VIEW_TYPE_ALL = 1;
    private final HandOverMessage<Cursor> handOverCursor;
    private final ResizeOptions resizeOptions = new ResizeOptions(150, 150);
    private final long recentHolderId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);

    private final HandOverMessage<Integer> handOverMessage = new HandOverMessage<Integer>() {
        @Override
        public void handOverMessage(@Nonnull Integer position) {

            final Object object = getItem(position);
            if (object instanceof Cursor) {
                Cursor cursor = (Cursor) object;
                handOverCursor.handOverMessage(cursor);

                if (cursor.getColumnCount() == ReachDatabaseHelper.MUSIC_DATA_LIST.length) {

                    currentlyPlayingSongId = cursor.getLong(20);
                    notifyItemChanged(currentlyPlayingSongPosition);
                    notifyItemChanged(position);

                } else if (cursor.getColumnCount() == MySongsHelper.DISK_LIST.length) {
                    currentlyPlayingSongId = cursor.getLong(0);
                    notifyItemChanged(currentlyPlayingSongPosition);
                    notifyItemChanged(position);

                } else
                    throw new IllegalArgumentException("Unknown cursor type found");
            }
            else
                throw new IllegalStateException("Position must correspond with a cursor");
        }
    };
    private long currentlyPlayingSongId;
    private int currentlyPlayingSongPosition;


    public MusicListAdapter(HandOverMessage<Cursor> handOverCursor,
                            HandOverMessage<MusicData> handOverSong,
                            Context context, long currentlyPlayingSongId) {

        this.handOverCursor = handOverCursor;
        this.currentlyPlayingSongId = currentlyPlayingSongId;
        setHasStableIds(true);
    }

    ///////////Data set ops
    @Nullable
    private Cursor downloadCursor = null;
    @Nullable
    private Cursor myLibraryCursor = null;

    public int myLibraryCount = 0;
    public int downloadedCount = 0;

    public void setNewDownLoadCursor(@Nullable Cursor newDownloadCursor) {

        //destroy
        if (this.downloadCursor != null)
            downloadCursor.close();

        //set
        this.downloadCursor = newDownloadCursor;
        notifyDataSetChanged();
    }

    public void setNewMyLibraryCursor(@Nullable Cursor newMyLibraryCursor) {

        //destroy
        if (this.myLibraryCursor != null)
            myLibraryCursor.close();

        //set
        this.myLibraryCursor = newMyLibraryCursor;
        notifyDataSetChanged();
    }

    @Override
    public void close() {

        MiscUtils.closeQuietly(downloadCursor, myLibraryCursor);
        downloadCursor = myLibraryCursor = null;
    }
    ///////////

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Log.i("Ayush", "Creating ViewHolder " + getClass().getName());

        switch (viewType) {

            case VIEW_TYPE_ALL: {

                return new SongItemHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.music_list_song_item, parent, false), handOverMessage);
            }

            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        Log.i("Ayush", "Binding ViewHolder " + getClass().getName());

        final Object friend = getItem(position);
        if (friend instanceof Cursor) {
            holder.itemView.setSelected(false);
            final Cursor cursorExactType = (Cursor) friend;
            final SongItemHolder songItemHolder = (SongItemHolder) holder;
            //holder.itemView.setBackgroundResource(0);
            /*holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(),R.color.));*/
            final String displayName, artist, album, actualName;
            if (cursorExactType.getColumnCount() == ReachDatabaseHelper.MUSIC_DATA_LIST.length) {

                displayName = cursorExactType.getString(5);
                artist = cursorExactType.getString(11);
                album = cursorExactType.getString(16);
                if(cursorExactType.getLong(20)==currentlyPlayingSongId){
                    holder.itemView.setSelected(true);
                    currentlyPlayingSongPosition = position;
                };
//                actualName = cursorExactType.getString(17);
            } else if (cursorExactType.getColumnCount() == MySongsHelper.DISK_LIST.length) {

                displayName = cursorExactType.getString(3);
                artist = cursorExactType.getString(4);
                album = cursorExactType.getString(6);
                if(cursorExactType.getLong(0)==currentlyPlayingSongId){
                    holder.itemView.setSelected(true);
                    currentlyPlayingSongPosition = position;
                };
//                actualName = cursorExactType.getString(9);
            } else
                throw new IllegalArgumentException("Unknown cursor type found");

            songItemHolder.songName.setText(displayName);
            songItemHolder.artistName.setText(artist);

            final Optional<Uri> uriOptional = AlbumArtUri.getUri(album, artist, displayName, false);

            if (uriOptional.isPresent()) {

//            Log.i("Ayush", "Url found = " + uriOptional.get().toString());
                final ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uriOptional.get())
                        .setResizeOptions(resizeOptions)
                        .build();

                final DraweeController controller = Fresco.newDraweeControllerBuilder()
                        .setOldController(songItemHolder.albumArt.getController())
                        .setImageRequest(request)
                        .build();

                songItemHolder.albumArt.setController(controller);
            } else
                songItemHolder.albumArt.setImageBitmap(null);

            //use
        }
    }

    /**
     * Will either return Cursor object OR flag for recent list
     *
     * @param position position to load
     * @return object
     */
    @Nonnull
    private Object getItem(int position) {
        //account for recent shit

            if (position < downloadedCount) {

                if (downloadCursor == null || downloadCursor.isClosed() || !downloadCursor.moveToPosition(position))
                    throw new IllegalStateException("Resource cursor has been corrupted");
                return downloadCursor;

            } else {

                position -= downloadedCount; //adjust fot myLibrary
                if (myLibraryCursor == null || myLibraryCursor.isClosed() || !myLibraryCursor.moveToPosition(position))
                    throw new IllegalStateException("Resource cursor has been corrupted");
                return myLibraryCursor;

        }
    }

    @Override
    public int getItemViewType(int position) {

        /*final Object item = getItem(position);
        if (item instanceof Cursor)*/
            return VIEW_TYPE_ALL;
        /*else
            return VIEW_TYPE_RECENT;*/
    }

    @Override
    public long getItemId(int position) {

        final Object item = getItem(position);
        /*if (item instanceof Cursor) {*/
            return ((Cursor)item).getLong(0); //_id || song_id
        /*} else
            return recentHolderId;*/
    }

    @Override
    public int getItemCount() {

        if (downloadCursor != null && !downloadCursor.isClosed())
            downloadedCount = downloadCursor.getCount();
        else
            downloadedCount = 0;

        if (myLibraryCursor != null && !myLibraryCursor.isClosed())
            myLibraryCount = myLibraryCursor.getCount();
        else
            myLibraryCount = 0;

            return myLibraryCount + downloadedCount; //adjust for recent list

    }
}
