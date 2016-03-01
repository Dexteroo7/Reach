package reach.project.music;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.common.hash.Hashing;

import org.joda.time.DateTime;

import reach.project.coreViews.friends.ReachFriendsHelper;
import reach.project.utils.MiscUtils;

/**
 * Created by dexter on 14/9/14.
 */
public final class ReachDatabase {

    private ReachDatabase(long id, long songId, long uniqueId, String metaHash, long receiverId, long senderId, OperationKind operationKind, String userName, String artistName, String albumName,
                          String genre, String displayName, String actualName, byte[] albumArtData, long length, DateTime dateAdded, long duration, boolean isLiked, ReachFriendsHelper.Status onlineStatus,
                          boolean visibility) {

        this.id = id;
        this.songId = songId;
        this.uniqueId = uniqueId;
        this.metaHash = metaHash;
        this.receiverId = receiverId;
        this.senderId = senderId;
        this.operationKind = operationKind;
        this.userName = userName;
        this.artistName = artistName;
        this.albumName = albumName;
        this.genre = genre;
        this.displayName = displayName;
        this.actualName = actualName;
        this.albumArtData = albumArtData;
        this.length = length;
        this.dateAdded = dateAdded;
        this.duration = duration;
        this.isLiked = isLiked;
        this.onlineStatus = onlineStatus;
        this.visibility = visibility;
    }

    private ReachDatabase() {
        throw new IllegalAccessError("Can not access");
    }

    public long getId() {
        return id;
    }

    public long getSongId() {
        return songId;
    }

    public String getMetaHash() {
        return metaHash;
    }

    public long getReceiverId() {
        return receiverId;
    }

    public long getSenderId() {
        return senderId;
    }

    public OperationKind getOperationKind() {
        return operationKind;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUserName() {
        return userName;
    }

    public ReachFriendsHelper.Status getOnlineStatus() {
        return onlineStatus;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public String getGenre() {
        return genre;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getActualName() {
        return actualName;
    }

    public byte[] getAlbumArtData() {
        return albumArtData;
    }

    public long getLength() {
        return length;
    }

    public long getProcessed() {
        return processed;
    }

    public void setProcessed(long processed) {
        this.processed = processed;
    }

    public DateTime getDateAdded() {
        return dateAdded;
    }

    public long getDuration() {
        return duration;
    }

    public long getUniqueId() {
        return uniqueId;
    }

    public int getLogicalClock() {
        return logicalClock;
    }

    public void setLogicalClock(int logicalClock) {
        this.logicalClock = logicalClock;
    }

    public boolean isVisibility() {
        return visibility;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public long getLastActive() {
        return lastActive;
    }

    public void setLastActive(long lastActive) {
        this.lastActive = lastActive;
    }

    public long getReference() {
        return reference;
    }

    public void setReference(long reference) {
        this.reference = reference;
    }

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

    private final long id;
    private final long songId;
    private final String metaHash;

    private final long receiverId;
    private final long senderId;
    private final OperationKind operationKind;

    private String path = "hello_world";

    private final String userName;
    private final ReachFriendsHelper.Status onlineStatus;
    private final String artistName;
    private final String albumName;
    private final boolean isLiked;
    private final String genre;

    private final String displayName;
    private final String actualName;
    private final byte[] albumArtData;

    private final long length;
    private long processed = 0;
    private final DateTime dateAdded;
    private final long duration;
    private final long uniqueId;

    private int logicalClock = 0;
    private final boolean visibility;
    private Status status = Status.NOT_WORKING;

    //not in sql
    private long lastActive = 0;
    private long reference = 0;

    public static final class Builder {

        private long id;
        private long songId;
        private String metaHash;

        private long receiverId;
        private long senderId;
        private OperationKind operationKind;

        private String path = "hello_world";

        private String userName;
        private ReachFriendsHelper.Status onlineStatus;
        private String artistName;
        private String albumName;
        private boolean isLiked;
        private String genre;

        private String displayName;
        private String actualName;
        private byte[] albumArtData;

        private long length;
        private long processed = 0;
        private DateTime dateAdded;
        private long duration;
        private long uniqueId;

        private int logicalClock = 0;
        private boolean visibility;
        private Status status = Status.NOT_WORKING;

        public Builder setVisibility(boolean visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder setSongId(long songId) {
            this.songId = songId;
            return this;
        }

        public Builder setReceiverId(long receiverId) {
            this.receiverId = receiverId;
            return this;
        }

        public Builder setSenderId(long senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder setOperationKind(OperationKind operationKind) {
            this.operationKind = operationKind;
            return this;
        }

        public Builder setUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder setArtistName(String artistName) {
            this.artistName = artistName;
            return this;
        }

        public Builder setAlbumName(String albumName) {
            this.albumName = albumName;
            return this;
        }

        public Builder setGenre(String genre) {
            this.genre = genre;
            return this;
        }

        public Builder setDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder setActualName(String actualName) {
            this.actualName = actualName;
            return this;
        }

        public Builder setAlbumArtData(byte[] albumArtData) {
            this.albumArtData = albumArtData;
            return this;
        }

        public Builder setLength(long length) {
            this.length = length;
            return this;
        }

        public Builder setDateAdded(DateTime dateAdded) {
            this.dateAdded = dateAdded;
            return this;
        }

        public Builder setDuration(long duration) {
            this.duration = duration;
            return this;
        }

        public ReachDatabase build() {

            if (TextUtils.isEmpty(metaHash))
                metaHash = MiscUtils.calculateSongHash(receiverId, duration, length, displayName, Hashing.sipHash24());
            return new ReachDatabase(id, songId, uniqueId, metaHash, receiverId, senderId, operationKind, userName, artistName, albumName,
                    genre, displayName, actualName, albumArtData, length, dateAdded, duration, isLiked, onlineStatus, visibility);
        }

        public Builder setUniqueId(long uniqueId) {
            this.uniqueId = uniqueId;
            return this;
        }

        public long getSongId() {
            return songId;
        }

        public long getSenderId() {
            return senderId;
        }

        public String getUserName() {
            return userName;
        }

        public String getArtistName() {
            return artistName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getActualName() {
            return actualName;
        }

        public long getLength() {
            return length;
        }

        public DateTime getDateAdded() {
            return dateAdded;
        }

        public long getDuration() {
            return duration;
        }

        public Builder setLiked(boolean liked) {
            isLiked = liked;
            return this;
        }

        public Builder setId(long id) {
            this.id = id;
            return this;
        }

        public Builder setOnlineStatus(ReachFriendsHelper.Status onlineStatus) {
            this.onlineStatus = onlineStatus;
            return this;
        }

        public Builder setMetaHash(String metaHash) {
            this.metaHash = metaHash;
            return this;
        }

        public long getId() {
            return id;
        }

        public String getPath() {
            return path;
        }

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public boolean isLiked() {
            return isLiked;
        }

        public Builder setProcessed(long processed) {
            this.processed = processed;
            return this;
        }

        public Builder setLogicalClock(int logicalClock) {
            this.logicalClock = logicalClock;
            return this;
        }

        public boolean isVisibility() {
            return visibility;
        }

        public Status getStatus() {
            return status;
        }

        public Builder setStatus(Status status) {
            this.status = status;
            return this;
        }
    }
}