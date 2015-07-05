package reach.backend.Entities;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;

import java.util.List;

import reach.backend.ObjectWrappers.ReachPlayList;
import reach.backend.ObjectWrappers.ReachSong;

/**
 * Created by dexter on 2/7/14.
 */
@Cache
@Entity
public class User {

    @Id private Long id;

    @Index private String phoneNumber = "hello_world";    //UUID
    @Index private String gcmId = "hello_world"; //Gcm Cloud Message Id
    @Index private String userName = "hello_world";       //Custom Username, can be changed

    @Unindex private String deviceId = "hello_world"; //secondary UUID, for tablets
    @Unindex private String friends = "hello_world";  //Either phoneNumber or deviceID
    @Unindex private String whoCanIAccess = "hello_world"; //Phone numbers
    @Unindex private String genres = "hello_world";    //Genres on phone

    @Unindex private String imageId = "hello_world";  //User Profile Image
    @Unindex private String statusSong = "hello_world"; //Status Song

    //heavy data
    @Unindex private List<ReachSong> mySongs;  //Songs
    @Unindex private List<ReachPlayList> myPlayLists;  //PlayLists

    @Unindex private long megaBytesSent;
    @Unindex private long megaBytesReceived;

    public long getMegaBytesSent() {
        return megaBytesSent;
    }

    public void setMegaBytesSent(long megaBytesSent) {
        this.megaBytesSent = megaBytesSent;
    }

    public long getMegaBytesReceived() {
        return megaBytesReceived;
    }

    public void setMegaBytesReceived(long megaBytesReceived) {
        this.megaBytesReceived = megaBytesReceived;
    }

    public List<ReachPlayList> getMyPlayLists() {
        return myPlayLists;
    }

    public void setMyPlayLists(List<ReachPlayList> myPlayLists) {
        this.myPlayLists = myPlayLists;
    }

    public List<ReachSong> getMySongs() {
        return mySongs;
    }

    public void setMySongs(List<ReachSong> mySongs) {
        this.mySongs = mySongs;
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

    public String getWhoCanIAccess() {
        return whoCanIAccess;
    }

    public void setWhoCanIAccess(String whoCanIAccess) {
        this.whoCanIAccess = whoCanIAccess;
    }

    public String getFriends() {
        return friends;
    }

    public void setFriends(String friends) {
        this.friends = friends;
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
}
