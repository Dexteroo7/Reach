package reach.project.utils.auxiliaryClasses;

/**
 * Created by dexter on 19/07/15.
 */
public class ReachFriend {

    private final long serverId;
    private final String phoneNumber;
    private final String userName;
    private final String imageId;
    private final short networkType;
    private final short status;
    private final int numberOfSongs;

    private final String statusSong;
    private final byte [] genres;
    private final int hash;
    private final long lastSeen;

    public int getHash() {
        return hash;
    }

    public int getNumberOfSongs() {
        return numberOfSongs;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public short getStatus() {
        return status;
    }

    public short getNetworkType() {
        return networkType;
    }

    public byte [] getGenres() {
        return genres;
    }

    public String getStatusSong() {
        return statusSong;
    }

    public String getImageId() {
        return imageId;
    }

    public String getUserName() {
        return userName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public long getServerId() {
        return serverId;
    }

    public ReachFriend(long serverId,
                       String phoneNumber,
                       String userName,
                       byte [] genres,
                       String imageId,
                       String statusSong,
                       short networkType,
                       int numberOfSongs,
                       long lastSeen,
                       short status,
                       int hash) {

        this.serverId = serverId;
        this.phoneNumber = phoneNumber;
        this.userName = userName;
        this.imageId = imageId;
        this.statusSong = statusSong;
        this.genres = genres;
        this.networkType = networkType;
        this.status = status;
        this.lastSeen = lastSeen;
        this.numberOfSongs = numberOfSongs;
        this.hash = hash;
    }

//    public static List<String> getFromBytes(byte [] array) {
//
//        final ByteArrayInputStream inputStream = new ByteArrayInputStream(array);
//        GZIPInputStream stream = null;
//        try {
//            stream = new GZIPInputStream(inputStream);
//            return IOUtils.deserialize(stream);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            MiscUtils.closeQuietly(inputStream, stream);
//        }
//
//        return null;
//    }

//    public static byte [] getFromList(List<String> genres) {
//
//        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        GZIPOutputStream outputStream = null;
//        try {
//            outputStream = new GZIPOutputStream(stream);
//            outputStream.write(IOUtils.serialize(genres));
//            return stream.toByteArray();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            MiscUtils.closeQuietly(stream, outputStream);
//        }
//
//        return null;
//    }
}
