package reach.project.musicbrainz;

import android.text.TextUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Optional;

import reach.project.utils.MiscUtils;

public enum CoverArt {
    ;


    public static String getJson(JsonNode jsonNode) throws JsonProcessingException {
        return Tools.nodeToJsonPretty(jsonNode);
    }

    private static JsonNode getFirstImageURL(JsonNode jsonNode) {
        return jsonNode.get("images").get(0);
    }

    /**
     * Fetches the coverartarchive.org image url for the release.
     *
     * @return image url
     */
    public static String getImageURL(JsonNode jsonNode) {
        return getFirstImageURL(jsonNode).get("image").asText();
    }

    private static JsonNode getThumbnails(JsonNode jsonNode) {
        return getFirstImageURL(jsonNode).get("thumbnails");
    }

    /**
     * Fetches the coverartarchive.org large thumbnail for the release.
     *
     * @return large thumbnail
     */
    public static String getLargeThumbnailURL(JsonNode jsonNode) {
        return getThumbnails(jsonNode).get("large").asText();
    }

    /**
     * Fetches the coverartarchive.org small thumbnail for the release.
     *
     * @return small thumbnail
     */
    public static String getSmallThumbnailURL(JsonNode jsonNode) {
        return getThumbnails(jsonNode).get("small").asText();
    }

    /**
     * Fetch the jsonNode from mbid
     * @param releaseGroupMBID the mbid
     * @return jsonNode
     */
    private static Optional<JsonNode> fetchCoverImagesFromMBID(String releaseGroupMBID) {

        return MiscUtils.autoRetry(() -> {

            final String res = Tools.httpGet("https://coverartarchive.org/release-group/" + releaseGroupMBID);
            if (TextUtils.isEmpty(res))
                return null;
            return Tools.jsonToNode(res);
        }, Optional.of(input -> input == null));
    }
}
