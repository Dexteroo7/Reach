package reach.backend.User;

import java.util.HashSet;

/**
 * Created by Dexter on 07-03-2015.
 */
public class MusicContainer {

    private HashSet<ReachSong> reachSongs = new HashSet<>();
    private HashSet<ReachPlayList> reachPlayLists = new HashSet<>();
    private boolean isSongsChanged = true;
    private boolean isPlayListsChanged = true;
    private int songsHash = 0;
    private int playListHash = 0;

    private String genres = "hello_world";
    private long clientId = 0;

    public MusicContainer() {

    }

    public MusicContainer(HashSet<ReachSong> reachSongs,
                          HashSet<ReachPlayList> reachPlayLists,
                          boolean isSongsChanged,
                          boolean isPlayListsChanged,
                          int songsHash, int playListHash) {

        this.reachSongs = reachSongs;
        this.reachPlayLists = reachPlayLists;
        this.isSongsChanged = isSongsChanged;
        this.isPlayListsChanged = isPlayListsChanged;
        this.songsHash = songsHash;
        this.playListHash = playListHash;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    public int getPlayListHash() {
        return playListHash;
    }

    public void setPlayListHash(int playListHash) {
        this.playListHash = playListHash;
    }

    public int getSongsHash() {
        return songsHash;
    }

    public void setSongsHash(int songsHash) {
        this.songsHash = songsHash;
    }

    public boolean isPlayListsChanged() {
        return isPlayListsChanged;
    }

    public void setPlayListsChanged(boolean isPlayListsChanged) {
        this.isPlayListsChanged = isPlayListsChanged;
    }

    public boolean isSongsChanged() {
        return isSongsChanged;
    }

    public void setSongsChanged(boolean isSongsChanged) {
        this.isSongsChanged = isSongsChanged;
    }

    public HashSet<ReachPlayList> getReachPlayLists() {
        return reachPlayLists;
    }

    public void setReachPlayLists(HashSet<ReachPlayList> reachPlayLists) {
        this.reachPlayLists = reachPlayLists;
    }

    public HashSet<ReachSong> getReachSongs() {
        return reachSongs;
    }

    public void setReachSongs(HashSet<ReachSong> reachSongs) {
        this.reachSongs = reachSongs;
    }
}
