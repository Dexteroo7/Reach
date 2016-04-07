package reach.project.utils.ancillaryClasses;

import android.support.v7.widget.Toolbar;

/**
 * Created by Dexter on 13-04-2015.
 */
public interface SuperInterface {

    Toolbar.OnMenuItemClickListener getMenuClickListener();

    void showSwipeCoach();

    void showYTVideo(String text);
}
