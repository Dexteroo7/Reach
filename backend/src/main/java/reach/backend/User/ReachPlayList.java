package reach.backend.user;

import java.util.Arrays;

/**
 * Created by dexter on 7/8/14.
 */
public class ReachPlayList {

    private String[] reachSongs; //array of reachSongIds
    private String playlistName = "hello_world";
    private String dateModified = "hello_world";
    private long userId = 0;
    private long playListId = -1;
    private short visibility = 1;

    public long getPlayListId() {
        return playListId;
    }

    public void setPlayListId(long playListId) {
        this.playListId = playListId;
    }

    public String[] getReachSongs() {
        return reachSongs;
    }

    public void setReachSongs(String[] reachSongs) {
        this.reachSongs = reachSongs;
    }

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

    public String getDateModified() {
        return dateModified;
    }

    public void setDateModified(String dateModified) {
        this.dateModified = dateModified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ReachPlayList that = (ReachPlayList) o;

        if (playListId != that.playListId) return false;
        if (userId != that.userId) return false;
        if (visibility != that.visibility) return false;
        if (dateModified != null ? !dateModified.equals(that.dateModified) : that.dateModified != null)
            return false;
        if (playlistName != null ? !playlistName.equals(that.playlistName) : that.playlistName != null)
            return false;
        return Arrays.equals(reachSongs, that.reachSongs);

    }

    @Override
    public int hashCode() {
        int result = reachSongs != null ? Arrays.hashCode(reachSongs) : 0;
        result = 31 * result + (playlistName != null ? playlistName.hashCode() : 0);
        result = 31 * result + (dateModified != null ? dateModified.hashCode() : 0);
        result = 31 * result + (int) (userId ^ (userId >>> 32));
        result = 31 * result + (int) (playListId ^ (playListId >>> 32));
        result = 31 * result + (int) visibility;
        return result;
    }
}