package reach.project.coreViews.explore;

import android.support.annotation.NonNull;

import reach.project.R;
import reach.project.utils.EnumHelper;

/**
 * Created by dexter on 15/10/15.
 */
enum ExploreTypes implements EnumHelper<String> {

    MUSIC(R.layout.explore_music),
    APP(android.R.layout.simple_list_item_1),

    MISC(R.layout.explore_music),

    LOADING(R.layout.explore_loading),
    DONE_FOR_TODAY(android.R.layout.simple_list_item_1);

    private final int layoutResId;

    ExploreTypes(int layoutResId) {
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