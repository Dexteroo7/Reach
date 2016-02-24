package reach.project.coreViews.friends;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.facebook.imagepipeline.common.ResizeOptions;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import reach.project.utils.AlbumArtUri;
import reach.project.utils.ReachCursorAdapter;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreQualifier;

/**
 * Created by dexter on 18/11/15.
 */
final class LockedFriendsAdapter extends ReachCursorAdapter<FriendsViewHolder> implements MoreQualifier {

    private final ResizeOptions resizeOptions = new ResizeOptions(150, 150);
    /*private boolean shouldShowCoach1;
    private SharedPreferences sharedPreferences;
    private TourGuide tourGuide = null;*/

    public LockedFriendsAdapter(HandOverMessage<Cursor> handOverMessage, int resourceId) {
        super(handOverMessage, resourceId);
        setHasStableIds(true);
        //this.sharedPreferences = sharedPreferences;
        //this.shouldShowCoach1 = !SharedPrefUtils.getFriendsCoach1Seen(this.sharedPreferences);
    }

    /*@Override
    public void handOverMessage(@NonNull Integer position) {
        if (tourGuide != null)
            tourGuide.cleanUp();
        super.handOverMessage(position);
    }*/

    @Nullable
    private WeakReference<RecyclerView.Adapter> reference = null;

    @Override
    public void setCursor(@Nullable Cursor newCursor) {
        super.setCursor(newCursor);

        final RecyclerView.Adapter adapter;
        if (reference != null && (adapter = reference.get()) != null) {

            //must set the new cursor
            ((ReachCursorAdapter) adapter).setCursor(newCursor);
            adapter.notifyDataSetChanged();
        }
    }



    @Override
    public long getItemId(@Nonnull Cursor cursor) {

        Log.i(LockedFriendsAdapter.class.getName(), "Getting locked cursor id " + cursor.getLong(0));
        return cursor.getLong(0); //_id
    }

    @Override
    public int getItemCount() {

        final int count = super.getItemCount();
        return count > 4 ? 4 : count;
    }

    public void onBindViewHolder(FriendsViewHolder friendsViewHolder, Cursor cursor) {

        /*if (shouldShowCoach1) {
            final ToolTip toolTip = new ToolTip()
                    .setTextColor(Color.WHITE)
                    .setTitle("Hello!")
                    .setShadow(false)
                    .setDescription("Click to view tutorial. Next button is disabled until tutorial is viewed");
            final Overlay overlay = new Overlay()
                    .setBackgroundColor(Color.parseColor("#99000000"))
                    .setStyle(Overlay.Style.Rectangle);
            tourGuide = TourGuide.init((Activity) friendsViewHolder.itemView.getContext()).with(TourGuide.Technique.Click)
                    .setToolTip(toolTip)
                    .setOverlay(overlay)
                    .playOn(friendsViewHolder.itemView);
            shouldShowCoach1 = false;
            SharedPrefUtils.setFriendsCoach1Seen(sharedPreferences);
        }*/
        friendsViewHolder.userNameList.setText(cursor.getString(2));
        friendsViewHolder.telephoneNumberList.setText(cursor.getInt(7) + "");
        friendsViewHolder.appCount.setText(cursor.getInt(8) + "");
        friendsViewHolder.coverPic.setImageURI(AlbumArtUri.getUserImageUri(
                cursor.getLong(0),
                "coverPicId",
                "rw",
                false,
                250,
                150));
        friendsViewHolder.profilePhotoList.setImageURI(AlbumArtUri.getUserImageUri(
                cursor.getLong(0),
                "imageId",
                "rw",
                true,
                150,
                150));

        friendsViewHolder.lockIcon.setVisibility(View.VISIBLE);
        
        Log.i(LockedFriendsAdapter.class.getName(), "Binding view holder " + friendsViewHolder.getAdapterPosition());
    }

    @Override
    public FriendsViewHolder getViewHolder(View itemView, HandOverMessage<Integer> handOverMessage) {

        final FriendsViewHolder friendsViewHolder = new FriendsViewHolder(itemView, handOverMessage);
        Log.i(LockedFriendsAdapter.class.getName(), "Creating view holder ");
        return friendsViewHolder;
    }

    @Override
    public void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference) {
        reference = adapterWeakReference;
    }
}
