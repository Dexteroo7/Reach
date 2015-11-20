package reach.project.yourProfile.music;

import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.common.base.Optional;
import com.squareup.wire.Wire;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import reach.project.R;
import reach.project.music.Song;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;

/**
 * Created by ashish on 18/11/15.
 */
public class MoreMusicFragment extends Fragment {

    private static WeakReference<MoreMusicFragment> reference = null;

    public static MoreMusicFragment newInstance(RecentSong recentSong) {

        final Bundle args;
        MoreMusicFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new MoreMusicFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing YourProfileMusicFragment object :)");
            args = fragment.getArguments();
        }
        args.putInt("type", 0);
        args.putByteArray("songList", recentSong.toByteArray());
        return fragment;
    }

    public static MoreMusicFragment newInstance(SmartSong recentSong) {

        final Bundle args;
        MoreMusicFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new MoreMusicFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing YourProfileMusicFragment object :)");
            args = fragment.getArguments();
        }
        args.putInt("type", 1);
        args.putByteArray("songList", recentSong.toByteArray());
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_more_music, container, false);

        final Toolbar mToolbar = (Toolbar) rootView.findViewById(R.id.moreMusicToolbar);
        final RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new CustomGridLayoutManager(container.getContext(), 2));
        byte[] bytes = getArguments().getByteArray("songList");
        if (bytes == null)
            return rootView;
        List<Song> songList = null;
        String title = null;
        try {
            if (getArguments().getInt("type") == 0) {
                RecentSong recentSong = new Wire(RecentSong.class).parseFrom(bytes, RecentSong.class);
                songList = recentSong.songList;
                title = recentSong.title;
            }
            else {
                SmartSong smartSong = new Wire(SmartSong.class).parseFrom(bytes, SmartSong.class);
                songList = smartSong.songList;
                title = smartSong.title;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mToolbar.setTitle(title);
        mRecyclerView.setAdapter(new MoreMusicAdapter(songList));

        return rootView;
    }

    private class MoreMusicAdapter
            extends RecyclerView.Adapter<MoreMusicAdapter.MoreMusicHolder> {

        private List<Song> mSongsList;

        private MoreMusicAdapter(List<Song> songsList) {
            this.mSongsList = songsList;
        }

        @Override
        public MoreMusicHolder onCreateViewHolder(ViewGroup parent, int mType) {
            return new MoreMusicHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.song_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(MoreMusicHolder holder, int position) {
            holder.bindTest(position);
            Song song = mSongsList.get(position);
            holder.mTextView.setText(song.displayName);
            holder.mTextView2.setText(song.artist);
            final Optional<Uri> uriOptional = AlbumArtUri.getUri(song.album, song.artist, song.displayName);

            if (uriOptional.isPresent()) {

//                Log.i("Ayush", "Url found = " + uriOptional.get().toString());

                final ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uriOptional.get())
                        .setResizeOptions(new ResizeOptions(200, 200))
                        .build();

                final DraweeController controller = Fresco.newDraweeControllerBuilder()
                        .setOldController(holder.mAlbumArt.getController())
                        .setImageRequest(request)
                        .build();

                holder.mAlbumArt.setController(controller);
            } else
                holder.mAlbumArt.setImageBitmap(null);
        }

        @Override
        public int getItemCount() {
            return mSongsList.size();
        }

        public class MoreMusicHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private TextView mTextView;
            private TextView mTextView2;
            private SimpleDraweeView mAlbumArt;
            private int pos;

            private MoreMusicHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
                mTextView = (TextView) itemView.findViewById(R.id.textView);
                mTextView2 = (TextView) itemView.findViewById(R.id.textView2);
                mAlbumArt = (SimpleDraweeView) itemView.findViewById(R.id.albumArt);
            }

            private void bindTest(int mPos) {
                pos = mPos;
            }

            @Override
            public void onClick(View v) {
                Snackbar.make(v, mSongsList.get(pos) + " clicked", Snackbar.LENGTH_SHORT).show();
            }
        }
    }
}
