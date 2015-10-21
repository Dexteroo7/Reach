package reach.project.explore;

/**
 * Created by dexter on 15/10/15.
 */
public enum ExploreTypes {

    MUSIC("Music", android.R.layout.simple_list_item_1),
    APP("App", android.R.layout.simple_list_item_1),
    PHOTO("Photo", android.R.layout.simple_list_item_1),
    LOADING("Loading", android.R.layout.simple_list_item_1),
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