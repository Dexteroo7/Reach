package reach.project.utils.ancillaryClasses;

import android.app.Activity;
import android.support.v4.app.Fragment;

/**
 * Created by dexter on 08/10/15.
 */
public interface UseFragmentAndActivity<Param extends Fragment> {
    void work(Activity activity, Param fragment);
}