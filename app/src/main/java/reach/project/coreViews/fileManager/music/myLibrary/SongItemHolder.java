package reach.project.coreViews.fileManager.music.myLibrary;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.Set;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.fileManager.HandOverMessageExtra;
import reach.project.coreViews.push.PushActivity;
import reach.project.coreViews.push.PushContainer;
import reach.project.music.Song;
import reach.project.music.SongCursorHelper;
import reach.project.music.SongHelper;
import reach.project.music.SongProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.viewHelpers.SingleItemViewHolder;

/**
 * Created by dexter on 18/11/15.
 */
class SongItemHolder extends SingleItemViewHolder {

    private static final String TAG = SongItemHolder.class.getSimpleName();

    public final TextView songName, artistName;
    public final ImageView extraButton, likeButton;
    public final SimpleDraweeView albumArt, userImage;

    //must set this position
    int position = -1;

    protected SongItemHolder(View itemView, HandOverMessageExtra<Object> handOverMessageExtra) {

        super(itemView, handOverMessageExtra);

        this.songName = (TextView) itemView.findViewById(R.id.songName);
        this.artistName = (TextView) itemView.findViewById(R.id.artistName);
        this.extraButton = (ImageView) itemView.findViewById(R.id.extraButton);
        this.likeButton = (ImageView) itemView.findViewById(R.id.likeButton);
        this.albumArt = (SimpleDraweeView) itemView.findViewById(R.id.albumArt);
        this.userImage = (SimpleDraweeView) itemView.findViewById(R.id.userImage);

        final Context context = itemView.getContext();
        final ContentResolver resolver = context.getContentResolver();

        this.likeButton.setOnClickListener(v -> {

            Log.d(TAG, "Like button called");

            final Object object = handOverMessageExtra.getExtra(position);
            final ContentValues mySong = new ContentValues();

            String fileHash;
            String isLiked;

            if (object instanceof Song) {

                Song musicData = (Song) object;
                isLiked = musicData.isLiked ? StaticData.one : StaticData.zero;
                Log.d(TAG, "Song object isliked = " + isLiked);
                musicData = new Song.Builder(musicData).isLiked(!musicData.isLiked).build();
                handOverMessageExtra.putExtra(position, musicData);
                fileHash = musicData.getFileHash();

                /*musicData.setIsLiked(!musicData.isLiked());*/
            } else if (object instanceof Cursor) {

                final Cursor cursor = (Cursor) object;
                fileHash = cursor.getString(2);
                isLiked = cursor.getString(13);
                //if (cursor.getColumnCount() == MySongsHelper.DISK_LIST.length) {
                //if (cursor.getShort(13) == 1) {

                //mySong.put(MySongsHelper.COLUMN_IS_LIKED, cursor.getString(13).equals("0") ? 1 : 0);

                ///}

                /*} else if (cursor.getColumnCount() == SongHelper.MUSIC_DATA_LIST.length) {
                    if (cursor.getString(7).equalsIgnoreCase("TRUE")) {
                        final long dbId = cursor.getLong(0);
                        final ContentValues mySong = new ContentValues();
                        mySong.put(SongHelper.COLUMN_IS_LIKED, cursor.getString(7).equalsIgnoreCase("FALSE") ? 1 : 0);
                        resolver.update(
                                Uri.parse(SongProvider.CONTENT_URI + "/" + dbId),
                                mySong,
                                SongHelper.COLUMN_ID + " = ?",
                                new String[]{dbId + ""});
                    }

                } else if (cursor.getColumnCount() == SongHelper.MUSIC_DATA_LIST.length) {
                    if (cursor.getString(7).equalsIgnoreCase("TRUE")) {
                        final long dbId = cursor.getLong(0);
                        final ContentValues mySong = new ContentValues();
                        mySong.put(SongHelper.COLUMN_IS_LIKED, cursor.getString(7).equalsIgnoreCase("FALSE") ? 1 : 0);
                        resolver.update(
                                Uri.parse(SongProvider.CONTENT_URI + "/" + dbId),
                                mySong,
                                SongHelper.COLUMN_ID + " = ?",
                                new String[]{dbId + ""});
                    }

                }*/ /*else
                    throw new IllegalArgumentException("Unknown column count found");*/
                //}
            } else
                throw new IllegalArgumentException("Invalid Object type detected");

            if (isLiked == null || isLiked.equals(StaticData.zero)) {
                mySong.put(SongHelper.COLUMN_IS_LIKED, 1);
                this.likeButton.setSelected(true);
            } else {
                mySong.put(SongHelper.COLUMN_IS_LIKED, 0);
                this.likeButton.setSelected(false);
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (resolver != null && mySong != null && fileHash != null)
                        resolver.update(
                    /*Uri.parse(*/SongProvider.CONTENT_URI /*+ "/" + fileHash)*/,
                                mySong,
                                SongHelper.COLUMN_META_HASH + " = ?",
                                new String[]{fileHash + ""});
                }
            }).start();


        });
