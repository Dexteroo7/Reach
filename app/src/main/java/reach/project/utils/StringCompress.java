package reach.project.utils;

import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;

import com.squareup.wire.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public enum StringCompress {
    ;

    public static String compressBytesToString(byte[] unCompressed) throws IOException {

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
        final Base64OutputStream base64OutputStream = new Base64OutputStream(gzipOutputStream, Base64.DEFAULT);

        base64OutputStream.write(unCompressed);

        MiscUtils.closeQuietly(gzipOutputStream, base64OutputStream);

        final String toReturn = new String(byteArrayOutputStream.toByteArray(), "UTF-8");

        byteArrayOutputStream.close();

        return toReturn;
    }

    public static byte[] deCompressStringToBytes(String compressed) throws IOException {

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressed.getBytes("UTF-8"));
        final GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
        final Base64InputStream base64InputStream = new Base64InputStream(gzipInputStream, Base64.DEFAULT);

        final byte[] buffer = new byte[8192];
        int len;

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while ((len = byteArrayInputStream.read(buffer)) > 0)
            byteArrayOutputStream.write(buffer, 0, len);

        final byte [] toReturn = byteArrayOutputStream.toByteArray();

        MiscUtils.closeQuietly(byteArrayInputStream, base64InputStream, byteArrayInputStream, byteArrayOutputStream);

        return toReturn;
    }

    public static <T extends Message> String compressBytesToString(T unCompressed) throws IOException {

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final OutputStream base64OutputStream = new Base64OutputStream(byteArrayOutputStream, Base64.DEFAULT);
        final OutputStream outputStream = new DeflaterOutputStream(base64OutputStream);
        outputStream.write(unCompressed.toByteArray());

        MiscUtils.closeQuietly(outputStream, base64OutputStream);

        final String toReturn = new String(byteArrayOutputStream.toByteArray(), "UTF-8");

        byteArrayOutputStream.close();

        return toReturn;
    }
}