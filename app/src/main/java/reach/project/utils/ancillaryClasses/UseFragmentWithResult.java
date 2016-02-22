package reach.project.utils.ancillaryClasses;

import android.support.v4.app.Fragment;

/**
 * Created by dexter on 06/08/15.
 */
public interface UseFragmentWithResult<Param extends Fragment, Result> {
    Result work(Param fragment);
}