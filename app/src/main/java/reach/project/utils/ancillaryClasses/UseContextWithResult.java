package reach.project.utils.ancillaryClasses;

import android.content.Context;

/**
 * Created by dexter on 01/08/15.
 */
public interface UseContextWithResult<Result> {
    Result work(Context context);
}