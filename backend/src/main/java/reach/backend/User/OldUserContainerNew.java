package reach.backend.User;

import java.util.Date;

/**
 * Created by dexter on 12/07/15.
 */
public class OldUserContainerNew {

    private final String name;
    private final String imageId;
    private final String promoCode;
    private final long serverId;

    private final String coverPic;
    private final String statusSong;
    private final String emailId;
    private final Date birthday;


    public long getServerId() {
        return serverId;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public String getImageId() {
        return imageId;
    }

    public String getName() {
        return name;
    }

    public OldUserContainerNew(String name, String promoCode, String imageId, long serverId,
                               String coverPic, String statusSong, String emailId, Date birthday) {
        this.name = name;
        this.promoCode = promoCode;
        this.imageId = imageId;
        this.serverId = serverId;

        this.coverPic = coverPic;
        this.statusSong = statusSong;
        this.emailId = emailId;
        this.birthday = birthday;
    }

    public String getCoverPic() {
        return coverPic;
    }

    public String getStatusSong() {
        return statusSong;
    }

    public String getEmailId() {
        return emailId;
    }

    public Date getBirthday() {
        return birthday;
    }
}