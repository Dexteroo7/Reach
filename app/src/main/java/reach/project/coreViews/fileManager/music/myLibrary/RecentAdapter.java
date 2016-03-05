package reach.project.coreViews.fileManager.music.myLibrary;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
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
import com.google.common.collect.Ordering;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.coreViews.friends.HandOverMessageExtra;
import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.coreViews.friends.ReachFriendsProvider;
import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;

/**
 * Created by dexter on 18/11/15.
 */
class RecentAdapter extends SimpleRecyclerAdapter<MusicData, SongItemHolder> implements MoreQualifier {

    private final ResizeOptions resizeOptions = new ResizeOptions(150, 150);

    public RecentAdapter(List<MusicData> recentMusic, HandOverMessage<MusicData> handOverMessage, int resourceId) {
        super(recentMusic, handOverMessage, resourceId);
    }

    private final HandOverMessageExtra<Object> handOverMessageExtra = new HandOverMessageExtra<Object>() {
        @Override
        public void handOverMessage(@Nonnull Integer position) {
            RecentAdapter.this.handOverMessage(position);
        }

        @Override
        public MusicData getExtra(@Nonnull Integer position) {

            final MusicData musicData = getItem(position);
            if (musicData != null)
                return musicData;
            else
                throw new IllegalStateException("Music data has been corrupted");
        }
    };

    private static final Comparator<MusicData> PRIMARY = (left, right) -> {

        final Long lhs = left == null ? 0 : left.getDateAdded();
        final Long rhs = right == null ? 0 : right.getDateAdded();

        return lhs.compareTo(rhs);
    };

    private static final Comparator<MusicData> SECONDARY = (left, right) -> {

        final String lhs = left == null ? "" : left.getDisplayName();
        final String rhs = right == null ? "" : right.getDisplayName();

        return lhs.compareTo(rhs);
    };

    @Nullable
    private WeakReference<RecyclerView.Adapter> adapterWeakReference = null;

    /**
     * MUST CALL FROM UI THREAD
     *
     * @param newMessages the new collection to display
     */
    public void updateRecent(List<MusicData> newMessages) {

        if (newMessages.isEmpty()) {

            synchronized (getMessageList()) {
                getMessageList().clear();
            }
            notifyItemRangeRemoved(0, getItemCount());
            final RecyclerView.Adapter adapter;
            if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
                adapter.notifyItemRangeRemoved(0, adapter.getItemCount());
        } else {

            synchronized (getMessageList()) {

                //remove to prevent duplicates
                getMessageList().removeAll(newMessages);
                //add new items
                getMessageList().addAll(newMessages);

                //pick top 20
                final List<MusicData> newSortedList = Ordering.from(PRIMARY).compound(SECONDARY).greatestOf(getMessageList(), 20);

                //remove all
                getMessageList().clear();
                //add top 20
                getMessageList().addAll(newSortedList);
            }

            notifyDataSetChanged();
            final RecyclerView.Adapter adapter;
            if (adapterWeakReference != null && (adapter = adapterWeakReference.get()) != null)
                adapter.notifyDataSetChanged();
        }
    }

    @Override
    public SongItemHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        Log.i("Ayush", "Creating ViewHolder " + getClass().getName());

        return new SongItemHolder(itemView, handOverMessageExtra);
    }

    @Override
    public long getItemId(MusicData item) {
        return item.getColumnId();
    }

    @Override
    public void onBindViewHolder(SongItemHolder holder, MusicData item) {

        Log.i("Ayush", "Binding ViewHolder " + getClass().getName());

        holder.position = holder.getAdapterPosition();
        holder.songName.setText(item.getDisplayName());
        if (item.getType() == MusicData.Type.MY_LIBRARY) {
            holder.userImage.setVisibility(View.GONE);
            holder.artistName.setTextColor(Color.parseColor("#878691"));
            holder.artistName.setText(item.getArtistName());
        }
        else if (item.getType() == MusicData.Type.DOWNLOADED) {

            final Context context = holder.itemView.getContext();
            holder.userImage.setVisibility(View.VISIBLE);
            holder.artistName.setTextColor(ContextCompat.getColor(context, R.color.reach_color));
            final long senderId = item.getSenderId();
            final Cursor cursor = context.getContentResolver().query(
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
            }
            holder.artistName.setText(cursor.getString(0));
            final int length = MiscUtils.dpToPx(20);
            holder.userImage.setImageURI(AlbumArtUri.getUserImageUri(
                    senderId,
                    "imageId",
                    "rw",
                    true,
                    length,
                    length));
        }
        else
            throw new IllegalArgumentException("Invalid MusicData type");

        final Optional<Uri> uriOptional = AlbumArtUri.getUri(
                item.getAlbumName(),
                item.getArtistName(),
                item.getDisplayName(),
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

        holder.likeButton.setImageResource(item.isLiked()
                ? R.drawable.icon_heart_outline_pink : R.drawable.icon_heart_outline_grayer);

        //TODO introduce visibility in MusicData
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