package reach.project.coreViews.fileManager.music.myLibrary;

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
import java.util.List;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.coreViews.fileManager.ReachDatabaseHelper;
import reach.project.coreViews.friends.HandOverMessageExtra;
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
class ParentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Closeable {

    public static final byte VIEW_TYPE_RECENT = 0;
    public static final byte VIEW_TYPE_ALL = 1;

    private final RecentAdapter recentAdapter;
    private final HandOverMessage<Cursor> handOverCursor;
    private final ResizeOptions resizeOptions = new ResizeOptions(150, 150);
    private final long recentHolderId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);

    private final HandOverMessageExtra<Object> handOverMessageExtra = new HandOverMessageExtra<Object>() {
        @Override
        public void handOverMessage(@Nonnull Integer position) {

            final Object object = getItem(position);
            if (object instanceof Cursor)
                ParentAdapter.this.handOverCursor.handOverMessage((Cursor) object);
            else
                throw new IllegalStateException("Resource cursor has been corrupted");
        }

        @Override
        public Cursor getExtra(@Nonnull Integer position) {

            final Object object = getItem(position);
            if (object instanceof Cursor)
                return (Cursor) object;
            else
                throw new IllegalStateException("Resource cursor has been corrupted");
        }
    };

    public ParentAdapter(HandOverMessage<Cursor> handOverCursor,
                         HandOverMessage<MusicData> handOverSong) {

        this.handOverCursor = handOverCursor;
        this.recentAdapter = new RecentAdapter(new ArrayList<>(20), handOverSong, R.layout.song_grid_item);
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

    public void updateRecentMusic(@NonNull List<MusicData> newRecent) {
        recentAdapter.updateRecent(newRecent);
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
                        .inflate(R.layout.song_list_item, parent, false), handOverMessageExtra);
            }

            case VIEW_TYPE_RECENT: {

                final MoreListHolder moreListHolder = new MoreListHolder(parent);
                moreListHolder.itemView.setBackgroundResource(0);
                moreListHolder.headerText.setText("Recently Added");
                moreListHolder.listOfItems.setLayoutManager(new CustomGridLayoutManager(moreListHolder.listOfItems.getContext(), 2));
                moreListHolder.listOfItems.setAdapter(recentAdapter);
                return moreListHolder;
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

            final Cursor cursorExactType = (Cursor) friend;
            final SongItemHolder songItemHolder = (SongItemHolder) holder;
            songItemHolder.position = cursorExactType.getPosition() + 1;
            holder.itemView.setBackgroundResource(0);

            final String displayName, artist, album, actualName;
            final boolean visible;
            if (cursorExactType.getColumnCount() == ReachDatabaseHelper.MUSIC_DATA_LIST.length) {

                displayName = cursorExactType.getString(5);
                artist = cursorExactType.getString(11);
                album = cursorExactType.getString(16);
                visible = cursorExactType.getShort(18) == 1;
//                actualName = cursorExactType.getString(17);
            } else if (cursorExactType.getColumnCount() == MySongsHelper.DISK_LIST.length) {

                displayName = cursorExactType.getString(3);
                artist = cursorExactType.getString(4);
                album = cursorExactType.getString(6);
                visible = cursorExactType.getShort(11) == 1;
//                actualName = cursorExactType.getString(9);
            } else
                throw new IllegalArgumentException("Unknown cursor type found");

            /*if (visible) {
                songItemHolder.toggleButton.setImageResource(R.drawable.icon_everyone);
                songItemHolder.toggleText.setText("Everyone");
            } else {
                songItemHolder.toggleButton.setImageResource(R.drawable.icon_locked);
                songItemHolder.toggleText.setText("Only Me");
            }*/

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
    public long getItemId(int position) {

        final Object item = getItem(position);
        if (item instanceof Cursor) {
            return ((Cursor)item).getLong(0); //_id || song_id
        } else
            return recentHolderId;
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
