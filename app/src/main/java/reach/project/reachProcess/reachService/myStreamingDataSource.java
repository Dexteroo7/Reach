package reach.project.reachProcess.reachService;


import android.content.res.AssetManager;
import android.util.Log;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.upstream.DataSpec;
import com.google.android.exoplayer.upstream.UriDataSource;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.annotation.Documented;

/**
 * Created by adminReach on 24/12/15.
 */
public final class myStreamingDataSource implements UriDataSource
{
    private Player.DecoderHandler handlerInterface;
    public myStreamingDataSource(Player.DecoderHandler handlerInteface)
    {
        this.handlerInterface=handlerInteface;
    }

    private String uriString;
    private InputStream inputStream;
    private long bytesRemaining;
    private boolean opened;

    @Override
        public long open(DataSpec dataspec)
        {
            try
            {
                uriString = dataspec.uri.toString();
                String path = dataspec.uri.getPath();

                //inputStream = assetManager.open(path, AssetManager.ACCESS_RANDOM);
                FileInputStream fileInputStream=new FileInputStream(path);
                inputStream=fileInputStream;
                PlayerSource playerSource=new PlayerSource(handlerInterface,path);
                //inputStream=(playerSource.getSource());

                Log.i("Aman ","inputstream feeded");

                Log.i("path", path);
                Log.i("path2",uriString);

                long skipped = inputStream.skip(dataspec.position);
                Log.i("Amanskipped","value of skipped = "+skipped);

                if (skipped < dataspec.position) {
                    Log.i("Aman ","skipped<dataspec.position");
                    // assetManager.open() returns an AssetInputStream, whose skip() implementation only skips
                    // fewer bytes than requested if the skip is beyond the end of the asset's data.
                    throw new EOFException();
                }
                if (dataspec.length != C.LENGTH_UNBOUNDED) {
                    {
                        bytesRemaining = dataspec.length;
                        Log.i("Amanbytes ","dataspec.length!=C.LENGTH "+bytesRemaining);

                    }
                } else {
                    bytesRemaining = inputStream.available();
                    Log.i("Amanbyytes ","else "+bytesRemaining);

                    if (bytesRemaining == Integer.MAX_VALUE) {
                        // assetManager.open() returns an AssetInputStream, whose available() implementation
                        // returns Integer.MAX_VALUE if the remaining length is greater than (or equal to)
                        // Integer.MAX_VALUE. We don't know the true length in this case, so treat as unbounded.
                        bytesRemaining = C.LENGTH_UNBOUNDED;
                        Log.i("Amanbytes ","Last "+bytesRemaining);
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            opened = true;

            return bytesRemaining;
        }

    @Override
    public int read(byte[] buffer, int offset, int readLength)
    {
        if (bytesRemaining == 0) {
            return -1;
        } else {
            int bytesRead = 0;
            try {
                Log.i("Aman ","try entered");
                int bytesToRead = bytesRemaining == C.LENGTH_UNBOUNDED ? readLength
                        : (int) Math.min(bytesRemaining, readLength);
                bytesRead = inputStream.read(buffer, offset, bytesToRead);
                Log.i("Aman ","bytesread = "+bytesRead);
            }
            catch (IOException e)
            {
                Log.i("Aman ","excepto");
            }

            if (bytesRead > 0) {
                if (bytesRemaining != C.LENGTH_UNBOUNDED) {
                    bytesRemaining -= bytesRead;
                }

            }

            return bytesRead;
        }
    }

    @Override
    public String getUri() {
        return uriString;
    }

    @Override
    public void close()
    {
        uriString = null;
        if (inputStream != null) {
            try {
                inputStream.close();
            }
            catch (IOException e)
            {

            }
            finally
            {
                inputStream = null;
                if (opened) {
                    opened = false;

                }
            }
        }
    }
}
