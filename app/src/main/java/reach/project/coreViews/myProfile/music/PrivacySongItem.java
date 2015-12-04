package reach.project.coreViews.myProfile.music;

/**
 * Created by dexter on 03/12/15.
 */
final class PrivacySongItem {

    static final byte DOWNLOADED_SONG = 0;
    static final byte MY_LIBRARY = 1;

    String displayName;
    String actualName;
    String artistName;
    String albumName;

    long size;
    long duration;
    long dateAdded;
    long userId;
    long songId;

    byte type;

    boolean visible;

    public PrivacySongItem(String displayName, String actualName, String artistName, String albumName,
                           long size, long duration, long dateAdded, long userId, long songId,
                           byte type, boolean visible) {

        this.displayName = displayName;
        this.actualName = actualName;
        this.artistName = artistName;
        this.albumName = albumName;
        this.size = size;
        this.duration = duration;
        this.dateAdded = dateAdded;
        this.userId = userId;
        this.songId = songId;
        this.type = type;
        this.visible = visible;
    }

    public PrivacySongItem() {

        this.displayName = "";
        this.actualName = "";
        this.artistName = "";
        this.albumName = "";
        this.size = 0;
        this.duration = 0;
        this.dateAdded = 0;
        this.userId = 0;
        this.songId = 0;
        this.type = -1;
        this.visible = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrivacySongItem)) return false;

        PrivacySongItem songItem = (PrivacySongItem) o;

        if (size != songItem.size) return false;
        if (duration != songItem.duration) return false;
        if (dateAdded != songItem.dateAdded) return false;
        if (userId != songItem.userId) return false;
        if (songId != songItem.songId) return false;
        if (type != songItem.type) return false;
        if (displayName != null ? !displayName.equals(songItem.displayName) : songItem.displayName != null)
            return false;
        if (actualName != null ? !actualName.equals(songItem.actualName) : songItem.actualName != null)
            return false;
        if (artistName != null ? !artistName.equals(songItem.artistName) : songItem.artistName != null)
            return false;
        return !(albumName != null ? !albumName.equals(songItem.albumName) : songItem.albumName != null);

    }

    @Override
    public int hashCode() {
        int result = displayName != null ? displayName.hashCode() : 0;
        result = 31 * result + (actualName != null ? actualName.hashCode() : 0);
        result = 31 * result + (artistName != null ? artistName.hashCode() : 0);
        result = 31 * result + (albumName != null ? albumName.hashCode() : 0);
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (int) (duration ^ (duration >>> 32));
        result = 31 * result + (int) (dateAdded ^ (dateAdded >>> 32));
        result = 31 * result + (int) (userId ^ (userId >>> 32));
        result = 31 * result + (int) (songId ^ (songId >>> 32));
        result = 31 * result + (int) type;
        return result;
    }
}
