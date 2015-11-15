package reach.project.music;

/**
 * Created by Dexter on 10-04-2015.
 */
public final class TransferSong {

    private final long songId;
    private final long size;
    private final long duration;
    private final String displayName;
    private final String actualName;
    private final String artistName;

    private final String albumName;
    private final String genre;
    private final byte [] albumArtData;

    public TransferSong(long size, long songId, long duration,
                        String displayName, String actualName, String artistName,
                        String albumName, String genre, byte [] albumArtData) {
        this.size = size;
        this.songId = songId;
        this.duration = duration;
        this.artistName = artistName;
        this.displayName = displayName;
        this.actualName = actualName;
        this.albumName = albumName;
        this.genre = genre;
        this.albumArtData = albumArtData;
    }

    public long getDuration() {
        return duration;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getActualName() {
        return actualName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getSongId() {
        return songId;
    }

    public long getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransferSong)) return false;

        TransferSong that = (TransferSong) o;

        if (getSize() != that.getSize()) return false;
        if (getSongId() != that.getSongId()) return false;
        if (getDisplayName() != null ? !getDisplayName().equals(that.getDisplayName()) : that.getDisplayName() != null)
            return false;
        return !(getActualName() != null ? !getActualName().equals(that.getActualName()) : that.getActualName() != null);

    }

    @Override
    public int hashCode() {
        int result = (int) (getSize() ^ (getSize() >>> 32));
        result = 31 * result + (int) (getSongId() ^ (getSongId() >>> 32));
        result = 31 * result + (getDisplayName() != null ? getDisplayName().hashCode() : 0);
        result = 31 * result + (getActualName() != null ? getActualName().hashCode() : 0);
        return result;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getGenre() {
        return genre;
    }

    public byte[] getAlbumArtData() {
        return albumArtData;
    }
}
