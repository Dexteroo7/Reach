package reach.project.coreViews.explore;

import android.content.Context;
import android.net.Uri;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import reach.project.R;
import reach.project.coreViews.saved_songs.SavedSongsDataModel;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.YouTubeDataModel;
import reach.project.utils.viewHelpers.HandOverMessage;

/**
 * Created by gauravsobti on 22/04/16.
 */
public class ExploreRecyclerViewAdapter extends RecyclerView.Adapter implements View.OnClickListener {

    public static final String TAG = ExploreRecyclerViewAdapter.class.getSimpleName();
    private final Explore explore;
    private final HandOverMessage<Object> handOverMessage;
    private static final int VIEW_TYPE_IS_DONE_FOR_TODAY = 0;
    private static final int VIEW_TYPE_LOADING = 1;
    private static final int VIEW_TYPE_MUSIC = 2;
    private static final int VIEW_TYPE_MISC = 3;
    private final LayoutInflater inflater;


    public ExploreRecyclerViewAdapter(Context context, Explore explore,
                                      HandOverMessage<Object> handOverId){

        this.explore = explore;
        this.handOverMessage = handOverId;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        switch (viewType){

            case VIEW_TYPE_IS_DONE_FOR_TODAY:
                return new IsDoneForTodayOrLoadingViewHolder(inflater.inflate(ExploreLoaderTypes.DONE_FOR_TODAY.getLayoutResId(),parent,false));

            case VIEW_TYPE_LOADING:
                return new IsDoneForTodayOrLoadingViewHolder(inflater.inflate(ExploreLoaderTypes.LOADING.getLayoutResId(),parent,false));

            case VIEW_TYPE_MUSIC:
                return new MusicViewHolder(inflater.inflate(R.layout.explore_music,parent,false));

            case VIEW_TYPE_MISC:
                return new MiscViewHolder(inflater.inflate(R.layout.explore_misc,parent,false));

        }

        return null;
    }




    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if(holder instanceof  MusicViewHolder){
            final MusicViewHolder musicHolder = (MusicViewHolder) holder;
            final JsonObject exploreJSON = explore.getContainerForIndex(position);

            musicHolder.downBtn.setOnClickListener(this);
            musicHolder.fb_share_btn.setOnClickListener(this);
            musicHolder.userHandle.setOnClickListener(this);
            musicHolder.userImage.setOnClickListener(this);
            musicHolder.saveBtn.setOnClickListener(this);

            final YouTubeDataModel ytbData = new YouTubeDataModel();

            final JsonElement ytElement = MiscUtils.get(exploreJSON, ExploreJSON.YOUTUBE_ID);
            final String ytID;
            if (ytElement == null)
                ytID = "";
            else
                ytID = ytElement.getAsString();

            ytbData.setId(ytID);

            final String albumArt = "https://i.ytimg.com/vi/" + ytID + "/hqdefault.jpg";
            if (!TextUtils.isEmpty(albumArt)) {
                musicHolder.image.setController(Fresco.newDraweeControllerBuilder()
                        .setOldController(musicHolder.image.getController())
                        .setImageRequest(ImageRequestBuilder.newBuilderWithSource(Uri.parse(albumArt))
                                .build())
                        .build());
                ytbData.setImageUrl(albumArt);
            }


            musicHolder.fb_share_btn.setTag(ytbData);
            final long userId = MiscUtils.get(exploreJSON, ExploreJSON.ID).getAsLong();
            musicHolder.userHandle.setTag(userId);
            musicHolder.userImage.setTag(userId);
            final Pair<String, String> userNameAndImageId = ExploreFragment.USER_INFO_CACHE.getUnchecked(userId);

            //take out view info from this object
            final JsonObject musicViewInfo = MiscUtils.get(exploreJSON, ExploreJSON.VIEW_INFO).getAsJsonObject();

            final String musicTitle = MiscUtils.get(musicViewInfo, ExploreJSON.MusicViewInfo.TITLE).getAsString();
            musicHolder.title.setText(musicTitle);
            final String subtitle = MiscUtils.get(musicViewInfo, ExploreJSON.MusicViewInfo.SUB_TITLE).getAsString();
            musicHolder.subTitle.setText(subtitle);
            final String originalUserName = MiscUtils.get(musicViewInfo, ExploreJSON.MusicViewInfo.SENDER_NAME, "").getAsString();
            if (TextUtils.isEmpty(originalUserName))
                musicHolder.userHandle.setText(userNameAndImageId.first);
            else
                musicHolder.userHandle.setText(originalUserName);

            musicHolder.userImage.setImageURI(AlbumArtUri.getUserImageUri(
                    userId,
                    "imageId",
                    "rw",
                    true,
                    100,
                    100));

            SavedSongsDataModel.Builder savedSongsDataModelBuilder = new SavedSongsDataModel.Builder()
                    .withYoutube_Id(ytID)
                    .withDate_Added(System.currentTimeMillis())
                    .withSenderId(userId)
                    .withArtistAlbumName(subtitle)
                    .withSongName(musicTitle)
                    .withDisplayName(musicTitle);

            musicHolder.downBtn.setTag(savedSongsDataModelBuilder.withType(2).build());
            musicHolder.saveBtn.setTag(savedSongsDataModelBuilder.withType(1).build());




        }
        else if(holder instanceof MiscViewHolder){

            final MiscViewHolder miscHolder = (MiscViewHolder) holder;
            final JsonObject exploreJSON = explore.getContainerForIndex(position);

            miscHolder.button.setOnClickListener(this);
            miscHolder.button.setTag(position);

            final JsonObject miscViewInfo = MiscUtils.get(exploreJSON, ExploreJSON.VIEW_INFO).getAsJsonObject();
            miscHolder.button.setText(MiscUtils.get(miscViewInfo, ExploreJSON.AppViewInfo.SUB_TITLE).getAsString());
            miscHolder.title.setText(MiscUtils.get(miscViewInfo, ExploreJSON.AppViewInfo.TITLE).getAsString());

            final String miscImage = MiscUtils.get(miscViewInfo, ExploreJSON.AppViewInfo.LARGE_IMAGE_URL, "").getAsString();
            if (!TextUtils.isEmpty(miscImage))
                Log.d(TAG, "Misc Image URI = " + miscImage);
            miscHolder.image.setController(MiscUtils.getControllerResize(miscHolder.image.getController(),
                    Uri.parse(miscImage), ExploreFragment.SMALL_IMAGE_SIZE));


        }


    }

    @Override
    public int getItemViewType(int position) {

        if (position == getItemCount() - 1) {
            //last item
            if (explore.isDoneForToday())
                return VIEW_TYPE_IS_DONE_FOR_TODAY;
            else
                return VIEW_TYPE_LOADING;

        }
        else{
            final JsonObject exploreJSON = explore.getContainerForIndex(position);
            final ExploreTypes exploreTypes = ExploreTypes.valueOf(MiscUtils.get(exploreJSON, ExploreJSON.TYPE).getAsString());

            switch (exploreTypes) {

                case MUSIC:

                    return VIEW_TYPE_MUSIC;

                case MISC:

                    return VIEW_TYPE_MISC;

            }

        }

        throw new IllegalStateException("No item type could be set");

    }

    @Override
    public int getItemCount() {
        return explore.getCount() + 1;
    }

    @Override
    public void onClick(View v) {

        handOverMessage.handOverMessage(v.getTag());

    }

    static class IsDoneForTodayOrLoadingViewHolder extends RecyclerView.ViewHolder{

        public IsDoneForTodayOrLoadingViewHolder(View itemView) {
            super(itemView);
        }

    }

    static class MusicViewHolder extends RecyclerView.ViewHolder{

        final ImageView downBtn;
        final TextView title ,subTitle,userHandle;
        final ImageView saveBtn, fb_share_btn;
        final SimpleDraweeView image,userImage;

        public MusicViewHolder(View itemView) {
            super(itemView);
            downBtn = (ImageView) itemView.findViewById(R.id.downBtn);
            title = (TextView) itemView.findViewById(R.id.title);
            subTitle = (TextView) itemView.findViewById(R.id.subtitle);
            userHandle = (TextView) itemView.findViewById(R.id.userHandle);
            saveBtn = (ImageView) itemView.findViewById(R.id.saveSong);
            image = (SimpleDraweeView) itemView.findViewById(R.id.image);
            userImage = (SimpleDraweeView) itemView.findViewById(R.id.userImage);
            fb_share_btn = (ImageView) itemView.findViewById(R.id.fb_share_btn);
        }
    }

    static class MiscViewHolder extends RecyclerView.ViewHolder{

        final Button button;
        final TextView title;
        final SimpleDraweeView image;

        public MiscViewHolder(View itemView) {
            super(itemView);
            button = (Button) itemView.findViewById(R.id.button);
            title = (TextView) itemView.findViewById(R.id.title);
            image = (SimpleDraweeView) itemView.findViewById(R.id.image);
        }
    }


    static interface Explore {

        JsonObject getContainerForIndex(int index);

        int getCount();

        boolean isDoneForToday();

        void playYTVideo(String search);
    }

}
