package reach.project.explore;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import reach.project.R;
import reach.project.explore.internal.InfinitePagerAdapter;

/**
 * Created by dexter on 16/10/15.
 */
public class ExploreAdapter extends InfinitePagerAdapter<Integer> {

    /**
     * Standard constructor.
     *
     * @param initValue the initial indicator value the ViewPager should start with.
     */
    private final Context context;
    private final Explorer explorer;

    public ExploreAdapter(Integer initValue, Context context, Explorer explorer) {
        super(initValue);
        this.context = context;
        this.explorer = explorer;
    }

    @Override
    public Integer getNextIndicator() {
        return getCurrentIndicator() + 1;
    }

    @Override
    public Integer getPreviousIndicator() {
        return getCurrentIndicator() - 1;
    }

    @Override
    public ViewGroup instantiateItem(Integer indicator) {

        //TODO use indicator to instantiate item

        Log.d("InfiniteViewPager", "instantiating page " + indicator);
        final LinearLayout layout = (LinearLayout) ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.chat_message_me, null);
        final TextView text = (TextView) layout.findViewById(R.id.message);
        text.setText(String.format("Page %s", indicator));
//        Log.i("InfiniteViewPager", String.format("textView.text() == %s", text.getText()));
        layout.setTag(indicator);

        final ExploreContainer container = explorer.getContainer(indicator);
        Log.i("Ayush", container.toString());
        return layout;
    }

    @Override
    public String getStringRepresentation(final Integer currentIndicator) {
        return String.valueOf(currentIndicator);
    }

    @Override
    public Integer convertToIndicator(final String representation) {
        return Integer.valueOf(representation);
    }

    public interface Explorer {
        ExploreContainer getContainer(int indicator);
    }
}
