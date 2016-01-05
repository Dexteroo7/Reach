package reach.project.reachProcess.reachService;

import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;

/**
 * Created by dexter on 06/08/15.
 */
final class PlayerSource implements Runnable, Closeable {

    public InputStream getSource() {
        return sourceStream;
    }

    private final AtomicBoolean kill = new AtomicBoolean(false);

    private final FileChannel fileChannel;
    private final RandomAccessFile randomAccessFile;

    private final WritableByteChannel sinkChannel;
    private final ReadableByteChannel sourceChannel;
    private final InputStream sourceStream;

    private final Player.DecoderHandler handler;
    private final long contentLength;

    public PlayerSource(Player.DecoderHandler handler,
                        String path,
                        long contentLength) throws IOException {

        this.handler = handler;
        this.contentLength = contentLength;

        //file to read from
        this.randomAccessFile = new RandomAccessFile(path, "r");
        this.fileChannel = randomAccessFile.getChannel();

        //pipe to control flow of bytes
        final Pipe pipe = Pipe.open();
        this.sinkChannel = (WritableByteChannel) pipe.sink().configureBlocking(false);
        this.sourceChannel = (ReadableByteChannel) pipe.source().configureBlocking(true);

        //get a stream from the pipe
        this.sourceStream = Channels.newInputStream(sourceChannel);
    }

    @Override
    public void close() {
        kill.set(true);
        MiscUtils.closeQuietly(fileChannel, randomAccessFile, sinkChannel, sourceChannel, sourceStream);
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

                //actual progress
                final short actualProgress = (short) ((downloaded * 100) / contentLength);
                if (actualProgress > lastSecondaryProgress)
                    handler.updateSecondaryProgress(actualProgress);
                lastSecondaryProgress = actualProgress;
                Log.i("Downloader", "Downloaded " + actualProgress);

                //perform transfer
                try {
                    transferred += fileChannel.transferTo(transferred, downloaded - transferred, sinkChannel);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

//                //actual progress
//                final short actualProgress = (short) ((transferred * 100) / contentLength);
//                if (actualProgress > lastSecondaryProgress)
//                    handler.updateSecondaryProgress(actualProgress);
//                lastSecondaryProgress = actualProgress;
//                Log.i("Downloader", "Transferred " + actualProgress);
            }
        }

        MiscUtils.closeQuietly(fileChannel, randomAccessFile, sinkChannel, sourceChannel, sourceStream);
        /////////////////////////
    }
}