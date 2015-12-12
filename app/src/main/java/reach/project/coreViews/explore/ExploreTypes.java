package reach.project.coreViews.explore;

import reach.project.R;

/**
 * Created by dexter on 15/10/15.
 */
enum ExploreTypes {

    MUSIC("Song", R.layout.explore_music),
    APP("App", android.R.layout.simple_list_item_1),

    MISC("Misc", R.layout.explore_music),

    LOADING("Loading", R.layout.explore_loading),
    DONE_FOR_TODAY("Done for today", android.R.layout.simple_list_item_1);

    private String title;
    private int layoutResId;

    ExploreTypes(String title, int layoutResId) {
        this.title = title;
        this.layoutResId = layoutResId;
    }

    public String getTitle() {
        return title;
    }

    public int getLayoutResId() {
        return layoutResId;
    }
}