/*
        this.likeButton.setOnClickListener(v -> ((ImageView) v).setImageResource(R.drawable.icon_heart_pink));
*/
        this.extraButton.setOnClickListener(v -> {
            if (position == -1)
                throw new IllegalArgumentException("Position not set for the view holder");

            final PopupMenu popupMenu = new PopupMenu(context, this.extraButton);
            popupMenu.inflate(R.menu.manager_popup_menu);
            popupMenu.setOnMenuItemClickListener(item -> {

                final Object object = handOverMessageExtra.getExtra(position);
                final Song musicData;
                if (object instanceof Song) {
                    musicData = (Song) object;
//                            if(musicData == null){
//                                Toast.makeText(context, "Sorry couldn't share!", Toast.LENGTH_SHORT).show();
//                                return true;
//                            }


                } else if (object instanceof Cursor) {
                    Cursor cursor = (Cursor) object;
                    musicData = SongCursorHelper.SONG_HELPER.parse(cursor);
//                            if(musicData == null){
//                                Toast.makeText(context, "Sorry couldn't share!", Toast.LENGTH_SHORT).show();
//                                return true;
//                            }

                } else {
                    //TODO throw error should not happen
                    Toast.makeText(context, "Sorry couldn't share!", Toast.LENGTH_SHORT).show();
                    return true;
                }

                switch (item.getItemId()) {

                    case R.id.manager_menu_1: {

                        //send

                        final Set<Song> selectedSongs = MiscUtils.getSet(1);
                        //final Song song = MySongsHelper.convertMusicDataToSong(currentPlaying);
                        Log.i(TAG, "Name of the song to push = " + musicData.displayName);
                        selectedSongs.add(musicData);

                        if (selectedSongs.isEmpty()) {
                            Toast.makeText(context, "Sorry couldn't share!", Toast.LENGTH_SHORT).show();
                            return true;
                        }

                        final SharedPreferences preferences = context.getSharedPreferences("Reach", Context.MODE_PRIVATE);
                        final PushContainer pushContainer = new PushContainer.Builder()
                                .senderId(SharedPrefUtils.getServerId(preferences))
                                .userName(SharedPrefUtils.getUserName(preferences))
                                .userImage(SharedPrefUtils.getImageId(preferences))
                                .firstSongName(selectedSongs.isEmpty() ? "" : selectedSongs.iterator().next().displayName)
                                .song(ImmutableList.copyOf(selectedSongs))
                                .songCount(selectedSongs.size())
                                .app(Collections.emptyList())
                                .appCount(0)
                                .firstAppName("")
                                .build();

                        try {
                            PushActivity.startPushActivity(pushContainer, context);
                        } catch (IOException e) {

                            e.printStackTrace();
                            //TODO Track
                            Toast.makeText(context, "Could not push", Toast.LENGTH_SHORT).show();
                        }

                        return true;
                    }
                    case R.id.manager_menu_2: {
                        //hide
                        return true;
                    }

                    case R.id.manager_menu_3: {
                        //delete
                        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                                .setMessage("Are you sure you want to delete it?")
                                .setPositiveButton("Yes", handleClick)
                                .setNegativeButton("No", handleClick)
                                .create();
                        alertDialog.setOnShowListener(dialog -> alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTag(musicData));
                        alertDialog.show();
                        return true;
                    }
                    default:
                        return false;
                }
            });

            final MenuItem hideItem = popupMenu.getMenu().findItem(R.id.manager_menu_2);
            final Object object = handOverMessageExtra.getExtra(position);
            boolean visible = false;
            if (object instanceof Song) {
                final Song musicData = (Song) object;
            } else if (object instanceof Cursor) {
                final Cursor cursor = (Cursor) object;
                /*if (cursor.getColumnCount() == MySongsHelper.DISK_LIST.length)
                    visible = cursor.getShort(11) == 1;
                else if (cursor.getColumnCount() == SongHelper.MUSIC_DATA_LIST.length)*/
                visible = cursor.getShort(12) == 1;
                /*else
                    throw new IllegalArgumentException("Unknown column count found");*/
            } else
                throw new IllegalArgumentException("Invalid Object type detected");
            hideItem.setTitle(visible ? "Everyone" : "Only Me");

            popupMenu.show();
        });
    }

    private static final DialogInterface.OnClickListener handleClick = (dialog, which) -> {

        if (which == AlertDialog.BUTTON_NEGATIVE) {
            dialog.dismiss();
            return;
        }

        final AlertDialog alertDialog = (AlertDialog) dialog;
        final ContentResolver resolver = alertDialog.getContext().getContentResolver();

        final Song song = (Song) alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).getTag();
//        final Uri uri = Uri.parse(SongProvider.CONTENT_URI + "/" + song.fileHash);

        //find path and delete the file
        final Cursor pathCursor = resolver.query(
                SongProvider.CONTENT_URI,
                new String[]{SongHelper.COLUMN_PATH},
                SongHelper.COLUMN_META_HASH + " = ?",
                new String[]{song.fileHash}, null);

        if (pathCursor != null) {

            if (pathCursor.moveToFirst()) {

                final String path = pathCursor.getString(0);
                if (!TextUtils.isEmpty(path) && !path.equals("hello_world")) {

                    final File toDelete = new File(path);
                    try {
                        final RandomAccessFile randomAccessFile = new RandomAccessFile(toDelete, "rws");
                        randomAccessFile.setLength(0);
                        randomAccessFile.close();
                    } catch (IOException ignored) {
                    } finally {

                        toDelete.delete();
                        toDelete.deleteOnExit();
                    }
                }
            }
            pathCursor.close();
        }

        //delete the database entry
        final boolean deleted = resolver.delete(
                SongProvider.CONTENT_URI,
                SongHelper.COLUMN_META_HASH + " = ?",
                new String[]{song.fileHash}) > 0;

        Log.i("Downloader", "Deleting " + song.displayName + " " + deleted);
        dialog.dismiss();
    };
}