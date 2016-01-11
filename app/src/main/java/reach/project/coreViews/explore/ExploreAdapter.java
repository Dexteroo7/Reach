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
import reach.project.core.StaticData;
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
                layout = layoutInflater.inflate(ExploreLoaderTypes.DONE_FOR_TODAY.getLayoutResId(), collection, false);
            layout.setTag(POSITION_NONE);

        } else {

            final JsonObject exploreJSON = explore.getContainerForIndex(position);
            final ExploreTypes exploreTypes = ExploreTypes.valueOf(MiscUtils.get(exploreJSON, ExploreJSON.TYPE).getAsString());

            layout = layoutInflater.inflate(exploreTypes.getLayoutResId(), collection, false);
            final ImageView downButton = (ImageView) layout.findViewById(R.id.downButton);
            final TextView title = (TextView) layout.findViewById(R.id.title);
            final TextView subTitle = (TextView) layout.findViewById(R.id.subtitle);
            final TextView userHandle = (TextView) layout.findViewById(R.id.userHandle);
            final TextView typeText = (TextView) layout.findViewById(R.id.typeText);
            final SimpleDraweeView image = (SimpleDraweeView) layout.findViewById(R.id.image);
            final SimpleDraweeView userImage = (SimpleDraweeView) layout.findViewById(R.id.userImage);

            switch (exploreTypes) {

                case MUSIC: {

                    downButton.setOnClickListener(this);
                    downButton.setTag(position);
                    final long userId = MiscUtils.get(exploreJSON, ExploreJSON.ID).getAsLong();
                    final Pair<String, String> userNameAndImageId = ExploreFragment.USER_INFO_CACHE.getUnchecked(userId);

                    //take out view info from this object
                    final JsonObject musicViewInfo = MiscUtils.get(exploreJSON, ExploreJSON.VIEW_INFO).getAsJsonObject();

                    title.setText(MiscUtils.get(musicViewInfo, MusicViewInfo.TITLE).getAsString());
                    subTitle.setText(MiscUtils.get(musicViewInfo, MusicViewInfo.SUB_TITLE, "").getAsString());
                    final String originalUserName = MiscUtils.get(musicViewInfo, MusicViewInfo.SENDER_NAME, "").getAsString();
                    if (TextUtils.isEmpty(originalUserName))
                        userHandle.setText(userNameAndImageId.first);
                    else
                        userHandle.setText(originalUserName);
                    typeText.setText(MiscUtils.get(musicViewInfo, MusicViewInfo.TYPE_TEXT).getAsString());

                    if (!TextUtils.isEmpty(userNameAndImageId.second))
                        userImage.setController(MiscUtils.getControllerwithResize(userImage.getController(),
                                Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + userNameAndImageId.second), 50, 50));

                    final String albumArt = MiscUtils.get(musicViewInfo, MusicViewInfo.LARGE_IMAGE_URL, "").getAsString();
                    if (!TextUtils.isEmpty(albumArt))
                        image.setController(MiscUtils.getControllerwithResize(image.getController(),
                                Uri.parse(albumArt), 500, 500));

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
                        image.setController(MiscUtils.getControllerwithResize(image.getController(),
                                Uri.parse(appIcon), 250, 250));

                    final String appRating = MiscUtils.get(appViewInfo, AppViewInfo.RATING, "").getAsString();
                    if (!TextUtils.isEmpty(appRating))
                        rating.setRating(Float.parseFloat(appRating));

                    if (!TextUtils.isEmpty(userNameAndImageId.second))
                        userImage.setController(MiscUtils.getControllerwithResize(userImage.getController(),
                                Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + userNameAndImageId.second), 50, 50));

//                container.getRating();
                    layout.setTag(POSITION_UNCHANGED);
                    break;

                case MISC:
                    final Button button = (Button) layout.findViewById(R.id.button);
                    button.setOnClickListener(this);
                    button.setTag(position);

                    final JsonObject miscViewInfo = MiscUtils.get(exploreJSON, ExploreJSON.VIEW_INFO).getAsJsonObject();
                    button.setText(MiscUtils.get(miscViewInfo, MusicViewInfo.TYPE_TEXT).getAsString());
                    title.setText(MiscUtils.get(miscViewInfo, AppViewInfo.TITLE).getAsString());

                    final String miscImage = MiscUtils.get(miscViewInfo, AppViewInfo.LARGE_IMAGE_URL, "").getAsString();
                    if (!TextUtils.isEmpty(miscImage))
                        image.setController(MiscUtils.getControllerwithResize(image.getController(),
                                Uri.parse(miscImage), 250, 250));
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
    }
}
