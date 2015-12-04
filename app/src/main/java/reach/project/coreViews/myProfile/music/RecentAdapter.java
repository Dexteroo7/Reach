package reach.project.coreViews.myProfile.music;

import android.net.Uri;
import android.support.v7.widget.RecyclerView;
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

import reach.project.R;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;

/**
 * Created by dexter on 18/11/15.
 */
class RecentAdapter extends SimpleRecyclerAdapter<PrivacySongItem, SongItemHolder> implements MoreQualifier {

    public RecentAdapter(List<PrivacySongItem> recentMusic, HandOverMessage<PrivacySongItem> handOverMessage, int resourceId) {
        super(recentMusic, handOverMessage, resourceId);
    }

    private final Comparator<PrivacySongItem> primary = (left, right) -> {

        final Long lhs = left == null ? 0 : left.dateAdded;
        final Long rhs = right == null ? 0 : right.dateAdded;

        return lhs.compareTo(rhs);
    };

    private final Comparator<PrivacySongItem> secondary = (left, right) -> {

        final String lhs = left == null ? "" : left.displayName;
        final String rhs = right == null ? "" : right.displayName;

        return lhs.compareTo(rhs);
    };
    private WeakReference<RecyclerView.Adapter> adapterWeakReference = null;

    /**
     * MUST CALL FROM UI THREAD
     *
     * @param newMessages the new collection to display
     */
    public synchronized void updateRecent(List<PrivacySongItem> newMessages) {

        final List<PrivacySongItem> recentMusic = getMessageList();
        //remove to prevent duplicates
        recentMusic.removeAll(newMessages);
        //add new items
        recentMusic.addAll(newMessages);

        //pick top 20
        final List<PrivacySongItem> newSortedList = Ordering.from(primary).compound(secondary).greatestOf(recentMusic, 20);

        //remove all
        recentMusic.clear();
        //add top 20
        recentMusic.addAll(newSortedList);

        notifyDataSetChanged();
        if (adapterWeakReference != null)
            adapterWeakReference.get().notifyDataSetChanged();
    }

    @Override
    public SongItemHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        return new SongItemHolder(itemView, handOverMessage);
    }

    @Override
    public void onBindViewHolder(SongItemHolder holder, PrivacySongItem item) {

        holder.artistName.setText(item.artistName);
        if (item.visible) {
            holder.toggleButton.setImageResource(R.drawable.ic_pending_lock);
            holder.toggleButton2.setVisibility(View.GONE);
            holder.toggleText.setText("Everyone");
        }
        else {
            holder.toggleButton.setImageResource(R.drawable.icon_locked);
            holder.toggleButton2.setVisibility(View.VISIBLE);
            holder.toggleText.setText("Only Me");
        }
        holder.songName.setText(item.displayName);
        final Optional<Uri> uriOptional = AlbumArtUri.getUri(
                item.albumName,
                item.artistName,
                item.displayName,
                false);

        if (uriOptional.isPresent()) {

//            Log.i("Ayush", "Url found = " + uriOptional.get().toString());
            final ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uriOptional.get())
                    .setResizeOptions(new ResizeOptions(200, 200))
                    .build();

            final DraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setOldController(holder.albumArt.getController())
                    .setImageRequest(request)
                    .build();

            holder.albumArt.setController(controller);
        } else
            holder.albumArt.setImageBitmap(null);
    }

    @Override
    public int getItemCount() {
        int size = super.getItemCount();
        return size < 4 ? size : 4;
    }

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        this.adapterWeakReference = adapterWeakReference;
    }
}