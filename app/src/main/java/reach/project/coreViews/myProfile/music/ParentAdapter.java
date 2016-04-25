package reach.project.coreViews.myProfile.music;

import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import java.util.List;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.music.Song;
import reach.project.music.SongCursorHelper;
import reach.project.music.SongHelper;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.ThreadLocalRandom;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreListHolder;
import reach.project.utils.viewHelpers.RecyclerViewMaterialAdapter;

/**
 * Created by dexter on 25/11/15.
 */
class ParentAdapter extends RecyclerViewMaterialAdapter<RecyclerView.ViewHolder> implements Closeable {

    private static final byte VIEW_TYPE_RECENT = 0;
    private static final byte VIEW_TYPE_ALL = 1;
    private static final String TAG = ParentAdapter.class.getSimpleName();

    private final RecentAdapter recentAdapter;
    private final HandOverMessage<Cursor> handOverCursor;
    private final ResizeOptions resizeOptions = new ResizeOptions(150, 150);
    private final long recentHolderId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);

    private final HandOverMessage<Integer> handOverMessage = new HandOverMessage<Integer>() {
        @Override
        public void handOverMessage(@Nonnull Integer position) {
            handOverCursor.handOverMessage((Cursor) getItem(position-1));
        }
    };

    public ParentAdapter(HandOverMessage<Cursor> handOverCursor,
                         HandOverMessage<Song> handOverSong) {

        this.handOverCursor = handOverCursor;
        this.recentAdapter = new RecentAdapter(new ArrayList<>(20), handOverSong, R.layout.song_mylibrary_grid_item);
        setHasStableIds(true);
    }

    ///////////Data-set ops
   /* @Nullable
    private Cursor downloadCursor = null;*/
    @Nullable
    private Cursor myLibraryCursor = null;

    public int myLibraryCount = 0;

    /*public void setNewDownLoadCursor(@Nullable Cursor newDownloadCursor) {

        //destroy
        if (downloadCursor != null)
            downloadCursor.close();

        //set
        downloadCursor = newDownloadCursor;
//        Log.i("Ayush", "Setting new download cursor");
        notifyDataSetChanged();
    }*/

    public void setNewMyLibraryCursor(@Nullable Cursor newMyLibraryCursor) {

        //destroy
        if (myLibraryCursor != null)
            myLibraryCursor.close();

        //set
        myLibraryCursor = newMyLibraryCursor;
//        Log.i("Ayush", "Setting new library cursor");
        notifyDataSetChanged();
    }

    public void updateRecentMusic(@NonNull List<Song> newRecent) {
        recentAdapter.updateRecent(newRecent);
    }

    /**
     * MUST CALL FROM UI THREAD
     *
     *
     */
    public synchronized void updateVisibility(String metaHash, boolean newVisibility) {
        recentAdapter.updateVisibility(metaHash, newVisibility);
    }

    @Override
    public void close() {

        MiscUtils.closeQuietly( myLibraryCursor);
        myLibraryCursor = null;
//        getActualAdapter.getActualAdapter().notifyItemRangeRemoved(0, latestTotalCount);
    }
    ///////////

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

            if (myLibraryCursor == null || myLibraryCursor.isClosed() || !myLibraryCursor.moveToPosition(position))
                throw new IllegalStateException("Resource cursor has been corrupted");
            return myLibraryCursor;
        }
    }

    ////////////////////

    @Override
    protected void newBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        final Object object = getItem(position);
        if (object instanceof Cursor) {

            final Cursor cursorExactType = (Cursor) object;
            final SongItemHolder songItemHolder = (SongItemHolder) holder;

            final String displayName, artist, album, actualName;
            final boolean visible;

           /* {

                SongHelper.COLUMN_ID, //0
                        SongHelper.COLUMN_UNIQUE_ID, //1
                        SongHelper.COLUMN_META_HASH, //2

                        SongHelper.COLUMN_DISPLAY_NAME, //3
                        SongHelper.COLUMN_ACTUAL_NAME, //4
                        SongHelper.COLUMN_ARTIST, //5
                        SongHelper.COLUMN_ALBUM, //6

                        SongHelper.COLUMN_DURATION, //7
                        SongHelper.COLUMN_SIZE, //8

                        SongHelper.COLUMN_GENRE, //9
                        SongHelper.COLUMN_PATH, //10
                        SongHelper.COLUMN_DATE_ADDED, //11

                        SongHelper.COLUMN_VISIBILITY, //12
                        SongHelper.COLUMN_IS_LIKED, //13
                        SongHelper.COLUMN_USER_NAME, //14
                        SongHelper.COLUMN_PROCESSED, //15
                        SongHelper.COLUMN_SENDER_ID, //16
                        SongHelper.COLUMN_USER_NAME //17

            }*/

            displayName = cursorExactType.getString(3);
            actualName = cursorExactType.getString(4);
            artist = cursorExactType.getString(5);
            album = cursorExactType.getString(6);
            visible = cursorExactType.getShort(12) == 1;

            if (visible) {
                songItemHolder.toggleButton.setImageResource(R.drawable.icon_everyone);
                songItemHolder.toggleText.setText("Everyone");
            } else {
                songItemHolder.toggleButton.setImageResource(R.drawable.icon_locked);
                songItemHolder.toggleText.setText("Only Me");
            }
            songItemHolder.songName.setText(displayName);
            songItemHolder.artistName.setText(artist);
            final Optional<Uri> uriOptional = AlbumArtUri.getUri(album, artist, displayName, false);

            if (uriOptional.isPresent()) {

            Log.i(TAG, "Url found = " + uriOptional.get().toString());
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
        } else {

            final MoreListHolder horizontalViewHolder = (MoreListHolder) holder;
            //holder.itemView.setBackgroundResource(R.drawable.border_shadow2);
            horizontalViewHolder.headerText.setText("Recently Added");
            if (horizontalViewHolder.listOfItems.getLayoutManager() == null)
                horizontalViewHolder.listOfItems.setLayoutManager(new CustomGridLayoutManager(horizontalViewHolder.listOfItems.getContext(), 2));
            horizontalViewHolder.listOfItems.setAdapter(recentAdapter);
        }
    }

    @Override
    protected RecyclerView.ViewHolder newCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case VIEW_TYPE_ALL: {

                return new SongItemHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.song_mylibrary_list_item, parent, false), handOverMessage);
            }

            case VIEW_TYPE_RECENT: {
                return new MoreListHolder(parent,
                        R.layout.list_with_more_button_padding,
                        R.id.headerText,
                        R.id.listOfItems,
                        R.id.moreButton,
                        false
                        );
            }

            default:
                return null;
        }
    }

    @Override
    protected int newGetItemCount() {

        /*if (downloadCursor != null && !downloadCursor.isClosed())
            downloadedCount = downloadCursor.getCount();
        else
            downloadedCount = 0;

        if (myLibraryCursor != null && !myLibraryCursor.isClosed())
            myLibraryCount = myLibraryCursor.getCount();
        else
            myLibraryCount = 0;

//        Log.i("Ayush", "Total size = " + (myLibraryCount + downloadedCount + 1));

        if (myLibraryCount + downloadedCount == 0)
            return 0;
        else
            return myLibraryCount + downloadedCount + 1; //adjust for recent list*/

        if (myLibraryCursor != null && !myLibraryCursor.isClosed())
            return (myLibraryCount = myLibraryCursor.getCount()) + 1;//adjust for recent list
        return 1;

    }

    @Override
    protected int newGetItemViewType(int position) {

        final Object item = getItem(position);
        if (item instanceof Cursor)
            return VIEW_TYPE_ALL;
        else
            return VIEW_TYPE_RECENT;
    }

    @Override
    protected long newGetItemId(int position) {

        final Object item = getItem(position);
        if (item instanceof Cursor)
            return ((Cursor) item).getLong(1); //song_id || unique_id
        else
            return recentHolderId;
    }

    @Override
    protected RecyclerView.ViewHolder inflatePlaceHolder(View view) {
        return new RecyclerView.ViewHolder(view) {
        };
    }
}
