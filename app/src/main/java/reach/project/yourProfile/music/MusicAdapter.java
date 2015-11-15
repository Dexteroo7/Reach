package reach.project.yourProfile.music;

import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.wire.Message;

import reach.project.R;
import reach.project.music.Song;

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
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType) {

            case 0:
                return new TestHolder<>(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_0, parent, false), cacheAdapterInterface);
//            case 1:
//                return new TestHolder1(LayoutInflater.from(parent.getContext())
//                        .inflate(R.layout.list_item_1, parent, false));
//            case 2:
//                return new TestHolder2(LayoutInflater.from(parent.getContext())
//                        .inflate(R.layout.list_item_2, parent, false));
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (position == 0)
            holder.itemView.setBackgroundResource(R.drawable.border_shadow2);
        switch (holder.getItemViewType()) {

            case 0:
                final Song song = (Song) cacheAdapterInterface.getItem(position);
                final TestHolder testHolder = (TestHolder) holder;
                testHolder.bindTest(position);
                testHolder.mTextView.setText(song.displayName);
                break;
//            case 1:
//                TestHolder1 testHolder1 = (TestHolder1) holder;
//                testHolder1.mTextView1.setText(yourProfile.song);
//                testHolder1.mRecyclerView1.setLayoutManager(new CustomGridLayoutManager(getActivity(), 2));
//                testHolder1.mRecyclerView1.setAdapter(new TestAdapter1(yourProfile.songList));
//                break;
//            case 2:
//                TestHolder2 testHolder2 = (TestHolder2) holder;
//                testHolder2.mTextView2.setText(yourProfile.song);
//                testHolder2.mRecyclerView2.setLayoutManager(new CustomLinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
//                testHolder2.mRecyclerView2.setAdapter(new TestAdapter2(yourProfile.songList));
//                break;
        }
    }

    private static class TestHolder<T extends Message> extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private final TextView mTextView;
        private final CacheAdapterInterface<T> cacheAdapterInterface;

        private int position;

        private TestHolder(View itemView, CacheAdapterInterface<T> cacheAdapterInterface) {

            super(itemView);
            itemView.setOnClickListener(this);

            this.mTextView = (TextView) itemView.findViewById(R.id.textView);
            this.cacheAdapterInterface = cacheAdapterInterface;
        }

        private void bindTest(int mPos) {
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
        Log.i("Ayush", "Returning new size " + cacheAdapterInterface.getCount());
        return cacheAdapterInterface.getCount();
    }

    public interface CacheAdapterInterface<T extends Message> {

        int getCount();

        long getItemId(T item);

        T getItem(int position);
    }
}
