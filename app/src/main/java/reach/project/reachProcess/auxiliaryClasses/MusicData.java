package reach.project.reachProcess.auxiliaryClasses;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Dexter on 15-05-2015.
 */

public class MusicData implements Parcelable {

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
    private boolean isLiked; //toggled by User
    private long processed; //changes
    private int currentPosition; //changes
    private int primaryProgress; //changes

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

    protected MusicData(Parcel in) {
        displayName = in.readString();
        path = in.readString();
        artistName = in.readString();
        id = in.readLong();
        length = in.readLong();
        senderId = in.readLong();
        type = in.readByte();
        duration = in.readLong();
        isLiked = in.readByte() != 0;
        processed = in.readLong();
        currentPosition = in.readInt();
        primaryProgress = in.readInt();
        secondaryProgress = in.readInt();
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
        dest.writeLong(id);
        dest.writeLong(length);
        dest.writeLong(senderId);
        dest.writeByte(type);
        dest.writeLong(duration);
        dest.writeByte((byte) (isLiked ? 1 : 0));
        dest.writeLong(processed);
        dest.writeInt(currentPosition);
        dest.writeInt(primaryProgress);
        dest.writeInt(secondaryProgress);
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
}