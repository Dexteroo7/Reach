package reach.project.utils.auxiliaryClasses;

import android.content.Context;

/**
 * Created by dexter on 01/08/15.
 */
public abstract class UseContext<Result, Param extends Context> {
    public abstract Result work(Param context);
}
