package reach.backend.ObjectWrappers;

import reach.backend.Entities.User;

/**
 * Created by dexter on 28/1/15.
 */
public class MyUser {

    private final long id;
    private final String phoneNumber;
    private final String userName;
    private final String statusSong;
    private final String imageId;
    private final String genres;

    private final long lastSeen;

    public boolean isOnline() {
        return online;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public String getGenres() {
        return genres;
    }

    public String getImageId() {
        return imageId;
    }

    public String getStatusSong() {
        return statusSong;
    }

    public String getUserName() {
        return userName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public long getId() {
        return id;
    }

    private final boolean online;

    public MyUser(long id, String phoneNumber, String userName, String statusSong, String imageId, String genres, long lastSeen, boolean online) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.userName = userName;
        this.statusSong = statusSong;
        this.imageId = imageId;
        this.genres = genres;
        this.lastSeen = lastSeen;
        this.online = online;
    }

    public MyUser(User user, long lastSeen, boolean online) {

        this.id = user.getId();
        this.phoneNumber = user.getPhoneNumber();
        this.userName = user.getUserName();
        this.statusSong = user.getStatusSong();
        this.imageId = user.getImageId();
        this.genres = user.getGenres();
        this.lastSeen = lastSeen;
        this.online = online;
    }
}
