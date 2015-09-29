package reach.project.music.songs;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Optional;
import com.squareup.picasso.Picasso;

import reach.project.R;
import reach.project.musicbrainz.CoverArt;
import reach.project.uploadDownload.ReachDatabaseHelper;
import reach.project.utils.AsyncCache;
import reach.project.utils.MiscUtils;

/**
 * Created by dexter on 7/8/14.
 */
public class ReachMusicAdapter extends ResourceCursorAdapter {

//    private final CircleTransform circleTransform = new CircleTransform();

    public String[] getProjectionDownloaded() {
        return projectionDownloaded;
    }

    public String[] getProjectionMyLibrary() {
        return projectionMyLibrary;
    }

    private final String[] projectionMyLibrary =
        {
                ReachSongHelper.COLUMN_ID, //0

                ReachSongHelper.COLUMN_SONG_ID, //1
                ReachSongHelper.COLUMN_USER_ID, //2

                ReachSongHelper.COLUMN_DISPLAY_NAME, //3
                ReachSongHelper.COLUMN_ACTUAL_NAME, //4

                ReachSongHelper.COLUMN_ARTIST, //5
                ReachSongHelper.COLUMN_ALBUM, //6

                ReachSongHelper.COLUMN_DURATION, //7
                ReachSongHelper.COLUMN_SIZE, //8

                ReachSongHelper.COLUMN_VISIBILITY, //9

                ReachSongHelper.COLUMN_ALBUM_ART_DATA, //10
                ReachSongHelper.COLUMN_GENRE //11
        };

    private final String[] projectionDownloaded =
            {
                    ReachDatabaseHelper.COLUMN_ID, //0

                    ReachDatabaseHelper.COLUMN_UNIQUE_ID, //1
                    ReachDatabaseHelper.COLUMN_RECEIVER_ID, //2

                    ReachDatabaseHelper.COLUMN_DISPLAY_NAME, //3
                    ReachDatabaseHelper.COLUMN_ACTUAL_NAME, //4

                    ReachDatabaseHelper.COLUMN_ARTIST, //5
                    ReachDatabaseHelper.COLUMN_ALBUM, //6

                    ReachDatabaseHelper.COLUMN_DURATION, //7
                    ReachDatabaseHelper.COLUMN_SIZE, //8

                    ReachDatabaseHelper.COLUMN_VISIBILITY, //9

                    ReachDatabaseHelper.COLUMN_ALBUM_ART_DATA, //10
                    ReachDatabaseHelper.COLUMN_GENRE //11
            };


    private static final String EMPTY_STRING = "empty_string";
    private static final AsyncCache<String, String> urlCache = new AsyncCache<>(releaseGroupMbid -> {

        /**
         * releaseGroupMbid is the key
         * smallThumbNail is data
         */
        final Optional<JsonNode> nodeOptional = CoverArt.fetchCoverImagesFromMBID(releaseGroupMbid);
        if (nodeOptional.isPresent()) {

            String imageUrl = null;
            try {
                imageUrl = CoverArt.getSmallThumbnailURL(nodeOptional.get());
            } catch (Exception ignored) {
                imageUrl = null;
            }

            if (TextUtils.isEmpty(imageUrl))
                try {
                    imageUrl = CoverArt.getLargeThumbnailURL(nodeOptional.get());
                } catch (Exception ignored) {
                    imageUrl = null;
                }

            if (TextUtils.isEmpty(imageUrl))
                try {
                    imageUrl = CoverArt.getImageURL(nodeOptional.get());
                } catch (Exception ignored) {
                    imageUrl = null;
                }

            if (TextUtils.isEmpty(imageUrl))
                return Optional.of(EMPTY_STRING); //no useless re-trials
            return Optional.of(imageUrl);
        } else {
            return Optional.of(EMPTY_STRING); //no useless re-trials
        }

        /**
         * In this case no key as such,
         * Use the string only
         */
    }, data -> data, () -> EMPTY_STRING);

    public static final byte PLAYER = 0;
    public static final byte LIST = 1;
    private final byte type;

    public ReachMusicAdapter(Context context, int layout, Cursor c, int flags, byte type) {
        super(context, layout, c, flags);
        this.type = type;
    }

