//package reach.project.musicbrainz;
//
//import java.util.NoSuchElementException;
//
//import org.codehaus.jackson.JsonNode;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * This just performs a lookup of the release group by mbid to get links
// *
// * @author tyler
// */
//public class ReleaseGroup {
//
//    private JsonNode json;
//
//    public String getJson() {
//        return Tools.nodeToJsonPretty(json);
//    }
//
//    /**
//     * Fetches the coverart for a given song from coverartarchive.org
//     *
//     * @param song
//     * @return
//     */
//    public static ReleaseGroup fetchReleaseGroup(String mbid) {
//        return new ReleaseGroup(mbid);
//    }
//
//    private ReleaseGroup(String mbid) {
//        json = fetchReleaseGroupFromMBID(mbid);
//
//        if (json == null) {
//            throw new NoSuchElementException("No Release group found for mbid: " + mbid);
//        }
//    }
//
//    /**
//     * Here's a sample query for pearl jam:
//     * https://musicbrainz.org/ws/2/release-group/7c4cab8d-dead-3870-b501-93c90fd0a580?inc=url-rels&fmt=json
//     *
//     * @param mbid
//     * @return
//     */
//    private static JsonNode fetchReleaseGroupFromMBID(String mbid) {
//
//        String query = "https://musicbrainz.org/ws/2/release-group/" + mbid + "?inc=url-rels&fmt=json";
//
//        String res = Tools.httpGet(query);
//
//        if (res.equals("")) {
//            // Wait some time before retrying
//            try {
//
//                Thread.sleep(1100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            return fetchReleaseGroupFromMBID(query);
//        }
//
//
//        JsonNode jsonNode = Tools.jsonToNode(res);
//
//        return jsonNode;
//    }
//
//    /**
//     * Gets the title of the release group
//     *
//     * @return
//     */
//    public String getTitle() {
//        return json.get("title").asText();
//    }
//
//
//    public String getYear() {
//        return json.get("first-release-date").asText().split("-")[0];
//    }
//
//    /**
//     * For some reason I found many type=discography, so I'd just select the first found
//     *
//     * @return
//     */
//    private JsonNode findFirstInRelationsArray(String typeSearch) {
//        JsonNode relationsArray = json.get("relations");
//
//        for (int i = 0; ; i++) {
//            JsonNode cNode = relationsArray.get(i);
//            if (cNode == null) {
//                throw new NoSuchElementException("The type " + typeSearch + " doesn't exist");
//            }
//            String cType = cNode.get("type").asText();
//            if (cType.equals(typeSearch)) {
//                return cNode;
//            }
//
//        }
//
//    }
//
//    public String getLink(String typeSearch) {
//        try {
//            String link = findFirstInRelationsArray(typeSearch).get("url").get("resource").asText();
//
//            return link;
//
//        } catch (NoSuchElementException e) {
//            log.error(e.getMessage());
//            return null;
//        }
//
//
//    }
//
//    /**
//     * Fetches the coverartarchive.org image url for the release.
//     *
//     * @return image url
//     */
//    public String getWikipedia() {
//        return getLink("wikipedia");
//    }
//
//    public String getLyrics() {
//        return getLink("lyrics");
//    }
//
//    public String getDiscography() {
//        return getLink("discography");
//    }
//
//    public String getAllMusic() {
//        return getLink("allmusic");
//    }
//
//    public String getIMDB() {
//        return getLink("IMDb");
//    }
//
//    public String getLastFM() {
//        return getLink("last.fm");
//    }
//
//    public String getOfficialHomepage() {
//        return getLink("official homepage");
//    }
//
//    public String getFanPage() {
//        return getLink("fanpage");
//    }
//
//    public String getWikiData() {
//        return getLink("wikidata");
//    }
//
//    public String getYoutube() {
//        return getLink("youtube");
//    }
//
//    public String getImage() {
//        return getLink("image");
//    }
//
//    public String getSoundCloud() {
//        return getLink("soundcloud");
//    }
//
//    public String getSocialNetwork() {
//        return getLink("social network");
//    }
//}