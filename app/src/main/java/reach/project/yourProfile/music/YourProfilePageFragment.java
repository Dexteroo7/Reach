package reach.project.yourProfile.music;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.florent37.materialviewpager.MaterialViewPagerHelper;
import com.github.florent37.materialviewpager.adapter.RecyclerViewMaterialAdapter;

import java.util.ArrayList;
import java.util.List;

import reach.project.R;
import reach.project.utils.viewHelpers.CustomGridLayoutManager;
import reach.project.utils.viewHelpers.CustomLinearLayoutManager;

/**
 * A placeholder fragment containing a simple view.
 * Created by ashish on 13/11/15.
 */
public class YourProfilePageFragment extends Fragment {

    private class YourProfile {
        //0-song,1-recent,2-smartlist
        private int type;

        private String song;
        private List<String> songList;

        public YourProfile(int type, String song, List<String> songList) {
            this.type = type;
            this.song = song;
            this.songList = songList;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_yourprofile_page, container, false);
        final RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        //mRecyclerView.setHasFixedSize(true);

        final List<YourProfile> list = new ArrayList<>();

        final List<String> songList = new ArrayList<>();
        for (int i = 0 ; i < 10; i++)
            songList.add("Recent " + (i+1));
        list.add(new YourProfile(1, "Recent", songList));

        for (int i = 0 ; i < 10; i++)
            list.add(new YourProfile(0, "Song " + (i+1), null));

        final List<String> songList2 = new ArrayList<>();
        for (int i = 0 ; i < 10; i++)
            songList2.add("Favourite " + (i+1));
        list.add(new YourProfile(2, "Favourite", songList2));

        for (int i = 0 ; i < 10; i++)
            list.add(new YourProfile(0, "Song " + (i+1), null));

        mRecyclerView.setLayoutManager(new CustomLinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(new RecyclerViewMaterialAdapter(new TestAdapter(list)));
        MaterialViewPagerHelper.registerRecyclerView(getActivity(), mRecyclerView, null);
        return rootView;
    }

    private class TestAdapter
            extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<YourProfile> mList;

        private TestAdapter(List<YourProfile> yourProfileList) {
            this.mList = yourProfileList;
        }

        @Override
        public int getItemViewType(int position) {
            return mList.get(position).type;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int mType) {
            switch (mType) {
                case 0:
                    return new TestHolder(LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.list_item_0, parent, false));
                case 1:
                    return new TestHolder1(LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.list_item_1, parent, false));
                case 2:
                    return new TestHolder2(LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.list_item_2, parent, false));
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position == 0)
                holder.itemView.setBackgroundResource(R.drawable.border_shadow2);
            YourProfile yourProfile = mList.get(position);
            switch (holder.getItemViewType()) {
                case 0:
                    TestHolder testHolder = (TestHolder) holder;
                    testHolder.bindTest(position);
                    testHolder.mTextView.setText(yourProfile.song);
                    break;
                case 1:
                    TestHolder1 testHolder1 = (TestHolder1) holder;
                    testHolder1.mTextView1.setText(yourProfile.song);
                    testHolder1.mRecyclerView1.setLayoutManager(new CustomGridLayoutManager(getActivity(), 2));
                    testHolder1.mRecyclerView1.setAdapter(new TestAdapter1(yourProfile.songList));
                    break;
                case 2:
                    TestHolder2 testHolder2 = (TestHolder2) holder;
                    testHolder2.mTextView2.setText(yourProfile.song);
                    testHolder2.mRecyclerView2.setLayoutManager(new CustomLinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
                    testHolder2.mRecyclerView2.setAdapter(new TestAdapter2(yourProfile.songList));
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        public class TestHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private TextView mTextView;
            private int pos;

            private TestHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
                mTextView = (TextView) itemView.findViewById(R.id.textView);
            }

            private void bindTest(int mPos) {
                pos = mPos;
            }

            @Override
            public void onClick(View v) {
                Snackbar.make(v, mList.get(pos).song + " clicked", Snackbar.LENGTH_SHORT).show();
            }
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
    }

    private class TestAdapter1
            extends RecyclerView.Adapter<TestAdapter1.Test1Holder> {

        private List<String> mSongsList;

        private TestAdapter1(List<String> songsList) {
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
            holder.mTextView.setText( mSongsList.get(position));
        }

        @Override
        public int getItemCount() {
            return mSongsList.size();
        }

        public class Test1Holder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private TextView mTextView;
            private int pos;

            private Test1Holder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
                mTextView = (TextView) itemView.findViewById(R.id.textView);
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

        private List<String> mSongsList;

        private TestAdapter2(List<String> songsList) {
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
            holder.mTextView.setText( mSongsList.get(position));
        }

        @Override
        public int getItemCount() {
            return mSongsList.size();
        }

        public class Test2Holder extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private TextView mTextView;
            private int pos;

            private Test2Holder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
                mTextView = (TextView) itemView.findViewById(R.id.textView);
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