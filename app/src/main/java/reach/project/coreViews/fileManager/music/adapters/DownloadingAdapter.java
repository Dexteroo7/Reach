package reach.project.coreViews.fileManager.music.adapters;

import android.database.Cursor;
import android.view.View;

import com.google.common.base.Optional;

import javax.annotation.Nonnull;

import reach.project.utils.ReachCursorAdapter;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by dexter on 23/11/15.
 */
public class DownloadingAdapter extends ReachCursorAdapter<DownloadingItemHolder> {

    public DownloadingAdapter(HandOverMessage<Cursor> handOverMessage, int resourceId) {
        super(handOverMessage, resourceId);
    }

    @Override
    public DownloadingItemHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {
        return new DownloadingItemHolder(itemView, handOverMessage);
    }

    @Override
    public int getItemId(@Nonnull Cursor cursor) {
        return 0; //TODO implement a unique itemId
    }

    @Override
    public void onBindViewHolder(DownloadingItemHolder holder, int position) {

        final Optional<Cursor> cursorOptional = getItem(position);
        holder.bindPosition(position);

        if (cursorOptional.isPresent()) {

            final Cursor cursor = cursorOptional.get();
//            holder.songName.setText(cursor.getString(2)); TODO
//            holder.telephoneNumberList.setText(cursor.getInt(6) + "");
//            holder.profilePhotoList.setImageURI(Uri.parse(StaticData.cloudStorageImageBaseUrl + cursor.getString(3)));
//            holder.lockIcon.setVisibility(View.VISIBLE);
        }
    }
}
