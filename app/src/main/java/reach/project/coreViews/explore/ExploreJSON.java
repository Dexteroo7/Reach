package reach.project.coreViews.explore;

import android.support.annotation.NonNull;

import reach.project.utils.EnumHelper;

/**
 * Created by dexter on 17/12/15.
 */
enum ExploreJSON implements EnumHelper<String> {

    //story details
    ID("id"),
    TIME_STAMP("timeStamp"),
    TYPE("type"),

    //meta info (used when acted upon)
    META_INFO("metaInfo"),
    //view details (used when displaying the item)
    VIEW_INFO("viewInfo");

    private final MetaInfo metaInfo;
    private final ViewInfo viewInfo;
    private final ExploreTypes types;
    private final String name;

    ExploreJSON(String name,
                MetaInfo metaInfo,
                ViewInfo viewInfo,
                ExploreTypes types) {

        this.metaInfo = metaInfo;
        this.viewInfo = viewInfo;
        this.types = types;
        this.name = name;
    }

    //not allowed to call
    private ExploreJSON(String name) {

        this.name = name;
        this.metaInfo = null;
        this.viewInfo = null;
        this.types = ExploreTypes.LOADING;
//        throw new IllegalArgumentException("Required params not provided");
    }

    public MetaInfo getMetaInfo() {
        return metaInfo;
    }

    public ViewInfo getViewInfo() {
        return viewInfo;
    }

    public ExploreTypes getTypes() {
        return types;
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }

    ////////////////////

    /**
     * Meta-Info in JSON is utilized
     * when an action is taking
     */
    interface MetaInfo {
        //empty
    }

    /**
     * View-Info in JSON is utilized
     * when displaying data in adapter
     */
    interface ViewInfo {
        //empty
    }

    enum MusicMetaInfo implements MetaInfo, EnumHelper<String> {

        SONG_ID("songId"),
        URL("url"),

        DISPLAY_NAME("displayName"),
        ACTUAL_NAME("actualName"),
        ARTIST("artist"),
        ALBUM("album"),

        SIZE("size"),
        DURATION("duration"),
        SENDER_ID("senderId"),
        SENDER_NAME("senderName");

        private final String name;

        MusicMetaInfo(String name) {
            this.name = name;
        }

        @NonNull
        @Override
        public final String getName() {
            return name;
        }
    }

    enum MusicViewInfo implements ViewInfo, EnumHelper<String> {

        TITLE("title"), //songName
        SUB_TITLE("subTitle"), //artist / album
        LARGE_IMAGE_URL("largeImageUrl"), //albumArt
        SMALL_IMAGE_URL("smallImageUrl"), //userImage
        TYPE_TEXT("typeText"), //Music

        SENDER_NAME("senderName");

        private final String name;

        MusicViewInfo(String name) {
            this.name = name;
        }

        @NonNull
        @Override
        public final String getName() {
            return name;
        }
    }

    enum AppMetaInfo implements MetaInfo, EnumHelper<String> {

        SENDER_ID("senderId"),
        PACKAGE_NAME("packageName");

        private final String name;

        AppMetaInfo(String name) {
            this.name = name;
        }

        @NonNull
        @Override
        public final String getName() {
            return name;
        }
    }

    enum AppViewInfo implements ViewInfo, EnumHelper<String> {

        TITLE("title"), //appName
        SUB_TITLE("subTitle"), //senderName
        LARGE_IMAGE_URL("largeImageUrl"), //coverPic / screenShot
        SMALL_IMAGE_URL("smallImageUrl"), //appIcon
        TYPE_TEXT("typeText"), //Application

        SENDER_NAME("senderName"),

        DESCRIPTION("description"),
        RATING("rating"),
        CATEGORY("category"),
        REVIEW_USER_PHOTO("reviewUserPhoto"),
        REVIEW_USER_NAME("reviewUserName");

        private final String name;

        AppViewInfo(String name) {
            this.name = name;
        }

        @NonNull
        @Override
        public final String getName() {
            return name;
        }
    }

    enum MiscMetaInfo implements MetaInfo, EnumHelper<String> {

        CLASS_NAME("className"),
        URL("url"),
        DATA("data");

        private final String name;

        MiscMetaInfo(String name) {
            this.name = name;
        }

        @NonNull
        @Override
        public final String getName() {
            return name;
        }
    }

    enum MiscViewInfo implements ViewInfo, EnumHelper<String> {

        TITLE("title"),
        SUB_TITLE("subTitle"),
        LARGE_IMAGE_URL("largeImageUrl"),
        SMALL_IMAGE_URL("smallImageUrl"),
        TYPE_TEXT("typeText");

        private final String name;

        MiscViewInfo(String name) {
            this.name = name;
        }

        @NonNull
        @Override
        public final String getName() {
            return name;
        }
    }
}