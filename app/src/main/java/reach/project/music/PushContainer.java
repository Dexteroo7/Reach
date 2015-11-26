package reach.project.music;

/**
 * Created by Dexter on 11-04-2015.
 */
public final class PushContainer {

    private final long receiverId, senderId;
    private final String songData; //transferSongs container base64 compressed
    private final String firstSongName;
    private final short songCount;
    private final String receiverName;
    private final String userName;
    private final String userImage;
    private final String networkType;
    private String customMessage;

    public PushContainer(long receiverId, long senderId, String songData, String userName, String receiverName,
                         short songCount, String userImage, String firstSongName, String networkType) {
        this.receiverId = receiverId;
        this.senderId = senderId;
        this.songData = songData;
        this.userName = userName;
        this.receiverName = receiverName;
        this.networkType = networkType;
        this.songCount = songCount;
        this.userImage = userImage;
        this.firstSongName = firstSongName;
    }

    public String getCustomMessage() {
        return customMessage;
    }

    public void setCustomMessage(String customMessage) {
        this.customMessage = customMessage;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getNetworkType() {
        return networkType;
    }

    public String getFirstSongName() {
        return firstSongName;
    }

    public String getUserImage() {
        return userImage;
    }

    public short getSongCount() {
        return songCount;
    }

    public String getUserName() {
        return userName;
    }

    public String getSongData() {
        return songData;
    }

    public long getSenderId() {
        return senderId;
    }

    public long getReceiverId() {
        return receiverId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PushContainer)) return false;

        PushContainer that = (PushContainer) o;

        if (getReceiverId() != that.getReceiverId()) return false;
        if (getSenderId() != that.getSenderId()) return false;
        if (getSongCount() != that.getSongCount()) return false;
        if (getUserName() != null ? !getUserName().equals(that.getUserName()) : that.getUserName() != null)
            return false;
        if (getCustomMessage() != null ? !getCustomMessage().equals(that.getCustomMessage()) : that.getCustomMessage() != null)
            return false;
        if (getUserImage() != null ? !getUserImage().equals(that.getUserImage()) : that.getUserImage() != null)
            return false;
        return !(getFirstSongName() != null ? !getFirstSongName().equals(that.getFirstSongName()) : that.getFirstSongName() != null);

    }

    @Override
    public int hashCode() {
        int result = (int) (getReceiverId() ^ (getReceiverId() >>> 32));
        result = 31 * result + (int) (getSenderId() ^ (getSenderId() >>> 32));
        result = 31 * result + (getUserName() != null ? getUserName().hashCode() : 0);
        result = 31 * result + (getCustomMessage() != null ? getCustomMessage().hashCode() : 0);
        result = 31 * result + (getUserImage() != null ? getUserImage().hashCode() : 0);
        result = 31 * result + (getFirstSongName() != null ? getFirstSongName().hashCode() : 0);
        result = 31 * result + (int) getSongCount();
        return result;
    }
}
