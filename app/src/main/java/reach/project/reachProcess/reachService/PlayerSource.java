package reach.project.reachProcess.reachService;

import android.util.Log;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;

/**
 * Created by dexter on 06/08/15.
 */
public final class PlayerSource implements Runnable, Closeable {

    public InputStream getSource() {
        return sourceStream;
    }

    private final AtomicBoolean kill = new AtomicBoolean(false);

    private final Player.DecoderHandler handler;
    private final InputStream sourceStream;
    private final WritableByteChannel sinkChannel;
    private final FileChannel sourceChannel;
    private final long contentLength;

    public PlayerSource(Player.DecoderHandler handler,
                        String path,
                        long contentLength) throws IOException {

        final PipedOutputStream pipedOutputStream = new PipedOutputStream();

        this.handler = handler;
        this.contentLength = contentLength;
        this.sourceChannel = new FileInputStream(path).getChannel();
        this.sourceStream = new PipedInputStream(pipedOutputStream, StaticData.PLAYER_BUFFER_DEFAULT);
        this.sinkChannel = Channels.newChannel(pipedOutputStream);
    }

    @Override
    public void close() {
        kill.set(true);
        MiscUtils.closeQuietly(sinkChannel, sourceStream, sourceChannel);
    }

    @Override
    public void run() {

        kill.set(false);

        short lastSecondaryProgress = 0;
        long transferred = 0, downloaded;

        while (!kill.get() && (downloaded = handler.getProcessed()) > 0 && sinkChannel.isOpen()) {

            if (transferred >= contentLength)
                break;
            if (transferred >= downloaded)
                try {
                    Thread.sleep(StaticData.LUCKY_DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }

            else {

                //estimate secondary progress
                final short estimatedProgress = (short) ((downloaded * 100) / contentLength);
                if (estimatedProgress > lastSecondaryProgress)
                    handler.updateSecondaryProgress(estimatedProgress);
                Log.i("Downloader", "Estimated transfer " + estimatedProgress);

                //perform transfer
                try {
                    transferred += sourceChannel.transferTo(transferred, downloaded - transferred, sinkChannel);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                //actual progress
                final short actualProgress = (short) ((transferred * 100) / contentLength);
                if (actualProgress > lastSecondaryProgress)
                    handler.updateSecondaryProgress(actualProgress);
                lastSecondaryProgress = actualProgress;
                Log.i("Downloader", "Transferred " + actualProgress);
            }
        }

        MiscUtils.closeQuietly(sinkChannel, sourceStream, sourceChannel);
        /////////////////////////
    }
}