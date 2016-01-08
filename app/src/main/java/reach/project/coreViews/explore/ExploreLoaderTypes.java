package reach.project.coreViews.explore;

import android.support.annotation.NonNull;

import reach.project.R;
import reach.project.utils.EnumHelper;

/**
 * Created by dexter on 08/01/16.
 */
public enum ExploreLoaderTypes implements EnumHelper<String> {

    LOADING(R.layout.explore_loading),
    DONE_FOR_TODAY(android.R.layout.simple_list_item_1);

    private final int layoutResId;

    ExploreLoaderTypes(int layoutResId) {
        this.layoutResId = layoutResId;
    }

    public int getLayoutResId() {
        return layoutResId;
    }

    @NonNull
    @Override
    public String getName() {
        return name();
    }
}
