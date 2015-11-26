package reach.backend.ObjectWrappers;

/**
 * Created by dexter on 20/11/15.
 */
public class SimpleSong {

    public Long songId;
    public Long size;
    public Boolean visibility;
    public Integer year;
    public Long dateAdded;
    public Long duration;
    public String genre;
    public String displayName;
    public String actualName;
    public String artist;
    public String album;
    public String fileHash;
    public String path;
    public Boolean isLiked;

    public SimpleSong() {

    }

    public SimpleSong(Long songId, Long size, Boolean visibility, Integer year, Long dateAdded, Long duration, String genre, String displayName, String actualName, String artist, String album, String fileHash, String path, Boolean isLiked) {
        this.songId = songId;
        this.size = size;
        this.visibility = visibility;
        this.year = year;
        this.dateAdded = dateAdded;
        this.duration = duration;
        this.genre = genre;
        this.displayName = displayName;
        this.actualName = actualName;
        this.artist = artist;
        this.album = album;
        this.fileHash = fileHash;
        this.path = path;
        this.isLiked = isLiked;
    }

    public Long getSongId() {
        return songId;
    }

    public void setSongId(Long songId) {
        this.songId = songId;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Boolean getVisibility() {
        return visibility;
    }

    public void setVisibility(Boolean visibility) {
        this.visibility = visibility;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(Long dateAdded) {
        this.dateAdded = dateAdded;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
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

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean getIsLiked() {
        return isLiked;
    }

    public void setIsLiked(Boolean isLiked) {
        this.isLiked = isLiked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleSong)) return false;

        SimpleSong that = (SimpleSong) o;

        if (songId != null ? !songId.equals(that.songId) : that.songId != null) return false;
        if (size != null ? !size.equals(that.size) : that.size != null) return false;
        if (duration != null ? !duration.equals(that.duration) : that.duration != null)
            return false;
        if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null)
            return false;
        if (actualName != null ? !actualName.equals(that.actualName) : that.actualName != null)
            return false;
        if (artist != null ? !artist.equals(that.artist) : that.artist != null) return false;
        return !(album != null ? !album.equals(that.album) : that.album != null);

    }

    @Override
    public int hashCode() {
        int result = songId != null ? songId.hashCode() : 0;
        result = 31 * result + (size != null ? size.hashCode() : 0);
        result = 31 * result + (duration != null ? duration.hashCode() : 0);
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (actualName != null ? actualName.hashCode() : 0);
        result = 31 * result + (artist != null ? artist.hashCode() : 0);
        result = 31 * result + (album != null ? album.hashCode() : 0);
        return result;
    }
}
