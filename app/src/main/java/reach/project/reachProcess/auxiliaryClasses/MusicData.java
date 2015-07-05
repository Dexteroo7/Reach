package reach.project.reachProcess.auxiliaryClasses;

import reach.project.database.sql.ReachDatabaseHelper;
import reach.project.database.sql.ReachSongHelper;

/**
 * Created by Dexter on 15-05-2015.
 */
public class MusicData {

    public static final String [] DOWNLOADED_LIST = new String[]{ //count = 14
            ReachDatabaseHelper.COLUMN_ID, //0
            ReachDatabaseHelper.COLUMN_LENGTH, //1
            ReachDatabaseHelper.COLUMN_RECEIVER_ID, //2
            ReachDatabaseHelper.COLUMN_PROCESSED, //3
            ReachDatabaseHelper.COLUMN_PATH, //4
            ReachDatabaseHelper.COLUMN_DISPLAY_NAME, //5

            ReachDatabaseHelper.COLUMN_STATUS, //6
            ReachDatabaseHelper.COLUMN_OPERATION_KIND, //7
            ReachDatabaseHelper.COLUMN_SENDER_ID, //8
            ReachDatabaseHelper.COLUMN_LOGICAL_CLOCK,//9
            ReachDatabaseHelper.COLUMN_SONG_ID, //10

            ReachDatabaseHelper.COLUMN_SENDER_NAME, //11
            ReachDatabaseHelper.COLUMN_ONLINE_STATUS, //12
            ReachDatabaseHelper.COLUMN_NETWORK_TYPE, //13
            ReachDatabaseHelper.COLUMN_IS_LIKED}; //14

    public static final String [] DISK_LIST = new String[] { //count = 8
            ReachSongHelper.COLUMN_SIZE,
            ReachSongHelper.COLUMN_PATH,
            ReachSongHelper.COLUMN_DISPLAY_NAME,
            ReachSongHelper.COLUMN_ID,
            ReachSongHelper.COLUMN_DURATION,
            ReachSongHelper.COLUMN_ARTIST,
            ReachSongHelper.COLUMN_ALBUM,
            ReachSongHelper.COLUMN_SONG_ID,
    };

    public static final String [] DOWNLOADED_PARTIAL = new String[]{
            ReachDatabaseHelper.COLUMN_ID, //0
            ReachDatabaseHelper.COLUMN_LENGTH, //1
            ReachDatabaseHelper.COLUMN_SENDER_ID, //2
            ReachDatabaseHelper.COLUMN_PROCESSED, //3
            ReachDatabaseHelper.COLUMN_PATH, //4
            ReachDatabaseHelper.COLUMN_DISPLAY_NAME, //5
            ReachDatabaseHelper.COLUMN_IS_LIKED, //6
            ReachDatabaseHelper.COLUMN_SONG_ID}; //7

    public static final String [] DISK_PARTIAL = new String[]{
            ReachSongHelper.COLUMN_ARTIST, //0
            ReachSongHelper.COLUMN_SONG_ID, //1
            ReachSongHelper.COLUMN_SIZE, //2
            ReachSongHelper.COLUMN_PATH, //3
            ReachSongHelper.COLUMN_DISPLAY_NAME, //4
            ReachSongHelper.COLUMN_ID, //5
    ReachSongHelper.COLUMN_DURATION}; //6

    public static final String[] DISK_COMPLETE_NO_PATH =
            {
                    ReachSongHelper.COLUMN_ID,

                    ReachSongHelper.COLUMN_SONG_ID,
                    ReachSongHelper.COLUMN_USER_ID,

                    ReachSongHelper.COLUMN_DISPLAY_NAME,
                    ReachSongHelper.COLUMN_ACTUAL_NAME,

                    ReachSongHelper.COLUMN_ARTIST,
                    ReachSongHelper.COLUMN_ALBUM,

                    ReachSongHelper.COLUMN_DURATION,
                    ReachSongHelper.COLUMN_SIZE,

                    ReachSongHelper.COLUMN_VISIBILITY,
            };

    private final String displayName;
    private final String path;
    private final String artistName;
    private final long id;
    private final long length;
    private final long senderId;
    private final byte type;

    public MusicData(String displayName, String path, String artistName,
                     long id, long length, long senderId, long processed, byte type, boolean isLiked, long duration) {
        this.displayName = displayName;
        this.path = path;
        this.id = id;
        this.length = length;
        this.senderId = senderId;
        this.processed = processed;
        this.artistName = artistName;
        this.type = type;
        this.isLiked = isLiked;
        this.duration = duration; //for MyLibrary case
    }

    private long duration; //found out by the musicHandler
    private boolean isLiked; //toggled by user
    private long processed; //changes
    private int currentPosition; //changes
    private short primaryProgress; //changes

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getSenderId() {
        return senderId;
    }

    public void setProcessed(long processed) {
        this.processed = processed;
    }

    public void setIsLiked(boolean isLiked) {
        this.isLiked = isLiked;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public byte getType() {
        return type;
    }

    public short getSecondaryProgress() {
        return secondaryProgress;
    }

    public void setSecondaryProgress(short secondaryProgress) {
        this.secondaryProgress = secondaryProgress;
    }

    public short getPrimaryProgress() {
        return primaryProgress;
    }

    public void setPrimaryProgress(short primaryProgress) {
        this.primaryProgress = primaryProgress;
    }

    private short secondaryProgress;

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }

    public long getDuration() {
        return duration;
    }

    public String getArtistName() {
        return artistName;
    }

    public long getProcessed() {
        return processed;
    }

    public long getLength() {
        return length;
    }

    public long getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MusicData)) return false;

        MusicData musicData = (MusicData) o;

        if (id != musicData.id) return false;
        if (length != musicData.length) return false;
        if (senderId != musicData.senderId) return false;
        if (type != musicData.type) return false;
        if (duration != musicData.duration) return false;
        if (!displayName.equals(musicData.displayName)) return false;
        if (!path.equals(musicData.path)) return false;
        return !(artistName != null ? !artistName.equals(musicData.artistName) : musicData.artistName != null);

    }

    @Override
    public int hashCode() {
        int result = displayName.hashCode();
        result = 31 * result + path.hashCode();
        result = 31 * result + (artistName != null ? artistName.hashCode() : 0);
        result = 31 * result + (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (length ^ (length >>> 32));
        result = 31 * result + (int) (senderId ^ (senderId >>> 32));
        result = 31 * result + (int) type;
        result = 31 * result + (int) (duration ^ (duration >>> 32));
        return result;
    }
}