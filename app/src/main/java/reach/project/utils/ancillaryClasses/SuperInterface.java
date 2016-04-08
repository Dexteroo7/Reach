package reach.project.utils.ancillaryClasses;

import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;

/**
 * Created by Dexter on 13-04-2015.
 */
public interface SuperInterface {

    Toolbar.OnMenuItemClickListener getMenuClickListener();

    void showSwipeCoach();

    void displayYourProfileFragment(long userId);

    void displayProfileFragment(long userId);

    void removeYourProfileFragment(Fragment fragment);

    void removeProfileFragment(Fragment fragment);

    void showYTVideo(String text);

}
