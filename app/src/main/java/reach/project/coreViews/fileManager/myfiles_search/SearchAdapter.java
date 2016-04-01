package reach.project.coreViews.fileManager.myfiles_search;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.common.base.Optional;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import reach.project.R;
import reach.project.apps.App;
import reach.project.coreViews.fileManager.HandOverMessageExtra;
import reach.project.music.Song;
import reach.project.music.SongCursorHelper;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.ThreadLocalRandom;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;
import reach.project.utils.viewHelpers.HandOverMessage;
import reach.project.utils.viewHelpers.MoreListHolder;

/**
 * Created by gauravsobti on 01/04/16.
 */


//TODO: Optimization: Instead of using another recyclerView for apps, use the same recyclerView

class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Closeable {

    private static final String TAG = SearchAdapter.class.getSimpleName();

    private static final byte VIEW_TYPE_APPS = 0;
    private static final byte VIEW_TYPE_SONGS = 1;
    private static final byte VIEW_TYPE_SONG_HEADER = 2;
    private final HandOverMessage<Cursor> handOverCursor;
    private final AppsSearchAdapter appsSearchAdapter;
    private final HandOverMessage<App> handOverApp;
    private final PackageManager packageManager;

    private final ResizeOptions resizeOptions = new ResizeOptions(150, 150);
    private final long appHolderId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
    private final long songHeaderHolderId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);

    private final HandOverMessageExtra<Object> handOverSongMessageExtra = new HandOverMessageExtra<Object>() {
        @Override
        public void handOverMessage(@Nonnull Integer position) {

            final Object object = getItem(position);
            if (object instanceof Cursor)
                SearchAdapter.this.handOverCursor.handOverMessage((Cursor) object);
            else if (object instanceof App)
                SearchAdapter.this.handOverApp.handOverMessage((App) object);
            else
                throw new IllegalStateException("Resource cursor has been corrupted");
        }

        @Override
        public Object getExtra(@Nonnull Integer position) {

            final Object object = getItem(position);
            if (object instanceof Cursor)
                return (Cursor) object;
            else if (object instanceof App)
                return (App) object;
            else
                throw new IllegalStateException("Resource cursor has been corrupted");
        }

        @Override
        public void putExtra(int position, Object item) {

        }

        @Override
        public void handOverAppVisibilityMessage(String packageName) {

        }

        @Override
        public void handOverSongVisibilityMessage(int position, Object message) {
            SearchAdapter.this.handOverVisibilityToggle.HandoverMessage(position,message);
        }


    };
    private final HandOverVisibilityToggle handOverVisibilityToggle;
    private List<App> appsData;

    public SearchAdapter(HandOverMessage<Cursor> handOverCursor,
                         HandOverVisibilityToggle handOverVisibilityToggle,
                         HandOverMessage<App> handOverApp, Context context
    ) {

        this.handOverCursor = handOverCursor;
        this.packageManager = context.getPackageManager();
        this.handOverApp = handOverApp;
        this.handOverVisibilityToggle = handOverVisibilityToggle;
        this.appsSearchAdapter = new AppsSearchAdapter(new ArrayList<>(20), handOverApp, packageManager, R.layout.app_list_item,context);
        setHasStableIds(true);
    }

    ///////////Data set ops
    @Nullable
    private Cursor myLibraryCursor = null;

    public void setNewMyLibraryCursor(@Nullable Cursor newMyLibraryCursor) {

        //destroy
        if (this.myLibraryCursor != null)
            myLibraryCursor.close();

        //set
        this.myLibraryCursor = newMyLibraryCursor;
        notifyDataSetChanged();
    }

    public void updateRecentApps(List<App> newRecent) {
        if (newRecent.isEmpty())
            return;
        appsSearchAdapter.updateRecent(newRecent);
        this.appsData = newRecent;
    }

    void filterApps(String constraint) {
        //List<App> appsData = appsSearchAdapter.getMessageList();
        List<App> filteredAppData = new ArrayList<>();
        for (App app : appsData) {
            if (app.applicationName.toLowerCase().contains(constraint)) {
                filteredAppData.add(app);
            }

        }
        appsSearchAdapter.getMessageList().clear();
        appsSearchAdapter.getMessageList().addAll(filteredAppData);
        appsSearchAdapter.notifyDataSetChanged();
    }


    @Override
    public void close() {

        MiscUtils.closeQuietly(myLibraryCursor);
        myLibraryCursor = null;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Log.i("Ayush", "Creating ViewHolder " + getClass().getName());

        switch (viewType) {

            case VIEW_TYPE_SONGS: {

                return new SongItemHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.song_list_item, parent, false), handOverSongMessageExtra);
            }

            case VIEW_TYPE_APPS: {

                final MoreListHolder moreListHolder = new MoreListHolder(parent);
                moreListHolder.itemView.setBackgroundResource(0);
                moreListHolder.headerText.setText("Apps");
                moreListHolder.moreButton.setVisibility(View.GONE);
                moreListHolder.listOfItems.setLayoutManager(new CustomLinearLayoutManager(moreListHolder.listOfItems.getContext()));
                moreListHolder.listOfItems.setAdapter(appsSearchAdapter);
                return moreListHolder;
            }
            case VIEW_TYPE_SONG_HEADER: {

                final SongHeaderHolder songHeaderHolder = new SongHeaderHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(android.R.layout.simple_list_item_1, parent, false)
                );
                return songHeaderHolder;

            }

            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        Log.i("Ayush", "Binding ViewHolder " + getClass().getName());

        final Object friend = getItem(position);
        if (friend instanceof Cursor) {

            final Cursor cursorExactType = (Cursor) friend;
            final Song song = SongCursorHelper.SONG_HELPER.parse((Cursor) friend);
            final SongItemHolder songItemHolder = (SongItemHolder) holder;

            songItemHolder.position = cursorExactType.getPosition() + 1;
            holder.itemView.setBackgroundResource(R.drawable.border_shadow1);

            final String displayName, artist, album, actualName, senderName;
            final boolean visible;
            //if (cursorExactType.getColumnCount() == SongHelper.MUSIC_DATA_LIST.length) {

            displayName = cursorExactType.getString(3);
            artist = cursorExactType.getString(5);
            album = cursorExactType.getString(6);
            visible = cursorExactType.getShort(12) == 1;
            senderName = cursorExactType.getString(16);


            /*if (visible) {
                songItemHolder.toggleImage.setImageResource(0);
                //songItemHolder.toggleText.setText("Everyone");
            } else {
                songItemHolder.toggleImage.setImageResource(R.drawable.icon_locked);
                //songItemHolder.toggleText.setText("Only Me");
            }*/

//                actualName = cursorExactType.getString(17);
            final long senderId = cursorExactType.getLong(15);

            final Context context = holder.itemView.getContext();
            //songItemHolder.userImage.setVisibility(View.VISIBLE);
            songItemHolder.artistName.setTextColor(ContextCompat.getColor(context, R.color.reach_color));
            //songItemHolder.artistName.setText(artist);
            final int length = MiscUtils.dpToPx(20);


            if (senderName == null || senderName.equals("") || senderName.equals("null")) {
                Log.d(TAG, "senderName = null, song Name = " + displayName);
                songItemHolder.userImage.setVisibility(View.GONE);
                songItemHolder.artistName.setText(artist);
            } else {
                songItemHolder.userImage.setVisibility(View.VISIBLE);
                songItemHolder.artistName.setText(senderName);
                songItemHolder.userImage.setImageURI(AlbumArtUri.getUserImageUri(
                        senderId,
                        "imageId",
                        "rw",
                        true,
                        length,
                        length));
            }

            final String liked = cursorExactType.getString(13);
            Log.d(TAG, "Position = " + position + " liked = " + liked);
            if (liked == null || liked.equals("0")) {
                songItemHolder.likeButton.setSelected(false);
            } else {
                songItemHolder.likeButton.setSelected(true);
            }


            songItemHolder.songName.setText(displayName);

            final Optional<Uri> uriOptional = AlbumArtUri.getUri(song.album, song.artist, song.displayName, false);

            if (uriOptional.isPresent()) {

//            Log.i("Ayush", "Url found = " + uriOptional.get().toString());
                final ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uriOptional.get())
                        .setResizeOptions(resizeOptions)
                        .build();

                final DraweeController controller = Fresco.newDraweeControllerBuilder()
                        .setOldController(songItemHolder.albumArt.getController())
                        .setImageRequest(request)
                        .build();

                songItemHolder.albumArt.setController(controller);
            } else
                songItemHolder.albumArt.setImageBitmap(null);

        }
    }

    @Nonnull
    private Object getItem(int position) {
        if(position == 0)
            return -1;

        else if (myLibraryCursor != null && position == myLibraryCursor.getCount()+1)
            return false; //apps

        else {
            position--; //account for songsHeader
            if (myLibraryCursor == null || myLibraryCursor.isClosed() || !myLibraryCursor.moveToPosition(position))
                throw new IllegalStateException("Resource cursor has been corrupted");
            return myLibraryCursor;
        }
    }

    @Override
    public int getItemViewType(int position) {
        final Object item = getItem(position);
        if (item instanceof Cursor)
            return VIEW_TYPE_SONGS;
        else if(item instanceof Boolean)
            return VIEW_TYPE_APPS;
        else
            return VIEW_TYPE_SONG_HEADER;
    }

    @Override
    public long getItemId(int position) {
        final Object item = getItem(position);
        if (item instanceof Cursor) {
            return ((Cursor) item).getLong(0); //_id || song_id
        } else if (item instanceof Boolean)
            return appHolderId;
        else {
            return songHeaderHolderId;
        }
    }

    @Override
    public int getItemCount() {

        if (myLibraryCursor != null && !myLibraryCursor.isClosed())
            return myLibraryCursor.getCount() + 2;//adjust for recent list
        return 0;
    }

    //Visibility Of Apps

    public static interface HandOverVisibilityToggle {

        public void HandoverMessage(int position, @NonNull Object message);
    }

    private static class SongHeaderHolder extends RecyclerView.ViewHolder{

        final TextView headerText;

        public SongHeaderHolder(View itemView) {
            super(itemView);
            headerText = (TextView) itemView.findViewById(android.R.id.text1);
            headerText.setText("Songs");
            headerText.setTextColor(Color.DKGRAY);
            headerText.setBackgroundColor(Color.WHITE);
        }
    }

}