    private final class ViewHolder {
        private final ImageView listToggle, albumArt;
        private final TextView listTitle, listSubTitle, songDuration;

        private ViewHolder(ImageView listToggle, ImageView albumArt, TextView listTitle, TextView listSubTitle, TextView songDuration) {

            this.listToggle = listToggle;
            this.albumArt = albumArt;
            this.listTitle = listTitle;
            this.listSubTitle = listSubTitle;
            this.songDuration = songDuration;
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        final ViewHolder viewHolder = (ViewHolder) view.getTag();

        final String displayName;
        final String artist;
        final String album;
        final long duration;
        final short visibility;

        final byte[] albumArtData;

        switch (type) {
            case PLAYER: {
                displayName = cursor.getString(3);
                artist = cursor.getString(4);
                duration = cursor.getLong(5);
                album = cursor.getString(6);
                albumArtData = cursor.getBlob(8);
                visibility = 1;
                break;
            }
            case LIST: {
                displayName = cursor.getString(3);
                artist = cursor.getString(5);
                album = cursor.getString(6);
                duration = cursor.getLong(7);
                visibility = cursor.getShort(9);
                albumArtData = cursor.getBlob(10);
                break;
            }
            default:
                return;
        }

        viewHolder.listTitle.setText(displayName); //displayName
        viewHolder.songDuration.setText(MiscUtils.combinationFormatter(duration)); //duration
        /////////////
        viewHolder.listSubTitle.setText(artist); //artist
        if (TextUtils.isEmpty(viewHolder.listSubTitle.getText()))
            viewHolder.listSubTitle.setText(album); //album
        if (viewHolder.listToggle != null) {
            //Privacy Fragment stuff
            if (visibility == 0) { //visibility
                viewHolder.songDuration.setText("Invisible");
                Picasso.with(context).load(R.drawable.ic_action_visibility_off).noFade().into(viewHolder.listToggle);
                view.setAlpha(0.4f);
            } else {
                viewHolder.songDuration.setText(" Visible ");
                Picasso.with(context).load(R.drawable.ic_action_visibility_on).noFade().into(viewHolder.listToggle);
                view.setAlpha(1f);
            }
        }
//        Log.i("Ayush", "ReachMusic Adapter " + releaseGroupMbid + " " + artistMbid);
//        viewHolder.albumArt.setImageResource(R.drawable.music_note);
//
//        if (albumArtData != null && albumArtData.length > 0) {
//
//            final AlbumArtData artData;
//            try {
//                artData = new Wire(AlbumArtData.class).parseFrom(albumArtData, AlbumArtData.class);
//                if (artData != null)
//                    Log.i("Ayush", "Music Adapter " + artData.toString());
//            } catch (IOException ignored) {
//            }
//        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        final View view = super.newView(context, cursor, parent);
        final ViewHolder viewHolder = new ViewHolder(
                (ImageView) view.findViewById(R.id.listToggle),
                (ImageView) view.findViewById(R.id.albumArt),
                (TextView) view.findViewById(R.id.listTitle),
                (TextView) view.findViewById(R.id.listSubTitle),
                (TextView) view.findViewById(R.id.songDuration));

        view.setTag(viewHolder);
        return view;
    }

//    private final class ShowImage implements AsyncCache.ResultCallback<String> {
//
//        final ImageView imageView;
//        final Picasso picasso;
//
//        private ShowImage(ImageView imageView, Picasso picasso) {
//            this.imageView = imageView;
//            this.picasso = picasso;
//        }
//
//        @Override
//        public void result(Optional<String> data) {
//
//            if (!data.isPresent() || data.get().equals(EMPTY_STRING))
//                return;
//
//            Log.i("Ayush", "Picasso plz work " + data.get());
//            picasso.load(data.get()).fit().transform(circleTransform).into(imageView, new Callback() {
//                @Override
//                public void onSuccess() {
//                    Log.i("Ayush", "Picasso worked probably");
//                }
//
//                @Override
//                public void onError() {
//                    Log.i("Ayush", "Possible problem in picasso it self");
//                }
//            });
//
//
//            try {
//                Thread.sleep(10000L);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
}