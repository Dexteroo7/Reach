package reach.project.coreViews.explore;

import android.animation.ValueAnimator;
import android.net.Uri;
import android.support.v4.util.Pair;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.gson.JsonObject;

import reach.project.R;
import reach.project.utils.AlbumArtUri;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.HandOverMessage;

import static reach.project.coreViews.explore.ExploreJSON.AppViewInfo;
import static reach.project.coreViews.explore.ExploreJSON.MusicViewInfo;

/**
 * Created by dexter on 16/10/15.
 */
class ExploreAdapter extends PagerAdapter implements View.OnClickListener {

    private final Explore explore;
    private final HandOverMessage<Integer> handOverMessage;

    public ExploreAdapter(Explore explore,
                          HandOverMessage<Integer> handOverId) {

        this.explore = explore;
        this.handOverMessage = handOverId;
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
            final ImageView downButton = (ImageView) layout.findViewById(R.id.downButton);
            final TextView title = (TextView) layout.findViewById(R.id.title);
            final TextView subTitle = (TextView) layout.findViewById(R.id.subtitle);
            final TextView userHandle = (TextView) layout.findViewById(R.id.userHandle);
            final TextView typeText = (TextView) layout.findViewById(R.id.typeText);
            final SimpleDraweeView image = (SimpleDraweeView) layout.findViewById(R.id.image);
            final SimpleDraweeView userImage = (SimpleDraweeView) layout.findViewById(R.id.userImage);

            switch (exploreTypes) {

                case MUSIC: {
                    //downButton.setTag(position);
                    final long userId = MiscUtils.get(exploreJSON, ExploreJSON.ID).getAsLong();
                    final Pair<String, String> userNameAndImageId = ExploreFragment.USER_INFO_CACHE.getUnchecked(userId);

                    //take out view info from this object
                    final JsonObject musicViewInfo = MiscUtils.get(exploreJSON, ExploreJSON.VIEW_INFO).getAsJsonObject();

                    final String musicTitle = MiscUtils.get(musicViewInfo, MusicViewInfo.TITLE).getAsString();
                    title.setText(musicTitle);
                    downButton.setOnClickListener(v -> {
                        explore.playYTVideo(fastSanitize(musicTitle));
                        /*percentRelativeLayout.setVisibility(View.GONE);
                        webView.setWebViewClient(new WebViewClient());
                        webView.getSettings().setJavaScriptEnabled(true);
                        webView.loadUrl("https://www.youtube.com/results?q=" + MiscUtils.get(musicViewInfo, MusicViewInfo.TITLE).getAsString());
                        webView.setVisibility(View.VISIBLE);*/

                    });
                    subTitle.setText(MiscUtils.get(musicViewInfo, MusicViewInfo.SUB_TITLE).getAsString());
                    final String originalUserName = MiscUtils.get(musicViewInfo, MusicViewInfo.SENDER_NAME, "").getAsString();
                    if (TextUtils.isEmpty(originalUserName))
                        userHandle.setText(userNameAndImageId.first);
                    else
                        userHandle.setText(originalUserName);
                    typeText.setText(MiscUtils.get(musicViewInfo, MusicViewInfo.TYPE_TEXT).getAsString());

                    userImage.setImageURI(AlbumArtUri.getUserImageUri(
                            userId,
                            "imageId",
                            "rw",
                            true,
                            100,
                            100));

                    final String albumArt = MiscUtils.get(musicViewInfo, MusicViewInfo.LARGE_IMAGE_URL, "").getAsString();
                    if (!TextUtils.isEmpty(albumArt))
                        image.setController(MiscUtils.getControllerResize(image.getController(),
                                Uri.parse(albumArt), ExploreFragment.FULL_IMAGE_SIZE));

                    layout.setTag(POSITION_UNCHANGED);
                    break;
                }
                case APP:

                    final RatingBar rating = (RatingBar) layout.findViewById(R.id.rating);

                    downButton.setOnClickListener(this);
                    downButton.setTag(position);
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
                    typeText.setText(MiscUtils.get(appViewInfo, AppViewInfo.TYPE_TEXT).getAsString());

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
                        image.setController(MiscUtils.getControllerResize(image.getController(),
                                Uri.parse(miscImage), ExploreFragment.SMALL_IMAGE_SIZE));
                    break;
            }
        }


