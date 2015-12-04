package reach.project.coreViews.explore;

class ExploreContainer {

    public final String imageId;
    public final String userImageId;
    public final String userHandle;
    public final ExploreTypes types;
    public final long id;

    ExploreContainer(String imageId, String userImageId, String userHandle,
                            ExploreTypes types, long id) {

        this.imageId = imageId;
        this.userImageId = userImageId;
        this.userHandle = userHandle;
        this.types = types;
        this.id = id;
    }

    ExploreContainer(ExploreTypes types, long id) {
        this.imageId = "";
        this.userImageId = "";
        this.userHandle = "";
        this.types = types;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExploreContainer)) return false;

        ExploreContainer that = (ExploreContainer) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
