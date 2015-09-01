//package reach.project.musicbrainz;
//
//import java.util.NoSuchElementException;
//
//import org.codehaus.jackson.JsonNode;
//
///** This just performs a lookup of the artist by mbid to get pictures and links of them
// *
// * @author tyler
// *
// */
//public class Artist {
//
//	private JsonNode json;
//
//	public String getJson() {
//		return Tools.nodeToJsonPretty(json);
//	}
//
//	/**
//	 * Fetches the coverart for a given song from coverartarchive.org
//	 * @param song
//	 * @return
//	 */
//	public static Artist fetchArtist(String mbid) {
//		return new Artist(mbid);
//	}
//
//	private Artist(String mbid) {
//		json = fetchArtistFromMBID(mbid);
//
//		if (json == null) {
//			throw new NoSuchElementException("No Artist found for mbid: " + mbid);
//		}
//	}
//
//	/**
//	 * Here's a sample query for pearl jam:
//	 * https://musicbrainz.org/ws/2/artist/83b9cbe7-9857-49e2-ab8e-b57b01038103?inc=url-rels&fmt=json
//	 * @param mbid
//	 * @return
//	 */
//	private static JsonNode fetchArtistFromMBID(String mbid) {
//
//		String query = "https://musicbrainz.org/ws/2/artist/" + mbid+ "?inc=url-rels&fmt=json";
//
//		String res = Tools.httpGet(query);
//
//		if (res.equals("")) {
//			// Wait some time before retrying
//			try {
//				Thread.sleep(1100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//
//			return fetchArtistFromMBID(query);
//		}
//
//
//		JsonNode jsonNode = Tools.jsonToNode(res);
//
//		return jsonNode;
//	}
//
//	/** For some reason I found many type=discography, so I'd just select the first found
//	 *
//	 * @return
//	 */
//	private JsonNode findFirstInRelationsArray(String typeSearch) {
//		JsonNode relationsArray = json.get("relations");
//
//		for (int i = 0;;i++) {
//			JsonNode cNode = relationsArray.get(i);
//			if (cNode == null) {
//				throw new NoSuchElementException("The type " + typeSearch + " doesn't exist");
//			}
//			String cType = cNode.get("type").asText();
//			if (cType.equals(typeSearch)) {
//				return cNode;
//			}
//
//		}
//
//	}
//
//	public String getLink(String typeSearch) {
//		try {
//		String link = findFirstInRelationsArray(typeSearch).get("url").get("resource").asText();
//
//		return link;
//
//		} catch(NoSuchElementException e) {
//			e.printStackTrace();
//			return null;
//		}
//	}
//
//	/**
//	 * Fetches the coverartarchive.org image url for the release.
//	 * @return image url
//	 */
//	public String getWikipedia() {
//		return getLink("wikipedia");
//	}
//
//	public String getLyrics() {
//		return getLink("lyrics");
//	}
//
//	public String getDiscography() {
//		return getLink("discography");
//	}
//
//	public String getAllMusic() {
//		return getLink("allmusic");
//	}
//
//	public String getIMDB() {
//		return getLink("IMDb");
//	}
//
//	public String getLastFM() {
//		return getLink("last.fm");
//	}
//
//	public String getOfficialHomepage() {
//		return getLink("official homepage");
//	}
//
//	public String getFanPage() {
//		return getLink("fanpage");
//	}
//
//	public String getWikiData() {
//		return getLink("wikidata");
//	}
//
//	public String getYoutube() {
//		return getLink("youtube");
//	}
//
//	public String getImage() {
//		return getLink("image");
//	}
//
//	public String getSoundCloud() {
//		return getLink("soundcloud");
//	}
//
//	public String getSocialNetwork() {
//		return getLink("social network");
//	}
//
//
//}
