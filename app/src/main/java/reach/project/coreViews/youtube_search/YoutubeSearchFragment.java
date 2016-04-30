package reach.project.coreViews.youtube_search;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import reach.project.R;
import reach.project.coreViews.saved_songs.SaveSongInDatabaseTask;
import reach.project.coreViews.saved_songs.SavedSongsDataModel;
import reach.project.utils.MiscUtils;
import reach.project.utils.ancillaryClasses.SuperInterface;

public class YoutubeSearchFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TAG = YoutubeSearchFragment.class.getSimpleName();

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private List<SearchResult> searchResults = new ArrayList<>(20);

    private SuperInterface mListener;
    private ListView listview;
    private YoutubeSearchResultAdapter searchdAdapter;
    private ImageButton searchButton;
    private EditText searchEditText;
    private ProgressBar loadingProgress;

    public YoutubeSearchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_youtube_search, container, false);

        final Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.exploreToolbar);
        toolbar.setTitle("Search");
        toolbar.inflateMenu(R.menu.explore_menu);
        toolbar.setOnMenuItemClickListener(mListener != null ? mListener.getMenuClickListener() : null);

        loadingProgress = (ProgressBar)rootView.findViewById(R.id.loadingProgress);
        listview = (ListView) rootView.findViewById(R.id.youtube_search_results_list);
        searchdAdapter = new YoutubeSearchResultAdapter(getActivity(), R.layout.saved_songs_list_item,
                R.id.songName,
                searchResults,
                mListener
        );

        listview.setAdapter(searchdAdapter);
        searchButton = (ImageButton) rootView.findViewById(R.id.youtube_search_button);
        searchEditText = (EditText) rootView.findViewById(R.id.youtube_search_edt);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() == 0){
                    searchdAdapter.clear();
                }

            }
        });

        searchEditText.setOnKeyListener(new View.OnKeyListener()
        {
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                Log.d(TAG, "onKey: " + event.toString());

                if (event.getAction() == KeyEvent.ACTION_DOWN)
                {
                    switch (keyCode)
                    {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                            break;
                        case KeyEvent.KEYCODE_ENTER:
                            if(TextUtils.isEmpty(searchEditText.getText()))
                               return true;
                            searchdAdapter.clear();

                            loadingProgress.setVisibility(View.VISIBLE);
                            new YTTest(YoutubeSearchFragment.this).execute(searchEditText.getText().toString());
                            hideSoftKeyboard();
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(TextUtils.isEmpty(searchEditText.getText()))
                    return;
                searchdAdapter.clear();

                loadingProgress.setVisibility(View.VISIBLE);
                new YTTest(YoutubeSearchFragment.this).execute(searchEditText.getText().toString());
                hideSoftKeyboard();

            }
        });


        return rootView;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SuperInterface) {
            mListener = (SuperInterface) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement SuperInterface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private static class YTTest extends AsyncTask<String, Void, List<SearchResult>> {

        private final WeakReference<YoutubeSearchFragment> reference;

        public YTTest(YoutubeSearchFragment activity) {
            reference = new WeakReference<YoutubeSearchFragment>(activity);


        }

        @Override
        protected List<SearchResult> doInBackground(String... params) {
            try {
                final HttpTransport transport = new NetHttpTransport();
                final JsonFactory factory = new JacksonFactory();
                final HttpRequestInitializer initialize = request -> {
                    request.setConnectTimeout(request.getConnectTimeout() * 2);
                    request.setReadTimeout(request.getReadTimeout() * 2);
                };
                final YouTube youTube = new YouTube.Builder(transport, factory, initialize).build();
                // Define the API request for retrieving search results.
                final YouTube.Search.List search = youTube.search().list("snippet");

                // Set your developer key from the Google Developers Console for
                // non-authenticated requests. See:
                // https://console.developers.google.com/
                final String apiKey = "AIzaSyAYH8mcrHrqG7HJwjyGUuwxMeV7tZP6nmY";
                search.setKey(apiKey);

                search.setQ(params[0]);

                // Restrict the search results to only include videos. See:
                // https://developers.google.com/youtube/v3/docs/search/list#type
                search.setType("video");

                search.setVideoCategoryId("10");

                // To increase efficiency, only retrieve the fields that the
                // application uses.
                search.setFields( "items(id/videoId,snippet/title,snippet/thumbnails/default/url)");
                search.setMaxResults(20L);

                // Call the API and print results.
                final SearchListResponse searchResponse = search.execute();
                final List<SearchResult> searchResultList = searchResponse.getItems();
                /*final StringBuilder stringBuilder = new StringBuilder();
                for (SearchResult searchResult : searchResultList)
                    stringBuilder.append(searchResult.getSnippet().getTitle()).append("\n\n");
                return stringBuilder.toString();*/
                if (searchResultList == null || searchResultList.isEmpty())
                    return null;

                return searchResultList;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<SearchResult> searchResult) {
            super.onPostExecute(searchResult);
            /*MiscUtils.useContextFromFragment(reference, activity -> {
                new AlertDialog.Builder(activity).setMessage(s).setTitle("Youtube").create().show();
            });*/
            if (searchResult == null)
                return;



            MiscUtils.useFragment(reference, fragment -> {
                fragment.loadingProgress.setVisibility(View.GONE);
                fragment.searchdAdapter.clear();
                fragment.searchdAdapter.addAll(searchResult);
            });
        }
    }

    static class YoutubeSearchResultAdapter extends ArrayAdapter<SearchResult> implements View.OnClickListener {


        private final LayoutInflater inflater;
        private final SuperInterface mListener;

        public YoutubeSearchResultAdapter(Context context, int resource, int textViewResourceId, List<SearchResult> objects,
                                          SuperInterface mListener) {
            super(context, resource, textViewResourceId, objects);
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.mListener = mListener;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if(convertView == null){
                holder = new ViewHolder(inflater.inflate(R.layout.saved_songs_list_item,parent,false));
                convertView = holder.itemView;
                convertView.setTag(holder);
            }
            else{
                holder = (ViewHolder) convertView.getTag();
            }

            final String albumArt = getItem(position).getSnippet().getThumbnails().getDefault().getUrl();

            if (!TextUtils.isEmpty(albumArt)) {
                holder.thumbnail.setController(Fresco.newDraweeControllerBuilder()
                        .setOldController(holder.thumbnail.getController())
                        .setImageRequest(ImageRequestBuilder.newBuilderWithSource(Uri.parse(albumArt))
                                .build())
                        .build());
            }
            holder.addSong.setTag(position);
            holder.addSong.setOnClickListener(this);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mListener == null){
                        Toast.makeText(v.getContext().getApplicationContext(), "Sorry! An error occured.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mListener.showYTVideo(getItem(position).getId().getVideoId());
                }
            });

            holder.song_name.setText(getItem(position).getSnippet().getTitle());

            return convertView;
        }

        @Override
        public int getCount() {
            return super.getCount();
        }

        @Override
        public void onClick(View v) {
            final int id = v.getId();

            final int pos = (int) v.getTag();
            final SearchResult data  = getItem(pos);

            switch (id){

                case R.id.popup_menu:

                    SavedSongsDataModel saved_song_data = new SavedSongsDataModel.Builder()
                            .withSongName(data.getSnippet().getTitle())
                            .withYoutube_Id(data.getId().getVideoId())
                            .withDisplayName(data.getSnippet().getTitle())
                            .withDate_Added(System.currentTimeMillis())
                            .withType(1)
                            .build();


                    new SaveSongInDatabaseTask(getContext(),saved_song_data).execute();

                    break;


            }

        }

        static class ViewHolder{

            final TextView song_name;
            final ImageView addSong;
            final SimpleDraweeView thumbnail;
            final View itemView;

            public ViewHolder(View v) {
                this.itemView = v;
                this.song_name = (TextView) v.findViewById(R.id.songName);
                this.addSong = (ImageView) v.findViewById(R.id.popup_menu);
                this.addSong.setImageResource(R.drawable.save_button_selector);
                this.thumbnail = (SimpleDraweeView) v.findViewById(R.id.songThumbnail);
            }
        }

    }

    private void hideSoftKeyboard(){
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


}
