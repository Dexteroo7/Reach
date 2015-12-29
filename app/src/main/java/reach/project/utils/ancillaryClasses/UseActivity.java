package reach.project.utils.ancillaryClasses;

import android.app.Activity;

/**
 * Created by ashish on 06/10/15.
 */
public interface UseActivity<Param extends Activity> {

    void work(Param activity);
}
