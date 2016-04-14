package reach.project.coreViews.explore;

import android.net.Uri;
import android.support.v4.util.Pair;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import reach.project.R;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.YouTubeDataModel;
import reach.project.utils.viewHelpers.CustomDraweeView;
import reach.project.utils.viewHelpers.HandOverMessage;

import static reach.project.coreViews.explore.ExploreJSON.AppViewInfo;
import static reach.project.coreViews.explore.ExploreJSON.MusicViewInfo;

/**
 * Created by dexter on 16/10/15.
 */
class ExploreAdapter extends PagerAdapter implements View.OnClickListener {

    private static final String TAG = ExploreAdapter.class.getSimpleName() ;
    private final Explore explore;
    private final HandOverMessage<Object> handOverMessage;
    /*//TODO: Delete when facebbok share is to be removed from explore
    boolean showFacebookButton = false;*/

    public ExploreAdapter(Explore explore,
                          HandOverMessage<Object> handOverId) {

        this.explore = explore;
        this.handOverMessage = handOverId;
        //showFacebookButton = SharedPrefUtils.getShowFacebookShareOrNot(SharedPrefUtils.getPreferences(context));
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {

        final LayoutInflater layoutInflater = LayoutInflater.from(collection.getContext());
        final View layout;

        if (position == getCount() - 1) {

            //last item
            if (explore.isDoneForToday()) //done for today
                layout = layoutInflater.inflate(ExploreLoaderTypes.DONE_FOR_TODAY.getLayoutResId(), collection, false);
            else //loading
                layout = layoutInflater.inflate(ExploreLoaderTypes.LOADING.getLayoutResId(), collection, false);
            layout.setTag(POSITION_NONE);

        } else {

            final JsonObject exploreJSON = explore.getContainerForIndex(position);
            final ExploreTypes exploreTypes = ExploreTypes.valueOf(MiscUtils.get(exploreJSON, ExploreJSON.TYPE).getAsString());

            layout = layoutInflater.inflate(exploreTypes.getLayoutResId(), collection, false);
            //final PercentRelativeLayout percentRelativeLayout = (PercentRelativeLayout) layout.findViewById(R.id.percentRelativeLayout);
            //final WebView webView = (WebView) layout.findViewById(R.id.webView);
            //final ImageView downButton = (ImageView) layout.findViewById(R.id.downButton);
            final ImageView downBtn = (ImageView) layout.findViewById(R.id.downBtn);
            final TextView title = (TextView) layout.findViewById(R.id.title);
            final TextView subTitle = (TextView) layout.findViewById(R.id.subtitle);
            final TextView userHandle = (TextView) layout.findViewById(R.id.userHandle);
            //final ImageView saveBtn = (ImageView) layout.findViewById(R.id.saveBtn);
            final SimpleDraweeView image = (SimpleDraweeView) layout.findViewById(R.id.image);
            final SimpleDraweeView userImage = (SimpleDraweeView) layout.findViewById(R.id.userImage);
            final ImageView fb_share_btn = (ImageView) layout.findViewById(R.id.fb_share_btn);
            //final TextView facebookShare = (TextView) layout.findViewById(R.id.facebook_share_text);

            switch (exploreTypes) {

                case MUSIC: {
                    downBtn.setOnClickListener(this);
                    fb_share_btn.setOnClickListener(this);
                    final YouTubeDataModel ytbData = new YouTubeDataModel();

                    final JsonElement ytElement = MiscUtils.get(exploreJSON, ExploreJSON.YOUTUBE_ID);
                    final String ytID;
                    if (ytElement == null)
                        ytID = "";
                    else
                        ytID = ytElement.getAsString();
                    downBtn.setTag(ytID);
                    ytbData.setId(ytID);

                    final String albumArt = "https://i.ytimg.com/vi/" + ytID + "/hqdefault.jpg";
                    if (!TextUtils.isEmpty(albumArt)) {
                        image.setController(Fresco.newDraweeControllerBuilder()
                                .setOldController(image.getController())
                                .setImageRequest(ImageRequestBuilder.newBuilderWithSource(Uri.parse(albumArt))
                                        .build())
                                .build());
                        ytbData.setImageUrl(albumArt);
                    }

                    fb_share_btn.setTag(ytbData);

                    final long userId = MiscUtils.get(exploreJSON, ExploreJSON.ID).getAsLong();
                    /*final Cursor cursor = collection.getContext().getContentResolver().query(
                            Uri.parse(ReachFriendsProvider.CONTENT_URI + "/" + userId),
                            new String[]{ReachFriendsHelper.COLUMN_STATUS},
                            ReachFriendsHelper.COLUMN_ID + " = ?",
                            new String[]{userId + ""}, null);
                    if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                        saveBtn.setVisibility(cursor.getShort(0) == ReachFriendsHelper.ONLINE_REQUEST_GRANTED ? View.VISIBLE : View.INVISIBLE);
                        cursor.close();
                    }*/
                    final Pair<String, String> userNameAndImageId = ExploreFragment.USER_INFO_CACHE.getUnchecked(userId);

                    //take out view info from this object
                    final JsonObject musicViewInfo = MiscUtils.get(exploreJSON, ExploreJSON.VIEW_INFO).getAsJsonObject();

                    final String musicTitle = MiscUtils.get(musicViewInfo, MusicViewInfo.TITLE).getAsString();
                    title.setText(musicTitle);
                    /*downButton.setOnClickListener(v -> {
                        explore.playYTVideo(fastSanitize(musicTitle));
                        *//*percentRelativeLayout.setVisibility(View.GONE);
                        webView.setWebViewClient(new WebViewClient());
                        webView.getSettings().setJavaScriptEnabled(true);
                        webView.loadUrl("https://www.youtube.com/results?q=" + MiscUtils.get(musicViewInfo, MusicViewInfo.TITLE).getAsString());
                        webView.setVisibility(View.VISIBLE);*//*

                    });*/
                    subTitle.setText(MiscUtils.get(musicViewInfo, MusicViewInfo.SUB_TITLE).getAsString());
                    final String originalUserName = MiscUtils.get(musicViewInfo, MusicViewInfo.SENDER_NAME, "").getAsString();
                    if (TextUtils.isEmpty(originalUserName))
                        userHandle.setText(userNameAndImageId.first);
                    else
                        userHandle.setText(originalUserName);
                    //typeText.setText(MiscUtils.get(musicViewInfo, MusicViewInfo.TYPE_TEXT).getAsString());

                    userImage.setImageURI(AlbumArtUri.getUserImageUri(
                            userId,
                            "imageId",
                            "rw",
                            true,
                            100,
                            100));

                    //final String albumArt = MiscUtils.get(musicViewInfo, MusicViewInfo.LARGE_IMAGE_URL, "").getAsString();


                    /*if(showFacebookButton){
                        facebookShare.setVisibility(View.VISIBLE);
                        facebookShare.setOnClickListener(this);
                    }
                    else{
                        facebookShare.setVisibility(View.GONE);
                    }*/

                    layout.setTag(POSITION_UNCHANGED);
                    break;
                }
                case APP:

                    final RatingBar rating = (RatingBar) layout.findViewById(R.id.rating);

                    //downButton.setOnClickListener(this);
                    //downButton.setTag(position);
                    final long userId = MiscUtils.get(exploreJSON, ExploreJSON.ID).getAsLong();
                    final Pair<String, String> userNameAndImageId = ExploreFragment.USER_INFO_CACHE.getUnchecked(userId);

                    //take out view info from this object
                    final JsonObject appViewInfo = MiscUtils.get(exploreJSON, ExploreJSON.VIEW_INFO).getAsJsonObject();

                    title.setText(MiscUtils.get(appViewInfo, AppViewInfo.TITLE).getAsString());
                    subTitle.setText(MiscUtils.get(appViewInfo, AppViewInfo.SUB_TITLE, "").getAsString());
                    final String originalUserName = MiscUtils.get(appViewInfo, AppViewInfo.SENDER_NAME, "").getAsString();
                    if (TextUtils.isEmpty(originalUserName))
                        userHandle.setText(userNameAndImageId.first);
                    else
                        userHandle.setText(originalUserName);
                    //typeText.setText(MiscUtils.get(appViewInfo, AppViewInfo.TYPE_TEXT).getAsString());

                    final String appIcon = MiscUtils.get(appViewInfo, AppViewInfo.SMALL_IMAGE_URL, "").getAsString();
                    if (!TextUtils.isEmpty(appIcon))
                        image.setController(MiscUtils.getControllerResize(image.getController(),
                                Uri.parse(appIcon), ExploreFragment.FULL_IMAGE_SIZE));

                    final String appRating = MiscUtils.get(appViewInfo, AppViewInfo.RATING, "").getAsString();
                    if (!TextUtils.isEmpty(appRating))
                        rating.setRating(Float.parseFloat(appRating));

                    userImage.setImageURI(AlbumArtUri.getUserImageUri(
                            userId,
                            "imageId",
                            "rw",
                            true,
                            100,
                            100));
                    /*if(showFacebookButton){
                        facebookShare.setVisibility(View.VISIBLE);
                        facebookShare.setOnClickListener(this);
                    }
                    else{
                        facebookShare.setVisibility(View.GONE);
                    }
*/
                    layout.setTag(POSITION_UNCHANGED);
                    break;

                case MISC:
                    final Button button = (Button) layout.findViewById(R.id.button);
                    button.setOnClickListener(this);
                    button.setTag(position);

                    final JsonObject miscViewInfo = MiscUtils.get(exploreJSON, ExploreJSON.VIEW_INFO).getAsJsonObject();
                    button.setText(MiscUtils.get(miscViewInfo, AppViewInfo.SUB_TITLE).getAsString());
                    title.setText(MiscUtils.get(miscViewInfo, AppViewInfo.TITLE).getAsString());

                    final String miscImage = MiscUtils.get(miscViewInfo, AppViewInfo.LARGE_IMAGE_URL, "").getAsString();
                    if (!TextUtils.isEmpty(miscImage))
                        Log.d(TAG, "Misc Image URI = " + miscImage);
                        image.setController(MiscUtils.getControllerResize(image.getController(),
                                Uri.parse(miscImage), ExploreFragment.SMALL_IMAGE_SIZE));
                    break;
            }
        }


        collection.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        return explore.getCount() + 1; //+1 for appending a loading / done for today response
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public int getItemPosition(Object object) {

        if (object instanceof View) {

            final View view = (View) object;
            final Object tag = view.getTag();

            if (tag == null)
                return POSITION_UNCHANGED; //default, should not happen

            if (tag instanceof Integer)
                return (int) tag; //can be POSITION_NONE or POSITION_UNCHANGED
            else
                Log.i("Ayush", "Fail of second order");

        } else
            Log.i("Ayush", "Fail of first order");

        return POSITION_UNCHANGED; //default, should not happen
    }

    @Override
    public void onClick(View view) {
        //final int id = view.getId();
        /*if(id == R.id.facebook_share_text){
            handOverMessage.handOverMessage(-11);
            return;
        }*/

        handOverMessage.handOverMessage(view.getTag());

        /*if (view instanceof ImageView) {

            ((ImageView) view).setImageResource(R.drawable.icon_downloading_active);
            final ValueAnimator animator = ValueAnimator.ofInt(0, MiscUtils.dpToPx(5));
            animator.setDuration(300);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    final int val = (int) animation.getAnimatedValue();
                    view.setPadding(val, val, val, val);
                }
            });
            animator.setInterpolator(new AccelerateInterpolator());
            animator.start();
        }*/
    }

    interface Explore {

        JsonObject getContainerForIndex(int index);

        int getCount();

        boolean isDoneForToday();

        void playYTVideo(String search);
    }
}
