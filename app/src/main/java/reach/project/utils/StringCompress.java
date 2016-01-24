package reach.project.utils;

import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;

public enum StringCompress {
    ;

    /*public static String compressBytesToString(byte[] unCompressed) throws IOException {

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);

        gzipOutputStream.write(unCompressed);

        MiscUtils.closeQuietly(gzipOutputStream, gzipOutputStream);

        final String toReturn = new String(byteArrayOutputStream.toByteArray(), "UTF-8");

        byteArrayOutputStream.close();

        return toReturn;
    }

    public static byte[] deCompressStringToBytes(String compressed) throws IOException {

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressed.getBytes("UTF-8"));
        final GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);

        final byte[] buffer = new byte[8192];
        int len;

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while ((len = gzipInputStream.read(buffer)) > 0)
            byteArrayOutputStream.write(buffer, 0, len);

        final byte [] toReturn = byteArrayOutputStream.toByteArray();

        MiscUtils.closeQuietly(byteArrayInputStream, byteArrayInputStream, byteArrayOutputStream);

        return toReturn;
    }*/

    public static byte[] deCompressStringToBytes(String compressed) throws IOException {

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressed.getBytes("UTF-8"));
        final InputStream base64InputStream = new Base64InputStream(byteArrayInputStream, Base64.DEFAULT);
        final InputStream inputStream = new DeflaterInputStream(base64InputStream);

        final byte[] buffer = new byte[8192];
        int len;

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while ((len = inputStream.read(buffer)) > 0)
            byteArrayOutputStream.write(buffer, 0, len);

        final byte [] toReturn = byteArrayOutputStream.toByteArray();

        MiscUtils.closeQuietly(byteArrayInputStream, base64InputStream, inputStream, byteArrayOutputStream);

        return toReturn;
    }

    public static String compressBytesToString(byte[] unCompressed) throws IOException {

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final OutputStream base64OutputStream = new Base64OutputStream(byteArrayOutputStream, Base64.DEFAULT);
        final OutputStream outputStream = new DeflaterOutputStream(base64OutputStream);
        outputStream.write(unCompressed);

        MiscUtils.closeQuietly(outputStream, base64OutputStream);

        final String toReturn = new String(byteArrayOutputStream.toByteArray(), "UTF-8");

        MiscUtils.closeQuietly(byteArrayOutputStream);

        return toReturn;
    }
}