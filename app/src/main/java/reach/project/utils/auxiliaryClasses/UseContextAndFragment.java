package reach.project.utils.auxiliaryClasses;

import android.content.Context;
import android.support.v4.app.Fragment;

/**
 * Created by dexter on 06/10/15.
 */
public interface UseContextAndFragment<Param1 extends Context, Param2 extends Fragment> {
    void work(Param1 context, Param2 fragment);
}
