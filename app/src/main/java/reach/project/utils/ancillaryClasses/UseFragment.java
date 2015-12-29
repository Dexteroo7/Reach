package reach.project.utils.ancillaryClasses;

import android.support.v4.app.Fragment;

/**
 * Created by dexter on 06/08/15.
 */
public interface UseFragment<Result, Param extends Fragment> {
    Result work(Param fragment);
}

