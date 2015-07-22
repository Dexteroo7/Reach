package reach.backend.User;

import com.google.appengine.repackaged.com.google.api.client.util.Charsets;
import com.google.appengine.repackaged.com.google.common.hash.Hashing;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.annotation.Unindex;

import java.util.HashSet;

/**
 * Created by dexter on 2/7/14.
 */
@Cache
@Entity
@Index
public class ReachUser {

    public static final int ONLINE_LIMIT = 60 * 1000; //30 seconds timeout

    @Id
    private Long id;
    private String phoneNumber = "hello_world";    //UUID
    private String deviceId = "hello_world";

    private int dirtyCheck = 0; //hashcode for checking dirty values

    private int numberOfSongs = 0;
    private String userName = "hello_world";       //Custom Username, can be changed
    private String imageId = "hello_world";  //User Profile Image
    private String gcmId = "hello_world"; //Gcm Cloud Message Id
    private String statusSong = "hello_world"; //Status Song

    private String promoCode = "hello_world"; //promo code of user
    private long megaBytesSent;
    private long megaBytesReceived;
    private long timeCreated;

    @Unindex
    private HashSet<Long> myReach;
    @Unindex
    private HashSet<Long> sentRequests;
    @Unindex
    private HashSet<Long> receivedRequests;

    //shit to remove
    private long splitterId = 0;
    private String genres = "hello_world";    //Genres
    @Unindex
    private HashSet<ReachSong> mySongs; //switch to compressed blob
    @Unindex
    private HashSet<ReachPlayList> myPlayLists; //switch to compressed blob

    //////////////////////////////////
    public int computeDirtyHash() {
        //basic hash function
        return Hashing.adler32().newHasher()
                .putInt(numberOfSongs)
                .putString(userName == null ? "" : userName, Charsets.UTF_8)
                .putString(imageId == null ? "" : imageId, Charsets.UTF_8)
                .putString(gcmId == null ? "" : gcmId, Charsets.UTF_8)
                .putString(statusSong == null ? "" : statusSong, Charsets.UTF_8)
                .hash().asInt();
    }

    public int getDirtyCheck() {
        return dirtyCheck;
    }

    public void setDirtyCheck(int dirtyCheck) {
        this.dirtyCheck = dirtyCheck;
    }

    public void setDirtyCheck() {
        this.dirtyCheck = computeDirtyHash();
    }

    public long getSplitterId() {
        return splitterId;
    }

    public void setSplitterId(long splitterId) {
        this.splitterId = splitterId;
    }

    public int getNumberOfSongs() {
        return numberOfSongs;
    }

    public void setNumberOfSongs(int numberOfSongs) {
        this.numberOfSongs = numberOfSongs;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public HashSet<Long> getReceivedRequests() {
        return receivedRequests;
    }

    public void setReceivedRequests(HashSet<Long> receivedRequests) {
        this.receivedRequests = receivedRequests;
    }

    public HashSet<Long> getSentRequests() {
        return sentRequests;
    }

    public void setSentRequests(HashSet<Long> sentRequests) {
        this.sentRequests = sentRequests;
    }

    public HashSet<Long> getMyReach() {
        return myReach;
    }

    public void setMyReach(HashSet<Long> myReach) {
        this.myReach = myReach;
    }

    public HashSet<ReachPlayList> getMyPlayLists() {
        return myPlayLists;
    }

    public void setMyPlayLists(HashSet<ReachPlayList> myPlayLists) {
        this.myPlayLists = myPlayLists;
    }

    public HashSet<ReachSong> getMySongs() {
        return mySongs;
    }

    public void setMySongs(HashSet<ReachSong> mySongs) {
        this.mySongs = mySongs;
    }

    public long getMegaBytesReceived() {
        return megaBytesReceived;
    }

    public void setMegaBytesReceived(long megaBytesReceived) {
        this.megaBytesReceived = megaBytesReceived;
    }

    public long getMegaBytesSent() {
        return megaBytesSent;
    }

    public void setMegaBytesSent(long megaBytesSent) {
        this.megaBytesSent = megaBytesSent;
    }

    public String getStatusSong() {
        return statusSong;
    }

    public void setStatusSong(String statusSong) {
        this.statusSong = statusSong;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getGcmId() {
        return gcmId;
    }

    public void setGcmId(String gcmId) {
        this.gcmId = gcmId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @OnSave
    void onSave() {
        setDirtyCheck();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReachUser reachUser = (ReachUser) o;

        return !(deviceId != null ? !deviceId.equals(reachUser.deviceId) : reachUser.deviceId != null) &&
                !(id != null ? !id.equals(reachUser.id) : reachUser.id != null);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (deviceId != null ? deviceId.hashCode() : 0);
        return result;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }
}
