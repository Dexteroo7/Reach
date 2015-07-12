package reach.backend.User;

/**
 * Created by dexter on 12/07/15.
 */
public class OldUserContainerNew {

    private final String name;
    private final String imageId;

    public String getPromoCode() {
        return promoCode;
    }

    public String getImageId() {
        return imageId;
    }

    public String getName() {
        return name;
    }

    private final String promoCode;

    public OldUserContainerNew(String name, String promoCode, String imageId) {
        this.name = name;
        this.promoCode = promoCode;
        this.imageId = imageId;
    }
}
