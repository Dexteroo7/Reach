package reach.project.coreViews.saved_songs;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.common.base.Optional;

import java.io.Closeable;
import java.util.List;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.music.Song;
import reach.project.music.SongCursorHelper;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.ThreadLocalRandom;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreListHolder;

/**
 * Created by gauravsobti on 20/04/16.
 */
class SavedSongsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Closeable {

    private static final String TAG = SavedSongsAdapter.class.getSimpleName();

    private static final byte VIEW_TYPE_HISTORY = 0;
    //private final HandOverMessage<Cursor> handOverCursor;
    private final ResizeOptions resizeOptions = new ResizeOptions(150, 150);
    private final HandOverMessage<Cursor> handOverCursor;


    public SavedSongsAdapter(HandOverMessage<Cursor> handOverCursor) {
        this.handOverCursor = handOverCursor;
        setHasStableIds(true);
    }

    ///////////Data set ops
    @Nullable
    private Cursor mySavedSongsCursor = null;

    public void setNewMySavesSongsCursor(@Nullable Cursor newMySavedSongsCursor) {

        //destroy
        if (this.mySavedSongsCursor != null)
            mySavedSongsCursor.close();

        //set
        this.mySavedSongsCursor = newMySavedSongsCursor;
        notifyDataSetChanged();
    }


    @Override
    public void close() {

        //TODO: Check if this is required
        //MiscUtils.closeQuietly(mySavedSongsCursor);
        mySavedSongsCursor = null;
    }
    ///////////

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Log.i(TAG, "Creating ViewHolder");

        switch (viewType) {

            case VIEW_TYPE_HISTORY: {

                return new SavedSongsViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.saved_songs_list_item, parent, false),handOverCursor);
            }

            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        Log.i("Ayush", "Binding ViewHolder " + getClass().getName());

        final Object saved_song = getItem(position);
        if (saved_song instanceof Cursor) {

            final Cursor cursorExactType = (Cursor) saved_song;
            final SavedSongsViewHolder savedSongHolder = (SavedSongsViewHolder) holder;


            final String albumArt = "https://i.ytimg.com/vi/" + cursorExactType.getString(0) + "/hqdefault.jpg";
            if (!TextUtils.isEmpty(albumArt)) {
                savedSongHolder.songThumbnail.setController(Fresco.newDraweeControllerBuilder()
                        .setOldController(savedSongHolder.songThumbnail.getController())
                        .setImageRequest(ImageRequestBuilder.newBuilderWithSource(Uri.parse(albumArt))
                                .build())
                        .build());
            }
            savedSongHolder.songName.setText(cursorExactType.getString(5));
            savedSongHolder.senderName.setText(cursorExactType.getString(2));
            savedSongHolder.added.setText(Long.toString(cursorExactType.getLong(1)));



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

            if (mySavedSongsCursor == null || mySavedSongsCursor.isClosed() || !mySavedSongsCursor.moveToPosition(position))
                throw new IllegalStateException("Resource cursor has been corrupted");
            return mySavedSongsCursor;

    }

    @Override
    public int getItemViewType(int position) {

            return VIEW_TYPE_HISTORY;

    }

    @Override
    public long getItemId(int position) {

        final Object item = getItem(position);
            return ((Cursor) item).getLong(8);

    }

    @Override
    public int getItemCount() {

        if (mySavedSongsCursor != null && !mySavedSongsCursor.isClosed())
            return mySavedSongsCursor.getCount() + 1;//adjust for recent list
        return 0;
    }

    static class SavedSongsViewHolder extends RecyclerView.ViewHolder {


        private final HandOverMessage<Cursor> handOverCursor;
        private SimpleDraweeView songThumbnail;
        private TextView songName;
        private TextView senderName;
        private TextView added;


        public SavedSongsViewHolder(View itemView, HandOverMessage<Cursor> handOverCursor) {
            super(itemView);
            songThumbnail = (SimpleDraweeView) itemView.findViewById( R.id.songThumbnail );
            songName = (TextView) itemView.findViewById( R.id.songName );
            senderName = (TextView) itemView.findViewById( R.id.senderName );
            added = (TextView) itemView.findViewById( R.id.added );
            this.handOverCursor = handOverCursor;
            itemView.setOnClickListener(this);
        }


    }

}
