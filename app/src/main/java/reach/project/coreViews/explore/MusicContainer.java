package reach.project.coreViews.explore;

/**
 * Created by dexter on 04/12/15.
 */
class MusicContainer extends ExploreContainer {

    MusicContainer(String imageId, String userImageId, String userHandle, ExploreTypes types, long id) {
        super(imageId, userImageId, userHandle, types, id);
    }

    public long songId;
    public long senderId;
    public long length;
    public long duration;

    public String artistName;
    public String albumName;
    public String displayName;
    public String actualName;
}
