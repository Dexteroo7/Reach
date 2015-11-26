package reach.project.coreViews.explore;

import android.animation.ValueAnimator;
import android.content.Context;
import android.net.Uri;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;

/**
 * Created by dexter on 16/10/15.
 */
public class ExploreAdapter extends PagerAdapter {

    private final Context context;
    private final Explore explore;

    public ExploreAdapter(Context context, Explore explore) {
        this.context = context;
        this.explore = explore;
    }

    private View.OnClickListener clickListener = v -> {
        //long id = (long) v.getTag();
        ValueAnimator animator = ValueAnimator.ofInt(0,  MiscUtils.dpToPx(5));
        animator.setDuration(300);
        animator.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            v.setPadding(val, val, val, val);
        });
        animator.setInterpolator(new AccelerateInterpolator());
        animator.start();
    };

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {

        final ExploreContainer container = explore.getContainerForIndex(position);
        final View layout = LayoutInflater.from(context).inflate(container.getTypes().getLayoutResId(), collection, false);

        final TextView title = (TextView) layout.findViewById(R.id.title);
        final TextView subTitle = (TextView) layout.findViewById(R.id.subtitle);
        final TextView userHandle = (TextView) layout.findViewById(R.id.userHandle);
        final TextView typeText = (TextView) layout.findViewById(R.id.typeText);
        final SimpleDraweeView image = (SimpleDraweeView) layout.findViewById(R.id.image);
        final SimpleDraweeView userImage = (SimpleDraweeView) layout.findViewById(R.id.userImage);
        final ImageView downButton = (ImageView) layout.findViewById(R.id.downButton);

        switch (container.getTypes()) {

            case MUSIC:
                title.setText(container.getTitle());
                subTitle.setText(container.getSubTitle());
                userHandle.setText(container.getUserHandle());
                typeText.setText(container.getTypes().getTitle());
                String imageId = container.getImageId();
                if (imageId!=null && !TextUtils.isEmpty(imageId))
                    image.setImageURI(Uri.parse(imageId));
                userImage.setImageURI(Uri.parse(StaticData.cloudStorageImageBaseUrl + container.getUserImageId()));
                //downButton.setTag(container.getId());
                downButton.setOnClickListener(clickListener);
                layout.setTag(POSITION_UNCHANGED);
                break;
            case APP:
                container.getRating();
                layout.setTag(POSITION_UNCHANGED);
                break;
            case PHOTO:
                layout.setTag(POSITION_UNCHANGED);
                break;
            case LOADING:
                layout.setTag(POSITION_NONE);
                break;
            case DONE_FOR_TODAY:
                layout.setTag(POSITION_NONE);
                break;
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
        return explore.getCount();
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

    public interface Explore {

        ExploreContainer getContainerForIndex(int index);

        int getCount();
    }
}
