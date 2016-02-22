package reach.project.reachProcess.auxiliaryClasses;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * Created by Dexter on 15-05-2015.
 */

public class MusicData implements Parcelable {

    public enum Type {
        DOWNLOADED,
        MY_LIBRARY
    }

    private final Type type;
    private final long columnId;
    private final String metaHash;

    private final String displayName;
    private final String artistName;
    private final String albumName;

    private long duration = 0; //effectively final
    private final long size;

    private final String path;
    private final long dateAdded;

    private boolean isLiked = false; //toggled by User

    private final long senderId;

    private long processed; //get updated
    private int currentPosition; //get updated
    private int primaryProgress; //get updated

    private MusicData() {
        throw new IllegalAccessError("Fuck off");
    }

    private MusicData(long id,
                      String metaHash,
                      long size,
                      long senderId,
                      long processed,
                      long dateAdded,
                      String path,
                      String displayName,
                      String artistName,
                      String albumName,
                      boolean isLiked,
                      long duration,
                      Type type) {

        this.metaHash = metaHash;
        this.displayName = displayName;
        this.path = path;
        this.columnId = id;
        this.size = size;
        this.senderId = senderId;
        this.processed = processed;
        this.artistName = artistName;
        this.type = type;
        this.isLiked = isLiked;
        this.duration = duration; //for MyLibrary case
        this.dateAdded = dateAdded;
        this.albumName = albumName;
    }

    public long getColumnId() {
        return columnId;
    }

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

    public Type getType() {
        return type;
    }

    public int getSecondaryProgress() {
        return secondaryProgress;
    }

    public void setSecondaryProgress(int secondaryProgress) {
        this.secondaryProgress = secondaryProgress;
    }

    public int getPrimaryProgress() {
        return primaryProgress;
    }

    public void setPrimaryProgress(int primaryProgress) {
        this.primaryProgress = primaryProgress;
    }

    private int secondaryProgress;

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

    public long getSize() {
        return size;
    }

    public String getPath() {
        return path;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public String getMetaHash() {
        return metaHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MusicData)) return false;

        MusicData musicData = (MusicData) o;

        if (columnId != musicData.columnId) return false;
        if (size != musicData.size) return false;
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
        result = 31 * result + (int) (columnId ^ (columnId >>> 32));
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (int) (senderId ^ (senderId >>> 32));
        switch (type) {

            case DOWNLOADED:
                result = 31 * result + 0;
                break;
            case MY_LIBRARY:
                result = 31 * result + 1;
                break;
        }
        result = 31 * result + (int) (duration ^ (duration >>> 32));
        return result;
    }

    protected MusicData(Parcel in) {

        displayName = in.readString();
        path = in.readString();
        artistName = in.readString();
        columnId = in.readLong();
        size = in.readLong();
        senderId = in.readLong();
        type = (Type) in.readSerializable();
        duration = in.readLong();
        isLiked = in.readByte() != 0;
        processed = in.readLong();
        currentPosition = in.readInt();
        primaryProgress = in.readInt();
        secondaryProgress = in.readInt();
        albumName = in.readString();
        dateAdded = in.readLong();

        metaHash = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(displayName);
        dest.writeString(path);
        dest.writeString(artistName);
        dest.writeLong(columnId);
        dest.writeLong(size);
        dest.writeLong(senderId);
        dest.writeSerializable(type);
        dest.writeLong(duration);
        dest.writeByte((byte) (isLiked ? 1 : 0));
        dest.writeLong(processed);
        dest.writeInt(currentPosition);
        dest.writeInt(primaryProgress);
        dest.writeInt(secondaryProgress);
        dest.writeString(albumName);
        dest.writeLong(dateAdded);

        dest.writeString(metaHash);
    }

    public static final Parcelable.Creator<MusicData> CREATOR = new Parcelable.Creator<MusicData>() {
        @Override
        public MusicData createFromParcel(Parcel in) {
            return new MusicData(in);
        }

        @Override
        public MusicData[] newArray(int size) {
            return new MusicData[size];
        }
    };

    public static final class Builder {

        //these must be set
        private long columnId = 0;
        private String metaHash = null;
        private long size = 0;
        private long senderId = 0;
        private Long processed = null;
        private long dateAdded = 0;
        private String path = null;
        private String displayName = null;
        private String artistName = null;
        private String albumName = null;
        private long duration = 0;
        private Type type = null;

        //optional to set
        private boolean isLiked = false;


        public Builder setMetaHash(String metaHash) {
            this.metaHash = metaHash;
            return this;
        }

        public Builder setSize(long size) {
            this.size = size;
            return this;
        }

        public Builder setSenderId(long senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder setProcessed(long processed) {
            this.processed = processed;
            return this;
        }

        public Builder setDateAdded(long dateAdded) {
            this.dateAdded = dateAdded;
            return this;
        }

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public Builder setDisplayName(String displayName) {
            this.displayName = displayName;
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

        public Builder setLiked(boolean liked) {
            isLiked = liked;
            return this;
        }

        public Builder setDuration(long duration) {
            this.duration = duration;
            return this;
        }

        public Builder setType(Type type) {
            this.type = type;
            return this;
        }

        public Builder setColumnId(long columnId) {
            this.columnId = columnId;
            return this;
        }

        public MusicData build() {

            if (columnId == 0 || size == 0 || senderId == 0 || processed == null || dateAdded == 0 ||
                    duration == 0 || type == null ||
                    TextUtils.isEmpty(metaHash) || TextUtils.isEmpty(path) || TextUtils.isEmpty(displayName) ||
                    artistName == null || albumName == null)
                throw new IllegalArgumentException("Required parameters not found");

            return new MusicData(columnId,
                    metaHash,
                    size,
                    senderId,
                    processed,
                    dateAdded,
                    path,
                    displayName,
                    artistName,
                    albumName,
                    isLiked,
                    duration,
                    type);
        }
    }
}