package reach.project.music.albums;

/**
 * Created by dexter on 30/12/14.
 */
public final class Album {

    private String albumName;
    private String artist; //multiple possible ?
    private String releaseGroupMbid; //this is where the album art will come from !

    private long userId;
    private int size = 0;
    private long id = -1;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void incrementSize() {
        this.size++;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getReleaseGroupMbid() {
        return releaseGroupMbid;
    }

    public void setReleaseGroupMbid(String releaseGroupMbid) {
        this.releaseGroupMbid = releaseGroupMbid;
    }
}