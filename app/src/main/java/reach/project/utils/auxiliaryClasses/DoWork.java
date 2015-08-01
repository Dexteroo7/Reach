package reach.project.utils.auxiliaryClasses;

import java.io.IOException;

/**
 * Created by Dexter on 27-03-2015.
 */
public abstract class DoWork<T> {
    public abstract T doWork() throws IOException;
}
