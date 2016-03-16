package reach.project.utils.ancillaryClasses;

import android.support.v4.app.Fragment;

/**
 * Created by dexter on 20/09/15.
 */
public interface UseFragment<Param extends Fragment> {
    void work(Param fragment);
}