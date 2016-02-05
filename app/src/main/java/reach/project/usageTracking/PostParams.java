package reach.project.usageTracking;

/**
 * Created by dexter on 23/10/15.
 */
public enum PostParams {

    EVENT_NAME("eventName"),
    META_INFO("metaInfo"),
    TIME_STAMP("timeStamp"),

    SCREEN_NAME("screenName"),
    USER_ID("userId"),
    DEVICE_ID("deviceId"), //fixed
    OS("os"), //fixed
    OS_VERSION("osVersion"), //fixed
    APP_VERSION("appVersion"), //fixed
    USER_NAME("userName"),
    USER_NUMBER("phoneNumber");

    private final String value;

    PostParams(String eventName) {
        this.value = eventName;
    }

    public String getValue() {
        return value;
    }
}