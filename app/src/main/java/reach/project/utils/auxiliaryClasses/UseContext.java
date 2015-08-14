package reach.project.utils.auxiliaryClasses;

import android.content.Context;

/**
 * Created by dexter on 01/08/15.
 */
public interface UseContext<Result, Param extends Context> {
    Result work(Param context);
}
