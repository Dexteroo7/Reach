package reach.project.coreViews.myProfile.music.myLibrary;

import android.net.Uri;
import android.support.annotation.Nullable;
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

import reach.project.reachProcess.auxiliaryClasses.MusicData;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;
import reach.project.utils.viewHelpers.SimpleRecyclerAdapter;

/**
 * Created by dexter on 18/11/15.
 */
class RecentAdapter extends SimpleRecyclerAdapter<MusicData, SongItemHolder> implements MoreQualifier {

    public RecentAdapter(List<MusicData> recentMusic, HandOverMessage<MusicData> handOverMessage, int resourceId) {
        super(recentMusic, handOverMessage, resourceId);
    }

    @Nullable
    private WeakReference<RecyclerView.Adapter> adapterWeakReference = null;

    /**
     * MUST CALL FROM UI THREAD
     *
     * @param newMessages the new collection to display
     */
    public void updateRecent(List<MusicData> newMessages) {

        final List<MusicData> recentMusic = getMessageList();
        //remove to prevent duplicates
        recentMusic.removeAll(newMessages);
        //add new items
        recentMusic.addAll(newMessages);

        //pick top 20
        final List<MusicData> newSortedList = Ordering.from(new Comparator<MusicData>() {
            @Override
            public int compare(MusicData lhs, MusicData rhs) {

                final Long a = lhs.getDateAdded();
                final Long b = rhs.getDateAdded();

                return a.compareTo(b);
            }
        }).greatestOf(recentMusic, 20);

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
    public void onBindViewHolder(SongItemHolder holder, MusicData item) {

        holder.songName.setText(item.getDisplayName());
        holder.artistName.setText(item.getArtistName());
        final Optional<Uri> uriOptional = AlbumArtUri.getUri(
                item.getAlbumName(),
                item.getArtistName(),
                item.getDisplayName());

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

        final int length = super.getItemCount();
        return length > 6 ? 6 : length;
    }

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        this.adapterWeakReference = adapterWeakReference;
    }
}