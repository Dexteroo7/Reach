package reach.project.coreViews.explore;

public class ExploreContainer {

    private final String title;
    private final String subTitle;
    private final String imageId;
    private final String userImageId;
    private final String userHandle;
    private final float rating;
    private final ExploreTypes types;
    private final long id;

    public ExploreContainer(String title, String subTitle, String imageId, String userImageId, String userHandle, float rating, ExploreTypes types, long id) {

        this.title = title;
        this.subTitle = subTitle;
        this.imageId = imageId;
        this.userImageId = userImageId;
        this.userHandle = userHandle;
        this.rating = rating;
        this.types = types;
        this.id = id;
    }

    public ExploreContainer(ExploreTypes types, long id) {
        this.title = "";
        this.subTitle = "";
        this.imageId = "";
        this.userImageId = "";
        this.userHandle = "";
        this.rating = 0;
        this.types = types;
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public String getImageId() {
        return imageId;
    }

    public String getUserHandle() {
        return userHandle;
    }

    public float getRating() {
        return rating;
    }

    public ExploreTypes getTypes() {
        return types;
    }

    public String getUserImageId() {
        return userImageId;
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExploreContainer that = (ExploreContainer) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return "ExploreContainer{" +
                "title='" + title + '\'' +
                ", subTitle='" + subTitle + '\'' +
                ", imageId='" + imageId + '\'' +
                ", userHandle='" + userHandle + '\'' +
                ", rating=" + rating +
                ", types=" + types +
                ", id=" + id +
                '}';
    }
}
