package reach.project.explore;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {

        final ExploreContainer container = explore.getContainerForIndex(position);
        final View layout = LayoutInflater.from(context).inflate(container.getTypes().getLayoutResId(), collection, false);

        switch (container.getTypes()) {
            case MUSIC:
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
