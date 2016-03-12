package reach.project.coreViews.fileManager.music.myLibrary;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
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
import reach.project.coreViews.fileManager.HandOverMessageExtra;
import reach.project.music.Song;
import reach.project.music.SongCursorHelper;
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

    private static final String TAG = ParentAdapter.class.getSimpleName();

    private static final byte VIEW_TYPE_RECENT = 0;
    private static final byte VIEW_TYPE_ALL = 1;
    private static final byte VIEW_TYPE_DOWNLOADING = 2;

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

        @Override
        public void putExtra(@Nonnull Integer position, Song song) {

        }
    };

    public ParentAdapter(HandOverMessage<Cursor> handOverCursor,
                         HandOverMessage<Song> handOverSong) {

        this.handOverCursor = handOverCursor;
        this.recentAdapter = new RecentAdapter(new ArrayList<>(20), handOverSong, R.layout.song_grid_item);
        setHasStableIds(true);
    }

    ///////////Data set ops
    @Nullable
    private Cursor myLibraryCursor = null;

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

    @Override
    public void close() {

        MiscUtils.closeQuietly(myLibraryCursor);
        myLibraryCursor = null;
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
            final Song song = SongCursorHelper.SONG_HELPER.parse((Cursor) friend);
            final SongItemHolder songItemHolder = (SongItemHolder) holder;

            songItemHolder.position = cursorExactType.getPosition() + 1;
            holder.itemView.setBackgroundResource(0);

            final String displayName, artist, album, actualName;
            final boolean visible;
            //if (cursorExactType.getColumnCount() == SongHelper.MUSIC_DATA_LIST.length) {

                displayName = cursorExactType.getString(3);
                artist = cursorExactType.getString(5);
                album = cursorExactType.getString(6);
                visible = cursorExactType.getShort(12) == 1;
//                actualName = cursorExactType.getString(17);
                final long senderId = cursorExactType.getLong(16);
                final String senderName = cursorExactType.getString(17);

                final Context context = holder.itemView.getContext();
                songItemHolder.userImage.setVisibility(View.VISIBLE);
                songItemHolder.artistName.setTextColor(ContextCompat.getColor(context, R.color.reach_color));
                /*final Cursor cursor = context.getContentResolver().query(
                        Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + senderId),
                        new String[]{ReachFriendsHelper.COLUMN_USER_NAME,
                                ReachFriendsHelper.COLUMN_IMAGE_ID},
                        ReachFriendsHelper.COLUMN_ID + " = ?",
                        new String[]{senderId + ""}, null);
                if (cursor == null)
                    return;
                if (!cursor.moveToFirst()) {
                    cursor.close();
                    return;
                }*/
                songItemHolder.artistName.setText(artist);
                final int length = MiscUtils.dpToPx(20);
                songItemHolder.userImage.setImageURI(AlbumArtUri.getUserImageUri(
                        senderId,
                        "imageId",
                        "rw",
                        true,
                        length,
                        length));
            final String liked = cursorExactType.getString(13);
            Log.d(TAG, "Position = " + position + " liked = " + liked);
            if (liked== null || liked.equals("0")){
                songItemHolder.likeButton.setSelected(false);
            }
            else{
                songItemHolder.likeButton.setSelected(true);
            }

                /*songItemHolder.likeButton.setImageResource(cursorExactType.getString(13).equalsIgnoreCase("TRUE")
                        ? R.drawable.icon_heart_pink : R.drawable.icon_heart_outline_pink);*/
            /*} else if (cursorExactType.getColumnCount() == MySongsHelper.DISK_LIST.length) {

                displayName = cursorExactType.getString(3);
                artist = cursorExactType.getString(4);
                album = cursorExactType.getString(6);
                visible = cursorExactType.getShort(11) == 1;

                songItemHolder.userImage.setVisibility(View.GONE);
                songItemHolder.artistName.setTextColor(Color.parseColor("#878691"));
                songItemHolder.artistName.setText(artist);
                songItemHolder.likeButton.setImageResource(cursorExactType.getShort(12) == 1
                        ? R.drawable.icon_heart_outline_pink : R.drawable.icon_heart_outline_grayer);
            } else
                throw new IllegalArgumentException("Unknown cursor type found");*/

            /*if (visible) {
                songItemHolder.toggleButton.setImageResource(R.drawable.icon_everyone);
                songItemHolder.toggleText.setText("Everyone");
            } else {
                songItemHolder.toggleButton.setImageResource(R.drawable.icon_locked);
                songItemHolder.toggleText.setText("Only Me");
            }*/

            songItemHolder.songName.setText(displayName);

            final Optional<Uri> uriOptional = AlbumArtUri.getUri(song.album, song.artist, song.displayName, false);

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

            if (myLibraryCursor == null || myLibraryCursor.isClosed() || !myLibraryCursor.moveToPosition(position))
                throw new IllegalStateException("Resource cursor has been corrupted");
            return myLibraryCursor;
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
            return ((Cursor) item).getLong(0); //_id || song_id
        } else
            return recentHolderId;
    }

    @Override
    public int getItemCount() {

        if (myLibraryCursor != null && !myLibraryCursor.isClosed())
            return myLibraryCursor.getCount() + 1;//adjust for recent list
        return 0;
    }
}
