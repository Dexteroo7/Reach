package reach.backend.User;

/**
 * Created by dexter on 04/11/15.
 */
public class AliveCheck {

    private long [] id;
    private boolean [] alive;

    public AliveCheck() {
    }

    public boolean[] getAlive() {
        return alive;
    }

    public void setAlive(boolean[] alive) {
        this.alive = alive;
    }

    public long[] getId() {
        return id;
    }

    public void setId(long[] id) {
        this.id = id;
    }
}
