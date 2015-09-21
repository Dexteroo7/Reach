package reach.project.musicbrainz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public enum Tools {
    ;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String USER_AGENT = System.getProperty("http.agent");

    public static String httpGet(String url) {

        final StringBuilder builder = new StringBuilder();
//        Log.i("Ayush", "Hitting with URL " + url);

        try {
            final URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", USER_AGENT);

            String inputLine;
            final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((inputLine = reader.readLine()) != null)
                builder.append("\n").append(inputLine);

            reader.close();
        } catch (IOException ignored) {
        }

        return builder.toString();
    }

    public static String replaceWhiteSpace(String s) {
        return s.replaceAll("\\s+", "%20");
    }

    public static String surroundWithQuotes(String toSurround) throws UnsupportedEncodingException {
//        toSurround = toSurround.replaceAll("\"", "");
//        return toSurround.replaceAll("\"", ""); // remove all quotes in between
        return URLEncoder.encode("\"" + toSurround + "\"", "UTF-8");
    }

    public static JsonNode jsonToNode(String json) throws IOException {
        return MAPPER.readTree(json);
    }

    public static String nodeToJson(ObjectNode a) throws JsonProcessingException {
        return MAPPER.writeValueAsString(a);
    }

    public static String nodeToJsonPretty(JsonNode node) throws JsonProcessingException {
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }

//    public static void printId3v1Tags(Mp3File mp3file) {
//
//        if (mp3file.hasId3v1Tag()) {
//            ID3v1 id3v1Tag = mp3file.getId3v1Tag();
//            System.out.println("Track: " + id3v1Tag.getTrack());
//            System.out.println("Artist: " + id3v1Tag.getArtist());
//            System.out.println("Title: " + id3v1Tag.getTitle());
//            System.out.println("Album: " + id3v1Tag.getAlbum());
//            System.out.println("Year: " + id3v1Tag.getYear());
//            System.out.println("Genre: " + id3v1Tag.getGenre() + " (" + id3v1Tag.getGenreDescription() + ")");
//            System.out.println("Comment: " + id3v1Tag.getComment());
//        }
//    }
//
//    public static void printId3v2Tags(Mp3File mp3file) {
//
//        if (mp3file.hasId3v2Tag()) {
//            ID3v2 id3v2Tag = mp3file.getId3v2Tag();
//            System.out.println("Track: " + id3v2Tag.getTrack());
//            System.out.println("Artist: " + id3v2Tag.getArtist());
//            System.out.println("Title: " + id3v2Tag.getTitle());
//            System.out.println("Album: " + id3v2Tag.getAlbum());
//            System.out.println("Year: " + id3v2Tag.getYear());
//            System.out.println("Date: " + id3v2Tag.getDate());
//            System.out.println("Genre: " + id3v2Tag.getGenre() + " (" + id3v2Tag.getGenreDescription() + ")");
//            System.out.println("Comment: " + id3v2Tag.getComment());
//            System.out.println("Composer: " + id3v2Tag.getComposer());
//            System.out.println("Publisher: " + id3v2Tag.getPublisher());
//            System.out.println("Original artist: " + id3v2Tag.getOriginalArtist());
//            System.out.println("Album artist: " + id3v2Tag.getAlbumArtist());
//            System.out.println("Copyright: " + id3v2Tag.getCopyright());
//            System.out.println("URL: " + id3v2Tag.getUrl());
//            System.out.println("Encoder: " + id3v2Tag.getEncoder());
//            byte[] albumImageData = id3v2Tag.getAlbumImage();
//            if (albumImageData != null) {
//                System.out.println("Have album image data, length: " + albumImageData.length + " bytes");
//                System.out.println("Album image mime type: " + id3v2Tag.getAlbumImageMimeType());
//            }
//
//            System.out.println("Length(ms): " + mp3file.getLengthInMilliseconds());
//        }
//    }
}
