package reach.project.explore;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
        final LayoutInflater inflater = LayoutInflater.from(context);

        final View layout = inflater.inflate(container.getTypes().getLayoutResId(), collection, false);

        if (container.getTypes().equals(ExploreTypes.LOADING) || container.getTypes().equals(ExploreTypes.DONE_FOR_TODAY))
            layout.setTag(POSITION_NONE);
        else
            layout.setTag(POSITION_UNCHANGED);

        final TextView sampleText = (TextView) layout.findViewById(android.R.id.text1);

//        container.getToShow(); //data to show
//        container.getTypes().getTitle(); //title string
//        container.getTypes().getLayoutResId(); //specific layout resource id !

        sampleText.setText(container.getToShow()); //stuff to display

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

    @Override
    public CharSequence getPageTitle(int position) {

        final ExploreContainer container = explore.getContainerForIndex(position);
        return container.getTypes().getTitle();
    }

    public interface Explore {

        ExploreContainer getContainerForIndex(int index);

        int getCount();
    }
}