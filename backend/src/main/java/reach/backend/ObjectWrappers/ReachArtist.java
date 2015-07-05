package reach.backend.ObjectWrappers;

import java.util.List;

/**
 * Created by dexter on 31/12/14.
 */
public class ReachArtist {

    private String artistName;
    private long userId;
    private List<Long> songList;

    public List<Long> getSongList() {
        return songList;
    }

    public void setSongList(List<Long> songList) {
        this.songList = songList;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }
}
