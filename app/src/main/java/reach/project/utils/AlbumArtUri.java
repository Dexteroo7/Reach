package reach.project.utils;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.google.common.base.Optional;

import java.net.URLEncoder;
import java.util.Arrays;

/**
 * Created by dexter on 13/10/15.
 */
public enum AlbumArtUri {
    ;

    private static final String baseURL = "http://52.74.53.245:8080/getImage/small?";
    private static final SparseArray<Uri> simpleCache = new SparseArray<>(500);
    private static final StringBuffer buffer = new StringBuffer(50);

    public synchronized static Optional<Uri> getUri(String album, String artist, String song) {

        final int key = Arrays.hashCode(new String[]{
                TextUtils.isEmpty(album) ? "" : album,
                TextUtils.isEmpty(artist) ? "" : artist,
                TextUtils.isEmpty(song) ? "" : song
        });

        Uri value = simpleCache.get(key, null);
        if (value != null)
            return Optional.of(value);

        //else calculate Uri

        buffer.setLength(0);
        buffer.append(baseURL);

        if (!TextUtils.isEmpty(album)) {

            buffer.append("album=").append(URLEncoder.encode(album));
            if (!TextUtils.isEmpty(artist))
                buffer.append("&artist=").append(URLEncoder.encode(artist));
            if (!TextUtils.isEmpty(song))
                buffer.append("&song=").append(URLEncoder.encode(song));
        } else if (!TextUtils.isEmpty(artist)) {

            buffer.append("artist=").append(URLEncoder.encode(artist));
            if (!TextUtils.isEmpty(song))
                buffer.append("&song=").append(URLEncoder.encode(song));
        } else if (!TextUtils.isEmpty(song))
            buffer.append("song=").append(URLEncoder.encode(song));
        else
            return Optional.absent();

        final String toParse = buffer.toString();
        Log.i("Ayush", toParse);

        if (TextUtils.isEmpty(toParse))
            return Optional.absent();

        value = Uri.parse(toParse);
        simpleCache.append(key, value);
        return Optional.of(value);
    }
}
