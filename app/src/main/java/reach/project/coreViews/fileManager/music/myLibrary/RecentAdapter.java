package reach.project.coreViews.fileManager.music.myLibrary;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.common.base.Optional;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.coreViews.fileManager.HandOverMessageExtra;
import reach.project.music.Song;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;

/**
 * Created by dexter on 18/11/15.
 */
class RecentAdapter extends SimpleRecyclerAdapter<Song, SongItemHolder> implements MoreQualifier {

    private static final String TAG = RecentAdapter.class.getSimpleName();
    private final ResizeOptions resizeOptions = new ResizeOptions(150, 150);
    private final ParentAdapter.HandOverVisibilityToggle handOverVisibilityToggle;

    public RecentAdapter(List<Song> recentMusic, HandOverMessage<Song> handOverMessage, int resourceId, ParentAdapter.HandOverVisibilityToggle handOverVisibilityToggle) {
        super(recentMusic, handOverMessage, resourceId);
        this.handOverVisibilityToggle = handOverVisibilityToggle;
    }

    private final HandOverMessageExtra<Object> handOverMessageExtra = new HandOverMessageExtra<Object>() {

        @Override
        public void handOverMessage(@Nonnull Integer position) {
            RecentAdapter.this.handOverMessage(position);
        }

        @Override
        public Song getExtra(@Nonnull Integer position) {

            final Song Song = getItem(position);
            if (Song != null)
                return Song;
            else
                throw new IllegalStateException("Music data has been corrupted");
        }

        @Override
        public void putExtra(int position, Object item) {
            Log.d(TAG, "putExtra called, new Visibility = " + ((Song) item).visibility);
            getMessageList().set(position, (Song) item);
            notifyItemChanged(position);
            final RecyclerView.Adapter adapter;
            if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
                adapter.notifyItemChanged(position);
        }

        @Override
        public void handOverAppVisibilityMessage(String packageName) {

        }

        @Override
        public void handOverSongVisibilityMessage(int position, Object message) {

            /*getMessageList().set(position, (Song) message);
            notifyItemChanged(position);*/
            RecentAdapter.this.handOverVisibilityToggle.HandoverMessage(position,message);

        }
    };


    public synchronized void updateVisibility(String metaHash, boolean newVisibility) {

        final List<Song> songItems = getMessageList();

        int position = -1;
        for (int index = 0; index < songItems.size(); index++) {

            final Song songItem = songItems.get(index);
            if (songItem.getFileHash().equals(metaHash)) {

                final Song newSong = new Song.Builder(songItem).visibility(newVisibility).build();
                newSong.setSecondaryProgress(songItem.getSecondaryProgress());
                newSong.setCurrentPosition(songItem.getCurrentPosition());
                newSong.setProcessed(songItem.getProcessed());
                newSong.setPrimaryProgress(songItem.getPrimaryProgress());
                newSong.setType(songItem.getType());
                newSong.setSenderName(songItem.getSenderName());
                newSong.setSenderId(songItem.getSenderId());
                songItems.set(index, newSong);
                position = index;
                break;
            }
        }

        //recent adapter might not contain everything, as is limited to 4
        if (position < getItemCount())
            notifyItemChanged(position);

        final RecyclerView.Adapter adapter;
        if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
            adapter.notifyItemChanged(position);
    }

    public synchronized void updateVisibility(int position, boolean newVisibility) {

        final List<Song> songItems = getMessageList();

        //int position = -1;
        final Song songItem = songItems.get(position);
                final Song newSong = new Song.Builder(songItem).visibility(newVisibility).build();
                newSong.setSecondaryProgress(songItem.getSecondaryProgress());
                newSong.setCurrentPosition(songItem.getCurrentPosition());
        newSong.setProcessed(songItem.getProcessed());
        newSong.setPrimaryProgress(songItem.getPrimaryProgress());
        newSong.setType(songItem.getType());
        newSong.setSenderName(songItem.getSenderName());
        newSong.setSenderId(songItem.getSenderId());
                songItems.set(position, newSong);

        //recent adapter might not contain everything, as is limited to 4
        if (position < getItemCount())
            notifyItemChanged(position);

        final RecyclerView.Adapter adapter;
        if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
            adapter.notifyItemChanged(position);

    }

