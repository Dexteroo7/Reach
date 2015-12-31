package reach.backend.ObjectWrappers;

import java.util.List;

/**
 * Created by dexter on 30/12/15.
 */
public class LongList {

    List<Long> longs;

    public LongList(List<Long> longs) {
        this.longs = longs;
    }

    public LongList() {

    }

    public List<Long> getLongs() {
        return longs;
    }

    public void setLongs(List<Long> longs) {
        this.longs = longs;
    }
}
