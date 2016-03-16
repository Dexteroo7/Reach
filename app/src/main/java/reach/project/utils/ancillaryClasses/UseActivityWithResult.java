package reach.project.utils.ancillaryClasses;

import android.app.Activity;
import android.support.annotation.NonNull;

/**
 * Created by dexter on 20/02/16.
 */
public interface UseActivityWithResult<Param extends Activity, Result> {

    Result work(@NonNull Param activity);
}