        collection.addView(layout);
        return layout;
    }

    private static String fastSanitize(String str) {

        final StringBuilder stringBuilder = new StringBuilder();

        str = replace(str, "MP3Khan", "", -1, stringBuilder);
        str = replace(str, "_Full-HD", "", -1, stringBuilder);
        str = replace(str, "songsweb", "", -1, stringBuilder);
        str = replace(str, "www.", "", -1, stringBuilder);
        str = replace(str, ".com", "", -1, stringBuilder);

        str = replace(str, ".Mobi", "", -1, stringBuilder);
        str = replace(str, ".mobi", "", -1, stringBuilder);
        str = replace(str, "[]", "", -1, stringBuilder);
        str = replace(str, "pagalworld", "", -1, stringBuilder);
        str = replace(str, "DownloadMing", "", -1, stringBuilder);
        str = replace(str, "  ", "", -1, stringBuilder);
        str = replace(str, "skymaza", "", -1, stringBuilder);
        str = replace(str, "DjGol", "", -1, stringBuilder);
        str = replace(str, "<unknown>", "", -1, stringBuilder);
        str = replace(str, "DJBoss", "", -1, stringBuilder);
        str = replace(str, "iPendu", "", -1, stringBuilder);
        str = replace(str, "SongPK", "", -1, stringBuilder);
        str = replace(str, "Songspk", "", -1, stringBuilder);
        str = replace(str, "DJJOhAL", "", -1, stringBuilder);
        str = replace(str, "Mobway", "", -1, stringBuilder);
        str = replace(str, "downloadming", "", -1, stringBuilder);
        str = replace(str, "DjPunjab", "", -1, stringBuilder);
        str = replace(str, "Bestwap", "", -1, stringBuilder);
        str = replace(str, "MyMp3Song", "", -1, stringBuilder);
        str = replace(str, "PagalWorld", "", -1, stringBuilder);
        str = replace(str, "KrazyWAP", "", -1, stringBuilder);
        str = replace(str, "lebewafa", "", -1, stringBuilder);
        str = replace(str, "Mp3Singer", "", -1, stringBuilder);
        str = replace(str, "Songspk", "", -1, stringBuilder);
        str = replace(str, "Mr-Jatt", "", -1, stringBuilder);
        str = replace(str, "MastiCity", "", -1, stringBuilder);
        str = replace(str, "finewap", "", -1, stringBuilder);
        str = replace(str, "hotmentos", "", -1, stringBuilder);
        str = replace(str, "MirchiFun", "", -1, stringBuilder);
        str = replace(str, "MyMp3Singer", "", -1, stringBuilder);
        str = replace(str, "FreshMaZa", "", -1, stringBuilder);
        str = replace(str, ".songs", "", -1, stringBuilder);
        str = replace(str, "SongsLover", "", -1, stringBuilder);
        str = replace(str, "Mixmp3", "", -1, stringBuilder);
        str = replace(str, "wapking", "", -1, stringBuilder);
        str = replace(str, "BDLovE24", "", -1, stringBuilder);
        str = replace(str, "DJMaza", "", -1, stringBuilder);
        str = replace(str, "RoyalJatt", "", -1, stringBuilder);
        str = replace(str, "SongPK", "", -1, stringBuilder);
        str = replace(str, "KrazyWap", "", -1, stringBuilder);
        str = replace(str, ".link", "", -1, stringBuilder);
        str = replace(str, "MobMaza", "", -1, stringBuilder);
        str = replace(str, "Mobway", "", -1, stringBuilder);
        str = replace(str, "youtube", "", -1, stringBuilder);
        str = replace(str, "MP3Juices", "", -1, stringBuilder);

        str = replace(str, "+", "", -1, stringBuilder);
        str = replace(str, ".name", "", -1, stringBuilder);
        str = replace(str, "^0[1-9] ", "", -1, stringBuilder);
        str = replace(str, ".pk", "", -1, stringBuilder);
        str = replace(str, ".in", "", -1, stringBuilder);
        str = replace(str, "-", "", -1, stringBuilder);
        str = replace(str, ".Com", "", -1, stringBuilder);
        str = replace(str, ".net", "", -1, stringBuilder);
        str = replace(str, ".", "", -1, stringBuilder);
        str = replace(str, ":", "", -1, stringBuilder);
        str = replace(str, ".fm", "", -1, stringBuilder);
        str = replace(str, "_", "", -1, stringBuilder);
        str = replace(str, ".In", "", -1, stringBuilder);
        str = replace(str, ".Net", "", -1, stringBuilder);
        str = replace(str, "()", "", -1, stringBuilder);


        return str;
    }

    public static String replace(final String text, final String searchString, final String replacement, int max, StringBuilder stringBuilder) {

        stringBuilder.setLength(0);
        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(searchString) || replacement == null || max == 0) {
            return text;
        }

        int start = 0;
        int end = text.indexOf(searchString, start);
        if (end < 0) {
            return text;
        }
        final int replLength = searchString.length();
        int increase = replacement.length() - replLength;
        increase = increase < 0 ? 0 : increase;
        increase *= max < 0 ? 16 : max > 64 ? 64 : max;

        stringBuilder.ensureCapacity(text.length() + increase);
        while (end > 0) {
            stringBuilder.append(text.substring(start, end)).append(replacement);
            start = end + replLength;
            if (--max == 0) {
                break;
            }
            end = text.indexOf(searchString, start);
        }
        stringBuilder.append(text.substring(start));
        return stringBuilder.toString();
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

        handOverMessage.handOverMessage((int) view.getTag());

        if (view instanceof ImageView) {

            ((ImageView) view).setImageResource(R.drawable.icon_downloading_active);
            final ValueAnimator animator = ValueAnimator.ofInt(0, MiscUtils.dpToPx(5));
            animator.setDuration(300);
            animator.addUpdateListener(animation -> {
                final int val = (int) animation.getAnimatedValue();
                view.setPadding(val, val, val, val);
            });
            animator.setInterpolator(new AccelerateInterpolator());
            animator.start();
        }
    }

    interface Explore {

        JsonObject getContainerForIndex(int index);

        int getCount();

        boolean isDoneForToday();

        void playYTVideo(String search);
    }
}
