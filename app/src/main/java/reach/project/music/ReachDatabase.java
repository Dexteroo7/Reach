package reach.project.music;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.common.hash.Hashing;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import reach.project.utils.MiscUtils;

/**
 * Created by dexter on 14/9/14.
 */
public final class ReachDatabase {

    public enum OperationKind {
        DOWNLOAD_OP(0),
        UPLOAD_OP(1);

        private final int value;

        OperationKind(int value) {
            this.value = value;
        }

        public static OperationKind getFromValue(short value) {

            switch (value) {

                case 0:
                    return DOWNLOAD_OP;
                case 1:
                    return UPLOAD_OP;
                default:
                    throw new IllegalArgumentException("Operation king can not have this value " + value);
            }
        }

        @NonNull
        public Integer getValue() {
            return value;
        }

        public String getString() {
            return value + "";
        }
    }

    public enum Status {

        NOT_WORKING(0),
        RELAY(1),
        WORKING(2),
        GCM_FAILED(3),
        FILE_NOT_FOUND(4),
        FILE_NOT_CREATED(5),
        FINISHED(6),
        PAUSED_BY_USER(7),
        PAUSED_BY_HOST(8),
        HOST_GONE(9);

        private final int value;

        Status(int value) {
            this.value = value;
        }

        public static Status getFromValue(short value) {

            switch (value) {

                case 0:
                    return NOT_WORKING;
                case 1:
                    return RELAY;
                case 2:
                    return WORKING;
                case 3:
                    return GCM_FAILED;
                case 4:
                    return FILE_NOT_FOUND;
                case 5:
                    return FILE_NOT_CREATED;
                case 6:
                    return FINISHED;
                case 7:
                    return PAUSED_BY_USER;
                case 8:
                    return PAUSED_BY_HOST;
                case 9:
                    return HOST_GONE;
                default:
                    throw new IllegalArgumentException("Operation king can not have this value " + value);
            }
        }

        @NonNull
        public Integer getValue() {
            return value;
        }

        public String getString() {
            return value + "";
        }
    }

    private long id = -1;
    private long songId = 0;
    private long uniqueId = 0;
    private String metaHash;

    private String displayName = "hello_world";
    private String actualName = "hello_world";
    private String artistName = "hello_world";
    private String albumName = "hello_world";

    private long duration = 0;
    private long length = 0;

    private String genre = "hello_world";
    private byte[] albumArtData;
    private String path = "hello_world";
    private DateTime dateAdded = DateTime.now();

    private boolean visibility = false;
    private boolean isLiked = false;

    private long receiverId = 0;
    private long senderId = 0;
    private String userName = "hello_world";
    private String onlineStatus = "hello_world";
    private OperationKind operationKind = OperationKind.DOWNLOAD_OP;
    private int logicalClock = 0;
    private long processed = 0;
    private Status status = Status.NOT_WORKING;

    //not in sql
    private long lastActive = 0;
    private long reference = 0;

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }


    public String getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(String onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String senderName) {
        this.userName = senderName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getActualName() {
        return actualName;
    }

    public void setActualName(String actualName) {
        this.actualName = actualName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(long receiverId) {
        this.receiverId = receiverId;
    }

    public long getSenderId() {
        return senderId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public long getSongId() {
        return songId;
    }

    public void setSongId(long songId) {
        this.songId = songId;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public DateTime getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(DateTime added) {
        this.dateAdded = added;
    }

    public int getLogicalClock() {
        return logicalClock;
    }

    public void setLogicalClock(int logicalClock) {
        this.logicalClock = logicalClock;
    }

    public long getLastActive() {
        return lastActive;
    }

    public void setLastActive(long lastActive) {
        this.lastActive = lastActive;
    }

    public long getProcessed() {
        return processed;
    }

    public void setProcessed(long processed) {
        this.processed = processed;
    }

    public OperationKind getOperationKind() {
        return operationKind;
    }

    public void setOperationKind(OperationKind operationKind) {
        this.operationKind = operationKind;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "ReachDatabase{" +
                ", songId=" + songId +
                ", receiverId=" + receiverId +
                ", senderId=" + senderId +
                ", operationKind=" + operationKind +
                ", displayName='" + displayName + '\'' +
                ", actualName='" + actualName + '\'' +
                ", length=" + length +
                ", added=" + dateAdded.toString(DateTimeFormat.fullTime()) +
                '}';
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ReachDatabase that = (ReachDatabase) o;
        return dateAdded == that.dateAdded &&
                length == that.length &&
                operationKind == that.operationKind &&
                receiverId == that.receiverId &&
                senderId == that.senderId &&
                songId == that.songId;
    }

    @Override
    public int hashCode() {
        int result = (int) (songId ^ (songId >>> 32));
        result = 31 * result + (int) (receiverId ^ (receiverId >>> 32));
        result = 31 * result + (int) (senderId ^ (senderId >>> 32));
        result = 31 * result + operationKind.hashCode();
        result = 31 * result + (int) (length ^ (length >>> 32));
        result = 31 * result + dateAdded.hashCode();
        return result;
    }

    public long getReference() {
        return reference;
    }

    public void setReference(long reference) {
        this.reference = reference;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setIsLiked(boolean isLiked) {
        this.isLiked = isLiked;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public byte[] getAlbumArtData() {
        return albumArtData;
    }

    public void setAlbumArtData(byte[] albumArtData) {
        this.albumArtData = albumArtData;
    }

    public boolean getVisibility() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }

    public long getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(long uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getMetaHash() {

        if (TextUtils.isEmpty(metaHash))
            metaHash = MiscUtils.calculateSongHash(
                    receiverId, duration, length, displayName, Hashing.sipHash24());

        return metaHash;
    }

    public void setMetaHash(String metaHash) {
        this.metaHash = metaHash;
    }
}