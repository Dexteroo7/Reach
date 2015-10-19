package reach.project.explore;

import reach.project.R;

/**
 * Created by dexter on 15/10/15.
 */
public enum ExploreTypes {

    MUSIC("Music", R.layout.musiclist_item),
    APP("App", R.layout.musiclist_item),
    PHOTO("Photo", R.layout.musiclist_item),
    LOADING("Loading", R.layout.musiclist_item),
    DONE_FOR_TODAY("Done for today", R.layout.musiclist_item);

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