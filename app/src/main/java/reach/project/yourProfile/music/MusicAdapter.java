package reach.project.yourProfile.music;

import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.squareup.wire.Message;

import java.util.List;

import reach.project.R;
import reach.project.music.Song;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;

/**
 * Created by dexter on 13/11/15.
 */
public class MusicAdapter<T extends Message> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final CacheAdapterInterface<T> cacheAdapterInterface;

    public MusicAdapter(CacheAdapterInterface<T> cacheAdapterInterface) {
        this.cacheAdapterInterface = cacheAdapterInterface;
        setHasStableIds(true);
    }

    @Override
    public int getItemViewType(int position) {

        final Message message = cacheAdapterInterface.getItem(position);
        if (message instanceof Song)
            return 0;
        else if (message instanceof RecentSong)
            return 1;
        else if (message instanceof SmartSong)
            return 2;
        else
            return super.getItemViewType(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {
            case 0:
                return new TestHolder<>(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_0, parent, false), cacheAdapterInterface);
            case 1:
                return new TestHolder1(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_1, parent, false));
            case 2:
                return new TestHolder2(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.list_item_2, parent, false));
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final Message message = cacheAdapterInterface.getItem(position);
        if (message instanceof Song) {

            final Song song = (Song) message;
            final TestHolder testHolder = (TestHolder) holder;
            testHolder.bindTest(position);
            testHolder.mTextView.setText(song.displayName);
            testHolder.mTextView2.setText(song.artist);
            final Optional<Uri> uriOptional = AlbumArtUri.getUri(song.album, song.artist, song.displayName);

            if (uriOptional.isPresent()) {

                Log.i("Ayush", "Url found = " + uriOptional.get().toString());

                final ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uriOptional.get())
                        .setResizeOptions(new ResizeOptions(50, 50))
                        .build();

                final DraweeController controller = Fresco.newDraweeControllerBuilder()
                        .setOldController(testHolder.mAlbumArt.getController())
                        .setImageRequest(request)
                        .build();

                testHolder.mAlbumArt.setController(controller);
            } else
                testHolder.mAlbumArt.setImageBitmap(null);

        } else if (message instanceof RecentSong) {

            final RecentSong recentSong = (RecentSong) message;
            final TestHolder1 testHolder1 = (TestHolder1) holder;
            testHolder1.mTextView1.setText(recentSong.title);
            testHolder1.mRecyclerView1.setLayoutManager(new CustomGridLayoutManager(holder.itemView.getContext(), 2));
            if (recentSong.songList.size() < 4)
                testHolder1.mRecyclerView1.setAdapter(new TestAdapter1(recentSong.songList));
            else
                testHolder1.mRecyclerView1.setAdapter(new TestAdapter1(recentSong.songList.subList(0,4)));

        } else if (message instanceof SmartSong) {

            final SmartSong smartSong = (SmartSong) message;
            final TestHolder2 testHolder2 = (TestHolder2) holder;
            testHolder2.mTextView2.setText(smartSong.title);
            testHolder2.mRecyclerView2.setLayoutManager(new CustomLinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            if (smartSong.songList.size() < 4)
                testHolder2.mRecyclerView2.setAdapter(new TestAdapter2(smartSong.songList));
            else
                testHolder2.mRecyclerView2.setAdapter(new TestAdapter2(smartSong.songList.subList(0,4)));
        }
    }

    private static class TestHolder<T extends Message> extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        public final TextView mTextView;
        public final TextView mTextView2;
        public final SimpleDraweeView mAlbumArt;
        private final CacheAdapterInterface<T> cacheAdapterInterface;

        private int position;

        private TestHolder(View itemView, CacheAdapterInterface<T> cacheAdapterInterface) {

            super(itemView);
            itemView.setOnClickListener(this);

            this.mTextView = (TextView) itemView.findViewById(R.id.textView);
            this.mTextView2 = (TextView) itemView.findViewById(R.id.textView2);
            this.mAlbumArt = (SimpleDraweeView) itemView.findViewById(R.id.albumArt);
            this.cacheAdapterInterface = cacheAdapterInterface;
        }

        public void bindTest(int mPos) {
            this.position = mPos;
        }

        @Override
        public void onClick(View v) {
            Snackbar.make(v, cacheAdapterInterface.getItem(position).hashCode() + " clicked", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public long getItemId(int position) {
        return cacheAdapterInterface.getItemId(cacheAdapterInterface.getItem(position));
    }

    @Override
    public int getItemCount() {
//        Log.i("Ayush", "Returning new size " + cacheAdapterInterface.getCount());
        return cacheAdapterInterface.getCount();
    }

    public interface CacheAdapterInterface<T extends Message> {

        int getCount();

        long getItemId(T item);

        T getItem(int position);
    }

    public class TestHolder1 extends RecyclerView.ViewHolder {

        private TextView mTextView1;
        private RecyclerView mRecyclerView1;

        private TestHolder1(View itemView) {
            super(itemView);
            mTextView1 = (TextView) itemView.findViewById(R.id.textView1);
            mRecyclerView1 = (RecyclerView) itemView.findViewById(R.id.recyclerView1);
        }
    }

    public class TestHolder2 extends RecyclerView.ViewHolder {

        private TextView mTextView2;
        private RecyclerView mRecyclerView2;

        private TestHolder2(View itemView) {
            super(itemView);
            mTextView2 = (TextView) itemView.findViewById(R.id.textView2);
            mRecyclerView2 = (RecyclerView) itemView.findViewById(R.id.recyclerView2);
        }
    }

    private class TestAdapter1
            extends RecyclerView.Adapter<TestAdapter1.Test1Holder> {

        private List<Song> mSongsList;

        private TestAdapter1(List<Song> songsList) {
            this.mSongsList = songsList;
        }

        @Override
        public Test1Holder onCreateViewHolder(ViewGroup parent, int mType) {
            return new Test1Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_1, parent, false));
        }

        @Override
        public void onBindViewHolder(Test1Holder holder, int position) {
            holder.bindTest(position);
            Song song = mSongsList.get(position);
            holder.mTextView.setText(song.displayName);
            holder.mTextView2.setText(song.artist);
            final Optional<Uri> uriOptional = AlbumArtUri.getUri(song.album, song.artist, song.displayName);

            if (uriOptional.isPresent()) {

                Log.i("Ayush", "Url found = " + uriOptional.get().toString());

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

        public class Test1Holder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private TextView mTextView;
            private TextView mTextView2;
            private SimpleDraweeView mAlbumArt;
            private int pos;

            private Test1Holder(View itemView) {
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

    private class TestAdapter2
            extends RecyclerView.Adapter<TestAdapter2.Test2Holder> {

        private List<Song> mSongsList;

        private TestAdapter2(List<Song> songsList) {
            this.mSongsList = songsList;
        }

        @Override
        public Test2Holder onCreateViewHolder(ViewGroup parent, int mType) {
            return new Test2Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_1, parent, false));
        }

        @Override
        public void onBindViewHolder(Test2Holder holder, int position) {
            holder.bindTest(position);
            Song song = mSongsList.get(position);
            holder.mTextView.setText(song.displayName);
            holder.mTextView2.setText(song.artist);
            final Optional<Uri> uriOptional = AlbumArtUri.getUri(song.album, song.artist, song.displayName);

            if (uriOptional.isPresent()) {

                Log.i("Ayush", "Url found = " + uriOptional.get().toString());

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

        public class Test2Holder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private TextView mTextView;
            private TextView mTextView2;
            private SimpleDraweeView mAlbumArt;
            private int pos;

            private Test2Holder(View itemView) {
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
