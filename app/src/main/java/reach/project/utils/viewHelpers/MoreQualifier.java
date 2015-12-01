package reach.project.utils.viewHelpers;

import android.support.v7.widget.RecyclerView;

import java.lang.ref.WeakReference;

/**
 * Created by dexter on 01/12/15.
 */
public interface MoreQualifier {

    void passNewAdapter(WeakReference<RecyclerView.Adapter> adapterWeakReference);
}
