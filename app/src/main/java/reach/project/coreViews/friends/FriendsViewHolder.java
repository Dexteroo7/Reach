package reach.project.coreViews.friends;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.io.IOException;
import java.lang.ref.WeakReference;

import reach.backend.entities.userApi.model.MyString;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

final class FriendsViewHolder extends SingleItemViewHolder {

    final TextView userNameList, telephoneNumberList, appCount, lockText, newSongs;
    final ImageView lockIcon;
    final ImageView optionsIcon;
    final SimpleDraweeView profilePhotoList, coverPic;
    final View newSongsView;

    //set this position to use inside listener
    int position = -1;

    protected FriendsViewHolder(View itemView,
                                HandOverMessageExtra<Cursor> handOverMessageExtra) {

        super(itemView, handOverMessageExtra);

        final Context context = itemView.getContext();

        this.userNameList = (TextView) itemView.findViewById(R.id.userNameList);
        this.telephoneNumberList = (TextView) itemView.findViewById(R.id.telephoneNumberList);
        this.appCount = (TextView) itemView.findViewById(R.id.appCount);
        this.profilePhotoList = (SimpleDraweeView) itemView.findViewById(R.id.profilePhotoList);
        this.coverPic = (SimpleDraweeView) itemView.findViewById(R.id.coverPic);
        this.lockIcon = (ImageView) itemView.findViewById(R.id.lockIcon);
        this.lockText = (TextView) itemView.findViewById(R.id.lockText);
        this.newSongs = (TextView) itemView.findViewById(R.id.newSongs);
        this.newSongsView = itemView.findViewById(R.id.newSongsView);

        this.optionsIcon = (ImageView) itemView.findViewById(R.id.optionsIcon);
        this.optionsIcon.setOnClickListener(view -> {

            if (position == -1)
                throw new IllegalArgumentException("Position not set for the view holder");
            Log.i("Ayush", "Position detected " + position);

            final PopupMenu popupMenu = new PopupMenu(context, optionsIcon);
            popupMenu.inflate(R.menu.friends_popup_menu);
            popupMenu.setOnMenuItemClickListener(item -> {

                switch (item.getItemId()) {

                    case R.id.friends_menu_1:
                        handOverMessageExtra.handOverMessage(position);
                        return true;
                    case R.id.friends_menu_2:

                        final WeakReference<ContentResolver> weakReference = new WeakReference<>(context.getContentResolver());
                        final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
                        final long myId = SharedPrefUtils.getServerId(preferences);
                        final long hostId = handOverMessageExtra.getExtra(position).getLong(0);
                        //remove the friend async
                        new RemoveFriend(weakReference).execute(myId, hostId);

                        return true;
                    default:
                        return false;
                }
            });

            if (handOverMessageExtra.getExtra(position).getShort(6) == ReachFriendsHelper.Status.REQUEST_SENT_NOT_GRANTED.getValue())
                popupMenu.getMenu().findItem(R.id.friends_menu_2).setTitle("Cancel Request");

            popupMenu.show();
        });
    }

    protected FriendsViewHolder(View itemView,
                                HandOverMessage<Integer> handOverMessage) {

        super(itemView, handOverMessage);

        this.userNameList = (TextView) itemView.findViewById(R.id.userNameList);
        this.telephoneNumberList = (TextView) itemView.findViewById(R.id.telephoneNumberList);
        this.appCount = (TextView) itemView.findViewById(R.id.appCount);
        this.profilePhotoList = (SimpleDraweeView) itemView.findViewById(R.id.profilePhotoList);
        this.coverPic = (SimpleDraweeView) itemView.findViewById(R.id.coverPic);
        this.lockIcon = (ImageView) itemView.findViewById(R.id.lockIcon);
        this.lockText = (TextView) itemView.findViewById(R.id.lockText);
        this.newSongs = (TextView) itemView.findViewById(R.id.newSongs);
        this.newSongsView = itemView.findViewById(R.id.newSongsView);
        itemView.findViewById(R.id.optionsIcon).setVisibility(View.GONE);
        this.optionsIcon = null;
    }

    private static final class RemoveFriend extends AsyncTask<Long, Void, Long> {

        private final WeakReference<ContentResolver> resolverWeakReference;

        private RemoveFriend(WeakReference<ContentResolver> resolverWeakReference) {
            this.resolverWeakReference = resolverWeakReference;
        }

        /**
         * @param params 0 : myID, 1 : hostID
         * @return hostId if success so that we can toggle
         */
        @Override
        protected Long doInBackground(Long... params) {

            try {
                final MyString response = StaticData.USER_API.removeFriend(params[0], params[1]).execute();
                return response == null || TextUtils.isEmpty(response.getString()) || response.getString().equals("false") ? 0L : params[1];
            } catch (IOException e) {
                e.printStackTrace();
                return 0L;
            }
        }

        @Override
        protected void onPostExecute(Long hostId) {

            super.onPostExecute(hostId);
            if (hostId == 0)
                Log.d("Ayush", "Friend removal failed");
            else {

                final ContentValues values = new ContentValues();
                values.put(ReachFriendsHelper.COLUMN_STATUS, ReachFriendsHelper.Status.REQUEST_NOT_SENT.getValue());
                MiscUtils.useReference(resolverWeakReference, contentResolver -> {

                    contentResolver.update(
                            Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + hostId),
                            values,
                            ReachFriendsHelper.COLUMN_ID + " = ?",
                            new String[]{hostId + ""});
                });

            }
        }
    }
}

