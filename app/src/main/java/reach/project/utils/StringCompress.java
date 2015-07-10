package reach.project.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public enum StringCompress {;

    public static byte[] compress(String text) throws IOException {

        final ByteArrayOutputStream baos =
                new ByteArrayOutputStream();
        final OutputStream out = new DeflaterOutputStream(baos);
        out.write(text.getBytes("UTF8")); out.close();
        return baos.toByteArray();
    }

    public static String decompress(byte[] bytes) throws IOException {

        final InputStream in = new InflaterInputStream(new ByteArrayInputStream(bytes));
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final byte[] buffer = new byte[8192];
        int len;
        while((len = in.read(buffer))>0) baos.write(buffer, 0, len);
        return new String(baos.toByteArray(), "UTF8"); }
}