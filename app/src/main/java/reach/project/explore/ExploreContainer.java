package reach.project.explore;

/**
 * Created by dexter on 16/10/15.
 */
public class ExploreContainer {

    private final String toShow;
    private final ExploreTypes types;

    public ExploreContainer(String toShow, ExploreTypes types) {
        this.toShow = toShow;
        this.types = types;
    }

    public String getToShow() {
        return toShow;
    }

    public ExploreTypes getTypes() {
        return types;
    }

    @Override
    public String toString() {
        return "ExploreContainer{" +
                "toShow='" + toShow + '\'' +
                ", types=" + types +
                '}';
    }
}
