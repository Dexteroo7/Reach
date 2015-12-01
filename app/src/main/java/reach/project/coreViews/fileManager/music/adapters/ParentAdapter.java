package reach.project.coreViews.fileManager.music.adapters;

import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.music.MySongsHelper;
import reach.project.music.Song;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.ListHolder;

/**
 * Created by dexter on 25/11/15.
 */
public class ParentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements HandOverMessage<Song> {

    private final HandOverMessage<Cursor> handOverCursor;
    private final HandOverMessage<Song> handOverSong;

    public ParentAdapter(HandOverMessage<Cursor> handOverCursor,
                         HandOverMessage<Song> handOverSong) {

        this.handOverCursor = handOverCursor;
        this.handOverSong = handOverSong;
    }

    public static final byte VIEW_TYPE_RECENT = 0;
    public static final byte VIEW_TYPE_ALL = 1;

    ///////////All songs cursor
    @Nullable
    private Cursor downloadCursor = null;
    @Nullable
    private Cursor myLibraryCursor = null;
    private int downloadedCount = 0;
    @SuppressWarnings("FieldCanBeLocal")
    private int myLibraryCount = 0;
    private int latestTotalCount = 0;

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

    public void destroy() {

        MiscUtils.closeQuietly(downloadCursor, myLibraryCursor);
        downloadCursor = myLibraryCursor = null;
        notifyItemRangeRemoved(0, latestTotalCount);
    }
    ///////////

    ///////////Recent music adapter
    private final RecentAdapter recentAdapter;

    {
        final List<Song> defaultList = new ArrayList<>(1);
        defaultList.add(new Song.Builder().build());
        recentAdapter = new RecentAdapter(defaultList, this, R.layout.song_list_item);
    }

    public void updateRecentMusic(@NonNull List<Song> newRecent) {
        if (newRecent.isEmpty())
            return;
        recentAdapter.updateRecent(newRecent);
    }
    ///////////

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case VIEW_TYPE_ALL: {

                return new SongItemHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.song_list_item, parent, false), position -> {

                    position++; //adjust for recent
                    final Object object= getItem(position);
                    if (object instanceof Cursor)
                        handOverCursor.handOverMessage((Cursor) object);
                    else
                        throw new IllegalStateException("Position must correspond with a cursor");

                });
            }

            case VIEW_TYPE_RECENT: {
                return new ListHolder(parent);
            }

            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final Object friend = getItem(position);
        if (friend instanceof Cursor) {

            final Cursor cursorExactType = (Cursor) friend;
            final SongItemHolder songItemHolder = (SongItemHolder) holder;
            songItemHolder.bindPosition(position);

            final String displayName, artist, album, actualName;
            if (cursorExactType.getColumnCount() == ReachDatabaseHelper.ADAPTER_LIST.length) {

                displayName = cursorExactType.getString(5);
                artist = cursorExactType.getString(6);
                album = cursorExactType.getString(16);
                actualName = cursorExactType.getString(17);
            } else if (cursorExactType.getColumnCount() == MySongsHelper.DISK_LIST.length) {

                displayName = cursorExactType.getString(3);
                artist = cursorExactType.getString(4);
                album = cursorExactType.getString(6);
                actualName = cursorExactType.getString(9);
            } else
                throw new IllegalArgumentException("Unknown cursor type found");

            songItemHolder.songName.setText(displayName);
            songItemHolder.artistName.setText(artist);
            final Optional<Uri> uriOptional = AlbumArtUri.getUri(album, artist, displayName);

            if (uriOptional.isPresent()) {

//            Log.i("Ayush", "Url found = " + uriOptional.get().toString());
                final ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uriOptional.get())
                        .setResizeOptions(new ResizeOptions(200, 200))
                        .build();

                final DraweeController controller = Fresco.newDraweeControllerBuilder()
                        .setOldController(songItemHolder.albumArt.getController())
                        .setImageRequest(request)
                        .build();

                songItemHolder.albumArt.setController(controller);
            } else
                songItemHolder.albumArt.setImageBitmap(null);

            //use
        } else {

            final ListHolder horizontalViewHolder = (ListHolder) holder;
            horizontalViewHolder.headerText.setText("Recently Added");
            horizontalViewHolder.listOfItems.setLayoutManager(
                    new CustomLinearLayoutManager(holder.itemView.getContext(),
                            LinearLayoutManager.HORIZONTAL, false));
            horizontalViewHolder.listOfItems.setAdapter(recentAdapter);
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

        if (position == 0)
            return false; //recent

        else {

            position--; //account for recent shit

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
    }

    @Override
    public int getItemViewType(int position) {

        final Object item = getItem(position);
        if (item instanceof Cursor)
            return VIEW_TYPE_ALL;
        else
            return VIEW_TYPE_RECENT;
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

        return latestTotalCount = myLibraryCount + downloadedCount + 1; //adjust for recent list
    }

    @Override
    public void handOverMessage(@Nonnull Song song) {
        handOverSong.handOverMessage(song);
    }
}
