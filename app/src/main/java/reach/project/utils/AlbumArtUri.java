package reach.project.utils;

import android.net.Uri;
import android.text.TextUtils;

import com.google.common.base.Optional;
import com.google.common.primitives.Booleans;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dexter on 13/10/15.
 */
public enum AlbumArtUri {
    ;

    private static final String baseURLSmall = "http://52.74.53.245:8080/getImage/small?";
    private static final String baseURLLarge = "http://52.74.53.245:8080/getImage/large?";

    private static final String userImageBase = "https://able-door-616.appspot.com/userImageEndpoint?";

    private static final Map<Integer, Uri> SIMPLE_CACHE = new HashMap<>(1000);

    private static final StringBuilder buffer = new StringBuilder(50);

    public synchronized static Uri getUserImageUri(long hostId,
                                                   String requestedImage,
                                                   String requestedFormat,
                                                   boolean requestCircularCrop,
                                                   int requestedWidth,
                                                   int requestedHeight) {

        int hash = 17;
        hash = hash * 23 + Longs.hashCode(hostId);
        hash = hash * 23 + requestedImage.hashCode();
        hash = hash * 23 + requestedFormat.hashCode();
        hash = hash * 23 + Booleans.hashCode(requestCircularCrop);
        hash = hash * 23 + Ints.hashCode(requestedWidth);
        hash = hash * 23 + Ints.hashCode(requestedHeight);

        final int key = hash;

        Uri value = SIMPLE_CACHE.get(key);
        if (value != null)
            return value;

        buffer.setLength(0);
        buffer.append(userImageBase)
                .append("hostIdString=").append(hostId).append("&")
                .append("requestedImage=").append(requestedImage).append("&")
                .append("requestedFormat=").append(requestedFormat).append("&")
                .append("requestCircularCrop=").append(requestCircularCrop).append("&")
                .append("requestedWidth=").append(requestedWidth).append("&")
                .append("requestedHeight=").append(requestedHeight);

        final String toParse = buffer.toString();
//        Log.i("Ayush", toParse);

        value = Uri.parse(toParse);
        SIMPLE_CACHE.put(key, value);
        return value;
    }

    public synchronized static Optional<Uri> getUri(String album, String artist, String song, boolean large) {

        int hash = 17;
        hash = hash * 23 + (TextUtils.isEmpty(album) ? "" : album).hashCode();
        hash = hash * 23 + (TextUtils.isEmpty(artist) ? "" : artist).hashCode();
        hash = hash * 23 + (TextUtils.isEmpty(song) ? "" : song).hashCode();
        hash = hash * 23 + Booleans.hashCode(large);

        final int key = hash;

        Uri value = SIMPLE_CACHE.get(key);
        if (value != null)
            return Optional.of(value);

        //else calculate Uri

        buffer.setLength(0);
        buffer.append(large ? baseURLLarge : baseURLSmall);

        try {

            if (!TextUtils.isEmpty(album)) {

                buffer.append("album=").append(URLEncoder.encode(album, "UTF-8"));
                if (!TextUtils.isEmpty(artist))
                    buffer.append("&artist=").append(URLEncoder.encode(artist, "UTF-8"));
                if (!TextUtils.isEmpty(song))
                    buffer.append("&song=").append(URLEncoder.encode(song, "UTF-8"));
            } else if (!TextUtils.isEmpty(artist)) {

                buffer.append("artist=").append(URLEncoder.encode(artist, "UTF-8"));
                if (!TextUtils.isEmpty(song))
                    buffer.append("&song=").append(URLEncoder.encode(song, "UTF-8"));
            } else if (!TextUtils.isEmpty(song))
                buffer.append("song=").append(URLEncoder.encode(song, "UTF-8"));
            else
                return Optional.absent();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); //throw as is
        }

        final String toParse = buffer.toString();
//        Log.i("Ayush", toParse);

        if (TextUtils.isEmpty(toParse))
            return Optional.absent();

        value = Uri.parse(toParse);
        SIMPLE_CACHE.put(key, value);
        return Optional.of(value);
    }
}
