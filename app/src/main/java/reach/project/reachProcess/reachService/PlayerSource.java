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
    private final FileChannel source;
    private final long contentLength;

    public PlayerSource(Player.DecoderHandler handler,
                        String path,
                        long contentLength) throws IOException {

        final PipedOutputStream pipedOutputStream = new PipedOutputStream();

        this.handler = handler;
        this.contentLength = contentLength;
        this.source = new FileInputStream(path).getChannel();
        this.sourceStream = new PipedInputStream(pipedOutputStream);
        this.sinkChannel = Channels.newChannel(pipedOutputStream);
    }

    @Override
    public void close() {
        kill.set(true);
        MiscUtils.closeAndIgnore(sinkChannel, sourceStream, source);
    }

    @Override
    public void run() {

        kill.set(false);

        short lastSecondaryProgress = 0;
        long transferred = 0, count;

        while (!kill.get() && (count = handler.getProcessed()) > 0 && sinkChannel.isOpen()) {

            if (transferred >= contentLength)
                break;
            if (transferred >= count)
                try {
                    Thread.sleep(StaticData.LUCKY_DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    MiscUtils.closeAndIgnore(sinkChannel, sourceStream, source);
                    return;
                }

            else {
                try {
                    transferred += source.transferTo(transferred, count - transferred, sinkChannel);
                } catch (IOException e) {
                    e.printStackTrace();
                    MiscUtils.closeAndIgnore(sinkChannel, sourceStream, source);
                    return;
                }
                final short progress = (short) ((transferred * 100) / contentLength);
                if (progress > lastSecondaryProgress)
                    handler.updateSecondaryProgress(progress);
                lastSecondaryProgress = progress;
                Log.i("Downloader", "Transferred " + progress);
            }
        }
        /////////////////////////
    }
}