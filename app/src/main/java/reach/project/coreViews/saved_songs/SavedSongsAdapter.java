package reach.project.coreViews.saved_songs;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import reach.project.utils.TimeAgo;
import reach.project.utils.YouTubeDataModel;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreListHolder;

/**
 * Created by gauravsobti on 20/04/16.
 */
class SavedSongsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Closeable, View.OnClickListener {

    private static final String TAG = SavedSongsAdapter.class.getSimpleName();

    private static final byte VIEW_TYPE_HISTORY = 0;
    private static final byte VIEW_TYPE_SAVED_SONG = 1;
    private static final byte VIEW_TYPE_SAVED_TEXT = 2;
    private static final byte VIEW_TYPE_HISTORY_TEXT = 3;
    //private final HandOverMessage<Cursor> handOverCursor;
    private final ResizeOptions resizeOptions = new ResizeOptions(150, 150);
    private final HandOverMessage<Object> handOverCursor;


    public SavedSongsAdapter(HandOverMessage<Object> handOverCursor) {
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
                        .inflate(R.layout.saved_songs_list_item, parent, false));
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

            final String youtube_id = cursorExactType.getString(0);

            final String albumArt = MiscUtils.getYoutubeThumbnailUrl(youtube_id);
            if (!TextUtils.isEmpty(albumArt)) {
                savedSongHolder.songThumbnail.setController(Fresco.newDraweeControllerBuilder()
                        .setOldController(savedSongHolder.songThumbnail.getController())
                        .setImageRequest(ImageRequestBuilder.newBuilderWithSource(Uri.parse(albumArt))
                                .build())
                        .build());
            }
            savedSongHolder.songName.setText(cursorExactType.getString(5));
            final String sender_name = cursorExactType.getString(2);
            Log.d(TAG, "onBindViewHolder: senderName = " + sender_name);
            savedSongHolder.senderName.setText(sender_name);
            final long milliseconds = cursorExactType.getLong(1);
            savedSongHolder.added.setText(TimeAgo.toDuration(System.currentTimeMillis() - milliseconds));
            savedSongHolder.itemView.setTag(new Pair<>(position,0));
            savedSongHolder.remove.setTag(new Pair<>(position,1));

            savedSongHolder.itemView.setOnClickListener(this);
            savedSongHolder.remove.setOnClickListener(this);
            savedSongHolder.senderName.setVisibility(View.GONE);

            /*final int type = cursorExactType.getInt(4);
            switch (type) {
                case 1:
                    savedSongHolder.senderName.setText("Saved");
                    break;

                case 2:
                    savedSongHolder.senderName.setText("History");
                    break;
            }*/



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
        /*if(mySavedSongsCursor!=null || !mySavedSongsCursor.isClosed()){
            if(pos)
        }*/

        if (mySavedSongsCursor == null || mySavedSongsCursor.isClosed() || !mySavedSongsCursor.moveToPosition(position))
            throw new IllegalStateException("Resource cursor has been corrupted, position = " + position);
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
            return mySavedSongsCursor.getCount();//adjust for recent list
        return 0;
    }

    @Override
    public void onClick(View v) {
        Pair data = (Pair) v.getTag();

        handOverCursor.handOverMessage(new Pair<>(getItem((int)data.first), data.second));
    }

    static class SavedSongsViewHolder extends RecyclerView.ViewHolder {

        private SimpleDraweeView songThumbnail;
        private TextView songName;
        private TextView senderName;
        private TextView added;
        private ImageView remove;


        public SavedSongsViewHolder(View itemView) {
            super(itemView);
            songThumbnail = (SimpleDraweeView) itemView.findViewById(R.id.songThumbnail);
            songName = (TextView) itemView.findViewById(R.id.songName);
            senderName = (TextView) itemView.findViewById(R.id.senderName);
            added = (TextView) itemView.findViewById(R.id.added);
            remove = (ImageView) itemView.findViewById(R.id.remove_image);
        }


    }


}
