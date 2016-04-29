package reach.project.coreViews.saved_songs;

/**
 * Created by gauravsobti on 20/04/16.
 */
public class SavedSongsDataModel {
    public static class Builder {

        private String youtube_id;
        private long date_added;
        private String sender_name;
        private long sender_id;
        private int type = -1; //0 is for saved songs and 1 is for history, can later also be used for making playlists
        private String song_name;
        private String artist_album_name;
        private String display_name;

        public Builder withYoutube_Id(String youtube_id) {
            this.youtube_id = youtube_id;
            return this;
        }

        public Builder withDate_Added(long date_added) {
            this.date_added = date_added;
            return this;
        }

        public Builder withSenderName(String sender_name) {
            this.sender_name = sender_name;
            return this;
        }

        public Builder withSenderId(long sender_id) {
            this.sender_id = sender_id;
            return this;
        }

        public Builder withType(int type) {
            this.type = type;
            return this;
        }

        public Builder withSongName(String song_name) {
            this.song_name = song_name;
            return this;
        }

        public Builder withArtistAlbumName(String artist_album_name) {
            this.artist_album_name = artist_album_name;
            return this;
        }

        public Builder withDisplayName(String display_name) {
            this.display_name = display_name;
            return this;
        }

        public SavedSongsDataModel build() {

            if (youtube_id == null
                    || date_added == 0L
                    || type == 0) {

                throw new IllegalStateException("Can not create object, not enough data");
            }

            return new SavedSongsDataModel(youtube_id, date_added, sender_name,
                    sender_id, type, song_name, artist_album_name, display_name);


        }

    }


    private final String youtube_id;
    private final long date_added;
    private final String sender_name;
    private final long sender_id;
    private final int type; //1 is for saved songs and 2 is for history
    private final String song_name;
    private final String artist_album_name;
    private final String display_name;

    public SavedSongsDataModel(String youtube_id, long date_added, String sender_name, long sender_id, int type, String song_name, String artist_album_name, String display_name) {
        this.youtube_id = youtube_id;
        this.date_added = date_added;
        this.sender_name = sender_name;
        this.sender_id = sender_id;
        this.type = type;
        this.song_name = song_name;
        this.artist_album_name = artist_album_name;
        this.display_name = display_name;
    }

    public String getYoutube_id() {
        return youtube_id;
    }

    public long getDate_added() {
        return date_added;
    }

    public String getSender_name() {
        return sender_name;
    }

    public long getSender_id() {
        return sender_id;
    }

    public int getType() {
        return type;
    }

    public String getSong_name() {
        return song_name;
    }

    public String getArtist_album_name() {
        return artist_album_name;
    }

    public String getDisplay_name() {
        return display_name;
    }
}
