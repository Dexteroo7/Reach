package reach.project.utils.auxiliaryClasses;

import android.app.Activity;
import android.support.v4.app.Fragment;

/**
 * Created by dexter on 08/10/15.
 */
public interface UseActivityAndFragment<Param1 extends Activity, Param2 extends Fragment> {
    void work(Param1 activity, Param2 fragment);
}
