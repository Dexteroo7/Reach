package reach.project.coreViews.myProfile.music;

import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;
import reach.project.utils.viewHelpers.GetActualAdapter;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.ListHolder;

/**
 * Created by dexter on 25/11/15.
 */
class ParentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Closeable {

    private final GetActualAdapter getActualAdapter;
    private final HandOverMessage<Cursor> handOverCursor;

    public ParentAdapter(HandOverMessage<Cursor> handOverCursor,
                         HandOverMessage<PrivacySongItem> handOverSong,
                         GetActualAdapter getActualAdapter) {

        this.handOverCursor = handOverCursor;
        this.getActualAdapter = getActualAdapter;

        final List<PrivacySongItem> defaultList = new ArrayList<>(1);
        defaultList.add(new PrivacySongItem());
        recentAdapter = new RecentAdapter(defaultList, handOverSong, R.layout.song_mylibrary_grid_item);
        setHasStableIds(true);
    }

    public static final byte VIEW_TYPE_RECENT = 0;
    public static final byte VIEW_TYPE_ALL = 1;

    ///////////Recent music adapter
    private final RecentAdapter recentAdapter;

    public void updateRecentMusic(@NonNull List<PrivacySongItem> newRecent) {
        if (newRecent.isEmpty())
            return;
        recentAdapter.updateRecent(newRecent);
    }
    ///////////

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
        Log.i("Ayush", "Setting new download cursor");
        getActualAdapter.getActualAdapter().notifyDataSetChanged();
    }

    public void setNewMyLibraryCursor(@Nullable Cursor newMyLibraryCursor) {

        //destroy
        if (this.myLibraryCursor != null)
            myLibraryCursor.close();

        //set
        this.myLibraryCursor = newMyLibraryCursor;
        Log.i("Ayush", "Setting new library cursor");
        getActualAdapter.getActualAdapter().notifyDataSetChanged();
    }

    @Override
    public void close() {

        MiscUtils.closeQuietly(downloadCursor, myLibraryCursor);
        downloadCursor = myLibraryCursor = null;
//        getActualAdapter.getActualAdapter().notifyItemRangeRemoved(0, latestTotalCount);
    }
    ///////////

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case VIEW_TYPE_ALL: {

                return new SongItemHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.song_mylibrary_list_item, parent, false), position -> {

                    final Object object = getItem(position);
                    if (object instanceof Cursor)
                        handOverCursor.handOverMessage((Cursor) object);
                    else
                        throw new IllegalStateException("Position must correspond with a cursor");

                });
            }

            case VIEW_TYPE_RECENT: {
                return new ListHolder(parent, R.layout.list_with_more_button_padding);
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
            final boolean visible;

            displayName = cursorExactType.getString(3);
            artist = cursorExactType.getString(5);
            album = cursorExactType.getString(6);
            actualName = cursorExactType.getString(4);
            visible = cursorExactType.getShort(9) == 1;

            if (visible) {
                songItemHolder.toggleButton.setImageResource(R.drawable.icon_locked);
                songItemHolder.toggleText.setText("Everyone");
            }
            else {
                songItemHolder.toggleButton.setImageResource(R.drawable.icon_locked);
                songItemHolder.toggleText.setText("Only Me");
            }
            songItemHolder.songName.setText(displayName);
            songItemHolder.artistName.setText(artist);
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

            final ListHolder horizontalViewHolder = (ListHolder) holder;
            holder.itemView.setBackgroundResource(R.drawable.border_shadow3);
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

    private final Object[] reUsable = new Object[6];

    @Override
    public long getItemId(int position) {

        final Object item = getItem(position);
        if (item instanceof Cursor) {

            final Cursor cursor = (Cursor) item;
            reUsable[0] = cursor.getLong(1); //songId
            reUsable[1] = cursor.getLong(2); //userId
            reUsable[2] = cursor.getString(3);//displayName
            reUsable[3] = cursor.getString(4);//actualName
            reUsable[4] = cursor.getString(5); //artist
            reUsable[5] = cursor.getString(6); //album
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

        return latestTotalCount = myLibraryCount + downloadedCount + 1; //adjust for recent list
    }
}
