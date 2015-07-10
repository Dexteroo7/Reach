package reach.backend.ObjectWrappers;

/**
 * Created by dexter on 7/8/14.
 */
import java.util.ArrayList;
import java.util.List;

public class ReachPlayList {

    private List<Long> reachSongs = new ArrayList<>();
    private String playlistName = "hello_world";
    private String dateModified = "hello_world";
    private long userId = 0;
    short visibility;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getPlaylistName() {
        return playlistName;
    }

    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
    }

    public short getVisibility() {
        return visibility;
    }

    public void setVisibility(short visibility) {
        this.visibility = visibility;
    }

    public List<Long> getReachSongs() {
        return reachSongs;
    }

    public void setReachSongs(List<Long> reachSongs) {
        this.reachSongs = reachSongs;
    }

    public String getDateModified() {
        return dateModified;
    }

    public void setDateModified(String dateModified) {
        this.dateModified = dateModified;
    }
}