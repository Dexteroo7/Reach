package reach.backend.User;

/**
 * Created by dexter on 12/07/15.
 */
public class OldUserContainerNew {

    private final String name;
    private final String imageId;
    private final String promoCode;
    private final long serverId;

    public long getServerId() {
        return serverId;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public String getImageId() {
        return imageId;
    }

    public String getName() {
        return name;
    }

    public OldUserContainerNew(String name, String promoCode, String imageId, long serverId) {
        this.name = name;
        this.promoCode = promoCode;
        this.imageId = imageId;
        this.serverId = serverId;
    }
}
