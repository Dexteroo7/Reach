package reach.project.coreViews.push.music;

import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.common.base.Optional;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.core.ReachActivity;
import reach.project.music.Song;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreListHolder;

/**
 * Created by dexter on 25/11/15.
 */
class ParentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Closeable {

    private final RecentAdapter recentAdapter;
    private final HandOverMessage<Cursor> handOverCursor;

    public ParentAdapter(HandOverMessage<Cursor> handOverCursor,
                         HandOverMessage<Song> handOverSong) {

        this.handOverCursor = handOverCursor;
        recentAdapter = new RecentAdapter(new ArrayList<>(20), handOverSong, R.layout.push_song_grid_item);
        setHasStableIds(true);
    }

    public static final byte VIEW_TYPE_RECENT = 0;
    public static final byte VIEW_TYPE_ALL = 1;

    ///////////Data set ops
    @Nullable
    private Cursor downloadCursor = null;
    @Nullable
    private Cursor myLibraryCursor = null;

    int myLibraryCount = 0;
    int downloadedCount = 0;

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

    public void updateRecentMusic(@NonNull List<Song> newRecent) {
        recentAdapter.updateRecent(newRecent);
    }

    public void setItemSelected(int position, long songId) {

        if (position == -1)
            notifyDataSetChanged();
        else
            notifyItemChanged(position);

        recentAdapter.toggleSelected(songId);
    }
    @Override
    public void close() {

        MiscUtils.closeQuietly(downloadCursor, myLibraryCursor);
        downloadCursor = myLibraryCursor = null;
    }
    ///////////

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case VIEW_TYPE_ALL: {

                return new SongItemHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.push_song_list_item, parent, false), position -> {

                    final Object object = getItem(position);
                    if (object instanceof Cursor)
                        handOverCursor.handOverMessage((Cursor) object);
                    else
                        throw new IllegalStateException("Position must correspond with a cursor");

                });
            }

            case VIEW_TYPE_RECENT: {
                return new MoreListHolder(parent);
            }

            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        //TODO reduce cursor overhead
        final Object friend = getItem(position);
        if (friend instanceof Cursor) {

            final Cursor cursorExactType = (Cursor) friend;
            final SongItemHolder songItemHolder = (SongItemHolder) holder;
            holder.itemView.setBackgroundResource(0);
            songItemHolder.bindPosition(position);

            final String displayName, artist, album, actualName;
            final boolean selected;

            displayName = cursorExactType.getString(1);
            artist = cursorExactType.getString(4);
            album = cursorExactType.getString(3);
            actualName = cursorExactType.getString(2);
            selected = ReachActivity.SELECTED_SONG_IDS.get(cursorExactType.getLong(0), false);

//            Log.i("Ayush", "Selected state " + displayName + " " + selected);

            songItemHolder.songName.setText(displayName);
            songItemHolder.artistName.setText(artist);
            songItemHolder.checkBox.setChecked(selected);
            songItemHolder.mask.setVisibility(selected ? View.VISIBLE : View.GONE);

            final Optional<Uri> uriOptional = AlbumArtUri.getUri(album, artist, displayName, false);

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

            final MoreListHolder horizontalViewHolder = (MoreListHolder) holder;
            holder.itemView.setBackgroundResource(0);
            horizontalViewHolder.headerText.setText("Recently Added");
            horizontalViewHolder.listOfItems.setLayoutManager(
                    new CustomGridLayoutManager(holder.itemView.getContext(), 2));
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

    final Object[] reUsable = new Object[4];

    @Override
    public long getItemId(int position) {

        final Object item = getItem(position);
        if (item instanceof Cursor) {

            final Cursor cursor = (Cursor) item;

            final long songId = cursor.getLong(0);

            reUsable[0] = songId;
            reUsable[1] = ReachActivity.SELECTED_SONG_IDS.get(songId, false);
            reUsable[2] = cursor.getShort(7); //visibility
            reUsable[3] = cursor.getString(1); //displayName

            return Arrays.hashCode(reUsable);
        } else
            return super.getItemId(position);
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

        return myLibraryCount + downloadedCount + 1; //adjust for recent list
    }
}
