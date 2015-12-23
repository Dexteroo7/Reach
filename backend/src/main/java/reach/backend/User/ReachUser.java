package reach.backend.User;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.annotation.Unindex;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

/**
 * Created by dexter on 2/7/14.
 */
@Cache
@Entity
@Index
public class ReachUser {

    public static final int ONLINE_LIMIT = 90 * 1000; //90 seconds timeout

    @Id
    private Long id;
    private String phoneNumber = "hello_world";    //UUID
    private String deviceId = "hello_world";

    private int dirtyCheck = 0; //hashcode for checking dirty values

    private int numberOfSongs = 0;
    private int numberOfApps = 0;

    private String userName = "hello_world";       //Custom Username, can be changed
    private String imageId = "hello_world";  //User Profile Image
    private String coverPicId = "hello_world"; //Cover Pic

    private String statusSong = "hello_world"; //Status Song
    private String emailId = "hello_world";
    private Date birthday = new Date(0);

    private String gcmId = "hello_world"; //Gcm Cloud Message Id
    private String chatToken = "hello_world"; //firebase token for devikaChat
    private String promoCode = "hello_world"; //promo code of User

    private long megaBytesSent;
    private long megaBytesReceived;
    private long timeCreated;

    @Unindex
    private HashSet<Long> myReach;
    @Unindex
    private HashSet<Long> sentRequests;
    @Unindex
    private HashSet<Long> receivedRequests;

    //////////////////////////////////
    public int computeDirtyHash() {

        //faster probably
        return Arrays.hashCode(new Object[]{
                numberOfSongs,
                numberOfApps,

                userName == null ? "" : userName,
                imageId == null ? "" : imageId,
                coverPicId == null ? "" : coverPicId,
                statusSong == null ? "" : statusSong,

                gcmId == null ? "" : gcmId});
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

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public String getChatToken() {
        return chatToken;
    }

    public void setChatToken(String chatToken) {
        this.chatToken = chatToken;
    }

    public int getNumberOfApps() {
        return numberOfApps;
    }

    public void setNumberOfApps(int numberOfApps) {
        this.numberOfApps = numberOfApps;
    }

    public String getCoverPicId() {
        return coverPicId;
    }

    public void setCoverPicId(String coverPicId) {
        this.coverPicId = coverPicId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReachUser)) return false;

        ReachUser user = (ReachUser) o;

        if (dirtyCheck != user.dirtyCheck) return false;
        if (id != null ? !id.equals(user.id) : user.id != null) return false;
        if (phoneNumber != null ? !phoneNumber.equals(user.phoneNumber) : user.phoneNumber != null)
            return false;
        return !(deviceId != null ? !deviceId.equals(user.deviceId) : user.deviceId != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (phoneNumber != null ? phoneNumber.hashCode() : 0);
        result = 31 * result + (deviceId != null ? deviceId.hashCode() : 0);
        result = 31 * result + dirtyCheck;
        return result;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }
}
