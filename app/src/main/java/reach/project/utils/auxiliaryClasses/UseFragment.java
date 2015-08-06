package reach.project.utils.auxiliaryClasses;

import android.support.v4.app.Fragment;

/**
 * Created by dexter on 06/08/15.
 */
public abstract class UseFragment<Result, Param extends Fragment> {
    public abstract Result work(Param fragment);
}