    private static final Comparator<Song> PRIMARY = (left, right) -> {

        final Long lhs = left == null ? 0 : left.dateAdded;
        final Long rhs = right == null ? 0 : right.dateAdded;

        return lhs.compareTo(rhs);
    };

    private static final Comparator<Song> SECONDARY = (left, right) -> {

        final String lhs = left == null ? "" : left.displayName;
        final String rhs = right == null ? "" : right.displayName;

        return lhs.compareTo(rhs);
    };

    @Nullable
    private WeakReference<RecyclerView.Adapter> adapterWeakReference = null;

    /**
     * MUST CALL FROM UI THREAD
     *
     * @param newMessages the new collection to display
     */
    public void updateRecent(List<Song> newMessages) {

        synchronized (getMessageList()) {
            getMessageList().clear();
            getMessageList().addAll(newMessages);
        }

        notifyDataSetChanged();
        final RecyclerView.Adapter adapter;
        if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
            adapter.notifyDataSetChanged();
    }

    @Override
    public SongItemHolder getViewHolder(View itemView) {

        Log.i("Ayush", "Creating ViewHolder " + getClass().getName());

        return new SongItemHolder(itemView, handOverMessageExtra);
    }

    @Override
    public long getItemId(Song item) {
        return item.fileHash.hashCode();
    }

    @Override
    public void onBindViewHolder(SongItemHolder holder, Song item) {

        Log.i("Ayush", "Binding ViewHolder " + getClass().getName());

        holder.position = holder.getAdapterPosition();
        holder.songName.setText(item.displayName);
        if (!item.isLiked) {
            holder.likeButton.setSelected(false);
        } else {
            holder.likeButton.setSelected(true);
        }

        /*if (item.visibility) {

            holder.toggleImage.setImageResource(0);
        } else {

            holder.toggleImage.setImageResource(R.drawable.icon_locked_white);
        }*/

        /*if (item.getType() == Song.Type.MY_LIBRARY) {

            holder.userImage.setVisibility(View.GONE);
            holder.artistName.setTextColor(Color.parseColor("#878691"));
            holder.artistName.setText(item.artist);
        } else if (item.getType() == Song.Type.DOWNLOADED) {
*/
        final Context context = holder.itemView.getContext();
        //holder.userImage.setVisibility(View.VISIBLE);
        holder.artistName.setTextColor(ContextCompat.getColor(context, R.color.reach_color));
        final long senderId = item.getSenderId();
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

        final int length = MiscUtils.dpToPx(20);

        if(item.getSenderName() == null || item.getSenderName().equals("") || item.getSenderName().equals("null")){
            Log.d(TAG, "senderName = null, song Name = " + item.getDisplayName());
            holder.userImage.setVisibility(View.GONE);
            holder.artistName.setText(item.getArtist());
        }
        else{
            holder.userImage.setVisibility(View.VISIBLE);
            holder.artistName.setText(item.getSenderName());
            holder.userImage.setImageURI(AlbumArtUri.getUserImageUri(
                    senderId,
                    "imageId",
                    "rw",
                    true,
                    length,
                    length));
        }


        /*holder.artistName.setText(item.artist == null ? "" : item.artist);
        holder.userImage.setImageURI(AlbumArtUri.getUserImageUri(
                senderId,
                "imageId",
                "rw",
                true,
                length,
                length));*/
        /*} else
            throw new IllegalArgumentException("Invalid Song type");*/

        final Optional<Uri> uriOptional = AlbumArtUri.getUri(
                item.album,
                item.artist,
                item.displayName,
                false);

        if (uriOptional.isPresent()) {

//            Log.i("Ayush", "Url found = " + uriOptional.get().toString());
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

        /*holder.likeButton.setImageResource(item.isLiked
                ? R.drawable.icon_heart_outline_pink : R.drawable.icon_heart_outline_grayer);*/

        //TODO introduce visibility in Song
        /*if (item.visible) {

            holder.toggleButton.setImageResource(R.drawable.icon_everyone);
            holder.toggleText.setText("Everyone");
        } else {

            holder.toggleButton.setImageResource(R.drawable.icon_locked);
            holder.toggleText.setText("Only Me");
        }*/
    }

    @Override
    public int getItemCount() {

        final int length = super.getItemCount();
        return length > 4 ? 4 : length;
    }

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        this.adapterWeakReference = adapterWeakReference;
    }
}