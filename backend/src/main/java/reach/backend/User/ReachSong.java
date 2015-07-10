package reach.backend.User;

/**
 * Created by dexter on 7/8/14.
 */
public class ReachSong {

    private long songId = -1;
    private long size = 0;
    private short visibility = 1;
    private int year = 0;
    private long dateAdded = 0;
    private long userId = 0;
    private long duration = 0;

    private String genre = "hello_world";
    private String displayName = "hello_world";
    private String actualName = "hello_world";
    private String artist = "hello_world";
    private String album = "hello_world";
    private String albumArtUrl = "hello_world";
    private String formattedDataAdded = "hello_world";
    private String fileHash = "hello_world";
    private String path = "hello_world";

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getAlbumArtUrl() {
        return albumArtUrl;
    }

    public void setAlbumArtUrl(String albumArtUrl) {
        this.albumArtUrl = albumArtUrl;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getFormattedDataAdded() {
        return formattedDataAdded;
    }

    public void setFormattedDataAdded(String formattedDataAdded) {
        this.formattedDataAdded = formattedDataAdded;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public long getSongId() {
        return songId;
    }

    public void setSongId(long songId) {
        this.songId = songId;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getActualName() {
        return actualName;
    }

    public void setActualName(String actualName) {
        this.actualName = actualName;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public short getVisibility() {
        return visibility;
    }

    public void setVisibility(short visibility) {
        this.visibility = visibility;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ReachSong reachSong = (ReachSong) o;

        if (dateAdded != reachSong.dateAdded) return false;
        if (duration != reachSong.duration) return false;
        if (size != reachSong.size) return false;
        if (songId != reachSong.songId) return false;
        if (userId != reachSong.userId) return false;
        if (visibility != reachSong.visibility) return false;
        if (year != reachSong.year) return false;
        if (actualName != null ? !actualName.equals(reachSong.actualName) : reachSong.actualName != null)
            return false;
        if (album != null ? !album.equals(reachSong.album) : reachSong.album != null) return false;
        if (artist != null ? !artist.equals(reachSong.artist) : reachSong.artist != null)
            return false;
        if (displayName != null ? !displayName.equals(reachSong.displayName) : reachSong.displayName != null)
            return false;
        if (formattedDataAdded != null ? !formattedDataAdded.equals(reachSong.formattedDataAdded) : reachSong.formattedDataAdded != null)
            return false;
        if (genre != null ? !genre.equals(reachSong.genre) : reachSong.genre != null) return false;
        return !(path != null ? !path.equals(reachSong.path) : reachSong.path != null);

    }

    @Override
    public int hashCode() {
        int result = (int) (songId ^ (songId >>> 32));
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (int) visibility;
        result = 31 * result + year;
        result = 31 * result + (int) (dateAdded ^ (dateAdded >>> 32));
        result = 31 * result + (int) (userId ^ (userId >>> 32));
        result = 31 * result + (int) (duration ^ (duration >>> 32));
        result = 31 * result + (genre != null ? genre.hashCode() : 0);
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (actualName != null ? actualName.hashCode() : 0);
        result = 31 * result + (artist != null ? artist.hashCode() : 0);
        result = 31 * result + (album != null ? album.hashCode() : 0);
        result = 31 * result + (formattedDataAdded != null ? formattedDataAdded.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ReachSong{" +
                "songId=" + songId +
                ", size=" + size +
                ", visibility=" + visibility +
                ", year=" + year +
                ", dateAdded=" + dateAdded +
                ", userId=" + userId +
                ", duration=" + duration +
                ", genre='" + genre + '\'' +
                ", displayName='" + displayName + '\'' +
                ", actualName='" + actualName + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", albumArtUrl='" + albumArtUrl + '\'' +
                ", formattedDataAdded='" + formattedDataAdded + '\'' +
                ", fileHash='" + fileHash + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}