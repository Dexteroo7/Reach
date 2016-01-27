package reach.project.utils;

import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public enum StringCompress {
    ;

    public static byte[] deCompressStringToBytes(String compressed) throws IOException {

        final byte [] decodedBytes = Base64.decode(compressed, Base64.DEFAULT);
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decodedBytes);
        final InputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);

        final byte[] buffer = new byte[8192];
        int len;

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while ((len = gzipInputStream.read(buffer)) > 0)
            byteArrayOutputStream.write(buffer, 0, len);

        final byte [] toReturn = byteArrayOutputStream.toByteArray();

        MiscUtils.closeQuietly(byteArrayInputStream, gzipInputStream, byteArrayOutputStream);

        return toReturn;
    }

    public static String compressBytesToString(byte[] unCompressed) throws IOException {

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final OutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);

        gzipOutputStream.write(unCompressed);

        MiscUtils.closeQuietly(gzipOutputStream);

        final String toReturn = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);

        MiscUtils.closeQuietly(byteArrayOutputStream);

        return toReturn;
    }
}