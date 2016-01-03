package reach.project.utils;

import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;

import com.squareup.wire.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public enum StringCompress {
    ;

    public static String compressBytesToString(byte[] unCompressed) throws IOException {

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final OutputStream base64OutputStream = new Base64OutputStream(byteArrayOutputStream, Base64.DEFAULT);
        final OutputStream outputStream = new DeflaterOutputStream(base64OutputStream);
        outputStream.write(unCompressed);

        MiscUtils.closeQuietly(outputStream, base64OutputStream);

        final String toReturn = new String(byteArrayOutputStream.toByteArray(), "US-ASCII");

        byteArrayOutputStream.close();

        return toReturn;
    }

    public static <T extends Message> String compressBytesToString(T unCompressed) throws IOException {

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final OutputStream base64OutputStream = new Base64OutputStream(byteArrayOutputStream, Base64.DEFAULT);
        final OutputStream outputStream = new DeflaterOutputStream(base64OutputStream);
        outputStream.write(unCompressed.toByteArray());

        MiscUtils.closeQuietly(outputStream, base64OutputStream);

        final String toReturn = new String(byteArrayOutputStream.toByteArray(), "US-ASCII");

        byteArrayOutputStream.close();

        return toReturn;
    }

    public static byte[] deCompressStringToBytes(String compressed) throws IOException {

        final byte[] bytes = Base64.decode(compressed, Base64.DEFAULT);

        final InputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        final InputStream base64InputStream = new Base64InputStream(byteArrayInputStream, Base64.DEFAULT);
        final InputStream inputStream = new InflaterInputStream(base64InputStream);

        final byte[] buffer = new byte[8192];
        int len;

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while ((len = inputStream.read(buffer)) > 0)
            outputStream.write(buffer, 0, len);

        final byte [] toReturn = outputStream.toByteArray();

        MiscUtils.closeQuietly(byteArrayInputStream, base64InputStream, inputStream, outputStream);

        return toReturn;
    }
}