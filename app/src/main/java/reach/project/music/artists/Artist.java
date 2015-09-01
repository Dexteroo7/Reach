package reach.project.music.artists;

/**
 * Created by dexter on 30/12/14.
 */
public final class Artist {

    private String artistName;
    private String album;

    public String getArtistMbid() {
        return artistMbid;
    }

    public void setArtistMbid(String artistMbid) {
        this.artistMbid = artistMbid;
    }

    private String artistMbid; //this is where the artist image will come from !

    private long userId;
    private int size = 0;
    private long id = -1;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public long getUserID() {
        return userId;
    }

    public void setUserID(long userID) {
        this.userId = userID;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }
}
