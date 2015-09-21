package reach.project.musicbrainz;

import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Optional;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import reach.project.utils.MiscUtils;

/**
 * Looks up the musicbrainz recording from a given music file, using 6 pieces of info:<br>
 * track title<br>
 * track #<br>
 * artist name<br>
 * album name<br>
 * album year<br>
 * song duration<br>
 */
public enum SongBrainz {
    ;

    private static final int DURATION_WINDOW_MS = 25000;

    /**
     * Give the prettified json response from musicbrainz
     *
     * @return json
     */
    public static String toJson(JsonNode jsonNode) throws JsonProcessingException {
        return Tools.nodeToJsonPretty(jsonNode);
    }

    private static JsonNode getFirstRecording(JsonNode jsonNode) {
        return jsonNode.get("recordings").get(0);
    }

    /**
     * Fetches the musicbrainz MBID for the recording.
     *
     * @return musicbrainz-MBID
     */
    public static String getRecordingMBID(JsonNode jsonNode) {
        return getFirstRecording(jsonNode.get("id")).asText().toLowerCase();
    }

    /**
     * Fetches the title for the recording.
     *
     * @return title
     */
    public static String getRecording(JsonNode jsonNode) {
        return getFirstRecording(jsonNode).get("title").asText();
    }

    private static JsonNode getFirstRelease(JsonNode jsonNode) {
        return getFirstRecording(jsonNode).get("releases").get(0);
    }

    /**
     * Fetches the musicbrainz MBID for the artist.
     *
     * @return musicbrainz-MBID
     */
    public static String getArtistMBID(JsonNode jsonNode) {
        return getFirstArtistCredit(jsonNode).get("id").asText().toLowerCase();
    }

    /**
     * Fetches the name of the artist.
     *
     * @return name
     */
    public static String getArtist(JsonNode jsonNode) {
        return getFirstArtistCredit(jsonNode).get("name").asText();
    }

    /**
     * Fetches the musicbrainz MBID for the release.
     *
     * @return musicbrainz-MBID
     */
    public static String getReleaseMBID(JsonNode jsonNode) {
        return getFirstRelease(jsonNode).get("id").asText().toLowerCase();
    }

    /**
     * Fetches the title for the release.
     *
     * @return title
     */
    public static String getRelease(JsonNode jsonNode) {
        return getFirstRelease(jsonNode).get("title").asText();
    }

    private static JsonNode getFirstReleaseGroup(JsonNode jsonNode) {
        return getFirstRecording(jsonNode).get("releases").get(0).get("release-group");
    }

    /**
     * Fetches the musicbrainz MBID for the release group.
     *
     * @return musicbrainz-MBID
     */
    public static String getReleaseGroupMBID(JsonNode jsonNode) {
        return getFirstReleaseGroup(jsonNode).get("id").asText().toLowerCase();
    }

    /**
     * Fetches all the release groups associated with this song
     *
     * @return release groups associated with this song
     */
    public static Set<ReleaseGroupInfo> getReleaseGroupInfos(JsonNode jsonNode) {

        final Set<ReleaseGroupInfo> releaseGroupInfos = new LinkedHashSet<>();
        final Set<String> releaseGroupMBIDs = new LinkedHashSet<>(); // for uniqueness
        final Set<String> secondaryTypes = new LinkedHashSet<>();

        final JsonNode releases = getFirstRecording(jsonNode).get("releases");

        JsonNode node;
        int index = 0;

        while (releases.has(index)) {

            node = releases.get(index);

            final String cReleaseGroupMBID = node.get("release-group").get("id").asText();
            final int discNo = node.get("media").get(0).get("position").asInt();
            final String trackNoStr = node.get("media").get(0).get("track").get(0).get("number").asText();
            final String primaryType = node.get("release-group").get("primary-type") == null ? null : node.get("release-group").get("primary-type").asText();
            final JsonNode secondaryTypesJson = node.get("release-group").get("secondary-types");

            secondaryTypes.clear();
            if (secondaryTypesJson != null) {

                int index2 = 0;
                while (secondaryTypesJson.has(index2))
                    secondaryTypes.add(secondaryTypesJson.get(index2++).asText());
            }

            // This was necessary because some track numbers had letters in them, IE A2
            int trackNo = 0;
            try {
                trackNo = Integer.valueOf(trackNoStr.replaceAll("[^\\d.]", ""));
            } catch (NumberFormatException e) {
                Log.e("Ayush", "Track # was " + trackNoStr + " , so changed it to 0");
                e.printStackTrace();
            }

            // Only create and add if its a unique releaseGroupMBID
            if (!releaseGroupMBIDs.contains(cReleaseGroupMBID)) {

                ReleaseGroupInfo releaseGroupInfo = ReleaseGroupInfo.create(cReleaseGroupMBID, trackNo, discNo, primaryType, secondaryTypes);
                releaseGroupInfos.add(releaseGroupInfo);
                releaseGroupMBIDs.add(cReleaseGroupMBID);
            }

            index++;
        }

        return releaseGroupInfos;
    }

