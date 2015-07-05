package reach.backend.ObjectWrappers;

import java.util.List;

/**
 * Created by dexter on 31/12/14.
 */
public class ReachAlbum {

    private String albumName;
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

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }
}
