package reach.project.utils;

import java.io.IOException;

/**
 * Created by Dexter on 27-03-2015.
 */
public abstract class DoWork<T> {
    protected abstract T doWork() throws IOException;
}