    public static class ReleaseGroupInfo {

        private String mbid, primaryType;
        private Set<String> secondaryTypes;
        private int trackNo, discNo;

        public static ReleaseGroupInfo create(
                String releaseGroupMBID, int trackNo, int discNo, String primaryType,
                Set<String> secondaryTypes) {
            return new ReleaseGroupInfo(releaseGroupMBID, trackNo, discNo, primaryType, secondaryTypes);
        }

        private ReleaseGroupInfo(String releaseGroupMBID, int trackNo, int discNo,
                                 String primaryType, Set<String> secondaryTypes) {
            this.mbid = releaseGroupMBID;
            this.trackNo = trackNo;
            this.discNo = discNo;
            this.primaryType = primaryType;
            this.secondaryTypes = secondaryTypes;
        }

        public String getMbid() {
            return mbid;
        }

        public int getTrackNo() {
            return trackNo;
        }

        public String getPrimaryType() {
            return primaryType;
        }

        public Set<String> getSecondaryTypes() {
            return secondaryTypes;
        }

        public int getDiscNo() {
            return discNo;
        }

        @Override
        public String toString() {
            return "mbid: " + mbid + " , track #: " + trackNo + " , disc #: " + discNo;
        }
    }

    private static JsonNode getFirstArtistCredit(JsonNode jsonNode) {
        return getFirstRecording(jsonNode).get("artist-credit").get(0).get("artist");
    }

    /**
     * Here's a sample musicBrainz query
     * search terms for recording:
     * recording(title)
     * artist
     * number(track#)
     * release(album)
     * date - yep, do the year
     * dur(duration in ms)
     * fmt=json
     * limit=1
     * http://musicbrainz.org/ws/2/recording/?query=recording:Closer+number:5+artist:Nine%Inch%Nails+dur:372666+release:The%Downward%Spiral&date:1994limit=10
     */
    public static class MusicBrainzRecordingQuery {

        private String recording; //title
        private String artist;
        private String release; //album

        public static class Builder {

            private String recording; //title
            private String artist;
            private String release; //album

            /**
             * The album name
             *
             * @param release album
             * @return builder
             */
            public Builder release(String release) {

                if (TextUtils.isEmpty(release))
                    throw new NoSuchElementException("The release is null");
                this.release = release;
                return this;
            }

            /**
             * The song title (Display Name)
             *
             * @param recording song title
             * @return builder
             */
            public Builder recording(String recording) {

                if (TextUtils.isEmpty(recording))
                    throw new NoSuchElementException("The recording is null");
                this.recording = recording;
                return this;
            }

            public Builder artist(String artist) {

                if (TextUtils.isEmpty(artist))
                    throw new NoSuchElementException("The artist is null");
                this.artist = artist;
                return this;
            }

            public MusicBrainzRecordingQuery build() {
                return new MusicBrainzRecordingQuery(this);
            }
        }

        private MusicBrainzRecordingQuery(Builder builder) {
            this.recording = builder.recording;
            this.artist = builder.artist;
            this.release = builder.release;
        }

        public String createQuery() throws UnsupportedEncodingException {


            final StringBuilder builder = new StringBuilder();
            builder.append("http://musicbrainz-555872579.us-east-1.elb.amazonaws.com:5000/ws/2/recording/?query=");

            //first of all try with release
            if (!TextUtils.isEmpty(release)) {

                builder.append("release:").append(Tools.surroundWithQuotes(release));
                if (!TextUtils.isEmpty(artist))
                    builder.append(" OR artist:").append(Tools.surroundWithQuotes(artist));
                if (!TextUtils.isEmpty(release))
                    builder.append(" OR recording:").append(Tools.surroundWithQuotes(recording));
            }

            //then try with artist
            else if (!TextUtils.isEmpty(artist)) {

                builder.append("artist:").append(Tools.surroundWithQuotes(artist));
                if (!TextUtils.isEmpty(artist))
                    builder.append(" OR recording:").append(Tools.surroundWithQuotes(recording));
            }

            //finally try with recording
            else if (!TextUtils.isEmpty(recording))
                builder.append("recording:").append(Tools.surroundWithQuotes(recording));

            builder.append("&limit=1");
            builder.append("&fmt=json");
//            return builder.toString();
            return Tools.replaceWhiteSpace(builder.toString());
        }
    }

    public static String createQuery(String title,
                                     String artist,
                                     String album) throws UnsupportedEncodingException {

        // Construct the query
        return new MusicBrainzRecordingQuery.Builder()
                .recording(title)
                .artist(artist)
                .release(album).build().createQuery();
    }

    public static Optional<JsonNode> fetchMBRecordingJSONFromQuery(String query) {

        return MiscUtils.autoRetry(() -> {

            final String res = Tools.httpGet(query);
            if (TextUtils.isEmpty(res))
                return null;
//            Log.i("Ayush", "HTTP GET RESULT " + res);
            return Tools.jsonToNode(res);
        }, Optional.absent());
    }
}