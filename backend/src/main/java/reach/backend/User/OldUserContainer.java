package reach.backend.user;

/**
 * Created by Dexter on 13-04-2015.
 */
public class OldUserContainer {

    private final String firstName;
    private final String lastName;
    private final String imageId;

    public OldUserContainer(String firstName, String lastName, String imageId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.imageId = imageId;
    }

    public String getImageId() {
        return imageId;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }
}
