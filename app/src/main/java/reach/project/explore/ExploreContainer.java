package reach.project.explore;

import java.util.Random;

/**
 * Created by dexter on 16/10/15.
 */
public class ExploreContainer {

    private final Random random = new Random();

    private final String toShow;
    private final ExploreTypes types;
    private final long id;

    public ExploreContainer(String toShow, long id, ExploreTypes types) {
        this.toShow = toShow;
        this.types = types;
        this.id = id;
    }

    public ExploreContainer(String toShow, ExploreTypes types) {
        this.toShow = toShow;
        this.types = types;
        this.id = random.nextInt(1000);
    }

    public String getToShow() {
        return toShow;
    }

    public ExploreTypes getTypes() {
        return types;
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExploreContainer)) return false;

        ExploreContainer container = (ExploreContainer) o;

        if (id != container.id) return false;
        if (toShow != null ? !toShow.equals(container.toShow) : container.toShow != null)
            return false;
        return types == container.types;

    }

    @Override
    public int hashCode() {
        int result = toShow != null ? toShow.hashCode() : 0;
        result = 31 * result + (types != null ? types.hashCode() : 0);
        result = 31 * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "ExploreContainer{" +
                "toShow='" + toShow + '\'' +
                ", types=" + types +
                ", id=" + id +
                '}';
    }
}
